# Exportación e Importación de Mazos en PDF

## Backend (Spring Boot)

### Descarga/Exportación (`PdfExportService`)

Servicio Spring (`@Service`) que genera un documento PDF con la lista de cartas de un mazo utilizando **Apache PDFBox**.

#### `export(DeckEntity deck) → byte[]`

1. **Resolución de nombres**: recorre las cartas del mazo (`DeckCardEntity`) y busca cada `cardId` en `CardJpaRepository` para obtener el nombre real de la carta. Almacena los nombres en un `Map<String, String>`.
2. **Generación del PDF**:
   - Crea un documento `PDDocument` con una página en tamaño A4 (`PDRectangle.A4`).
   - Escribe el **nombre del mazo** como título con fuente Helvetica Bold 16pt (`# NombreDelMazo`).
   - Por cada carta, escribe una línea con `{cantidad} {nombre}` (ej: `4 Pikachu`) en Helvetica 10pt.
   - Coordenada Y inicial: 720px; salto de línea: 14px.
   - Si la Y baja de 60px, se crea una **nueva página** automáticamente (soporte multi-página).
3. **Salida**: escribe el PDF a un `ByteArrayOutputStream` y retorna el arreglo de bytes.

#### Endpoint

`GET /api/decks/{deckId}/export` — el frontend consume este endpoint con `responseType: 'blob'` y dispara la descarga del archivo.

---

### Importación desde PDF (`PdfImportService`)

Servicio que extrae y parsea el contenido de un archivo PDF subido por el usuario, validando su estructura y extrayendo mazos.

#### `extractDecksFromPdf(MultipartFile file) → List<ImportDeckRequest>`

##### Validaciones de seguridad y formato

1. **Tamaño máximo**: 2MB (`MAX_FILE_SIZE = 2 * 1024 * 1024`).
2. **Magic bytes**: verifica que el archivo comience con `%PDF-` (5 bytes ASCII).
3. **Apertura segura**: usa `Loader.loadPDF(bytes)` de PDFBox.
4. **Protecciones rechazadas**:
   - PDFs con contraseña (`document.isEncrypted()`).
   - PDFs con formularios AcroForm (`catalog.getAcroForm() != null`).
   - PDFs con archivos embebidos (`catalog.getNames().getEmbeddedFiles() != null`).
5. **Cantidad de páginas**: máximo 3 páginas (`MAX_PAGES = 3`).

##### Extracción de texto

Usa `PDFTextStripper` de PDFBox para extraer el texto plano del PDF.

##### Parseo del contenido (`parseDeckContent`)

Divide el texto en líneas y procesa cada una según estas expresiones regulares, en orden de precedencia:

| Patrón | Expresión regular | Ejemplo | Acción |
|---|---|---|---|
| **Nombre de mazo** | `^#+\s+(.+)$` | `# Mi Mazo` | Inicia un nuevo mazo con ese nombre. Si había cartas acumuladas, las guarda como mazo anterior. |
| **CardId** (formato ID) | `^([a-z0-9]+-\d+):(\d+)$` | `xy1-10:4` | 4 copias de la carta con ID `xy1-10`. |
| **Legible con set** | `^(\d+)\s+(.+?)\s+([a-zA-Z0-9]+)\s+(\d+)$` | `4 Pikachu xy1 10` | 4 copias; construye cardId como `setCode-number`. |
| **Solo nombre** | `^(\d+)\s+(.+)$` | `4 Pikachu` | Busca el nombre en `CardJpaRepository.findByNameIgnoreCase()`. Si no existe, lanza `ValidationException`. |

##### Validaciones post-parseo

- **Cada mazo debe tener exactamente 60 cartas**: si el total no es 60, se lanza `ValidationException` indicando el nombre del mazo y la cantidad encontrada.
- **Al menos un mazo válido**: si no se encontraron cartas válidas, lanza `ValidationException`.

#### `ImportDeckRequest` (DTO)

```java
public record ImportDeckRequest(
    String name,
    List<CreateDeckRequest.DeckCardRequest> cards
) {}
```

Contiene el nombre del mazo y la lista de cartas (cada una con `cardId` y `quantity`).

#### Endpoint

`POST /api/decks/import?playerId={id}&format={pdf}` — recibe el archivo como `multipart/form-data`. El frontend construye un `FormData` con el archivo y lo envía.

---

## Frontend (Angular)

### Importación desde archivo (`ImportDeckModalComponent`)

#### Flujo de 3 pasos

| Paso | Vista | Descripción |
|---|---|---|
| **1. Formato** (`format`) | Tres botones: TXT, JSON, PDF | Usuario elige el formato del archivo a importar. |
| **2. Subida** (`upload`) | Panel dividido: ejemplo del formato + zona de drag & drop | Muestra un ejemplo del formato elegido y un área para arrastrar o hacer clic para seleccionar archivo. |
| **3. Confirmación** (`confirm`) | Nombre del archivo, formato detectado, cantidad de mazos | Usuario confirma la importación. |

#### Drag & Drop

- Eventos `dragover`, `dragleave`, `drop` para resaltar el área y capturar el archivo.
- También se puede hacer clic en el área para abrir el selector de archivos nativo (`fileInput.click()`).

#### Detección de formato

Si el archivo subido tiene una extensión diferente al formato seleccionado, el modal detecta automáticamente el formato real y muestra un mensaje informativo.

#### Importación

Al confirmar, se llama a:

```typescript
this.deckApi.importDecks(file, playerId, format).subscribe(...)
```

Esto construye un `FormData` con el archivo y lo envía como `POST` al endpoint `/api/decks/import?playerId=&format=`. La respuesta es una lista de `DeckResponse[]` con los mazos importados. Se muestra una notificación con la cantidad de mazos importados.

### Exportación a PDF

En `DeckItemComponent`, al hacer clic en el botón de descarga y confirmar:

```typescript
this.deckApi.exportDeckPdf(id).subscribe(blob => {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${this.deck().name}.pdf`;
    a.click();
    URL.revokeObjectURL(url);
});
```

Se obtiene el PDF como `Blob` desde `GET /api/decks/{id}/export` y se descarga usando un enlace temporal creado con `URL.createObjectURL()`.
