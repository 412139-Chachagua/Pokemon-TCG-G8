import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal, OnInit, OnDestroy } from '@angular/core';
import { CardImagePipe } from '../../../../shared/pipes/card-image.pipe';
import { CardRepositoryService } from '../../../../core/services/card-repository.service';
import { PrivateHandCardModel } from '../../../../shared/models/game-state.models';

interface DeckCardVm {
  instanceId: string;
  cardId: string;
  deckIndex: number;
  name: string;
  supertype: string;
}

@Component({
  selector: 'app-deck-viewer',
  standalone: true,
  imports: [CardImagePipe],
  template: `
    <div
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
      role="dialog"
      aria-modal="true"
      aria-label="Seleccionar carta del mazo"
      (click)="close.emit()">
      <div
        class="bg-[var(--pk-surface)] rounded-xl p-4 max-w-4xl w-full max-h-[80vh] flex flex-col shadow-2xl"
        (click)="$event.stopPropagation()">
        <div class="flex justify-between items-center mb-3">
          <h3 class="text-lg font-bold text-slate-100">{{ title() }}</h3>
        </div>
        <div class="overflow-y-auto flex-1 pr-1">
          @if (viewModels().length > 0) {
            <div class="grid grid-cols-4 sm:grid-cols-5 md:grid-cols-6 lg:grid-cols-7 gap-3">
              @for (vm of viewModels(); track vm.instanceId ?? vm.cardId + $index) {
                <div class="flex flex-col items-center">
                  <img
                    [src]="vm.cardId | cardImage"
                    [alt]="vm.name"
                    loading="lazy"
                    (error)="onImageError($event)"
                    [class]="cardImageClass(vm)"
                    (click)="onCardClick(vm)" />
                  <span class="text-[0.65rem] text-center text-slate-300 mt-1 truncate w-full">
                    {{ vm.name }}
                  </span>
                </div>
              }
            </div>
          } @else {
            <div class="flex flex-col items-center justify-center h-48 text-slate-400">
              <p class="text-sm">No hay cartas disponibles</p>
            </div>
          }
        </div>
        <div class="flex justify-between items-center mt-3">
          @if (selectionMode() === 'multi') {
            <span class="text-sm text-slate-400">{{ selectedIndexes().length }} / {{ maxSelections() }} seleccionadas</span>
          }
          <div class="flex gap-2 ml-auto">
            <button
              class="px-4 py-2 rounded bg-slate-600 hover:bg-slate-500 text-white text-sm cursor-pointer border-none"
              (click)="close.emit()">
              Cancelar
            </button>
            @if (selectionMode() === 'multi' && selectedIndexes().length > 0) {
              <button
                class="px-4 py-2 rounded bg-green-600 hover:bg-green-500 text-white text-sm cursor-pointer border-none"
                (click)="confirmMulti()">
                Confirmar
              </button>
            }
          </div>
        </div>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeckViewerComponent implements OnInit, OnDestroy {
  private readonly cardRepo = inject(CardRepositoryService);

  readonly deck = input<PrivateHandCardModel[]>([]);
  readonly title = input('Seleccionar carta');
  readonly selectionMode = input<'single' | 'multi'>('single');
  readonly maxSelections = input(2);
  readonly allowedSupertype = input<string | null>(null);

  readonly close = output<void>();
  readonly cardSelected = output<{ cardIndex: number; cardId: string; instanceId: string }>();
  readonly cardsSelected = output<{ cardIndexes: number[]; cardIds: string[] }>();

  protected readonly selectedIndexes = signal<number[]>([]);
  protected readonly cardBackUrl = 'assets/images/card-back.svg';

  private readonly onKeyDown = (event: KeyboardEvent) => {
    if (event.key === 'Escape') {
      this.close.emit();
    }
  };

  readonly viewModels = computed<DeckCardVm[]>(() => {
    const deck = this.deck() ?? [];
    return deck.map((card, i) => {
      const cached = this.cardRepo.getFromCache(card.cardId);
      return {
        instanceId: card.instanceId,
        cardId: card.cardId,
        deckIndex: i,
        name: cached?.name ?? card.name,
        supertype: cached?.supertype ?? card.supertype,
      };
    });
  });

  ngOnInit(): void {
    document.addEventListener('keydown', this.onKeyDown);
    document.body.style.overflow = 'hidden';
  }

  ngOnDestroy(): void {
    document.removeEventListener('keydown', this.onKeyDown);
    document.body.style.overflow = '';
  }

  protected cardImageClass(vm: DeckCardVm): string {
    const allowed = this.allowedSupertype();
    const isAllowed = !allowed || vm.supertype === allowed;
    if (!isAllowed) {
      return 'w-full aspect-[3/4] rounded-md object-cover bg-[var(--pk-panel)] opacity-40 cursor-not-allowed';
    }
    if (this.selectionMode() === 'multi') {
      const isSelected = this.selectedIndexes().includes(vm.deckIndex);
      return 'w-full aspect-[3/4] rounded-md object-cover bg-[var(--pk-panel)] cursor-pointer ring-2 transition-all '
        + (isSelected ? 'ring-green-400 scale-[1.03]' : 'ring-transparent hover:ring-green-400 hover:scale-[1.03]');
    }
    return 'w-full aspect-[3/4] rounded-md object-cover bg-[var(--pk-panel)] cursor-pointer ring-2 ring-transparent hover:ring-green-400 hover:scale-[1.03] transition-all';
  }

  protected onCardClick(vm: DeckCardVm): void {
    const allowed = this.allowedSupertype();
    if (allowed && vm.supertype !== allowed) return;

    if (this.selectionMode() === 'multi') {
      const current = this.selectedIndexes();
      if (current.includes(vm.deckIndex)) {
        this.selectedIndexes.set(current.filter(i => i !== vm.deckIndex));
      } else if (current.length < this.maxSelections()) {
        this.selectedIndexes.set([...current, vm.deckIndex]);
      }
      return;
    }

    this.cardSelected.emit({
      cardIndex: vm.deckIndex,
      cardId: vm.cardId,
      instanceId: vm.instanceId,
    });
  }

  protected confirmMulti(): void {
    const indexes = this.selectedIndexes();
    if (indexes.length === 0) return;
    const cardIds = indexes.map(i => this.viewModels()[i]?.cardId ?? '');
    this.cardsSelected.emit({ cardIndexes: indexes, cardIds });
  }

  protected onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (!img || img.src.endsWith(this.cardBackUrl)) return;
    img.src = this.cardBackUrl;
  }
}
