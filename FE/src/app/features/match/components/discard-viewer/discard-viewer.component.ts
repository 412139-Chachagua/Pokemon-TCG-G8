import { ChangeDetectionStrategy, Component, computed, inject, input, output, OnInit, OnDestroy } from '@angular/core';
import { CardImagePipe } from '../../../../shared/pipes/card-image.pipe';
import { CardRepositoryService } from '../../../../core/services/card-repository.service';
import { CardPreviewService } from '../../services/card-preview.service';
import { PublicDiscardCardModel } from '../../../../shared/models/game-state.models';

interface DiscardCardVm {
  instanceId: string;
  cardId: string;
  discardIndex: number;
  name: string;
  supertype: string;
  subtypes: string[];
}

@Component({
  selector: 'app-discard-viewer',
  standalone: true,
  imports: [CardImagePipe],
  template: `
    <div
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
      role="dialog"
      aria-modal="true"
      aria-label="Pila de descarte"
      (click)="close.emit()">
      <div
        class="bg-[var(--pk-surface)] rounded-xl p-4 max-w-4xl w-full max-h-[80vh] flex flex-col shadow-2xl"
        (click)="$event.stopPropagation()">
        <div class="flex justify-between items-center mb-3">
          <h3 class="text-lg font-bold text-slate-100">{{ selectable() ? 'Seleccioná un Pokémon' : 'Pila de descarte' }}</h3>
          <span class="text-sm text-slate-400">{{ discardCount() }} cartas</span>
        </div>
        <div class="overflow-y-auto flex-1 pr-1">
          @if (viewModels().length > 0) {
            <div class="grid grid-cols-4 sm:grid-cols-5 md:grid-cols-6 lg:grid-cols-7 gap-3">
              @for (vm of viewModels(); track vm.instanceId ?? vm.cardId + $index) {
                <div class="flex flex-col items-center relative group">
                  <img
                    [src]="vm.cardId | cardImage"
                    [alt]="vm.name"
                    loading="lazy"
                    (error)="onImageError($event)"
                    [class]="cardImageClass(vm)"
                    (click)="onCardClick(vm)" />
                  <button
                    class="absolute top-1 right-1 w-5 h-5 flex items-center justify-center rounded-full bg-black/60 text-white text-[0.625rem] opacity-0 group-hover:opacity-100 transition-opacity duration-150 cursor-pointer hover:bg-black/80 z-10 border-none leading-none shadow"
                    (mousedown)="$event.stopPropagation()"
                    (click)="$event.stopPropagation(); previewSvc.open({ cardId: vm.cardId, name: vm.name })"
                    title="Ver detalle"
                  >+</button>
                  <span class="text-[0.65rem] text-center text-slate-300 mt-1 truncate w-full">
                    {{ vm.name }}
                  </span>
                </div>
              }
            </div>
          } @else {
            <div class="flex flex-col items-center justify-center h-48 text-slate-400">
              <img [src]="cardBackUrl" alt="Pila vacía" class="w-20 opacity-50 mb-3" />
              <p class="text-sm">Pila de descarte vacía</p>
            </div>
          }
        </div>
        @if (!selectable()) {
          <button
            class="mt-4 self-end px-4 py-2 rounded bg-blue-600 hover:bg-blue-500 text-white text-sm"
            (click)="close.emit()">
            Cerrar
          </button>
        }
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DiscardViewerComponent implements OnInit, OnDestroy {
  private readonly cardRepo = inject(CardRepositoryService);
  protected readonly previewSvc = inject(CardPreviewService);

  readonly discard = input<PublicDiscardCardModel[]>([]);
  readonly discardCount = input(0);
  readonly selectable = input(false);
  readonly close = output<void>();
  readonly cardSelected = output<{ cardIndex: number; cardId: string; instanceId: string }>();

  protected readonly cardBackUrl = 'assets/images/card-back.svg';

  private readonly onKeyDown = (event: KeyboardEvent) => {
    if (event.key === 'Escape') {
      this.close.emit();
    }
  };

  readonly viewModels = computed<DiscardCardVm[]>(() => {
    const discard = this.discard() ?? [];
    return [...discard]
      .map((card, i) => ({ card, originalIndex: i }))
      .reverse()
      .map(({ card, originalIndex }) => {
        const cached = this.cardRepo.getFromCache(card.cardId);
        return {
          instanceId: card.instanceId,
          cardId: card.cardId,
          discardIndex: originalIndex,
          name: cached?.name ?? card.cardId,
          supertype: cached?.supertype ?? 'Unknown',
          subtypes: cached?.subtypes ?? [],
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

  protected cardImageClass(vm: DiscardCardVm): string {
    if (!this.selectable()) return 'w-full aspect-[3/4] rounded-md object-cover bg-[var(--pk-panel)]';
    if (vm.supertype === 'POKEMON') {
      return 'w-full aspect-[3/4] rounded-md object-cover bg-[var(--pk-panel)] cursor-pointer ring-2 ring-transparent hover:ring-green-400 hover:scale-[1.03] transition-all';
    }
    return 'w-full aspect-[3/4] rounded-md object-cover bg-[var(--pk-panel)] opacity-40 cursor-not-allowed';
  }

  protected onCardClick(vm: DiscardCardVm): void {
    if (!this.selectable()) return;
    if (vm.supertype !== 'POKEMON') return;
    this.cardSelected.emit({
      cardIndex: vm.discardIndex,
      cardId: vm.cardId,
      instanceId: vm.instanceId,
    });
  }

  protected onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (!img || img.src.endsWith(this.cardBackUrl)) {
      return;
    }
    img.src = this.cardBackUrl;
  }
}
