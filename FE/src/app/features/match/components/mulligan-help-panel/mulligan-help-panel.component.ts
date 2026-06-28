import { ChangeDetectionStrategy, Component, signal } from '@angular/core';

@Component({
  selector: 'app-mulligan-help-panel',
  standalone: true,
  template: `
    <div class="fixed right-0 top-0 bottom-0 z-[201]">
      <button
        class="absolute top-1/2 -translate-y-1/2 right-0 flex items-center gap-1 px-2 py-3 rounded-l-md bg-[var(--pk-surface)] border border-r-0 border-[var(--pk-btn-border)] text-[var(--pk-text-dim)] text-xs font-bold uppercase tracking-widest cursor-pointer hover:text-[var(--pk-text)] hover:bg-[var(--pk-panel)] transition-all [writing-mode:vertical-lr]"
        (click)="toggle()"
      >
        Mulligan
      </button>

      <div
        class="absolute inset-y-0 right-0 z-40 xl:w-[320px] w-[280px] max-w-[90vw] bg-[var(--pk-surface)] border border-[var(--pk-btn-border)] rounded-l-lg shadow-2xl transition-transform duration-300 ease-in-out flex flex-col overflow-hidden"
        [class.translate-x-0]="isOpen()"
        [class.translate-x-full]="!isOpen()"
      >
        <div class="flex items-center justify-between px-4 py-3 border-b border-[var(--pk-btn-border)] shrink-0">
          <h3 class="m-0 text-sm font-bold text-[var(--pk-text)]">REGLAS: MULLIGAN</h3>
          <button class="w-6 h-6 flex items-center justify-center rounded-full bg-transparent text-[var(--pk-text-dim)] text-sm cursor-pointer hover:bg-[var(--pk-panel)] hover:text-[var(--pk-text)] border-none leading-none" (click)="close()">✕</button>
        </div>
        <div class="px-5 py-4 space-y-5 text-sm text-center text-[var(--pk-text)] leading-6 overflow-y-auto">
          <div>
            <p class="m-0 font-bold text-blue-400 text-base mb-1">¿Qué es Mulligan?</p>
            <p class="m-0">
              Si al repartir las 7 cartas iniciales no tenés ningún Pokémon Básico en tu mano,
              debés mostrar esas cartas, barajarlas de nuevo en tu mazo y robar 7 cartas nuevas.
            </p>
          </div>

          <div class="border-t border-[var(--pk-btn-border)]/30"></div>

          <div>
            <p class="m-0 font-bold text-red-400 text-base mb-1">Consecuencia</p>
            <p class="m-0">
              Por cada Mulligan que hagas, tu oponente puede robar 1 carta extra de su mazo
              antes de comenzar la partida. Esto aplica también si ambos hacen Mulligan.
            </p>
          </div>

          <div class="border-t border-[var(--pk-btn-border)]/30"></div>

          <div>
            <p class="m-0 font-bold text-blue-400 text-base mb-1">Límite</p>
            <p class="m-0">
              No hay límite de Mulligans. Seguís repitiendo el proceso hasta tener al menos
              un Pokémon Básico en tu mano inicial.
            </p>
          </div>

          <div class="border-t border-[var(--pk-btn-border)]/30 pt-3">
            <p class="m-0 text-[0.625rem] text-[var(--pk-text-dim)] italic">
              Reglas oficiales del Juego de Cartas Coleccionables Pokémon.
            </p>
          </div>
        </div>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MulliganHelpPanelComponent {
  protected readonly isOpen = signal(false);

  protected toggle(): void {
    this.isOpen.update(v => !v);
  }

  protected close(): void {
    this.isOpen.set(false);
  }
}
