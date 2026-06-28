import { ChangeDetectionStrategy, Component, output } from '@angular/core';

@Component({
  selector: 'app-new-deck-modal',
  template: `
    <div
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
      (click)="onBackdropClick($event)"
    >
      <div
        class="pk-panel w-full max-w-lg mx-4 animate-[pk-fade-in_0.2s_ease-out]"
      >
        <div class="flex flex-col gap-4">
          <h2 class="text-[var(--pk-accent)] text-[var(--pk-fz-lg)] text-center mb-2">
            Nuevo Mazo
          </h2>
          <p class="text-[var(--pk-text-dim)] text-center" style="font-size: var(--pk-fz-base);">
            Elegí cómo querés empezar
          </p>
          <div class="grid grid-cols-2 gap-3">
            <button
              class="pk-panel flex flex-col items-center justify-center gap-3 cursor-pointer hover:!border-[var(--pk-accent)] transition-colors"
              style="min-height: 140px; border-style: solid; background: var(--pk-surface); border-color: var(--pk-text-dim);"
              (click)="onPredefined()"
            >
              <svg viewBox="0 0 40 40" width="40" height="40" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="4" y="6" width="32" height="28" rx="3" stroke="#9b6dff" stroke-width="2" fill="none"/>
                <line x1="10" y1="14" x2="30" y2="14" stroke="#9b6dff" stroke-width="1.8" stroke-linecap="round"/>
                <line x1="10" y1="20" x2="30" y2="20" stroke="#9b6dff" stroke-width="1.8" stroke-linecap="round"/>
                <line x1="10" y1="26" x2="24" y2="26" stroke="#9b6dff" stroke-width="1.8" stroke-linecap="round"/>
                <text x="20" y="35" text-anchor="middle" fill="#9b6dff" font-size="4.5" font-family="Arial" font-weight="bold">MAZO</text>
              </svg>
              <span class="text-[var(--pk-accent)]" style="font-size: var(--pk-fz-lg);">PREDEFINIDO</span>
              <span class="text-[var(--pk-text-dim)]" style="font-size: var(--pk-fz-sm);">Mazo pre-armado listo para usar</span>
            </button>
            <button
              class="pk-panel flex flex-col items-center justify-center gap-3 cursor-pointer hover:!border-[var(--pk-accent)] transition-colors"
              style="min-height: 140px; border-style: solid; background: var(--pk-surface); border-color: var(--pk-text-dim);"
              (click)="onFromScratch()"
            >
              <svg viewBox="0 0 40 40" width="40" height="40" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="8" y="4" width="24" height="32" rx="2" stroke="#9b6dff" stroke-width="2" fill="none"/>
                <line x1="20" y1="12" x2="20" y2="28" stroke="#9b6dff" stroke-width="2" stroke-linecap="round"/>
                <line x1="12" y1="20" x2="28" y2="20" stroke="#9b6dff" stroke-width="2" stroke-linecap="round"/>
              </svg>
              <span class="text-[var(--pk-accent)]" style="font-size: var(--pk-fz-lg);">DESDE CERO</span>
              <span class="text-[var(--pk-text-dim)]" style="font-size: var(--pk-fz-sm);">Creá tu mazo desde cero</span>
            </button>
          </div>
          <button
            class="pk-btn pk-btn--ghost justify-center"
            (click)="close.emit()"
          >
            Cancelar
          </button>
        </div>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NewDeckModalComponent {
  readonly close = output<void>();
  readonly predefined = output<void>();
  readonly fromScratch = output<void>();

  protected onPredefined(): void {
    this.predefined.emit();
    this.close.emit();
  }

  protected onFromScratch(): void {
    this.fromScratch.emit();
    this.close.emit();
  }

  protected onBackdropClick(e: MouseEvent): void {
    if ((e.target as HTMLElement).classList.contains('fixed')) {
      this.close.emit();
    }
  }
}
