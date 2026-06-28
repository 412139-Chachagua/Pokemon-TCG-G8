import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { DeckResponse } from '../../../../shared/models/deck.models';
import { DeckItemComponent } from './deck-item.component';

@Component({
  selector: 'app-deck-list',
  imports: [DeckItemComponent],
  template: `
    @if (loading()) {
      <div class="flex justify-center py-12">
        <div class="h-8 w-8 animate-spin rounded-full border-4 border-[var(--pk-btn-border)] border-t-[var(--pk-accent)]"></div>
      </div>
    } @else if (error()) {
      <div class="pk-panel text-center">
        <p class="text-[var(--pk-error)]">{{ error() }}</p>
        <button
          class="pk-btn pk-btn--danger mt-3"
          (click)="retry.emit()"
        >
          Reintentar
        </button>
      </div>
    } @else if (empty()) {
      <div class="pk-panel text-center">
        <p class="text-[var(--pk-text-dim)]">No hay mazos disponibles.</p>
      </div>
    } @else {
      <div class="flex flex-col gap-3">
        @for (deck of decks(); track deck.id) {
          <app-deck-item
            [deck]="deck"
            (delete)="delete.emit($event)"
            (play)="play.emit($event)"
            (edit)="edit.emit($event)"
          />
        }
      </div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeckListComponent {
  decks = input<DeckResponse[]>([]);
  loading = input(false);
  error = input<string | null>(null);
  empty = input(false);

  readonly delete = output<string>();
  readonly play = output<string>();
  readonly edit = output<string>();
  readonly retry = output<void>();
}
