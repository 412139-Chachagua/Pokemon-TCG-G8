import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { CardImagePipe } from '../../../../shared/pipes/card-image.pipe';
import { PublicDiscardCardModel } from '../../../../shared/models/game-state.models';

@Component({
  selector: 'app-discard-pile',
  standalone: true,
  imports: [CardImagePipe],
  template: `
    <div class="relative group">
      <div
        class="flex flex-col items-center border-2 border-dashed border-[var(--pk-panel)] rounded-lg h-full bg-[var(--pk-surface)] overflow-hidden p-1 cursor-pointer"
        (click)="openViewer.emit()">
        <div class="flex-1 min-h-0 flex items-center justify-center w-full">
          @if (topCard(); as top) {
            <img [src]="top.cardId | cardImage" [alt]="'Descarte: ' + top.cardId" class="h-full w-auto max-w-full object-contain rounded" />
          } @else {
            <img [src]="cardBackUrl" alt="Descartes" class="h-full w-auto max-w-full object-contain opacity-70" />
          }
        </div>
        <div class="shrink-0 pb-0.5">
          <span class="text-[0.625rem] font-semibold text-[var(--pk-text-dim)] leading-tight">
            {{ discardCount() }}
          </span>
        </div>
      </div>
      <div class="absolute top-full left-1/2 -translate-x-1/2 mt-1 hidden group-hover:block bg-gray-900 border border-gray-700 rounded-lg px-4 py-3 z-50 shadow-xl pointer-events-none text-[0.7rem] text-gray-300 leading-relaxed whitespace-nowrap">
        Descarte ({{ discardCount() }} cartas)
      </div>
    </div>
  `,
  host: { style: 'display: block; height: 100%;' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DiscardPileComponent {
  readonly discardCount = input.required<number>();
  readonly discard = input<PublicDiscardCardModel[]>([]);
  readonly openViewer = output<void>();

  protected readonly cardBackUrl = 'assets/images/card-back.svg';

  readonly topCard = computed(() => {
    const cards = this.discard();
    if (!cards || cards.length === 0) return null;
    return cards[cards.length - 1];
  });
}
