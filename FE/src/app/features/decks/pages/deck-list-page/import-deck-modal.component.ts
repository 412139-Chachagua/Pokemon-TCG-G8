import { ChangeDetectionStrategy, Component, inject, output, signal } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { DeckApiService } from '../../../../core/api/deck-api.service';
import { AuthService } from '../../../../core/services/auth.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { DeckResponse } from '../../../../shared/models/deck.models';

type ImportFormat = 'txt' | 'json' | 'pdf';
type ModalStep = 'format' | 'upload' | 'confirm';

const FORMAT_EXAMPLES: Record<ImportFormat, string> = {
  txt: `# Nombre del Mazo
4 Pikachu
3 Charmander
2 Charmeleon

# Otro Mazo
1 Snorlax
2 Eevee`,
  json: `{
  "name": "Mi Mazo",
  "cards": [
    { "cardId": "xy1-10", "quantity": 4 },
    { "cardId": "xy1-11", "quantity": 3 }
  ]
}`,
  pdf: `# Mazo Fuego
4 Pikachu
3 Charmander
2 Charmeleon

# Mazo Agua
2 Squirtle
1 Lapras`,
};

const ACCEPT_MAP: Record<ImportFormat, string> = {
  txt: '.txt',
  json: '.json',
  pdf: '.pdf',
};

const EXTENSION_MAP: Record<string, ImportFormat> = {
  '.txt': 'txt',
  '.json': 'json',
  '.pdf': 'pdf',
};

