import { inject, computed } from '@angular/core';
import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { DeckResponse } from '../../../../shared/models/deck.models';
import { DeckApiService } from '../../../../core/api/deck-api.service';

@Component({
  selector: 'app-deck-item',
  imports: [],
  template: `
    <div class="pk-panel flex items-center justify-between gap-4">
      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-2 min-w-0">
          <h3 class="truncate min-w-0 text-[var(--pk-text-bright)] font-semibold">{{ deck().name }}</h3>

        </div>
        <p class="mt-1 text-[var(--pk-text-dim)]" style="font-size: calc(var(--pk-fz-sm) * 1.1);">
          {{ composition().pokemon }} PC · {{ composition().trainer }} ENTRENADOR · {{ composition().energy }} ENERGÍA
        </p>
        <p class="text-[var(--pk-text-dim)]" style="font-size: var(--pk-fz-sm); margin-top: 0.15rem;">
          Creado el {{ formatDate(deck().createdAt) }}
        </p>
      </div>
      <div class="flex items-center gap-2 shrink-0">
        @if (confirmingDelete()) {
          <span class="text-[var(--pk-text-dim)]" style="font-size: var(--pk-fz-sm);">¿Eliminar?</span>
          <button
            class="pk-btn pk-btn--danger h-9"
            style="padding: 0.5rem 1rem; font-size: var(--pk-fz-sm);"
            (click)="onConfirmDelete()"
          >
            ELIMINAR
          </button>
          <button
            class="pk-btn pk-btn--ghost h-9"
            style="padding: 0.5rem 1rem; font-size: var(--pk-fz-sm);"
            (click)="confirmingDelete.set(false)"
          >
            Cancelar
          </button>
        } @else if (confirmingDownload()) {
          <span class="text-[var(--pk-text-dim)]" style="font-size: var(--pk-fz-sm);">¿Descargar mazo?</span>
          <button
            class="pk-btn pk-btn--primary h-9"
            style="padding: 0.5rem 1rem; font-size: var(--pk-fz-sm);"
            (click)="onConfirmDownload()"
          >
            DESCARGAR
          </button>
          <button
            class="pk-btn pk-btn--ghost h-9"
            style="padding: 0.5rem 1rem; font-size: var(--pk-fz-sm);"
            (click)="confirmingDownload.set(false)"
          >
            Cancelar
          </button>
        } @else {
          <button
            class="pk-btn h-9"
            style="padding: 0.5rem 0.7rem; font-size: var(--pk-fz-sm);"
            title="Descargar PDF"
            (click)="confirmingDownload.set(true)"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/>
              <line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
          </button>
          <button
            class="pk-btn h-9"
            style="padding: 0.5rem 1rem; font-size: var(--pk-fz-sm);"
            (click)="onEdit()"
          >
            EDITAR
          </button>
          @if (deck().valid) {
            <button
            class="pk-btn h-9"
            style="padding: 0.5rem 1rem; font-size: var(--pk-fz-sm);"
            (click)="onPlay()"
          >
            JUGAR
            </button>
          }
          <button
            class="pk-btn pk-btn--danger h-9"
            style="padding: 0.5rem 1rem; font-size: var(--pk-fz-sm);"
            (click)="confirmingDelete.set(true)"
          >
            ELIMINAR
          </button>
        }
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeckItemComponent {
  deck = input.required<DeckResponse>();
  readonly delete = output<string>();
  readonly play = output<string>();
  readonly edit = output<string>();

  protected readonly confirmingDelete = signal(false);
  protected readonly confirmingDownload = signal(false);

  protected readonly composition = computed(() => {
    const cards = this.deck().cards;
    const pokemon = cards.filter(c => c.supertype === 'POKEMON').reduce((sum, c) => sum + c.quantity, 0);
    const trainer = cards.filter(c => c.supertype === 'TRAINER').reduce((sum, c) => sum + c.quantity, 0);
    const energy = cards.filter(c => c.supertype === 'ENERGY').reduce((sum, c) => sum + c.quantity, 0);
    return { pokemon, trainer, energy };
  });

  private readonly deckApi = inject(DeckApiService);

  formatDate(iso: string | null | undefined): string {
    if (!iso) return '-';
    const d = new Date(iso);
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
  }

  onEdit(): void {
    this.edit.emit(this.deck().id);
  }

  onPlay(): void {
    this.play.emit(this.deck().id);
  }

  onConfirmDelete(): void {
    this.delete.emit(this.deck().id);
    this.confirmingDelete.set(false);
  }

  onConfirmDownload(): void {
    const id = this.deck().id;
    this.confirmingDownload.set(false);
    this.deckApi.exportDeckPdf(id).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${this.deck().name}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    });
  }
}
