import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { DragDropModule, CdkDragDrop } from '@angular/cdk/drag-drop';
import { PrivateHandCardModel, PublicPlayerStateModel, PublicPokemonSlotModel } from '../../../../shared/models/game-state.models';
import { CardDetailResponse } from '../../../../shared/models/card.models';
import { CardImagePipe } from '../../../../shared/pipes/card-image.pipe';
import { CardPreviewService } from '../../services/card-preview.service';
import { MatchStateService } from '../../services/match-state.service';

@Component({
  selector: 'app-setup-overlay',
  imports: [DragDropModule, CardImagePipe],
  template: `
    <div class="fixed inset-0 z-[100] bg-[var(--pk-bg)]">
      <div class="absolute inset-0 bg-cover bg-center" [style.background-image]="'url(/assets/images/setup-mulligan-background-' + timeOfDay() + '.png)'" [style.filter]="'brightness(0.5) saturate(0.6)'"></div>

      <div class="relative z-10 flex flex-col items-center justify-start p-4 overflow-y-auto h-full">
        <div class="w-full flex flex-col xl:flex-row gap-4 xl:h-full min-h-0">

          <div class="xl:w-[20%] w-full flex flex-col shrink-0">
            <div class="bg-[var(--pk-surface)] border border-amber-500 rounded-lg p-3 min-h-[120px]">
              <h3 class="m-0 mb-2 text-xs font-bold text-amber-300">Cartas descartadas por mulligan</h3>
              @if (allMulliganCards().length > 0) {
                <div class="flex flex-wrap gap-1 py-1">
                  @for (card of previewMulliganCards(); track $index) {
                    <img [src]="card.cardId | cardImage" alt="" class="w-[35px] aspect-[3/4] rounded bg-[var(--pk-panel)] border border-amber-500/30 cursor-pointer" (click)="previewSvc.open({ cardId: card.cardId, name: cardName(card.cardId) }); $event.stopPropagation()" />
                  }
                </div>
                <button (click)="openMulliganModal()" class="mt-2 w-full text-[0.5rem] font-bold text-amber-400 bg-amber-400/10 border border-amber-500/30 rounded py-1 cursor-pointer hover:bg-amber-400/20 transition-colors">
                  + Ver cartas
                </button>
              } @else {
                <p class="text-xs text-[var(--pk-text-dim)] text-center py-2">No se realizaron mulligans</p>
              }
            </div>
          </div>

          <div class="xl:w-[60%] w-full flex justify-start min-w-0">
            <div class="w-full">
            <div class="bg-[var(--pk-surface)] border border-[var(--pk-panel)] rounded-xl p-6 flex flex-col gap-2" cdkDropListGroup>
              <h2 class="m-0 mb-1 text-xl font-extrabold text-center text-[var(--pk-text-bright)]">Configura tu Pokémon Inicial</h2>

              <p class="m-0 text-xs font-semibold text-center text-blue-400 uppercase tracking-wide mt-2">POKÉMON ACTIVO</p>
              <div
                class="drop-zone min-h-[120px]"
                cdkDropList
                (cdkDropListDropped)="onActiveDrop($event)"
              >
                @if (activePokemon(); as active) {
                  <div class="flex flex-col items-center gap-1 p-2 relative group transition-transform duration-200 group-hover:scale-[1.3] group-hover:z-10">
                    <img [src]="active.cardId | cardImage" alt="" class="w-[70px] aspect-[3/4] object-cover rounded bg-[var(--pk-panel)] cursor-pointer" (click)="previewSvc.open({ cardId: active.cardId, name: cardName(active.cardId) }); $event.stopPropagation()" />
                    <span class="text-[0.6875rem] font-semibold text-[var(--pk-text-bright)]">{{ cardName(active.cardId) }}</span>
                    <button class="absolute top-[2px] right-[2px] w-5 h-5 rounded-full border-none bg-red-500 text-white text-xs leading-none cursor-pointer flex items-center justify-center p-0 opacity-0 transition-opacity group-hover:opacity-100" (click)="onFieldCardRemove(active.instanceId); $event.stopPropagation()" title="Quitar de activo">✕</button>
                  </div>
                } @else {
                  <div class="flex flex-col items-center gap-1">
                    <span class="text-sm text-[var(--pk-text-dim)]">Arrastra aquí tu Pokémon activo</span>
                  </div>
                }
              </div>

              <p class="m-0 text-xs font-semibold text-center text-red-400 uppercase tracking-wide mt-1">BANCA</p>
              <div class="flex gap-2">
                @for (slot of benchSlots(); track $index; let idx = $index) {
                  <div
                    class="drop-zone flex-1 min-w-0 min-h-[100px]"
                    cdkDropList
                    (cdkDropListDropped)="onBenchDrop($event, idx)"
                  >
                    @if (slot; as poke) {
                      <div class="flex flex-col items-center gap-1 p-2 relative group transition-transform duration-200 group-hover:scale-[1.3] group-hover:z-10">
                        <img [src]="poke.cardId | cardImage" alt="" class="w-[60px] aspect-[3/4] object-cover rounded bg-[var(--pk-panel)] cursor-pointer" (click)="previewSvc.open({ cardId: poke.cardId, name: cardName(poke.cardId) }); $event.stopPropagation()" />
                        <span class="text-[0.6875rem] font-semibold text-[var(--pk-text-bright)]">{{ cardName(poke.cardId) }}</span>
                        <button class="absolute top-[2px] right-[2px] w-5 h-5 rounded-full border-none bg-red-500 text-white text-xs leading-none cursor-pointer flex items-center justify-center p-0 opacity-0 transition-opacity group-hover:opacity-100" (click)="onFieldCardRemove(poke.instanceId); $event.stopPropagation()" title="Quitar de banca">✕</button>
                      </div>
                    } @else {
                      <span class="text-lg text-[var(--pk-text-dim)]">+</span>
                    }
                  </div>
                }
              </div>

              <p class="m-0 text-xs font-semibold text-center text-blue-400 uppercase tracking-wide mt-1">TUS CARTAS</p>
              <div class="flex gap-2 p-2 bg-[var(--pk-surface)] rounded-lg border border-[var(--pk-panel)] overflow-x-auto flex-nowrap" cdkDropList>
                @for (card of hand(); track card.instanceId) {
                  <div
                    class="flex flex-col items-center gap-1 p-1.5 border-2 border-[var(--pk-panel)] rounded-md bg-[var(--pk-surface)] min-w-[80px] w-[80px] cursor-grab transition-opacity border-color text-[var(--pk-text-bright)] relative [touch-action:none]"
                    cdkDrag
                    [cdkDragData]="card.instanceId"
                    [cdkDragDisabled]="!isBasicPokemon(card)"
                    [class.opacity-55]="!isBasicPokemon(card)"
                    [class.border-red-800]="!isBasicPokemon(card)"
                    [class.cursor-not-allowed]="!isBasicPokemon(card)"
                  >
                    <div class="relative group transition-transform duration-200 group-hover:scale-[1.3] group-hover:z-10">
                      <img [src]="card.cardId | cardImage" alt="{{ card.name }}" class="w-[60px] aspect-[3/4] object-cover rounded bg-[var(--pk-panel)] cursor-grab" />
                      <button class="absolute top-0.5 right-0.5 w-4 h-4 flex items-center justify-center rounded-full bg-black/60 text-white text-[0.5rem] opacity-0 group-hover:opacity-100 transition-opacity duration-150 cursor-pointer hover:bg-black/80 z-10 border-none leading-none shadow" (mousedown)="$event.stopPropagation()" (click)="previewSvc.open({ cardId: card.cardId, name: card.name }); $event.stopPropagation()" title="Ver detalle">+</button>
                    </div>
                    <span class="text-[0.625rem] font-semibold text-center leading-tight max-w-full truncate">{{ card.name }}</span>
                    @if (!isBasicPokemon(card)) {
                      <div class="absolute top-[2px] right-[2px] text-sm leading-none [filter:drop-shadow(0_1px_2px_rgba(0,0,0,0.5))]" title="No es un Pokémon básico">⛔</div>
                    }
                  </div>
                }
              </div>

              <div class="flex flex-col items-center justify-center gap-3 mt-4 pt-4 border-t border-[var(--pk-panel)]">
                <div class="text-sm font-semibold text-center">
                  @if (opponentSetupConfirmed()) {
                    <span class="text-blue-400">Tu oponente está listo</span>
                  } @else if (opponentResolvingMulligan()) {
                    <span class="text-[var(--pk-text-dim)]">El oponente está resolviendo su mulligan...</span>
                  } @else {
                    <span class="text-[var(--pk-text-dim)]">Esperando oponente...</span>
                  }
                </div>
                <button
                  class="pk-btn text-base font-bold px-10 py-3"
                  [disabled]="!canConfirm()"
                  (click)="onConfirm()"
                >
                  {{ mySetupConfirmed() ? 'Esperando oponente...' : 'Confirmar' }}
                </button>
              </div>
            </div>
          </div>
          </div>

          <div class="xl:w-[20%] w-full flex flex-col shrink-0">
            <div class="bg-[var(--pk-surface)] border border-amber-500 rounded-lg p-3">
              <h3 class="m-0 mb-2 text-xs font-bold text-amber-300">Reglas: Mulligan</h3>
              <div class="text-[0.625rem] text-[var(--pk-text)] leading-relaxed">
                <p class="m-0 font-bold text-blue-400">¿Qué es Mulligan?</p>
                <p class="m-0 mt-0.5">Si al repartir las 7 cartas iniciales no tenés ningún Pokémon Básico...</p>
              </div>
              <button (click)="openRulesModal()" class="mt-2 w-full text-[0.5rem] font-bold text-amber-400 bg-amber-400/10 border border-amber-500/30 rounded py-1 cursor-pointer hover:bg-amber-400/20 transition-colors">
                + Ver reglas
              </button>
            </div>
          </div>

        </div>
      </div>

      @if (_showMulliganModal()) {
        <div class="fixed inset-0 z-[200] bg-black/70 flex items-center justify-center" (click)="closeMulliganModal()">
          <div class="bg-[var(--pk-surface)] border border-amber-500 rounded-xl p-5 max-w-2xl w-[90vw] max-h-[85vh] flex flex-col" (click)="$event.stopPropagation()">
            <div class="flex items-center justify-between mb-3 shrink-0">
              <h3 class="m-0 text-sm font-bold text-amber-300">Cartas descartadas por mulligan</h3>
              <button (click)="closeMulliganModal()" class="w-6 h-6 flex items-center justify-center rounded-full bg-transparent text-[var(--pk-text-dim)] text-sm cursor-pointer hover:bg-[var(--pk-panel)] hover:text-[var(--pk-text)] border-none leading-none">✕</button>
            </div>
            <div class="overflow-y-auto flex-1 max-h-[70vh]">
              @if (opponentMulliganRevealedCards().length > 0) {
                <div class="flex flex-col gap-1 mb-6">
                  <span class="text-xs font-semibold text-[var(--pk-text-dim)]">Cartas descartadas por el oponente</span>
                  @for (revealGroup of opponentMulliganRevealedCards(); track $index) {
                    <div class="flex overflow-x-auto justify-center gap-2 py-2 [scrollbar-width:thin]">
                      @for (cardId of revealGroup; track $index) {
                        <div class="flex flex-col items-center gap-0.5 relative group transition-transform duration-200 group-hover:scale-[1.3] group-hover:z-10 shrink-0">
                          <img [src]="cardId | cardImage" alt="" class="w-[45px] aspect-[3/4] rounded bg-[var(--pk-panel)] cursor-pointer" (click)="previewSvc.open({ cardId, name: cardName(cardId) }); $event.stopPropagation()" />
                          <span class="text-[0.5625rem] font-semibold text-[var(--pk-text-bright)] max-w-[50px] truncate text-center">{{ cardName(cardId) }}</span>
                          <button class="absolute top-0.5 right-0.5 w-4 h-4 flex items-center justify-center rounded-full bg-black/60 text-white text-[0.5rem] opacity-0 group-hover:opacity-100 transition-opacity duration-150 cursor-pointer hover:bg-black/80 z-10 border-none leading-none shadow" (mousedown)="$event.stopPropagation()" (click)="previewSvc.open({ cardId, name: cardName(cardId) }); $event.stopPropagation()" title="Ver detalle">+</button>
                        </div>
                      }
                    </div>
                  }
                </div>
              }
              @if (myMulliganRevealedCards().length > 0) {
                <div class="flex flex-col gap-1">
                  <span class="text-xs font-semibold text-[var(--pk-text-dim)]">Cartas descartadas por ti</span>
                  @for (revealGroup of myMulliganRevealedCards(); track $index) {
                    <div class="flex overflow-x-auto justify-center gap-2 py-2 [scrollbar-width:thin]">
                      @for (cardId of revealGroup; track $index) {
                        <div class="flex flex-col items-center gap-0.5 relative group transition-transform duration-200 group-hover:scale-[1.3] group-hover:z-10 shrink-0">
                          <img [src]="cardId | cardImage" alt="" class="w-[45px] aspect-[3/4] rounded bg-[var(--pk-panel)] cursor-pointer" (click)="previewSvc.open({ cardId, name: cardName(cardId) }); $event.stopPropagation()" />
                          <span class="text-[0.5625rem] font-semibold text-[var(--pk-text-bright)] max-w-[50px] truncate text-center">{{ cardName(cardId) }}</span>
                          <button class="absolute top-0.5 right-0.5 w-4 h-4 flex items-center justify-center rounded-full bg-black/60 text-white text-[0.5rem] opacity-0 group-hover:opacity-100 transition-opacity duration-150 cursor-pointer hover:bg-black/80 z-10 border-none leading-none shadow" (mousedown)="$event.stopPropagation()" (click)="previewSvc.open({ cardId, name: cardName(cardId) }); $event.stopPropagation()" title="Ver detalle">+</button>
                        </div>
                      }
                    </div>
                  }
                </div>
              }
            </div>
          </div>
        </div>
      }

      @if (_showRulesModal()) {
        <div class="fixed inset-0 z-[200] bg-black/70 flex items-center justify-center" (click)="closeRulesModal()">
          <div class="bg-[var(--pk-surface)] border border-amber-500 rounded-xl p-5 max-w-lg w-[90vw] max-h-[85vh] flex flex-col" (click)="$event.stopPropagation()">
            <div class="flex items-center justify-between mb-3 shrink-0">
              <h3 class="m-0 text-sm font-bold text-amber-300">Reglas: Mulligan</h3>
              <button (click)="closeRulesModal()" class="w-6 h-6 flex items-center justify-center rounded-full bg-transparent text-[var(--pk-text-dim)] text-sm cursor-pointer hover:bg-[var(--pk-panel)] hover:text-[var(--pk-text)] border-none leading-none">✕</button>
            </div>
            <div class="overflow-y-auto flex-1 text-sm text-[var(--pk-text)] leading-6 space-y-4">
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
              <div class="border-t border-[var(--pk-btn-border)]/30 pt-2">
                <p class="m-0 text-[0.625rem] text-[var(--pk-text-dim)] italic">
                  Reglas oficiales del Juego de Cartas Coleccionables Pokémon.
                </p>
              </div>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .drop-zone {
      border: 2px dashed var(--pk-panel); border-radius: 0.5rem;
      min-height: 80px; display: flex; align-items: center; justify-content: center;
      transition: border-color 0.15s, background 0.15s;
    }
    .drop-zone.cdk-drop-list-dragover { border-color: #22d3ee; background: var(--pk-surface); }
    [cdkDrag].cdk-drag-dragging { opacity: 0.3 !important; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SetupOverlayComponent {
  private readonly matchState = inject(MatchStateService);
  protected readonly previewSvc = inject(CardPreviewService);

  readonly timeOfDay = computed(() => {
    const h = new Date().getHours();
    if (h >= 6 && h < 12) return 'morning';
    if (h >= 12 && h < 18) return 'afternoon';
    return 'night';
  });

  readonly myPlayerState = input<PublicPlayerStateModel | null>(null);
  readonly opponentSetupConfirmed = input(false);
  readonly mySetupConfirmed = input(false);
  readonly mulliganDrawPending = input(false);
  readonly initialMulliganPending = input(false);
  readonly cardDefs = input<Map<string, CardDetailResponse | null>>(new Map());

  readonly activeDropped = output<string>();
  readonly benchDropped = output<{ cardInstanceId: string; benchIndex: number }>();
  readonly fieldCardRemoved = output<string>();
  readonly confirmSetup = output<void>();

  readonly hand = computed(() => this.matchState.privateState()?.hand ?? []);

  readonly activePokemon = computed(() => this.myPlayerState()?.activePokemon ?? null);

  readonly benchSlots = computed<(PublicPokemonSlotModel | null)[]>(() => {
    const me = this.myPlayerState();
    const bench = me?.bench ?? [];
    const result: (PublicPokemonSlotModel | null)[] = [...bench];
    while (result.length < 5) result.push(null);
    return result;
  });

  readonly opponentResolvingMulligan = computed(() => {
    const pub = this.matchState.publicState();
    if (!pub?.pendingInitialMulliganPlayers?.length) return false;
    const myId = this.matchState.myPlayerId();
    if (!myId) return false;
    return !pub.pendingInitialMulliganPlayers.includes(myId);
  });

  readonly opponentMulliganRevealedCards = computed(() => {
    const pub = this.matchState.publicState();
    const myId = this.matchState.myPlayerId();
    if (!pub || !myId) return [];
    const opponent = pub.players.find(p => p.playerId !== myId);
    return opponent?.mulliganRevealedCards ?? [];
  });

  readonly myMulliganRevealedCards = computed(() => {
    const pub = this.matchState.publicState();
    const myId = this.matchState.myPlayerId();
    if (!pub || !myId) return [];
    const me = pub.players.find(p => p.playerId === myId);
    return me?.mulliganRevealedCards ?? [];
  });

  readonly canConfirm = computed(() => {
    const me = this.myPlayerState();
    if (!me) return false;
    if (me.setupConfirmed) return false;
    if (this.mulliganDrawPending()) return false;
    if (this.initialMulliganPending()) return false;
    if (this.opponentResolvingMulligan()) return false;
    return me.activePokemon !== null;
  });

  protected readonly _showMulliganModal = signal(false);

  protected readonly previewCardLimit = 7;

  protected readonly allMulliganCards = computed(() => {
    const result: { cardId: string }[] = [];
    const opponent = this.opponentMulliganRevealedCards();
    const mine = this.myMulliganRevealedCards();
    for (const group of opponent) {
      for (const cardId of group) {
        result.push({ cardId });
      }
    }
    for (const group of mine) {
      for (const cardId of group) {
        result.push({ cardId });
      }
    }
    return result;
  });

  protected readonly previewMulliganCards = computed(() => {
    return this.allMulliganCards().slice(0, this.previewCardLimit);
  });

  protected readonly remainingMulliganCount = computed(() => {
    return Math.max(0, this.allMulliganCards().length - this.previewCardLimit);
  });

  protected openMulliganModal(): void {
    this._showMulliganModal.set(true);
  }

  protected closeMulliganModal(): void {
    this._showMulliganModal.set(false);
  }

  protected readonly _showRulesModal = signal(false);

  protected openRulesModal(): void {
    this._showRulesModal.set(true);
  }

  protected closeRulesModal(): void {
    this._showRulesModal.set(false);
  }

  protected isBasicPokemon(card: PrivateHandCardModel): boolean {
    if (card.supertype !== 'POKEMON') return false;
    const def = this.cardDefs().get(card.cardId);
    if (!def) return false;
    return !def.stage || def.stage === 'BASIC';
  }

  protected cardName(cardId: string): string {
    const def = this.cardDefs().get(cardId);
    return def?.name ?? '?';
  }

  protected onActiveDrop(event: CdkDragDrop<string>): void {
    this.activeDropped.emit(event.item.data);
  }

  protected onBenchDrop(event: CdkDragDrop<string>, benchIndex: number): void {
    this.benchDropped.emit({ cardInstanceId: event.item.data, benchIndex });
  }

  protected onFieldCardRemove(instanceId: string): void {
    this.fieldCardRemoved.emit(instanceId);
  }

  protected onConfirm(): void {
    this.confirmSetup.emit();
  }

  protected scrollLeft(el: HTMLDivElement): void {
    el.scrollBy({ left: -200, behavior: 'smooth' });
  }

  protected scrollRight(el: HTMLDivElement): void {
    el.scrollBy({ left: 200, behavior: 'smooth' });
  }

  protected onScroll(el: HTMLDivElement): void {
    // no-op; can be used to toggle button visibility if needed
  }
}
