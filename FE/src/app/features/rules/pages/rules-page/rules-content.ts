export interface SubSection {
  id: string;
  title: string;
}

export interface GuideSection {
  id: string;
  title: string;
  content: string;
  subsections: SubSection[];
}

export const SECTIONS: GuideSection[] = [
  {
    id: 'conviértete-en-maestro-pokémon',
    title: '¡Conviértete en Maestro Pokémon!',
    subsections: [],
    content: `<div align="center">
  <h2><strong>¡Conviértete en Maestro Pokémon!</strong></h2>
</div>

<p>¡Eres un Entrenador Pokémon! Tu misión es recorrer el mundo enfrentándote a otros Entrenadores con tus Pokémon, criaturas que adoran luchar y que tienen poderes increíbles.</p>

<p>En estas reglas encontrarás todo lo que necesitas saber para jugar a JCC Pokémon. Tu baraja de cartas representa tanto a tus Pokémon como a los objetos y aliados que pueden ayudarte en tus aventuras.</p>

<p>Los juegos de cartas coleccionables se basan en el uso de estrategias y permiten que cada jugador personalice su juego. Una buena forma de aprender es usando las barajas temáticas, ya que te permitirán descubrir los distintos estilos de juego. Cuando estés listo, podrás crear tu propia baraja de 60 cartas, jugar con tus amigos e intercambiar cartas para conseguir los Pokémon más fuertes.</p>

<hr>`,
  },
  {
    id: 'conceptos-básicos-de-jcc-pokémon',
    title: 'Conceptos básicos de JCC Pokémon',
    subsections: [
      { id: 'cómo-ganar', title: 'Cómo ganar' },
      { id: 'tipos-de-energía', title: 'Tipos de Energía' },
    ],
    content: `<div align="center">
  <h2><strong>Conceptos básicos de JCC Pokémon</strong></h2>
</div>

<div align="center">
  <h3 id="cómo-ganar"><strong>Cómo ganar</strong></h3>
</div>

<p>En JCC Pokémon, tus Pokémon luchan contra los Pokémon de tu rival. Hay tres formas de ganar una partida:</p>

<ol>
  <li>Consiguiendo todas tus cartas de Premio.</li>
  <li>Dejando Fuera de Combate a todos los Pokémon en juego de tu rival.</li>
  <li>Si tu rival no puede robar ninguna carta al comenzar su turno.</li>
</ol>

<hr>

<div align="center">
  <h3 id="tipos-de-energía"><strong>Tipos de Energía</strong></h3>
</div>

<p>Los Pokémon atacan y usan habilidades para dejar Fuera de Combate a los rivales. Para poder usar ataques, los Pokémon necesitan cartas de Energía. JCC Pokémon tiene 11 tipos de Energía, y en el juego encontrarás Pokémon de cada uno de ellos.</p>

<div class="table-scroll">
<table>
  <thead>
    <tr><th>Tipo</th><th>Características</th></tr>
  </thead>
  <tbody>
    <tr><td><strong>Planta</strong></td><td>Suelen tener ataques con los que pueden curarse a sí mismos o envenenar a sus rivales.</td></tr>
    <tr><td><strong>Fuego</strong></td><td>¡Tienen grandes ataques! Pueden quemar a sus rivales, pero sus ataques necesitan tiempo para recargarse.</td></tr>
    <tr><td><strong>Agua</strong></td><td>Pueden manipular la Energía y mover los Pokémon del equipo rival.</td></tr>
    <tr><td><strong>Rayo</strong></td><td>Pueden recuperar Energía usada de la pila de descartes y paralizar a sus rivales.</td></tr>
    <tr><td><strong>Psíquica</strong></td><td>¡Dominan los poderes especiales! Sus rivales suelen acabar Dormidos, Confundidos o Envenenados.</td></tr>
    <tr><td><strong>Lucha</strong></td><td>Pueden correr riesgos mayores para hacer más daño, y algunos combinan ataques lanzando monedas.</td></tr>
    <tr><td><strong>Oscura</strong></td><td>Poseen ataques sigilosos que suelen hacer que los rivales descarten cartas.</td></tr>
    <tr><td><strong>Metálica</strong></td><td>Resisten los ataques durante más tiempo que casi todos los demás Pokémon.</td></tr>
    <tr><td><strong>Hada</strong></td><td>Tienen trucos para reducir la efectividad de los ataques del equipo rival.</td></tr>
    <tr><td><strong>Dragón</strong></td><td>Tienen ataques muy fuertes, pero normalmente requieren <strong>2 tipos de Energía</strong> para poder usarlos.</td></tr>
    <tr><td><strong>Incolora</strong></td><td>Poseen muchos movimientos diferentes y funcionan con cualquier tipo de baraja.</td></tr>
  </tbody>
</table>
</div>

<hr>`,
  },
  {
    id: 'partes-de-una-carta-pokémon',
    title: 'Partes de una carta Pokémon',
    subsections: [
      { id: 'anatomía-de-una-carta-pokémon', title: 'Anatomía de una carta Pokémon' },
      { id: 'anatomía-de-una-carta-de-entrenador', title: 'Anatomía de una carta de Entrenador' },
      { id: 'los-3-tipos-de-cartas', title: 'Los 3 tipos de cartas' },
    ],
    content: `<div align="center">
  <h2><strong>Partes de una carta Pokémon</strong></h2>
</div>

<div align="center">
  <h3 id="anatomía-de-una-carta-pokémon"><strong>Anatomía de una carta Pokémon</strong></h3>
</div>

<p>En una carta Pokémon (ej. Chesnaught Fase 2) podés identificar las siguientes partes:</p>

<div class="table-scroll">
<table>
  <thead>
    <tr><th>Elemento</th><th>Ubicación</th></tr>
  </thead>
  <tbody>
    <tr><td><strong>Tipo de Pokémon</strong></td><td>Icono en la esquina superior derecha</td></tr>
    <tr><td><strong>PS (Puntos de Salud)</strong></td><td>Número junto al tipo</td></tr>
    <tr><td><strong>Nombre de la carta</strong></td><td>Nombre del Pokémon</td></tr>
    <tr><td><strong>Fase</strong></td><td>Indica si es Básico, Fase 1 o Fase 2</td></tr>
    <tr><td><strong>Pokémon del que evoluciona</strong></td><td>Aparece si es carta de evolución</td></tr>
    <tr><td><strong>Habilidad</strong> <em>(si tiene)</em></td><td>Recuadro especial con nombre y efecto</td></tr>
    <tr><td><strong>Ataques</strong></td><td>Nombre, coste de Energía y daño de cada ataque</td></tr>
    <tr><td><strong>Debilidad / Resistencia / Coste de Retirada</strong></td><td>Parte inferior izquierda</td></tr>
    <tr><td><strong>Número de carta coleccionable</strong></td><td>Abajo al centro</td></tr>
    <tr><td><strong>Símbolo de la expansión</strong></td><td>Abajo a la derecha</td></tr>
  </tbody>
</table>
</div>

<div align="center">
  <h3 id="anatomía-de-una-carta-de-entrenador"><strong>Anatomía de una carta de Entrenador</strong></h3>
</div>

<p>En una carta de Entrenador (ej. Profesor Ciprés – Partidario) podés identificar:</p>

<div class="table-scroll">
<table>
  <thead>
    <tr><th>Elemento</th><th>Ubicación</th></tr>
  </thead>
  <tbody>
    <tr><td><strong>Nombre de la carta</strong></td><td>Parte superior</td></tr>
    <tr><td><strong>Tipo de carta</strong></td><td>Indica que es ENTRENADOR</td></tr>
    <tr><td><strong>Subtipo de Entrenador</strong></td><td>PARTIDARIO, OBJETO o ESTADIO</td></tr>
    <tr><td><strong>Cuadro de texto</strong></td><td>Efecto de la carta</td></tr>
    <tr><td><strong>Regla de Entrenador</strong></td><td>Restricción de uso, en la parte inferior</td></tr>
  </tbody>
</table>
</div>

<hr>

<div align="center">
  <h3 id="los-3-tipos-de-cartas"><strong>Los 3 tipos de cartas</strong></h3>
</div>

<p>En JCC Pokémon encontrarás 3 tipos de cartas diferentes:</p>

<div align="center">
  <h4><strong>Cartas de Pokémon</strong></h4>
</div>

<p>¡Estas son las cartas más importantes! La mayoría son Pokémon Básicos, Pokémon de Fase 1 o Pokémon de Fase 2. Los Pokémon de Fase 1 y Fase 2 también se denominan cartas de Evolución. En la esquina superior izquierda de cada carta verás la Fase del Pokémon y, en su caso, el Pokémon del que evoluciona.</p>

<blockquote>
  <p><strong>Cadena de evolución:</strong> Los Pokémon evolucionan en línea: Básico → Fase 1 → Fase 2 (ej. Froakie → Frogadier → Greninja). Cada carta se coloca encima de la anterior al evolucionar.</p>
</blockquote>

<div align="center">
  <h4><strong>Cartas de Energía</strong></h4>
</div>

<p>En la mayoría de los casos, un Pokémon no puede atacar sin cartas de Energía. El símbolo del coste del ataque debe coincidir con el de la carta de Energía. Si el símbolo es ★ (incoloro), se puede usar cualquier tipo de Energía.</p>

<div align="center">
  <h4><strong>Cartas de Entrenador</strong></h4>
</div>

<p>Las cartas de Entrenador representan los Objetos, Partidarios y Estadios que puede utilizar un Entrenador durante el combate. En la esquina superior derecha de cada carta verás el subtipo, y en la parte inferior las reglas especiales para ese subtipo.</p>

<hr>`,
  },
  {
    id: 'zonas-de-jcc-pokémon',
    title: 'Zonas de JCC Pokémon',
    subsections: [
      { id: 'pokémon-activo', title: 'Pokémon Activo' },
      { id: 'banca', title: 'Banca' },
      { id: 'cartas-de-premio', title: 'Cartas de Premio' },
      { id: 'pila-de-descartes', title: 'Pila de Descartes' },
    ],
    content: `<div align="center">
  <h2><strong>Zonas de JCC Pokémon</strong></h2>
</div>

<p>El tablero es simétrico: cada jugador tiene su propia sección con las siguientes zonas:</p>

<div class="table-scroll">
<table>
  <thead>
    <tr><th>Zona</th><th>Descripción</th></tr>
  </thead>
  <tbody>
    <tr><td><strong>Pokémon Activo</strong></td><td>Centro superior del área. El único Pokémon que puede atacar. Solo puede haber 1.</td></tr>
    <tr><td><strong>Banca</strong></td><td>Fila inferior. Hasta <strong>5 Pokémon</strong> al mismo tiempo.</td></tr>
    <tr><td><strong>Mano</strong></td><td>Cartas ocultas del jugador. Se empieza con 7.</td></tr>
    <tr><td><strong>Baraja</strong></td><td>Mazo de <strong>60 cartas</strong> boca abajo. Nadie puede mirar ni reordenarlas sin permiso de una carta.</td></tr>
    <tr><td><strong>Cartas de Premio</strong></td><td><strong>6 cartas</strong> boca abajo separadas al inicio. Al dejar Fuera de Combate un Pokémon rival, cogés 1.</td></tr>
    <tr><td><strong>Pila de Descartes</strong></td><td>Cartas eliminadas del juego, siempre boca arriba. Cualquiera puede mirarlas en cualquier momento.</td></tr>
  </tbody>
</table>
</div>

<div align="center">
  <h3 id="pokémon-activo"><strong>Pokémon Activo</strong></h3>
</div>

<p>Cada jugador empieza con 1 Pokémon Activo y debe tener 1 en todo momento. Si tu rival no tiene más Pokémon en juego, ¡ganás la partida!</p>

<div align="center">
  <h3 id="banca"><strong>Banca</strong></h3>
</div>

<p>Los Pokémon en Banca se colocan en la parte inferior del área. Cada jugador puede tener hasta 5 Pokémon en Banca al mismo tiempo. Los Pokémon en Banca no aplican Debilidad ni Resistencia al recibir daño.</p>

<div align="center">
  <h3 id="cartas-de-premio"><strong>Cartas de Premio</strong></h3>
</div>

<p>Son 6 cartas separadas boca abajo al inicio, elegidas al azar. Al dejar Fuera de Combate un Pokémon rival, cogés 1 carta de Premio y la ponés en tu mano. Si sos el primero en robar tu última carta de Premio, ¡ganás!</p>

<div align="center">
  <h3 id="pila-de-descartes"><strong>Pila de Descartes</strong></h3>
</div>

<p>Las cartas eliminadas del juego van aquí, salvo que una carta diga lo contrario. Cuando un Pokémon queda Fuera de Combate, él y todas las cartas unidas a él van a la pila de descartes de su propietario. Están siempre boca arriba y cualquiera puede mirarlas.</p>

<hr>`,
  },
  {
    id: 'cómo-jugar',
    title: 'Cómo jugar',
    subsections: [
      { id: 'cómo-iniciar-una-partida', title: 'Cómo iniciar una partida' },
      { id: 'partes-de-un-turno', title: 'Partes de un turno' },
      { id: 'acciones-en-cada-turno', title: 'Acciones en cada turno' },
      { id: 'el-ataque', title: 'El ataque' },
      { id: 'paso-entre-turno-y-turno', title: 'Paso entre turno y turno' },
      { id: 'condiciones-especiales', title: 'Condiciones Especiales' },
    ],
    content: `<div align="center">
  <h2><strong>Cómo jugar</strong></h2>
</div>

<div align="center">
  <h3 id="cómo-iniciar-una-partida"><strong>Cómo iniciar una partida</strong></h3>
</div>

<ol>
  <li>Dale la mano a tu rival.</li>
  <li>Lanzad 1 moneda. El ganador decide quién empieza.</li>
  <li>Barajá todas tus cartas y robá las 7 primeras.</li>
  <li>Comprobá si tenés algún Pokémon Básico en tu mano.</li>
  <li>Poné 1 Pokémon Básico boca abajo como tu Pokémon Activo.</li>
  <li>Colocá hasta 5 Pokémon Básicos más boca abajo en tu Banca.</li>
  <li>Separé las 6 primeras cartas de tu baraja como cartas de Premio.</li>
  <li>Ambos jugadores dan la vuelta a sus Pokémon. ¡Empieza la partida!</li>
</ol>

<blockquote>
  <p><strong>Sin Pokémon Básico:</strong> Si no tenés ningún Pokémon Básico en la mano inicial, mostrá tu mano al rival, barajá y robá 7 cartas nuevamente. Cada vez que esto ocurra, tu rival puede robar <strong>1 carta extra</strong>.</p>
</blockquote>

<hr>

<div align="center">
  <h3 id="partes-de-un-turno"><strong>Partes de un turno</strong></h3>
</div>

<p>Cada turno tiene 3 partes principales:</p>

<ol>
  <li>Robá 1 carta.</li>
  <li>Realizá cualquiera de estas acciones, en el orden que quieras:</li>
</ol>

<ul>
  <li><strong>A.</strong> Colocá cartas de Pokémon Básico de tu mano en tu Banca (todas las que quieras).</li>
  <li><strong>B.</strong> Hacé evolucionar a tus Pokémon (todos los que quieras).</li>
  <li><strong>C.</strong> Uní <strong>1 carta de Energía</strong> a 1 de tus Pokémon <em>(solo una vez por turno)</em>.</li>
  <li><strong>D.</strong> Jugá cartas de Entrenador <em>(1 Partidario y 1 Estadio máximo por turno)</em>.</li>
  <li><strong>E.</strong> Retirá a tu Pokémon Activo <em>(solo una vez por turno)</em>.</li>
  <li><strong>F.</strong> Usá habilidades (todas las que quieras).</li>
</ul>

<ol start="3">
  <li>Atacá. Después, tu turno termina.</li>
</ol>

<blockquote>
  <p><strong>Primer turno:</strong> El jugador que empieza <strong>se salta el ataque</strong> en su primer turno.</p>
</blockquote>

<hr>

<div align="center">
  <h3 id="acciones-en-cada-turno"><strong>Acciones en cada turno</strong></h3>
</div>

<div align="center">
  <h4><strong>A — Colocar Pokémon Básicos en la Banca</strong></h4>
</div>

<p>Elegí 1 carta de Pokémon Básico de tu mano y colocala boca arriba en tu Banca. Tu Banca admite un máximo de 5 Pokémon, por lo que solo podrás hacerlo si tenés 4 Pokémon o menos en Banca.</p>

<div align="center">
  <h4><strong>B — Hacer evolucionar Pokémon</strong></h4>
</div>

<p>Si tenés en tu mano una carta donde pone "Evoluciona de X" y X es el nombre de un Pokémon que tenías en juego al comienzo de tu turno, podés colocarla sobre ese Pokémon.</p>

<p>El Pokémon evolucionado conserva todos sus contadores de daño y las cartas que tenía unidas, pero pierde las Condiciones Especiales (Dormido, Confundido, etc.) y no puede usar los ataques ni habilidades de su forma anterior.</p>

<blockquote>
  <p><strong>Restricciones de evolución:</strong></p>
  <ul>
    <li>No podés evolucionar a un Pokémon en el <strong>primer turno en que entró en juego</strong>.</li>
    <li>No podés evolucionar a un Pokémon en el <strong>primer turno de la partida</strong>.</li>
    <li>Un Pokémon que acaba de evolucionar <strong>no puede volver a evolucionar en el mismo turno</strong>.</li>
    <li>Podés evolucionar tanto al Pokémon Activo como a los de la Banca.</li>
  </ul>
</blockquote>

<div align="center">
  <h4><strong>C — Unir 1 carta de Energía <em>(una vez por turno)</em></strong></h4>
</div>

<p>Cogé 1 carta de Energía de tu mano y colocala bajo tu Pokémon Activo o bajo 1 de tus Pokémon en Banca. ¡Solo podés unir Energía una vez por turno!</p>

<div align="center">
  <h4><strong>D — Jugar cartas de Entrenador</strong></h4>
</div>

<div class="table-scroll">
<table>
  <thead>
    <tr><th>Subtipo</th><th>Regla</th></tr>
  </thead>
  <tbody>
    <tr><td><strong>Objeto</strong></td><td>Podés jugar todas las que quieras. Seguí las instrucciones y descartala.</td></tr>
    <tr><td><strong>Partidario</strong></td><td>Se juega como un Objeto, pero solo <strong>1 por turno</strong>.</td></tr>
    <tr><td><strong>Estadio</strong></td><td>Solo <strong>1 por turno</strong>. Se mantiene en juego hasta que otra carta la elimina.</td></tr>
  </tbody>
</table>
</div>

<div align="center">
  <h4><strong>E — Retirar al Pokémon Activo <em>(una vez por turno)</em></strong></h4>
</div>

<p>Para retirar a un Pokémon, descartá cartas de Energía equivalentes a su Coste de Retirada (indicado con ★ en la carta). Si no aparece ningún ★, la retirada es gratis.</p>

<p>Al cambiar los Pokémon, ambos conservan sus contadores de daño y cartas unidas. Las Condiciones Especiales y efectos de ataque desaparecen al pasar a la Banca.</p>

<blockquote>
  <p><strong>¡Importante!</strong> Los Pokémon <strong>Dormidos o Paralizados no se pueden retirar</strong>.</p>
</blockquote>

<div align="center">
  <h4><strong>F — Usar habilidades</strong></h4>
</div>

<p>Algunos Pokémon tienen habilidades especiales. Muchas se pueden usar antes de atacar. Podés usar habilidades tanto del Pokémon Activo como de los de la Banca.</p>

<blockquote>
  <p>Las habilidades <strong>no son ataques</strong>, así que podés seguir atacando en el mismo turno aunque uses una habilidad.</p>
</blockquote>

<hr>

<div align="center">
  <h3 id="el-ataque"><strong>El ataque</strong></h3>
</div>

<p>Cuando estés listo para atacar, asegurate de haber hecho todo lo que querías en el paso 2. Una vez que ataques, tu turno termina y no podrás volver atrás.</p>

<p>El ataque se realiza en tres pasos:</p>

<p><strong>A — Comprobá la Energía unida a tu Pokémon Activo</strong></p>

<p>Para atacar, tu Pokémon debe tener unida la cantidad de Energía adecuada. El símbolo ★ en el coste indica que se acepta cualquier tipo de Energía. Un coste vacío (0) significa que el ataque no necesita Energía.</p>

<p><strong>B — Comprobá la Debilidad y la Resistencia del Pokémon Activo rival</strong></p>

<p>Los Pokémon pueden tener Debilidad o Resistencia ante ciertos tipos, indicadas en la esquina inferior izquierda.</p>

<ul>
  <li><strong>Debilidad:</strong> el Pokémon rival recibe <strong>más daño</strong> si tiene Debilidad ante el tipo atacante.</li>
  <li><strong>Resistencia:</strong> el Pokémon rival recibe <strong>menos daño</strong> si tiene Resistencia ante el tipo atacante.</li>
</ul>

<blockquote>
  <p>¡La Debilidad y la Resistencia <strong>no se aplican a los Pokémon en Banca</strong>!</p>
</blockquote>

<p><strong>C — Colocá contadores de daño</strong></p>

<p>Colocá 1 contador de daño por cada 10 puntos de daño que haga el ataque. Un Pokémon queda Fuera de Combate cuando su daño total es igual o superior a sus PS. En ese caso:</p>

<ul>
  <li>El Pokémon y todas las cartas unidas van a la pila de descartes.</li>
  <li>El rival coge 1 carta de Premio.</li>
  <li>El jugador cuyo Pokémon quedó Fuera de Combate elige un nuevo Pokémon Activo de su Banca.</li>
  <li>Si no puede hacerlo porque tiene la Banca vacía, vos ganás la partida.</li>
</ul>

<hr>

<div align="center">
  <h3 id="paso-entre-turno-y-turno"><strong>Paso entre turno y turno</strong></h3>
</div>

<p>Antes de que empiece el siguiente turno, se deben aplicar las Condiciones Especiales en este orden:</p>

<ol>
  <li>Envenenado</li>
  <li>Quemado</li>
  <li>Dormido</li>
  <li>Paralizado</li>
</ol>

<p>Luego se aplican los efectos de habilidades u otras cartas que ocurran entre turnos. Finalmente, se comprueba si algún Pokémon afectado quedó Fuera de Combate.</p>

<hr>

<div align="center">
  <h3 id="condiciones-especiales"><strong>Condiciones Especiales</strong></h3>
</div>

<p>Las Condiciones Especiales solo las puede sufrir el Pokémon Activo. Al pasar a la Banca o al evolucionar, se eliminan.</p>

<div class="table-scroll">
<table>
  <thead>
    <tr><th>Condición</th><th>Indicador</th><th>Efecto</th></tr>
  </thead>
  <tbody>
    <tr><td><strong>Dormido</strong></td><td>Girar la carta en sentido antihorario</td><td>No puede atacar ni retirarse. Entre turnos: lanzá 1 moneda — <strong>cara</strong> = se despierta, <strong>cruz</strong> = sigue Dormido.</td></tr>
    <tr><td><strong>Quemado</strong></td><td>Marcador de Quemado</td><td>Entre turnos: lanzá 1 moneda — <strong>cruz</strong> = recibe <strong>2 contadores de daño</strong>. Solo puede haber 1 marcador a la vez.</td></tr>
    <tr><td><strong>Confundido</strong></td><td>Girar la carta con la cabeza hacia vos</td><td>Al atacar: lanzá 1 moneda — <strong>cara</strong> = el ataque funciona con normalidad; <strong>cruz</strong> = recibe <strong>3 contadores de daño</strong> y el ataque falla.</td></tr>
    <tr><td><strong>Paralizado</strong></td><td>Girar la carta en sentido horario</td><td>No puede atacar ni retirarse. Se elimina al <strong>final del turno siguiente</strong> del jugador afectado.</td></tr>
    <tr><td><strong>Envenenado</strong></td><td>Marcador de Envenenado</td><td>Entre turnos: recibe <strong>1 contador de daño</strong>. Solo puede haber 1 marcador a la vez.</td></tr>
  </tbody>
</table>
</div>

<blockquote>
  <p><strong>Combinaciones posibles:</strong> Como Dormido, Confundido y Paralizado requieren girar la carta, <strong>solo puede estar activa la última de estas tres condiciones</strong> aplicada. En cambio, <strong>Quemado y Envenenado</strong> usan marcadores y pueden coexistir con cualquier otra condición. Por tanto, un Pokémon con mala suerte podría estar <strong>Quemado, Paralizado y Envenenado al mismo tiempo</strong>.</p>
</blockquote>

<div align="center">
  <h4><strong>Cómo eliminar Condiciones Especiales</strong></h4>
</div>

<p>Las Condiciones Especiales desaparecen cuando el Pokémon:</p>

<ul>
  <li><strong>Se retira</strong> a la Banca.</li>
  <li><strong>Evoluciona</strong>.</li>
</ul>

<p>Las únicas condiciones que impiden retirarse son Dormido y Paralizado.</p>

<hr>`,
  },
  {
    id: 'ligas-pokémon',
    title: 'Ligas Pokémon',
    subsections: [],
    content: `<div align="center">
  <h2><strong>Ligas Pokémon</strong></h2>
</div>

<p>En las Ligas Pokémon podés:</p>

<ul>
  <li>¡Aprender a jugar y mejorar tus estrategias!</li>
  <li>¡Ganar magníficos premios!</li>
  <li>¡Luchar contra otros jugadores y hacer amigos!</li>
</ul>

<p>Preguntá en tu tienda habitual o buscá una Liga cerca en <a href="http://support-es.pokemon.com/">support-es.pokemon.com</a>. Para más información, visitá <a href="http://www.pokemon.es/">www.pokemon.es</a>.</p>

<hr>`,
  },
  {
    id: 'reglas-avanzadas',
    title: 'Reglas avanzadas',
    subsections: [
      { id: 'mulligan', title: 'Mulligan' },
      { id: 'qué-se-considera-un-ataque', title: '¿Qué se considera un ataque?' },
      { id: 'todos-los-detalles-del-ataque', title: 'Todos los detalles del ataque' },
      { id: 'robar-más-cartas-de-las-disponibles', title: 'Robar más cartas de las disponibles' },
      { id: 'ambos-jugadores-ganan-al-mismo-tiempo', title: 'Ambos jugadores ganan al mismo tiempo' },
      { id: 'qué-forma-parte-del-nombre-de-un-pokémon', title: '¿Qué forma parte del nombre de un Pokémon?' },
    ],
    content: `<div align="center">
  <h2><strong>Reglas avanzadas</strong></h2>
</div>

<div align="center">
  <h3 id="mulligan"><strong>Mulligan</strong></h3>
</div>

<p>Si un jugador no tiene ningún Pokémon Básico en su mano inicial, declara un mulligan.</p>

<p>Si ambos jugadores no tienen Pokémon Básico: ambos muestran su mano y empiezan de nuevo.</p>

<p>Si solo uno no tiene Pokémon Básico:</p>

<ol>
  <li>Ese jugador espera a que el rival coloque su Pokémon Activo, su Banca y sus cartas de Premio.</li>
  <li>El jugador sin Pokémon Básico muestra su mano, la baraja y roba 7 cartas nuevamente. Repite hasta tener al menos 1 Pokémon Básico.</li>
  <li>El jugador que no tuvo que repetir puede robar 1 carta por cada mulligan declarado por el rival.</li>
  <li>Ambos dan la vuelta a sus Pokémon. ¡Empieza la partida!</li>
</ol>

<hr>

<div align="center">
  <h3 id="qué-se-considera-un-ataque"><strong>Qué se considera un ataque</strong></h3>
</div>

<p>Cada ataque tiene un coste, un nombre y puede incluir daño y texto de efecto. Todo lo demás son habilidades, no ataques.</p>

<blockquote>
  <p><strong>Ejemplo:</strong> <em>Feromoción</em> de Illumise no causa daño, pero sigue siendo un ataque. En cambio, <em>Mano Siniestra</em> de Dusknoir mueve contadores de daño, pero es una habilidad, no un ataque.</p>
</blockquote>

<hr>

<div align="center">
  <h3 id="todos-los-detalles-del-ataque"><strong>Todos los detalles del ataque</strong></h3>
</div>

<p>En ataques complejos, seguí estos pasos en orden:</p>

<ul>
  <li><strong>A.</strong> Elegí el ataque y asegurate de tener la Energía correcta. Anunciá el ataque.</li>
  <li><strong>B.</strong> Si tu Pokémon está Confundido, comprobá si el ataque falla.</li>
  <li><strong>C.</strong> Realizá todas las selecciones que requiera el ataque (ej. elegir un Pokémon objetivo).</li>
  <li><strong>D.</strong> Ejecutá todo lo indicado en el texto del ataque (ej. lanzar monedas).</li>
  <li><strong>E.</strong> Aplicá efectos que puedan modificar o cancelar el ataque (ej. efectos del turno anterior).</li>
  <li><strong>F.</strong> Aplicá efectos anteriores al daño → colocá contadores de daño → aplicá los efectos restantes.</li>
</ul>

<p>Cálculo del daño — pasos en orden:</p>

<ol>
  <li>Empezá con el daño básico indicado a la derecha del ataque.</li>
  <li>Aplicá efectos de daño sobre tu propio Pokémon Activo (cartas de Entrenador, etc.). Si el daño base es 0, no hace falta continuar.</li>
  <li>Aumentá el daño según la Debilidad del Pokémon rival (si aplica).</li>
  <li>Restá la Resistencia del Pokémon rival (si aplica).</li>
  <li>Aplicá efectos de Entrenadores, Energías u otros efectos sobre el Pokémon rival.</li>
  <li>Por cada 10 puntos de daño final, colocá 1 contador de daño. Si el resultado es 0 o menos, no se colocan contadores.</li>
</ol>

<hr>

<div align="center">
  <h3 id="robar-más-cartas-de-las-disponibles"><strong>Robar más cartas de las disponibles</strong></h3>
</div>

<p>Si una carta te indica robar más cartas de las que quedan en tu baraja, robá las que te queden y seguí jugando normalmente.</p>

<p>Si al comienzo de tu turno no podés robar 1 carta, perdés la partida. Esto no aplica si el efecto de una carta te pide robar y no podés.</p>

<hr>

<div align="center">
  <h3 id="ambos-jugadores-ganan-al-mismo-tiempo"><strong>Ambos jugadores ganan al mismo tiempo</strong></h3>
</div>

<p>Si ambos jugadores cumplen una condición de victoria simultáneamente, se juega una partida de Muerte Súbita.</p>

<blockquote>
  <p><strong>Excepción:</strong> Si vos ganás en dos condiciones y tu rival solo en una, <strong>vos ganás</strong>.</p>
</blockquote>

<div align="center">
  <h4><strong>Partida de Muerte Súbita</strong></h4>
</div>

<p>Es una nueva partida idéntica a la normal, pero cada jugador tiene 1 única carta de Premio en lugar de 6. Si también termina en empate, se sigue jugando Muerte Súbita hasta que alguien gane.</p>

<hr>

<div align="center">
  <h3 id="qué-forma-parte-del-nombre-de-un-pokémon"><strong>Qué forma parte del nombre de un Pokémon</strong></h3>
</div>

<div class="table-scroll">
<table>
  <thead>
    <tr><th>Elemento</th><th>¿Parte del nombre?</th><th>Ejemplo</th></tr>
  </thead>
  <tbody>
    <tr><td><strong>Nivel</strong></td><td>❌ No</td><td>Gengar, Gengar Nv. 43 y Gengar Nv. X tienen el <strong>mismo</strong> nombre</td></tr>
    <tr><td><strong>Símbolos al final</strong></td><td>✅ Sí</td><td>Alakazam y Alakazam ☆ son nombres <strong>distintos</strong></td></tr>
    <tr><td><strong>δ (especie Delta)</strong></td><td>❌ No</td><td>Excepción a la regla de símbolos</td></tr>
    <tr><td><strong>Nombre del propietario</strong></td><td>✅ Sí</td><td>Geodude y Geodude de Brock son nombres <strong>distintos</strong></td></tr>
  </tbody>
</table>
</div>

<p>Solo se pueden tener 4 copias de una carta con el mismo nombre (excepto Energías Básicas). Al evolucionar, el nombre después de "Evoluciona de" debe coincidir exactamente.</p>

<hr>`,
  },
  {
    id: 'creación-de-barajas',
    title: 'Creación de barajas',
    subsections: [
      { id: 'reglas-obligatorias', title: 'Reglas obligatorias' },
      { id: 'indicaciones-para-principiantes', title: 'Indicaciones para principiantes' },
    ],
    content: `<div align="center">
  <h2><strong>Creación de barajas</strong></h2>
</div>

<div align="center">
  <h3 id="reglas-obligatorias"><strong>Reglas obligatorias</strong></h3>
</div>

<ul>
  <li>La baraja debe tener exactamente 60 cartas.</li>
  <li>Solo se pueden incluir 4 cartas con el mismo nombre (excepto Energía Básica, sin límite).</li>
  <li>La baraja debe contener al menos 1 carta de Pokémon Básico.</li>
  <li>Solo se puede incluir 1 carta de AS Táctico en toda la baraja.</li>
</ul>

<div align="center">
  <h3 id="indicaciones-para-principiantes"><strong>Indicaciones para principiantes</strong></h3>
</div>

<div class="table-scroll">
<table>
  <thead>
    <tr><th>Componente</th><th>Cantidad recomendada</th></tr>
  </thead>
  <tbody>
    <tr><td><strong>Tipos de Energía</strong></td><td>1 o 2 tipos máximo</td></tr>
    <tr><td><strong>Cartas de Energía</strong></td><td>Entre 18 y 22</td></tr>
    <tr><td><strong>Cartas de Entrenador</strong></td><td>Entre 13 y 20</td></tr>
    <tr><td><strong>Copias de tu Pokémon favorito</strong></td><td>4 copias (y 4 de cada eslabón de su evolución)</td></tr>
  </tbody>
</table>
</div>

<hr>`,
  },
  {
    id: 'apéndice-a-pokémon-ex',
    title: 'Apéndice A: Pokémon-EX',
    subsections: [
      { id: 'reglas-especiales', title: 'Reglas especiales' },
    ],
    content: `<div align="center">
  <h2><strong>Apéndice A: Pokémon-EX</strong></h2>
</div>

<p>Los Pokémon-EX son Pokémon más poderosos, con más PS y ataques más fuertes. Sin embargo, tienen un riesgo importante.</p>

<div align="center">
  <h3 id="reglas-especiales"><strong>Reglas especiales</strong></h3>
</div>

<ul>
  <li><strong>EX es parte del nombre.</strong> Kyurem Negro y Kyurem Negro-EX tienen nombres distintos. Podés tener hasta 4 de cada uno.</li>
  <li>Cuando un Pokémon-EX queda Fuera de Combate, el rival coge 2 cartas de Premio en lugar de 1.</li>
  <li>En todo lo demás, se juegan igual que cualquier otro Pokémon.</li>
</ul>

<hr>`,
  },
  {
    id: 'apéndice-b-pokémon-megaevolución',
    title: 'Apéndice B: Pokémon Megaevolución',
    subsections: [
      { id: 'reglas-especiales', title: 'Reglas especiales' },
    ],
    content: `<div align="center">
  <h2><strong>Apéndice B: Pokémon Megaevolución</strong></h2>
</div>

<p>Los Pokémon Megaevolución convierten a un Pokémon-EX en algo aún más poderoso. Son una fase llamada MEGA y cuentan como cartas de Evolución.</p>

<div align="center">
  <h3 id="reglas-especiales"><strong>Reglas especiales</strong></h3>
</div>

<ul>
  <li><strong>M es parte del nombre.</strong> Mega-Venusaur-EX y Venusaur-EX tienen nombres distintos. Podés tener hasta 4 de cada uno.</li>
  <li>Cuando uno queda Fuera de Combate, el rival coge 2 cartas de Premio.</li>
  <li>Cuando un Pokémon se convierte en Megaevolución, tu turno termina inmediatamente.</li>
  <li>Un Pokémon Mega evoluciona de su forma EX, no de su forma normal (ej. Mega-Venusaur-EX evoluciona de Venusaur-EX, no de Venusaur).</li>
</ul>

<hr>`,
  },
  {
    id: 'apéndice-c-pokémon-recreados',
    title: 'Apéndice C: Pokémon Recreados',
    subsections: [
      { id: 'reglas-importantes', title: 'Reglas importantes' },
    ],
    content: `<div align="center">
  <h2><strong>Apéndice C: Pokémon Recreados</strong></h2>
</div>

<p>Los Pokémon Fósil tienen su propia Fase: Pokémon Recreado. La única forma de ponerlos en Banca es jugando primero la carta de Objeto correspondiente (ej. Fósil Pluma para Archen). Una vez en juego, funcionan como cualquier Pokémon de Fase 1.</p>

<div align="center">
  <h3 id="reglas-importantes"><strong>Reglas importantes</strong></h3>
</div>

<ul>
  <li>No podés colocar un Pokémon Recreado en tu Banca directamente desde la mano.</li>
  <li><strong>No son Pokémon Básicos:</strong> no podés jugarlos como tu Pokémon Activo o en Banca durante la preparación. Tu baraja igualmente necesita al menos 1 Pokémon Básico.</li>
  <li>Las cartas o efectos que mencionan "Básico", "Fase 1", "Fase 2" o "Evolución" no afectan a los Pokémon Recreados, salvo que digan "Pokémon" en general.</li>
  <li>Las reglas de Evolución normales aplican: para hacer evolucionar a un Pokémon Recreado, el nombre en "Evoluciona de" debe coincidir exactamente.</li>
</ul>

<hr>`,
  },
  {
    id: 'apéndice-d-cartas-de-entrenador-de-as-táctico',
    title: 'Apéndice D: Cartas de Entrenador de AS TÁCTICO',
    subsections: [],
    content: `<div align="center">
  <h2><strong>Apéndice D: Cartas de Entrenador de AS TÁCTICO</strong></h2>
</div>

<p>Las cartas de AS TÁCTICO son tan poderosas que solo podés tener 1 única carta de AS TÁCTICO en toda tu baraja, sin importar cuál sea. Estudiá todas las opciones para decidir cuál te conviene más.</p>

<hr>`,
  },
  {
    id: 'apéndice-e-cartas-del-equipo-plasma',
    title: 'Apéndice E: Cartas del Equipo Plasma',
    subsections: [
      { id: 'reglas-importantes', title: 'Reglas importantes' },
    ],
    content: `<div align="center">
  <h2><strong>Apéndice E: Cartas del Equipo Plasma</strong></h2>
</div>

<p>Las cartas del Equipo Plasma (Pokémon, Entrenadores y Energía) se identifican por su borde azul y el escudo del Equipo Plasma en la zona del texto.</p>

<div align="center">
  <h3 id="reglas-importantes"><strong>Reglas importantes</strong></h3>
</div>

<ul>
  <li>En las cartas de Pokémon, "Equipo Plasma" no se considera parte del nombre. Por ello, si tenés 4 Liepard del Equipo Plasma en tu baraja, ya no podés incluir ninguna otra carta de Liepard.</li>
  <li>Los Pokémon del Equipo Plasma evolucionan de la manera habitual.</li>
</ul>

<hr>`,
  },
  {
    id: 'glosario',
    title: 'Glosario',
    subsections: [],
    content: `<div align="center">
  <h2><strong>Glosario</strong></h2>
</div>

<p><strong>ATAQUE:</strong> 1) Cuando tu Pokémon Activo lucha con el Pokémon de tu rival. 2) El texto en una carta de Pokémon que indica qué hace cuando ataca.</p>
<hr>
<p><strong>BANCA:</strong> El lugar donde se sitúan los Pokémon en juego que no están luchando activamente. Los Pokémon en Banca no aplican Debilidad ni Resistencia al recibir daño.</p>
<hr>
<p><strong>CONDICIONES ESPECIALES:</strong> Los estados <strong>Dormido</strong>, <strong>Quemado</strong>, <strong>Confundido</strong>, <strong>Paralizado</strong> y <strong>Envenenado</strong>.</p>
<hr>
<p><strong>CONTADOR DE DAÑO:</strong> Contador que se coloca sobre un Pokémon para indicar el daño recibido. Se mantiene aunque el Pokémon se traslade a la Banca o evolucione.</p>
<hr>
<p><strong>DAÑO:</strong> Lo que ocurre cuando un Pokémon ataca a otro. Si el daño total de un Pokémon es igual o superior a sus PS, queda Fuera de Combate.</p>
<hr>
<p><strong>DEBILIDAD:</strong> Un Pokémon con Debilidad recibirá más daño cuando sea atacado por un tipo de Pokémon determinado.</p>
<hr>
<p><strong>EN JUEGO:</strong> Se considera que las cartas están en juego cuando están sobre la mesa.</p>
<hr>
<p><strong>ENERGÍA BÁSICA, CARTA DE:</strong> Una carta de Energía Planta, Fuego, Agua, Rayo, Psíquica, Lucha, Oscura, Metálica, Dragón, Hada o Incolora.</p>
<hr>
<p><strong>ENTRENADOR, CARTA DE:</strong> Cartas especiales que se juegan para obtener ventajas. Incluye <strong>Objetos</strong>, <strong>Estadios</strong> y <strong>Partidarios</strong>.</p>
<hr>
<p><strong>ENTRENADOR DE AS TÁCTICO, CARTA DE:</strong> Una carta de Entrenador muy poderosa. Solo podés tener <strong>1</strong> en toda tu baraja.</p>
<hr>
<p><strong>ESTADIO, CARTA DE:</strong> Tipo de carta de Entrenador que se mantiene en juego. No puede haber más de 1 activo a la vez. Solo se puede jugar 1 por turno.</p>
<hr>
<p><strong>EVOLUCIÓN, CARTA DE:</strong> Carta que se juega encima de un Pokémon Básico (o de otra carta de Evolución) para hacerlo más fuerte.</p>
<hr>
<p><strong>FUERA DE COMBATE:</strong> Un Pokémon queda Fuera de Combate si recibe un daño total igual o superior a sus PS. Ese Pokémon y todas las cartas unidas van a la pila de descartes. El rival coge <strong>1 carta de Premio</strong> (o <strong>2</strong> si era un Pokémon-EX).</p>
<hr>
<p><strong>HABILIDAD:</strong> Un efecto en un Pokémon que no es un ataque. Algunas permanecen activas todo el tiempo; otras hay que activarlas.</p>
<hr>
<p><strong>HERRAMIENTA POKÉMON:</strong> Tipo especial de carta de Entrenador (un Objeto) que se puede unir a un Pokémon. No se puede unir más de 1 Herramienta a un Pokémon al mismo tiempo.</p>
<hr>
<p><strong>INVOLUCIONAR:</strong> Hacer que un Pokémon evolucionado vuelva a su forma anterior. Pierde sus Condiciones Especiales y cualquier otro efecto.</p>
<hr>
<p><strong>MÁQUINA TÉCNICA:</strong> Tipo de carta de Entrenador (un Objeto) que se une a un Pokémon para que use el ataque de la Máquina Técnica como si fuera propio.</p>
<hr>
<p><strong>MUERTE SÚBITA:</strong> Partida especial que se juega cuando ambos jugadores ganan al mismo tiempo, con <strong>1 única carta de Premio</strong> por jugador en lugar de 6.</p>
<hr>
<p><strong>OBJETO, CARTA DE:</strong> Tipo de carta de Entrenador. Seguí las instrucciones y descartala.</p>
<hr>
<p><strong>PARTIDARIO, CARTA DE:</strong> Tipo de carta de Entrenador. Solo se puede jugar <strong>1 por turno</strong>.</p>
<hr>
<p><strong>PASO ENTRE TURNO Y TURNO:</strong> Período en que el juego pasa de un jugador a otro. Se comprueban las Condiciones Especiales y los Pokémon Fuera de Combate.</p>
<hr>
<p><strong>PILA DE DESCARTES:</strong> Las cartas descartadas. Siempre boca arriba; cualquiera puede mirarlas en cualquier momento.</p>
<hr>
<p><strong>POKÉMON ACTIVO:</strong> Tu Pokémon en juego que no está en la Banca. Es el único que puede atacar.</p>
<hr>
<p><strong>POKÉMON-EX:</strong> Forma más fuerte de Pokémon. Cuando queda Fuera de Combate, el rival coge <strong>2 cartas de Premio</strong>.</p>
<hr>
<p><strong>POKÉMON MEGAEVOLUCIÓN:</strong> Tipo de Pokémon-EX muy poderoso. Cuando uno de tus Pokémon se convierte en Megaevolución, <strong>tu turno termina</strong>.</p>
<hr>
<p><strong>PREMIO, CARTAS DE:</strong> Las <strong>6 cartas</strong> colocadas boca abajo al inicio. Cada vez que un Pokémon rival quede Fuera de Combate, cogés 1. ¡Al robar la última, ganás!</p>
<hr>
<p><strong>PS (PUNTOS DE SALUD):</strong> Indica la cantidad de daño que puede recibir un Pokémon antes de quedar Fuera de Combate.</p>
<hr>
<p><strong>RESISTENCIA:</strong> Un Pokémon con Resistencia recibirá menos daño cuando sea atacado por un tipo determinado.</p>
<hr>
<p><strong>RETIRADA:</strong> Cambiar el Pokémon Activo por uno de la Banca, descartando Energía equivalente al <strong>Coste de Retirada</strong>. Solo se puede retirar <strong>1 Pokémon por turno</strong>.</p>
<hr>
<p><strong>UNIR:</strong> Coger una carta de la mano y ponerla sobre uno de los Pokémon en juego.</p>
<hr>
<p><strong>ZONA PERDIDA:</strong> Las cartas que van a la Zona Perdida <strong>no pueden usarse</strong> durante el resto de la partida.</p>

<hr>`,
  },
];
