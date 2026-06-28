import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { DeckCardEntry } from '../../services/deck-builder-facade.service';

@Component({
  selector: 'app-deck-card-list',
  imports: [DragDropModule],
  template: `
    <div
      cdkDropList
      id="deck-list"
      [cdkDropListSortingDisabled]="true"
      [cdkDropListConnectedTo]="['search-grid']"
      class="flex flex-col gap-2 min-h-[100px]"
      (cdkDropListDropped)="onDrop($event)"
    >
      @for (entry of cards(); track entry.cardId) {
        <div class="flex items-center justify-between rounded-md border border-[var(--pk-btn-border)] bg-[var(--pk-surface)] px-3 py-2">
          <div class="flex-1">
            <p class="text-sm font-medium text-[var(--pk-text)]">{{ entry.name }}</p>
            <p class="text-xs text-[var(--pk-text-dim)]">{{ entry.supertype }}</p>
          </div>
          <div class="flex items-center gap-1">
            <button
              class="flex h-7 w-7 items-center justify-center rounded-md bg-[var(--pk-btn)] text-sm font-medium text-[var(--pk-text)] hover:bg-[var(--pk-btn-hover)] disabled:opacity-30"
              (click)="decrement.emit(entry.cardId)"
            >
              −
            </button>
            <span class="w-6 text-center text-sm font-semibold text-[var(--pk-text)]">
              {{ entry.quantity }}
            </span>
            <button
              class="flex h-7 w-7 items-center justify-center rounded-md bg-[var(--pk-accent)] text-sm font-medium text-white hover:bg-[var(--pk-accent-glow)] disabled:opacity-30"
              [disabled]="
                (!entry.isBasicEnergy && entry.supertype !== 'ENERGY' && entry.quantity >= 4)
              "
              (click)="increment.emit(entry.cardId)"
            >
              +
            </button>
          </div>
        </div>
      } @empty {
        <p class="py-8 text-center text-sm text-[var(--pk-text-dim)]">
          No hay cartas en el mazo
        </p>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeckCardListComponent {
  cards = input<DeckCardEntry[]>([]);
  readonly increment = output<string>();
  readonly decrement = output<string>();
  readonly cardDropped = output<{ cardId: string; name: string; supertype: string; subtypes: string[]; stage?: string }>();

  onDrop(event: CdkDragDrop<unknown>): void {
    this.cardDropped.emit(event.item.data as { cardId: string; name: string; supertype: string; subtypes: string[]; stage?: string });
  }
}