@Component({
  selector: 'app-import-deck-modal',
  template: `
    <div
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
      (click)="onBackdropClick($event)"
    >
      <div
        class="pk-panel w-full max-w-3xl mx-4 animate-[pk-fade-in_0.2s_ease-out]"
        style="max-height: 85vh; overflow-y: auto;"
      >
        @if (step() === 'format') {
          <div class="flex flex-col gap-4">
            <h2 class="text-[var(--pk-accent)] text-[var(--pk-fz-lg)] text-center mb-2">
              Importar Mazos
            </h2>
            <p class="text-[var(--pk-text-dim)] text-center" style="font-size: var(--pk-fz-base);">
              Seleccioná el formato del archivo
            </p>
            <p class="text-[var(--pk-text-dim)] text-center" style="font-size: var(--pk-fz-sm);">
              Podés añadir múltiples mazos con un solo archivo
            </p>
            <div class="grid grid-cols-3 gap-3">
              @for (fmt of formats; track fmt) {
                <button
                  class="pk-panel flex flex-col items-center justify-center gap-3 cursor-pointer hover:!border-[var(--pk-accent)] transition-colors"
                  style="min-height: 110px; border-style: solid; background: var(--pk-surface); border-color: var(--pk-text-dim);"
                  (click)="selectFormat(fmt)"
                >
                  <span style="width: 2.5rem; height: 2.5rem;" [innerHTML]="formatSvgs[fmt]"></span>
                  <span class="text-[var(--pk-accent)]" style="font-size: var(--pk-fz-lg);">{{ formatLabel(fmt) }}</span>
                </button>
              }
            </div>
            <button
              class="pk-btn pk-btn--ghost justify-center"
              (click)="close.emit()"
            >
              Cancelar
            </button>
          </div>
        }

        @if (step() === 'upload') {
          <div class="flex flex-col gap-4">
            <div class="flex flex-col" style="gap: 0.15rem;">
              <div class="flex items-center gap-2">
                <button class="pk-btn pk-btn--ghost pk-btn--sm" (click)="step.set('format')">
                  ← Volver
                </button>
              </div>
              <h2 class="text-[var(--pk-accent)] text-[var(--pk-fz-lg)] text-center" style="margin: 0;">
                Importar {{ formatLabel(selectedFormat()!) }}
              </h2>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div class="pk-panel" style="padding: 0.75rem; border-color: var(--pk-text-dim);">
                <p class="pk-panel__header mb-2" style="font-size: var(--pk-fz-base);">
                  Formato {{ formatLabel(selectedFormat()!) }}
                </p>
                <pre class="text-[var(--pk-text-dim)]" style="font-size: var(--pk-fz-base); line-height: 1.8; white-space: pre-wrap; word-break: break-word;">{{ formatExample }}</pre>
              </div>

              <div
                class="pk-panel flex flex-col items-center justify-center text-center cursor-pointer"
                style="min-height: 220px; padding: 2rem; border-style: dashed; border-color: var(--pk-text-dim);"
                [class.!border-[var(--pk-accent)]!]="dragOver()"
                (dragover)="onDragOver($event)"
                (dragleave)="onDragLeave($event)"
                (drop)="onDrop($event)"
                (click)="fileInput.click()"
              >
                <p class="text-[var(--pk-text-dim)]" style="font-size: var(--pk-fz-lg);">
                  Arrastrá tu archivo<br>acá
                </p>
                <p class="text-[var(--pk-text-dim)] mt-3" style="font-size: var(--pk-fz-base);">
                  o hacé clic para buscar
                </p>
                <p class="text-[var(--pk-accent)] mt-3" style="font-size: var(--pk-fz-base);">
                  {{ ACCEPT_MAP[selectedFormat()!] }}
                </p>
              </div>
            </div>

            <input
              #fileInput
              type="file"
              [accept]="ACCEPT_MAP[selectedFormat()!]"
              hidden
              (change)="onFileSelected($event)"
            />

            <button
              class="pk-btn pk-btn--ghost justify-center"
              (click)="close.emit()"
            >
              Cancelar
            </button>
          </div>
        }

        @if (step() === 'confirm') {
          <div class="flex flex-col gap-4">
            <h2 class="text-[var(--pk-accent)] text-[var(--pk-fz-lg)] text-center">
              Importar Mazos
            </h2>

            <div class="pk-panel text-center">
              <p class="text-[var(--pk-text-bright)]" style="font-size: var(--pk-fz-base);">
                {{ selectedFile()!.name }}
              </p>
              @if (formatMessage()) {
                <p class="text-[var(--pk-accent)] mt-2" style="font-size: var(--pk-fz-sm);">
                  {{ formatMessage() }}
                </p>
              }
              <p class="text-[var(--pk-text-dim)] mt-2" style="font-size: var(--pk-fz-base);">
                {{ deckCount() }} archivo(s) detectado(s)
              </p>
            </div>

            <div class="flex gap-2 justify-center">
              <button
                class="pk-btn pk-btn--ghost"
                (click)="reset()"
              >
                Cancelar
              </button>
              <button
                class="pk-btn"
                [disabled]="importing()"
                (click)="onImport()"
              >
                @if (importing()) {
                  <span class="inline-block h-3 w-3 animate-spin rounded-full border-2 border-[var(--pk-btn-border)] border-t-[var(--pk-accent)]"></span>
                }
                Importar
              </button>
            </div>
          </div>
        }
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportDeckModalComponent {
  private readonly sanitizer = inject(DomSanitizer);
  private readonly deckApi = inject(DeckApiService);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);

  readonly close = output<void>();
  readonly imported = output<number>();

    protected readonly formats: ImportFormat[] = ['txt', 'json', 'pdf'];
  protected readonly ACCEPT_MAP = ACCEPT_MAP;

  protected readonly step = signal<ModalStep>('format');
  protected readonly selectedFormat = signal<ImportFormat | null>(null);
  protected readonly selectedFile = signal<File | null>(null);
  protected readonly dragOver = signal(false);
  protected readonly importing = signal(false);
  protected readonly deckCount = signal(0);
  protected readonly formatMessage = signal<string | null>(null);

  protected get formatExample(): string {
    return this.selectedFormat() ? FORMAT_EXAMPLES[this.selectedFormat()!] : '';
  }

  protected formatLabel(fmt: ImportFormat): string {
    return { txt: 'TXT', json: 'JSON', pdf: 'PDF' }[fmt];
  }

  protected readonly txtSvg = this.sanitizer.bypassSecurityTrustHtml(
    `<svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg" width="100%" height="100%">
      <rect x="4" y="2" width="32" height="36" rx="2" stroke="#9b6dff" stroke-width="2" fill="none"/>
      <line x1="10" y1="12" x2="30" y2="12" stroke="#9b6dff" stroke-width="1.8" stroke-linecap="round"/>
      <line x1="10" y1="18" x2="26" y2="18" stroke="#9b6dff" stroke-width="1.8" stroke-linecap="round"/>
      <line x1="10" y1="24" x2="28" y2="24" stroke="#9b6dff" stroke-width="1.8" stroke-linecap="round"/>
      <line x1="10" y1="30" x2="22" y2="30" stroke="#9b6dff" stroke-width="1.8" stroke-linecap="round"/>
      <text x="20" y="9" text-anchor="middle" fill="#9b6dff" font-size="4.5" font-family="Arial" font-weight="bold">TXT</text>
    </svg>`
  );
  protected readonly jsonSvg = this.sanitizer.bypassSecurityTrustHtml(
    `<svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg" width="100%" height="100%">
      <rect x="4" y="2" width="32" height="36" rx="2" stroke="#9b6dff" stroke-width="2" fill="none"/>
      <text x="20" y="23" text-anchor="middle" fill="#9b6dff" font-size="15" font-family="Arial" font-weight="bold">{ }</text>
    </svg>`
  );
  protected readonly pdfSvg = this.sanitizer.bypassSecurityTrustHtml(
    `<svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg" width="100%" height="100%">
      <rect x="4" y="2" width="32" height="36" rx="2" stroke="#9b6dff" stroke-width="2" fill="none"/>
      <line x1="10" y1="12" x2="30" y2="12" stroke="#9b6dff" stroke-width="1.8" stroke-linecap="round"/>
      <line x1="10" y1="18" x2="30" y2="18" stroke="#9b6dff" stroke-width="1.8" stroke-linecap="round"/>
      <line x1="10" y1="24" x2="30" y2="24" stroke="#9b6dff" stroke-width="1.8" stroke-linecap="round"/>
      <line x1="10" y1="30" x2="22" y2="30" stroke="#9b6dff" stroke-width="1.8" stroke-linecap="round"/>
      <circle cx="28" cy="28" r="6" stroke="#e74c3c" stroke-width="1.5" fill="none"/>
      <text x="28" y="31" text-anchor="middle" fill="#e74c3c" font-size="9" font-family="Arial" font-weight="bold">P</text>
    </svg>`
  );

  protected readonly formatSvgs: Record<ImportFormat, SafeHtml> = {
    txt: this.txtSvg,
    json: this.jsonSvg,
    pdf: this.pdfSvg,
  };

  protected selectFormat(fmt: ImportFormat): void {
    this.selectedFormat.set(fmt);
    this.step.set('upload');
  }

  protected onDragOver(e: DragEvent): void {
    e.preventDefault();
    this.dragOver.set(true);
  }

  protected onDragLeave(e: DragEvent): void {
    e.preventDefault();
    this.dragOver.set(false);
  }

  protected onDrop(e: DragEvent): void {
    e.preventDefault();
    this.dragOver.set(false);
    const file = e.dataTransfer?.files?.[0];
    if (file) this.setFile(file);
  }

  protected onFileSelected(e: Event): void {
    const input = e.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.setFile(file);
  }

  private detectFormat(file: File): ImportFormat | null {
    const ext = '.' + file.name.split('.').pop()?.toLowerCase();
    return EXTENSION_MAP[ext] ?? null;
  }

  private setFile(file: File): void {
    this.selectedFile.set(file);
    const detected = this.detectFormat(file);
    const current = this.selectedFormat();
    if (detected && current && detected !== current) {
      this.selectedFormat.set(detected);
      this.formatMessage.set(
        `Este archivo es de formato ${this.formatLabel(detected)}. Cambiando formato a ${this.formatLabel(detected)}.`
      );
    } else {
      this.formatMessage.set(null);
    }
    this.deckCount.set(1);
    this.step.set('confirm');
  }

  protected async onImport(): Promise<void> {
    const file = this.selectedFile();
    const format = this.selectedFormat();
    if (!file || !format) return;

    this.importing.set(true);
    const playerId = this.authService.playerId() || 'player-dev';
    this.deckApi.importDecks(file, playerId, format).subscribe({
      next: (decks) => {
        this.importing.set(false);
        this.notificationService.show(
          `${decks.length} mazo(s) importado(s) correctamente`,
          'success'
        );
        this.imported.emit(decks.length);
        this.close.emit();
      },
      error: () => {
        this.importing.set(false);
        this.notificationService.show('Error al importar mazos', 'error');
      },
    });
  }

  protected onBackdropClick(e: MouseEvent): void {
    if ((e.target as HTMLElement).classList.contains('fixed')) {
      this.close.emit();
    }
  }

  protected reset(): void {
    this.step.set('format');
    this.selectedFormat.set(null);
    this.selectedFile.set(null);
    this.deckCount.set(0);
    this.formatMessage.set(null);
    this.close.emit();
  }
}
