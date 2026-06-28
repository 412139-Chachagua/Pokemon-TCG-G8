import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked, OnInit, OnDestroy, DestroyRef } from '@angular/core';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { AudioService } from '../../../../core/audio/audio.service';
import { CardRepositoryService } from '../../../../core/services/card-repository.service';
import { MatchApiService } from '../../../../core/api/match-api.service';
import { MatchStateService } from '../../services/match-state.service';
import { MatchInteractionService } from '../../services/match-interaction.service';
import { MatchFacadeService } from '../../services/match-facade.service';
import { GameActionDispatcherService } from '../../services/game-action-dispatcher.service';
import { MatchHeaderComponent } from '../../components/match-header/match-header.component';
import { PlayerAreaComponent } from '../../components/player-area/player-area.component';
import { OpponentAreaComponent } from '../../components/opponent-area/opponent-area.component';
import { StadiumZoneComponent } from '../../components/stadium-zone/stadium-zone.component';
import { DiscardViewerComponent } from '../../components/discard-viewer/discard-viewer.component';
import { DeckViewerComponent } from '../../components/deck-viewer/deck-viewer.component';
import { HandZoneComponent } from '../../components/hand-zone/hand-zone.component';
import { ActionOverlayComponent } from '../../../../shared/components/action-overlay/action-overlay.component';
import { ActionPanelComponent } from '../../components/action-panel/action-panel.component';
import { VictoryOverlayComponent } from '../../components/victory-overlay/victory-overlay.component';
import { SetupOverlayComponent } from '../../components/setup-overlay/setup-overlay.component';
import { GameLogComponent } from '../../components/game-log/game-log.component';
import { LegendPanelComponent } from '../../components/legend-panel/legend-panel.component';
import { MatchMenuComponent } from '../../components/match-menu/match-menu.component';
import { CardPreviewOverlayComponent } from '../../components/card-preview-overlay/card-preview-overlay.component';
import { ChatBoxComponent } from '../../components/chat-box/chat-box.component';
import { ToastContainerComponent } from '../../components/toast-container/toast-container.component';
import { ToastService } from '../../../../shared/services/toast.service';
import { WaitingPanelComponent } from '../../components/waiting-panel/waiting-panel.component';
import { BackButtonComponent } from '../../../../shared/components/back-button/back-button.component';
import { CardDetailResponse, EnergyType, normalizeCardSubtypes } from '../../../../shared/models/card.models';
import { PublicPokemonSlotModel, PrivateHandCardModel, PublicDiscardCardModel } from '../../../../shared/models/game-state.models';
import { GameActionType, GameEventDto } from '../../../../shared/models/game-action.models';
import { PokemonClickedEvent } from '../../../../shared/models/ui-state.models';
import { EnergyIconPipe } from '../../../../shared/pipes/energy-icon.pipe';

interface PendingAttackData {
  attackIndex: number;
  attackName: string;
  cost: string[];
  targetId?: string;
  isSelfSwitch?: boolean;
  useOptionalBonus?: boolean;
  isOptionalDiscard?: boolean;
  needsAttackRestriction?: boolean;
  conditionOptions?: string[];
  chosenCondition?: string;
}

@Component({
  selector: 'app-match-page',
  imports: [
    DragDropModule,
    MatchHeaderComponent, PlayerAreaComponent, OpponentAreaComponent, StadiumZoneComponent, DiscardViewerComponent, DeckViewerComponent,
    HandZoneComponent, ActionPanelComponent, VictoryOverlayComponent,
    SetupOverlayComponent, GameLogComponent, LegendPanelComponent, MatchMenuComponent, CardPreviewOverlayComponent, WaitingPanelComponent, BackButtonComponent, EnergyIconPipe, ChatBoxComponent,
    ToastContainerComponent, ActionOverlayComponent,
  ],
  host: {
    '(document:keydown.escape)': 'onEscapeKey()',
  },
  template: `
    <app-toast-container />

    @if (matchState.opponentDisconnected()) {
      <div class="fixed top-0 left-0 right-0 z-[300] bg-yellow-800/90 text-yellow-200 text-center py-2 text-sm font-medium shadow-lg">
        El oponente se desconect\u00f3. Esperando reconexi\u00f3n...
      </div>
    }

    @if (reconnecting()) {
      <div class="fixed inset-0 z-[400] flex items-center justify-center bg-black/70">
        <div class="pk-panel p-8 text-center max-w-sm">
          <div class="w-10 h-10 border-4 border-[var(--pk-accent)] border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p class="text-[var(--pk-text)] text-lg font-semibold mb-2">Esperando al oponente...</p>
          <p class="text-[var(--pk-text-dim)] text-sm">La partida se reanudar\u00e1 cuando ambos jugadores est\u00e9n listos</p>
        </div>
      </div>
    }

    @if (matchState.publicState(); as state) {
      @if (matchState.attackCoinFlip(); as result) {
        <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-[200] bg-slate-800/95 border-2 border-yellow-400 rounded-xl px-5 py-4 shadow-2xl animate-fade-in flex items-center gap-4">
          <div class="w-12 h-12 rounded-full flex items-center justify-center text-xl font-black border-2 border-yellow-400 bg-gradient-to-br from-yellow-200 to-yellow-400 text-yellow-900 shadow">
            {{ result === 'HEADS' ? 'C' : 'S' }}
          </div>
          <span class="text-white font-bold text-lg">{{ result === 'HEADS' ? '¡Cara!' : 'Cruz' }}</span>
        </div>
      }
      @if (matchState.multiCoinFlips(); as flips) {
        @if (flips.length > 0) {
          <div class="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-[200] flex flex-col items-center gap-3">
            <div class="flex gap-2 flex-wrap justify-center">
              @for (flip of flips; track flip.flipIndex) {
                <div class="w-10 h-10 rounded-full flex items-center justify-center text-sm font-black border-2 shadow animate-fade-in"
                  [class.border-yellow-400]="flip.result === 'HEADS'"
                  [class.border-slate-500]="flip.result === 'TAILS'"
                  [class.bg-gradient-to-br]="true"
                  [class.from-yellow-200]="flip.result === 'HEADS'"
                  [class.to-yellow-400]="flip.result === 'HEADS'"
                  [class.text-yellow-900]="flip.result === 'HEADS'"
                  [class.bg-slate-700]="flip.result === 'TAILS'"
                  [class.text-slate-300]="flip.result === 'TAILS'">
                  {{ flip.result === 'HEADS' ? 'C' : 'S' }}
                </div>
              }
            </div>
             <span class="text-sm text-slate-300 font-semibold">{{ countHeads(flips) }} cara(s)</span>
          </div>
        }
      }
      <main class="relative" [style.--board-bg]="boardBg()" [class.animate-attack-shake]="_screenShake()">
        <div class="absolute inset-0 bg-cover bg-top pointer-events-none" [style.background-image]="'var(--board-bg)'" [style.filter]="'brightness(1.0) saturate(0.9)'"></div>
        <div class="flex flex-col gap-3 p-4 min-h-screen relative z-10">
        <app-match-header [publicState]="state" [myPlayerId]="matchState.myPlayerId()" />

        @if (state.status !== 'SETUP') {
          @if (opponentPlayerState(); as opponent) {
            <div [class.hidden]="_boardSplash()">
            <button
              (click)="onMenuToggle()"
              class="fixed left-0 top-1/2 -translate-y-1/2 z-[100] flex items-center gap-2 px-2 py-4 rounded-r-md bg-slate-800/95 border border-l-0 border-yellow-600/60 text-yellow-400 text-xs font-bold uppercase tracking-widest cursor-pointer select-none hover:bg-yellow-900/60 hover:text-yellow-300 hover:border-yellow-500 transition-all shadow-lg shadow-black/40 [writing-mode:vertical-lr]"
            >
              Menú
            </button>
             <div class="flex flex-col flex-1 min-h-0" cdkDropListGroup>
               <div class="flex flex-col min-h-[520px]">
                  <div class="flex flex-row gap-2 min-h-0 relative">
                    @if (myPlayerState(); as me) {
                      <div class="flex-1 min-w-0 flex flex-col">
                        <app-player-area
                          [playerState]="me"
                          [validTargets]="needsKOReplacement() ? koModeTargets() : selectionState().validTargets"
                          [selectionMode]="needsKOReplacement() ? 'NONE' : selectionState().mode"
                          [dragEnabled]="canDragToBench()"
                          [connectedDropListIds]="[handZoneDroplistId]"
                          (pokemonClicked)="onPokemonClicked($event)"
                          (abilityClicked)="onPokemonAbilityClicked($event)"
                          (energyClicked)="onEnergyClickedForSuperPotion($event)"
                          (benchDropped)="onHandToBenchDropped($event)"
                          (energyDropped)="onEnergyDropped($event)"
                          (evolutionDropped)="onEvolutionDropped($event)"
                          (trainerDropped)="onTrainerDropped($event)"
                          (viewDiscard)="openDiscardViewer(matchState.myPlayerId()!)"
                        />
                      </div>
                    }
                    <div class="flex-1 min-w-0 flex flex-col">
                      <app-opponent-area
                        [playerState]="opponent"
                        [validTargets]="selectionState().validTargets"
                        [selectionMode]="opponentSelectionMode()"
                        (pokemonClicked)="onPokemonClicked($event)"
                        (viewDiscard)="openDiscardViewer(opponent.playerId)"
                      />
                    </div>

                    <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-10">
                      <app-stadium-zone
                        [stadiumCardInstanceId]="state.stadiumCardInstanceId ?? null"
                        [stadiumCardDefinitionId]="state.stadiumCardDefinitionId ?? null"
                      />
                    </div>
                  </div>

                 @if (showAttackBar()) {
                   <div class="flex flex-row gap-2 justify-center items-center flex-wrap shrink-0 pt-1.5 mt-1">
                     @for (attack of attacksWithAvailability(); track attack.index) {
                       <div class="relative group">
                         <button
                           class="flex flex-row items-center gap-1.5 px-3 py-2 rounded font-bold text-sm transition-all"
                           [disabled]="!attack.available"
                           (click)="onAttackClicked(attack.index)"
                           [class.text-white]="attack.available"
                           [class.text-slate-400]="!attack.available"
                           [class.cursor-pointer]="attack.available"
                           [class.cursor-not-allowed]="!attack.available"
                           [class.hover:opacity-90]="attack.available"
                           [class.bg-slate-700]="!attack.available"
                           [style.background]="attack.available ? (attackBg(attack.pokemonType) ?? '#2563eb') : undefined"
                         >
                           <span>{{ attack.name }}</span>
                           @if (attack.damage) {
                             <span class="text-xs opacity-80">{{ attack.damage }} daño</span>
                           }
                           @for (costItem of attack.cost; track $index) {
                             <img [src]="costItem | energyIcon" alt="{{ costItem }}" class="w-4 h-4" />
                           }
                         </button>
                         <div class="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden group-hover:block bg-gray-900 border border-gray-700 rounded-lg px-4 py-3 w-80 z-50 shadow-xl pointer-events-none text-[0.7rem] text-gray-300 leading-relaxed">
                           {{ attack.text || attack.name }}
                         </div>
                       </div>
                     }
                     @if (retreatInfo(); as retreat) {
                       <button
                         class="flex flex-row items-center gap-1.5 px-3 py-2 rounded font-bold text-sm transition-opacity"
                         [class]="retreat.canRetreat ? 'bg-red-600 text-white hover:opacity-90 cursor-pointer' : 'bg-slate-700 text-slate-400 cursor-not-allowed'"
                         [disabled]="!retreat.canRetreat"
                         (click)="onRetreatInitiated()"
                       >
                         <span>Retirar</span>
                         @for (costItem of retreat.cost; track $index) {
                           <img [src]="costItem | energyIcon" alt="{{ costItem }}" class="w-4 h-4" />
                         }
                       </button>
                     }
                     @if (activeAbilityInfo(); as ability) {
                       <div class="relative group">
                         <button
                           class="relative flex items-center gap-1.5 px-3 py-2 rounded font-bold text-sm transition-all border-0 overflow-hidden select-none outline-none"
                           [class.opacity-80]="!ability.canUse"
                           [class.grayscale-[0.3]]="!ability.canUse"
                           [class.cursor-pointer]="ability.canUse"
                           [class.hover:opacity-90]="ability.canUse"
                           [class.cursor-default]="!ability.canUse"
                           [disabled]="!ability.canUse"
                           (click)="onAbilityButtonClick()"
                         >
                           <div class="absolute inset-0 bg-gradient-to-b from-[#ff8c42] to-[#d92b2b] rounded"></div>
                           <div class="absolute inset-0 rounded shadow-inner shadow-black/30"></div>
                           <div class="absolute top-0 left-0 w-10 h-5 bg-[#ff6b35] rounded-tl" style="clip-path: polygon(0 0, 100% 0, 70% 100%, 0 100%);"></div>
                           <span class="relative z-10 font-bold italic text-white drop-shadow-[0_1px_1px_rgba(0,0,0,0.5)] tracking-wide uppercase text-[0.7rem]">
                             {{ ability.name }}
                           </span>
                         </button>
                         <div class="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden group-hover:block bg-gray-900 border border-gray-700 rounded-lg px-4 py-3 w-80 z-50 shadow-xl pointer-events-none">
                           <p class="text-[0.7rem] font-bold" [class.text-green-400]="ability.isActivable" [class.text-blue-400]="!ability.isActivable">
                             Habilidad tipo: {{ ability.isActivable ? 'ACTIVA' : 'PASIVA' }}
                           </p>
                           <div class="border-t border-gray-700 my-1"></div>
                           <p class="text-[0.65rem] text-gray-300 leading-relaxed">{{ ability.text }}</p>
                         </div>
                       </div>
                     }
                   </div>
                 } @else if (matchState.currentPhase() === 'DRAW' && matchState.isMyTurn()) {
                   <div class="flex justify-center shrink-0 pt-1.5 mt-1">
                     <div class="flex items-center gap-3 px-4 py-2 bg-blue-950 border border-blue-500 rounded-md">
                       <span class="text-blue-300 text-sm font-semibold">Debés robar una carta para comenzar tu turno</span>
                       <button class="px-4 py-1.5 rounded-md bg-blue-600 text-white text-sm font-semibold cursor-pointer hover:bg-blue-500 border-none transition-colors" (click)="onDrawCard()">Robar carta</button>
                     </div>
                   </div>
                 }
               </div>

               <div class="flex flex-col gap-4 mt-auto">
                 @if (matchState.privateState(); as priv) {
                   <app-hand-zone
                     [hand]="priv.hand"
                     [selectionMode]="selectionState().mode"
                     [selectedHandIndex]="selectionState().selectedHandIndex"
                     [dragEnabled]="canDragToBench()"
                     [connectedDropListIds]="benchSlotDroplistIds()"
                      [hasPlayedSupporter]="matchState.publicState()?.hasPlayedSupporter ?? false"
                      [hasPlayedStadium]="matchState.publicState()?.hasPlayedStadium ?? false"
                      [canAttachEnergy]="matchState.canAttachEnergy()"
                     (cardClicked)="onHandCardClicked($event.card, $event.handIndex)"
                   />
                 }
                 <app-action-panel (actionSelected)="onActionSelected($event)" />
                 <app-game-log [events]="matchState.events()" [myPlayerId]="matchState.myPlayerId()" />
               </div>
             </div>
             <app-legend-panel />
            </div>
          } @else {
            <div class="flex flex-col items-center justify-center p-8 relative flex-1">
              <app-back-button (beforeBack)="onBackFromWaiting()" />
              <app-waiting-panel
                [playerName]="matchFacade.playerName() || authService.player()?.displayName || 'Jugador'"
                [deckName]="matchFacade.deckName() || '—'"
                (cancelMatch)="onCancelMatch()"
              />
            </div>
          }
        } @else {
          <div class="flex-1 text-center p-6 border border-dashed border-slate-600 rounded text-slate-500 text-sm">
            <p>El oponente está preparando su tablero...</p>
          </div>
        }



        <app-action-overlay [show]="needsKOReplacement() && !_koModalDismissed()" mode="modal" layout="info" orientation="vertical" color="red"
          message="Tu Pokémon activo fue debilitado. Seleccioná un Pokémon del banco para reemplazarlo."
          hint="Hacé click en un Pokémon de tu banco" confirmText="Aceptar"
          (confirm)="_koModalDismissed.set(true)" />

        <app-action-overlay [show]="opponentNeedsKOReplacement() && !_opponentKoModalDismissed()" mode="modal" layout="info" orientation="vertical" color="yellow"
          message="Esperá un momento, tu rival está reemplazando su Pokémon activo."
          confirmText="Aceptar"
          (confirm)="_opponentKoModalDismissed.set(true)" />

        @if (pendingAttack(); as atk) {
          @if (!anySelectionPending()) {
          <div class="fixed bottom-0 left-0 right-0 z-[160] bg-slate-800 border-t-2 border-blue-500 px-6 py-3 flex flex-row items-center justify-between gap-4 shadow-lg animate-slide-up"
            title="benchDmg={{benchDamagePending()}} heal={{benchHealPending()}} disc={{energyDiscardPending()}} selfDisc={{selfEnergyDiscardPending()}} sw={{switchPending()}} moveEn={{moveEnergyPending()}} moveBn={{moveBenchPending()}} bonus={{optionalBonusPending()}}">
            <div class="flex items-center gap-4 flex-1">
              <span class="text-slate-200 text-[0.9375rem]">
                ¿Usar <strong class="text-blue-400">{{ atk.attackName }}</strong>
                @if (atk.cost.length) { ({{ atk.cost.join(', ') }}) }?
              </span>
              @if (atk.conditionOptions && atk.conditionOptions.length > 1) {
                <div class="flex gap-2 ml-4">
                  @for (opt of atk.conditionOptions; track opt) {
                    <button
                      class="px-3 py-1 text-xs font-bold rounded border cursor-pointer transition-colors
                        {{ atk.chosenCondition === opt ? 'bg-blue-500 text-white border-blue-400' : 'bg-slate-700 text-slate-300 border-slate-600 hover:bg-slate-600' }}"
                      (click)="onChooseCondition(opt)">
                      {{ opt === 'ASLEEP' ? 'Dormido' : opt === 'POISONED' ? 'Envenenado' : opt }}
                    </button>
                  }
                </div>
              }
            </div>
            <div class="flex gap-2 shrink-0">
              <button class="px-5 py-2 text-sm font-bold rounded bg-slate-600 text-white hover:opacity-90 cursor-pointer border-none transition-opacity" (click)="onCancelAttack()">Cancelar</button>
              <button class="px-5 py-2 text-sm font-bold rounded bg-green-500 text-white hover:opacity-90 cursor-pointer border-none transition-opacity" (click)="onConfirmAttack()">Confirmar</button>
            </div>
          </div>
          }
        }

        <app-match-menu
          [isOpen]="_isMatchMenuOpen()"
          [matchId]="matchId ?? ''"
          (closeMenu)="closeMatchMenu()"
          (concede)="onConcede($event)"
        />

        @if (state.status === 'FINISHED') {
          <app-victory-overlay
            [winnerPlayerId]="state.winnerPlayerId ?? null"
            [myPlayerId]="matchState.myPlayerId()"
            [opponentName]="opponentPlayerState()?.displayName ?? 'El oponente'"
            (returnToLobby)="onReturnToLobby()"
          />
        }

        @if (coinFlipPhase(); as phase) {
          <div class="fixed inset-0 z-[200] bg-black/40 flex items-center justify-center">
            <div class="flex flex-col items-center gap-4 p-8 rounded-xl bg-slate-800 border-2 border-amber-400 animate-fade-in">
              @if (phase === 'spinning') {
                <div class="[perspective:400px] w-20 h-20">
                  <div class="w-20 h-20 relative [transform-style:preserve-3d] rounded-full" [class.animate-coinflip-toss]="phase === 'spinning'">
                    <div class="coin-face">P</div>
                    <div class="coin-face [transform:rotateY(180deg)]">T</div>
                  </div>
                </div>
                <span class="text-base text-slate-400">Lanzando moneda...</span>
              } @else {
                <div class="[perspective:400px] w-20 h-20">
                  <div class="w-20 h-20 relative rounded-full">
                    <div class="absolute inset-0 rounded-full flex items-center justify-center text-2xl font-black border-3 border-amber-600 bg-gradient-to-br from-amber-300 to-amber-500 text-amber-900 shadow-lg">{{ coinFlipWinnerIsMe() ? 'T' : 'P' }}</div>
                  </div>
                </div>
                <span class="text-xl font-bold text-slate-100 text-center animate-fade-in">{{ coinFlipWinnerIsMe() ? 'Tú' : 'El oponente' }} comienza la partida</span>
              }
            </div>
          </div>
        }

        @if (benchDamagePending()) {
          <div class="fixed bottom-0 left-0 right-0 z-[160] bg-slate-800 border-t-2 border-cyan-500 px-6 py-3 flex flex-col items-center gap-3 shadow-lg animate-slide-up">
            <span class="text-slate-200 text-sm font-bold">Seleccioná Pokémon en banca para dañar:</span>
            <div class="flex gap-3 flex-wrap justify-center">
              @for (bench of opponentPlayerState()?.bench ?? []; track bench.instanceId) {
                @let benchName = cardRepo.getFromCache(bench.cardId)?.name ?? bench.cardId;
                <button
                  class="px-3 py-2 rounded text-xs font-bold cursor-pointer border-2 transition-all"
                  [class.bg-cyan-600]="selectedBenchTargets().includes(bench.instanceId)"
                  [class.bg-slate-700]="!selectedBenchTargets().includes(bench.instanceId)"
                  [class.border-cyan-400]="selectedBenchTargets().includes(bench.instanceId)"
                  [class.border-slate-500]="!selectedBenchTargets().includes(bench.instanceId)"
                  (click)="onSelectBenchTarget(bench.instanceId)"
                >{{ benchName }}</button>
              }
            </div>
            <div class="flex gap-2">
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-slate-600 text-white cursor-pointer border-none" (click)="onCancelBenchTargets()">Cancelar</button>
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-cyan-500 text-white cursor-pointer border-none disabled:opacity-50" [disabled]="selectedBenchTargets().length === 0" (click)="onConfirmBenchTargets()">Confirmar</button>
            </div>
          </div>
        }

        @if (benchAttachPending()) {
          <div class="fixed bottom-0 left-0 right-0 z-[160] bg-slate-800 border-t-2 border-green-500 px-6 py-3 flex flex-col items-center gap-3 shadow-lg animate-slide-up">
            <span class="text-slate-200 text-sm font-bold">Seleccioná Pokémon de tu banca para recibir Energía (en orden):</span>
            <div class="flex gap-3 flex-wrap justify-center">
              @for (bench of myPlayerState()?.bench ?? []; track bench.instanceId) {
                @let benchName = cardRepo.getFromCache(bench.cardId)?.name ?? bench.cardId;
                <button
                  class="px-3 py-2 rounded text-xs font-bold cursor-pointer border-2 transition-all"
                  [class.bg-green-600]="selectedBenchTargets().includes(bench.instanceId)"
                  [class.bg-slate-700]="!selectedBenchTargets().includes(bench.instanceId)"
                  [class.border-green-400]="selectedBenchTargets().includes(bench.instanceId)"
                  [class.border-slate-500]="!selectedBenchTargets().includes(bench.instanceId)"
                  (click)="onSelectBenchTarget(bench.instanceId)"
                >{{ benchName }}</button>
              }
            </div>
            <div class="flex gap-2">
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-slate-600 text-white cursor-pointer border-none" (click)="onCancelBenchTargets()">Cancelar</button>
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-green-500 text-white cursor-pointer border-none disabled:opacity-50" [disabled]="selectedBenchTargets().length === 0" (click)="onConfirmBenchAttach()">Confirmar</button>
            </div>
          </div>
        }

        @if (benchHealPending()) {
          <div class="fixed bottom-0 left-0 right-0 z-[160] bg-slate-800 border-t-2 border-green-500 px-6 py-3 flex flex-col items-center gap-3 shadow-lg animate-slide-up">
            <span class="text-slate-200 text-sm font-bold">Seleccioná Pokémon en banca para curar:</span>
            <div class="flex gap-3 flex-wrap justify-center">
              @for (bench of myPlayerState()?.bench ?? []; track bench.instanceId) {
                @let benchName = cardRepo.getFromCache(bench.cardId)?.name ?? bench.cardId;
                <button
                  class="px-3 py-2 rounded text-xs font-bold cursor-pointer border-2 transition-all"
                  [class.bg-green-600]="selectedHealTarget() === bench.instanceId"
                  [class.bg-slate-700]="selectedHealTarget() !== bench.instanceId"
                  [class.border-green-400]="selectedHealTarget() === bench.instanceId"
                  [class.border-slate-500]="selectedHealTarget() !== bench.instanceId"
                  (click)="onSelectHealTarget(bench.instanceId)"
                >{{ benchName }}</button>
              }
            </div>
            <div class="flex gap-2">
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-slate-600 text-white cursor-pointer border-none" (click)="onCancelBenchTargets()">Cancelar</button>
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-green-500 text-white cursor-pointer border-none disabled:opacity-50" [disabled]="!selectedHealTarget()" (click)="onConfirmHealTarget()">Confirmar</button>
            </div>
          </div>
        }

        @if (energyDiscardPending()) {
          @let oppActive = opponentPlayerState()?.activePokemon;
          @let opponentActiveEnergy = oppActive?.attachedCards ?? [];
          @let opponentActiveEnergyIds = oppActive?.attachedEnergyInstanceIds ?? [];
          <div class="fixed bottom-0 left-0 right-0 z-[160] bg-slate-800 border-t-2 border-red-500 px-6 py-3 flex flex-col items-center gap-3 shadow-lg animate-slide-up">
            <span class="text-slate-200 text-sm font-bold">Seleccioná energías para descartar del Pokémon rival:</span>
            <div class="flex gap-3 flex-wrap justify-center">
              @for (energyType of opponentActiveEnergy; track $index) {
                @let energyId = opponentActiveEnergyIds[$index] ?? energyType;
                <button
                  class="px-3 py-2 rounded text-xs font-bold cursor-pointer border-2 transition-all"
                  [class.bg-red-600]="selectedEnergyDiscard().includes(energyId)"
                  [class.bg-slate-700]="!selectedEnergyDiscard().includes(energyId)"
                  [class.border-red-400]="selectedEnergyDiscard().includes(energyId)"
                  [class.border-slate-500]="!selectedEnergyDiscard().includes(energyId)"
                  (click)="onToggleEnergyDiscard(energyId)"
                >{{ energyType }}</button>
              }
            </div>
            <div class="flex gap-2">
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-slate-600 text-white cursor-pointer border-none" (click)="onCancelBenchTargets()">Cancelar</button>
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-red-500 text-white cursor-pointer border-none disabled:opacity-50" [disabled]="selectedEnergyDiscard().length === 0" (click)="onConfirmEnergyDiscard()">Confirmar</button>
            </div>
          </div>
        }

        @if (selfEnergyDiscardPending()) {
          @let myActive = myPlayerState()?.activePokemon;
          @let myActiveEnergy = myActive?.attachedCards ?? [];
          @let myActiveEnergyIds = myActive?.attachedEnergyInstanceIds ?? [];
          <div class="fixed bottom-0 left-0 right-0 z-[160] bg-slate-800 border-t-2 border-orange-500 px-6 py-3 flex flex-col items-center gap-3 shadow-lg animate-slide-up">
            <span class="text-slate-200 text-sm font-bold">Seleccioná energías propias para descartar:</span>
            <div class="flex gap-3 flex-wrap justify-center">
              @for (energyType of myActiveEnergy; track $index) {
                @let energyId = myActiveEnergyIds[$index] ?? energyType;
                <button
                  class="px-3 py-2 rounded text-xs font-bold cursor-pointer border-2 transition-all"
                  [class.bg-orange-600]="selfSelectedEnergyDiscard().includes(energyId)"
                  [class.bg-slate-700]="!selfSelectedEnergyDiscard().includes(energyId)"
                  [class.border-orange-400]="selfSelectedEnergyDiscard().includes(energyId)"
                  [class.border-slate-500]="!selfSelectedEnergyDiscard().includes(energyId)"
                  (click)="onToggleSelfEnergyDiscard(energyId)"
                >{{ energyType }}</button>
              }
            </div>
            <div class="flex gap-2">
              @if (pendingAttack()?.isOptionalDiscard) {
                <button class="px-4 py-1.5 text-sm font-bold rounded bg-slate-500 text-white cursor-pointer border-none hover:opacity-90" (click)="onSkipSelfEnergyDiscard()">Omitir</button>
              }
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-slate-600 text-white cursor-pointer border-none" (click)="onCancelBenchTargets()">Cancelar</button>
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-orange-500 text-white cursor-pointer border-none disabled:opacity-50" [disabled]="selfSelectedEnergyDiscard().length === 0" (click)="onConfirmSelfEnergyDiscard()">Confirmar</button>
            </div>
          </div>
        }

        @if (switchPending()) {
          @let switchBench = pendingAttack()?.isSelfSwitch ? (myPlayerState()?.bench ?? []) : (opponentPlayerState()?.bench ?? []);
          <div class="fixed bottom-0 left-0 right-0 z-[160] bg-slate-800 border-t-2 border-purple-500 px-6 py-3 flex flex-col items-center gap-3 shadow-lg animate-slide-up">
            <span class="text-slate-200 text-sm font-bold">Seleccioná Pokémon del banco para intercambiar:</span>
            <div class="flex gap-3 flex-wrap justify-center">
              @for (slot of switchBench; track slot.instanceId) {
                @let slotName = cardRepo.getFromCache(slot.cardId)?.name ?? slot.cardId;
                <button
                  class="px-3 py-2 rounded text-xs font-bold cursor-pointer border-2 transition-all"
                  [class.bg-purple-600]="selectedSwitchTarget() === slot.instanceId"
                  [class.bg-slate-700]="selectedSwitchTarget() !== slot.instanceId"
                  [class.border-purple-400]="selectedSwitchTarget() === slot.instanceId"
                  [class.border-slate-500]="selectedSwitchTarget() !== slot.instanceId"
                  (click)="onSelectSwitchTarget(slot.instanceId)"
                >{{ slotName }}</button>
              }
            </div>
            <div class="flex gap-2">
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-slate-600 text-white cursor-pointer border-none" (click)="onCancelBenchTargets()">Cancelar</button>
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-purple-500 text-white cursor-pointer border-none disabled:opacity-50" [disabled]="!selectedSwitchTarget()" (click)="onConfirmSwitchTarget()">Confirmar</button>
            </div>
          </div>
        }

        @if (attackRestrictionPending()) {
          @let opponentPkm = opponentPlayerState()?.activePokemon;
          @let opponentDef = opponentPkm ? cardRepo.getFromCache(opponentPkm.cardId) : null;
          @let opponentAttacks = opponentDef?.attacks ?? [];
          <div class="fixed bottom-0 left-0 right-0 z-[160] bg-slate-800 border-t-2 border-orange-500 px-6 py-3 flex flex-col items-center gap-3 shadow-lg animate-slide-up">
            <span class="text-slate-200 text-sm font-bold">Elegí un ataque del rival para impedir:</span>
            <div class="flex gap-3 flex-wrap justify-center">
              @for (atk of opponentAttacks; track atk.index) {
                <button
                  class="px-3 py-2 rounded text-xs font-bold cursor-pointer border-2 transition-all"
                  [class.bg-orange-600]="selectedRestrictedAttack() === atk.name"
                  [class.bg-slate-700]="selectedRestrictedAttack() !== atk.name"
                  [class.border-orange-400]="selectedRestrictedAttack() === atk.name"
                  [class.border-slate-500]="selectedRestrictedAttack() !== atk.name"
                  (click)="onSelectRestrictedAttack(atk.name)"
                >{{ atk.name }}</button>
              }
            </div>
            <div class="flex gap-2">
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-slate-600 text-white cursor-pointer border-none" (click)="onCancelBenchTargets()">Cancelar</button>
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-orange-500 text-white cursor-pointer border-none disabled:opacity-50" [disabled]="!selectedRestrictedAttack()" (click)="onConfirmRestrictedAttack()">Confirmar</button>
            </div>
          </div>
        }

        @if (moveEnergyPending()) {
          @let sourcePkm = isMoveFromAttacker() ? (myPlayerState()?.activePokemon) : (opponentPlayerState()?.activePokemon);
          @let sourceEnergies = sourcePkm?.attachedCards ?? [];
          @let sourceEnergyIds = sourcePkm?.attachedEnergyInstanceIds ?? [];
          <div class="fixed bottom-0 left-0 right-0 z-[160] bg-slate-800 border-t-2 border-teal-500 px-6 py-3 flex flex-col items-center gap-3 shadow-lg animate-slide-up">
            <span class="text-slate-200 text-sm font-bold">Seleccioná energía para mover:</span>
            <div class="flex gap-3 flex-wrap justify-center">
              @for (energyType of sourceEnergies; track $index) {
                @let energyId = sourceEnergyIds[$index] ?? energyType;
                <button
                  class="px-3 py-2 rounded text-xs font-bold cursor-pointer border-2 transition-all"
                  [class.bg-teal-600]="moveSelectedEnergy().includes(energyId)"
                  [class.bg-slate-700]="!moveSelectedEnergy().includes(energyId)"
                  [class.border-teal-400]="moveSelectedEnergy().includes(energyId)"
                  [class.border-slate-500]="!moveSelectedEnergy().includes(energyId)"
                  (click)="onToggleMoveEnergy(energyId)"
                >{{ energyType }}</button>
              }
            </div>
            <div class="flex gap-2">
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-slate-600 text-white cursor-pointer border-none" (click)="onCancelBenchTargets()">Cancelar</button>
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-teal-500 text-white cursor-pointer border-none disabled:opacity-50" [disabled]="moveSelectedEnergy().length === 0" (click)="onConfirmMoveEnergy()">Confirmar</button>
            </div>
          </div>
        }

        @if (moveBenchPending()) {
          @let destBench = isMoveFromAttacker() ? (myPlayerState()?.bench ?? []) : (opponentPlayerState()?.bench ?? []);
          <div class="fixed bottom-0 left-0 right-0 z-[160] bg-slate-800 border-t-2 border-teal-500 px-6 py-3 flex flex-col items-center gap-3 shadow-lg animate-slide-up">
            <span class="text-slate-200 text-sm font-bold">Seleccioná Pokémon del banco destino:</span>
            <div class="flex gap-3 flex-wrap justify-center">
              @for (slot of destBench; track slot.instanceId) {
                @let slotName = cardRepo.getFromCache(slot.cardId)?.name ?? slot.cardId;
                <button
                  class="px-3 py-2 rounded text-xs font-bold cursor-pointer border-2 transition-all"
                  [class.bg-teal-600]="moveBenchTarget() === slot.instanceId"
                  [class.bg-slate-700]="moveBenchTarget() !== slot.instanceId"
                  [class.border-teal-400]="moveBenchTarget() === slot.instanceId"
                  [class.border-slate-500]="moveBenchTarget() !== slot.instanceId"
                  (click)="onSelectMoveBenchTarget(slot.instanceId)"
                >{{ slotName }}</button>
              }
            </div>
            <div class="flex gap-2">
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-slate-600 text-white cursor-pointer border-none" (click)="onCancelBenchTargets()">Cancelar</button>
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-teal-500 text-white cursor-pointer border-none disabled:opacity-50" [disabled]="!moveBenchTarget()" (click)="onConfirmMoveBench()">Confirmar</button>
            </div>
          </div>
        }

        @if (_abilityMoveEnergyPending()) {
          @let abilityPlayerPkm = getPlayerPokemonList();
          <div class="fixed bottom-0 left-0 right-0 z-[160] bg-slate-800 border-t-2 border-purple-500 px-6 py-3 flex flex-col items-center gap-3 shadow-lg animate-slide-up">
            <span class="text-slate-200 text-sm font-bold">Seleccioná el Pokémon que tiene la Energía Fairy a transferir:</span>
            <div class="flex gap-3 flex-wrap justify-center">
              @for (slot of abilityPlayerPkm; track slot.instanceId) {
                @let slotName = cardRepo.getFromCache(slot.cardId)?.name ?? slot.cardId;
                <button
                  class="px-3 py-2 rounded text-xs font-bold cursor-pointer border-2 transition-all"
                  [class.bg-purple-600]="_abilityMoveSelectedSource() === slot.instanceId"
                  [class.bg-slate-700]="_abilityMoveSelectedSource() !== slot.instanceId"
                  [class.border-purple-400]="_abilityMoveSelectedSource() === slot.instanceId"
                  [class.border-slate-500]="_abilityMoveSelectedSource() !== slot.instanceId"
                  (click)="onSelectAbilitySourceSlot(slot.instanceId)"
                >{{ slotName }}</button>
              }
            </div>
            <div class="flex gap-2">
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-slate-600 text-white cursor-pointer border-none" (click)="onCancelAbility()">Cancelar</button>
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-purple-500 text-white cursor-pointer border-none disabled:opacity-50" [disabled]="!_abilityMoveSelectedSource()" (click)="onConfirmAbilitySourceSlot()">Confirmar</button>
            </div>
          </div>
        }

        @if (_abilityMoveBenchPending()) {
          @let abilityDestPkm = getPlayerPokemonList();
          <div class="fixed bottom-0 left-0 right-0 z-[160] bg-slate-800 border-t-2 border-purple-500 px-6 py-3 flex flex-col items-center gap-3 shadow-lg animate-slide-up">
            <span class="text-slate-200 text-sm font-bold">Seleccioná el Pokémon destino de la Energía Fairy:</span>
            <div class="flex gap-3 flex-wrap justify-center">
              @for (slot of abilityDestPkm; track slot.instanceId) {
                @if (slot.instanceId !== _abilityMoveSourcePkmId()) {
                  @let slotName = cardRepo.getFromCache(slot.cardId)?.name ?? slot.cardId;
                  <button
                    class="px-3 py-2 rounded text-xs font-bold cursor-pointer border-2 transition-all"
                    [class.bg-purple-600]="_abilityMoveBenchTarget() === slot.instanceId"
                    [class.bg-slate-700]="_abilityMoveBenchTarget() !== slot.instanceId"
                    [class.border-purple-400]="_abilityMoveBenchTarget() === slot.instanceId"
                    [class.border-slate-500]="_abilityMoveBenchTarget() !== slot.instanceId"
                    (click)="onSelectAbilityMoveBenchTarget(slot.instanceId)"
                  >{{ slotName }}</button>
                }
              }
            </div>
            <div class="flex gap-2">
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-slate-600 text-white cursor-pointer border-none" (click)="onCancelAbility()">Cancelar</button>
              <button class="px-4 py-1.5 text-sm font-bold rounded bg-purple-500 text-white cursor-pointer border-none disabled:opacity-50" [disabled]="!_abilityMoveBenchTarget()" (click)="onConfirmAbilityMoveBench()">Confirmar</button>
            </div>
          </div>
        }

        @if (optionalBonusPending()) {
          <div class="fixed bottom-0 left-0 right-0 z-[160] bg-slate-800 border-t-2 border-amber-500 px-6 py-3 flex flex-col items-center gap-3 shadow-lg animate-slide-up">
            <span class="text-slate-200 text-sm font-bold">¿Querés aplicar el bonus de daño adicional?</span>
            <span class="text-slate-400 text-xs">Si lo hacés, también se aplicará el costo asociado (autodaño, descarte, etc.).</span>
            <div class="flex gap-3">
              <button class="px-5 py-2 text-sm font-bold rounded bg-green-600 text-white cursor-pointer border-none hover:opacity-90" (click)="onConfirmOptionalBonus(true)">Sí</button>
              <button class="px-5 py-2 text-sm font-bold rounded bg-red-600 text-white cursor-pointer border-none hover:opacity-90" (click)="onConfirmOptionalBonus(false)">No</button>
            </div>
          </div>
        }

        @if (_pendingTrainerPlay() || selectionState().mode === 'SELECT_NEW_ACTIVE') {
          <div class="fixed bottom-0 left-0 right-0 z-[150] bg-slate-800 border-t-2 border-blue-400 px-6 py-3 flex flex-row items-center justify-between gap-4 shadow-lg animate-slide-up">
            <span class="text-slate-200 text-[0.9375rem]">
              @if (selectionState().mode === 'SELECT_NEW_ACTIVE') {
                Elegí qué Pokémon de Banca pasará a ser el Activo
              } @else {
                Seleccioná un Pokémon para aplicar la carta de Entrenador
              }
            </span>
            <button class="px-4 py-1.5 text-sm font-bold rounded bg-slate-600 text-white cursor-pointer border-none" (click)="onCancelTrainerPlay()">Cancelar</button>
          </div>
        }

        @if (_pendingTrainerConfirm(); as confirm) {
          <div class="fixed bottom-0 left-0 right-0 z-[150] bg-slate-800 border-t-2 border-amber-400 px-6 py-3 flex flex-col items-center gap-3 shadow-lg animate-slide-up">
            <span class="text-slate-200 text-sm font-bold">¿Querés aplicar {{ confirm.cardName }}?</span>
            <span class="text-slate-400 text-xs">
              @if (confirm.cardName === 'Professor Sycamore') {
                Se descartará toda tu mano.
              } @else {
                Se mezclará tu mano en el mazo y robarás 5 cartas.
              }
            </span>
            <div class="flex gap-3">
              <button class="px-5 py-2 text-sm font-bold rounded bg-green-600 text-white cursor-pointer border-none hover:opacity-90" (click)="onConfirmTrainerPlay(true)">Sí</button>
              <button class="px-5 py-2 text-sm font-bold rounded bg-red-600 text-white cursor-pointer border-none hover:opacity-90" (click)="onConfirmTrainerPlay(false)">No</button>
            </div>
          </div>
        }

        @if (showInitialMulliganDialog()) {
          <div class="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-[200] bg-[var(--pk-surface)] border-2 border-amber-400 rounded-xl px-8 py-6 flex flex-col items-center gap-4 shadow-2xl max-w-md w-full">
            <p class="text-[var(--pk-text)] text-center text-sm leading-relaxed m-0">
              Tu mano no tiene Pokémon Básico.<br />
              Si hacés <strong class="text-amber-400">mulligan</strong>, tu oponente podrá robar una carta extra.
            </p>
            <button class="pk-btn text-base font-bold px-8 py-3" (click)="onInitialMulliganDecision('MULLIGAN')">Hacer mulligan</button>
          </div>
        }

        @if (showMulliganDrawDialog()) {
          <div class="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-[200] bg-[var(--pk-surface)] border-2 border-amber-400 rounded-xl px-8 py-6 flex flex-col items-center gap-4 shadow-2xl max-w-md w-full">
            <p class="text-[var(--pk-text)] text-center text-sm leading-relaxed m-0">
              El rival hizo mulligan. Tenés <strong class="text-amber-400">{{ handCardCount() }}</strong> carta(s) en mano.<br />
              ¿Querés robar <strong class="text-amber-400">{{ mulliganDrawCount() }}</strong> carta(s) extra?
            </p>
            <span
              class="font-mono text-lg font-bold"
              [class]="mulliganDrawTimeLeft() > 0 && mulliganDrawTimeLeft() <= 10000 ? 'text-red-500 animate-pulse' : 'text-slate-400'"
            >{{ mulliganDrawTimeLeftFormatted() }}</span>
            <div class="flex gap-3">
              <button class="pk-btn text-base font-bold px-8 py-3 !bg-green-600 !border-green-500 hover:!bg-green-500" (click)="onMulliganDrawDecision(true)">Robar</button>
              <button class="pk-btn text-base font-bold px-8 py-3" (click)="onMulliganDrawDecision(false)">No robar</button>
            </div>
          </div>
        }

        @if (state.status === 'SETUP'; as priv) {
          <app-setup-overlay
            [myPlayerState]="myPlayerState()"
            [opponentSetupConfirmed]="opponentSetupConfirmed()"
            [mySetupConfirmed]="mySetupConfirmed()"
            [mulliganDrawPending]="showMulliganDrawDialog()"
            [initialMulliganPending]="showInitialMulliganDialog()"
            [cardDefs]="allCardDefs()"
            (activeDropped)="onActiveDropped($event)"
            (benchDropped)="onBenchDropped($event)"
            (fieldCardRemoved)="onFieldDragStarted($event)"
            (confirmSetup)="onConfirmSetup()"
          />
        }

        @if (viewingDiscard(); as viewer) {
          <app-discard-viewer
            [discard]="viewer.cards"
            [discardCount]="viewer.count"
            [selectable]="!!_pendingDiscardSelection()"
            (cardSelected)="onDiscardCardSelected($event)"
            (close)="closeDiscardViewer()" />
        }
        @if (_showDeckViewer(); as deckViewer) {
          <app-deck-viewer
            [deck]="deckViewer.cards"
            [title]="deckViewer.title"
            [selectionMode]="deckViewer.selectionMode"
            [allowedSupertype]="deckViewer.allowedSupertype"
            (cardSelected)="onDeckCardSelected($event)"
            (cardsSelected)="onDeckCardsSelected($event)"
            (close)="_showDeckViewer.set(null); _pendingDeckSelection.set(null)" />
        }
      </div></main>
    } @else {
      <main class="relative flex flex-col items-center justify-center min-h-screen gap-6">
        <app-back-button (beforeBack)="onBackFromWaiting()" />
        <app-waiting-panel
          [playerName]="matchFacade.playerName() || authService.player()?.displayName || 'Jugador'"
          [deckName]="matchFacade.deckName() || '—'"
          (cancelMatch)="onCancelMatch()"
        />
      </main>
    }
    <app-card-preview-overlay />
    <app-chat-box [matchId]="matchId ?? ''" />

    @if (_attackFlash(); as flash) {
      <div
        class="fixed inset-0 z-[500] pointer-events-none animate-attack-flash"
        [style.background]="flashBg(_attackFlashType())"
      ></div>
    }
    @if (_koDarken()) {
      <div class="fixed inset-0 z-[500] pointer-events-none animate-ko-darken bg-black"></div>
    }
    @if (_flyingCard(); as flyCard) {
      <div class="fixed bottom-36 left-1/2 -translate-x-1/2 z-[600] pointer-events-none">
        <img [src]="flyCard.imageUrl" alt="{{ flyCard.name }}"
             class="w-24 h-auto sm:w-32 rounded-lg shadow-2xl animate-card-to-discard" />
      </div>
    }
  `,

  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MatchPage implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  protected readonly authService = inject(AuthService);
  protected readonly cardRepo = inject(CardRepositoryService);
  private readonly audioService = inject(AudioService);
  private readonly matchApi = inject(MatchApiService);
  private readonly dispatcher = inject(GameActionDispatcherService);
  private readonly toastService = inject(ToastService);
  protected readonly matchState = inject(MatchStateService);
  protected readonly interactionService = inject(MatchInteractionService);
  protected readonly matchFacade = inject(MatchFacadeService);

  protected matchId: string | null = null;
  private localPlayerId: string | null = null;
  private leaveNormally = false;
  private readonly destroyRef = inject(DestroyRef);
  private mulliganTimerInterval: ReturnType<typeof setInterval> | null = null;
  protected readonly reconnecting = signal(false);

  readonly timeOfDay = computed(() => {
    const h = new Date().getHours();
    if (h >= 6 && h < 12) return 'morning';
    if (h >= 12 && h < 18) return 'afternoon';
    return 'night';
  });

  readonly boardBg = computed(() => {
    const stadium = this.matchState.stadiumBg();
    if (stadium) return `url('${stadium}')`;
    return `url(assets/images/board-${this.timeOfDay()}.png)`;
  });

  protected readonly _pendingTrainerPlay = signal<{ handIndex: number; effectCode: string } | null>(null);
  protected readonly _pendingTrainerConfirm = signal<{ handIndex: number; cardName: string } | null>(null);
  protected readonly _pendingDiscardSelection = signal<{ handIndex: number } | null>(null);
  protected readonly _pendingDeckSelection = signal<{ handIndex: number; effectCode: string } | null>(null);
  protected readonly _showDeckViewer = signal<{
    cards: PrivateHandCardModel[];
    title: string;
    selectionMode: 'single' | 'multi';
    allowedSupertype: string | null;
  } | null>(null);

  readonly myPlayerState = computed(() => {
    const state = this.matchState.publicState();
    const myId = this.matchState.myPlayerId();
    if (!state || !myId) return null;
    return state.players.find(p => p.playerId === myId) ?? null;
  });

  readonly opponentPlayerState = computed(() => {
    const state = this.matchState.publicState();
    const myId = this.matchState.myPlayerId();
    if (!state || !myId) return null;
    return state.players.find(p => p.playerId !== myId) ?? null;
  });

  private readonly _viewingDiscard = signal<{ playerId: string; cards: PublicDiscardCardModel[]; count: number } | null>(null);
  readonly viewingDiscard = this._viewingDiscard.asReadonly();

  openDiscardViewer(playerId: string): void {
    const player = this.matchState.publicState()?.players.find(p => p.playerId === playerId);
    if (!player) return;
    this._viewingDiscard.set({
      playerId,
      cards: player.discard ?? [],
      count: player.discardCount ?? 0,
    });
  }

  closeDiscardViewer(): void {
    this._pendingDiscardSelection.set(null);
    this._viewingDiscard.set(null);
  }

  readonly opponentSetupConfirmed = computed(() => {
    const opponent = this.opponentPlayerState();
    return opponent?.setupConfirmed ?? false;
  });

  readonly selectionState = computed(() => this.interactionService.selection());

  protected readonly opponentSelectionMode = computed(() => {
    const mode = this.selectionState().mode;
    if (mode === 'SELECT_TARGET_POKEMON') {
      const pending = this._pendingTrainerPlay();
      if (pending?.effectCode === 'TEAM_FLARE_GRUNT') {
        return mode;
      }
      return 'NONE';
    }
    if (
      mode === 'SELECT_BENCH_SLOT' ||
      mode === 'SELECT_RETREAT_TARGET' ||
      mode === 'SELECT_ENERGIES_TO_DISCARD' ||
      mode === 'SELECT_NEW_ACTIVE'
    ) {
      return 'NONE';
    }
    return mode;
  });

  readonly canDragToBench = computed(() => {
    const state = this.matchState.publicState();
    if (!state) return false;
    return state.status === 'ACTIVE'
        && state.phase === 'MAIN'
        && this.matchState.isMyTurn();
  });

  protected readonly handZoneDroplistId = 'hand-zone-droplist';
  protected readonly benchSlotDroplistIds = computed(() => {
    const ids = ['active-slot'];
    for (let i = 0; i < 5; i++) {
      ids.push('bench-slot-' + i);
    }
    return ids;
  });

  private readonly allPokemonInstanceIds = computed(() => {
    const me = this.myPlayerState();
    if (!me) return [];
    const ids: string[] = [];
    if (me.activePokemon) ids.push(me.activePokemon.instanceId);
    for (const poke of me.bench) {
      if (poke) ids.push(poke.instanceId);
    }
    return ids;
  });

  private readonly pokemonWithoutToolInstanceIds = computed(() => {
    const me = this.myPlayerState();
    if (!me) return [];
    const ids: string[] = [];
    if (me.activePokemon && !me.activePokemon.attachedToolCardInstanceId) {
      ids.push(me.activePokemon.instanceId);
    }
    for (const poke of me.bench) {
      if (poke && !poke.attachedToolCardInstanceId) ids.push(poke.instanceId);
    }
    return ids;
  });

  private readonly opponentPokemonInstanceIds = computed(() => {
    const opponent = this.opponentPlayerState();
    if (!opponent) return [];
    const ids: string[] = [];
    if (opponent.activePokemon) ids.push(opponent.activePokemon.instanceId);
    for (const poke of opponent.bench) {
      if (poke) ids.push(poke.instanceId);
    }
    return ids;
  });

  private findPublicPokemon(instanceId: string): import('../../../../shared/models/game-state.models').PublicPokemonSlotModel | null {
    const state = this.matchState.publicState();
    if (!state) return null;
    const myId = this.matchState.myPlayerId();
    const me = state.players.find(p => p.playerId === myId);
    if (!me) return null;
    if (me.activePokemon?.instanceId === instanceId) return me.activePokemon;
    return me.bench.find(p => p?.instanceId === instanceId) ?? null;
  }

  private readonly benchInstanceIds = computed(() => {
    const me = this.myPlayerState();
    if (!me) return [];
    return me.bench.filter((p): p is NonNullable<typeof p> => p !== null).map(p => p.instanceId);
  });

  readonly allCardDefs = computed(() => {
    const map = new Map<string, CardDetailResponse | null>();
    const state = this.matchState.publicState();
    const priv = this.matchState.privateState();
    if (state) {
      for (const player of state.players) {
        if (player.activePokemon) map.set(player.activePokemon.cardId, this.cardRepo.getFromCache(player.activePokemon.cardId));
        for (const poke of player.bench) {
          if (poke) map.set(poke.cardId, this.cardRepo.getFromCache(poke.cardId));
        }
      }
    }
    if (priv?.hand) {
      for (const card of priv.hand) {
        if (!map.has(card.cardId)) map.set(card.cardId, this.cardRepo.getFromCache(card.cardId));
      }
    }
    return map;
  });

  readonly mySetupConfirmed = computed(() => {
    const me = this.myPlayerState();
    return me?.setupConfirmed ?? false;
  });

  private readonly checkCost = (cost: string[], energies: string[]): boolean => {
    const upper = (s: string) => s?.toUpperCase() ?? '';
    const missing = cost.map(upper).filter(e => e !== 'COLORLESS');
    let colorless = cost.filter(e => upper(e) === 'COLORLESS').length;
    for (const e of energies) {
      const ut = upper(e);
      const idx = missing.indexOf(ut);
      if (idx !== -1) {
        missing.splice(idx, 1);
      } else if (colorless > 0) {
        colorless--;
      }
    }
    return missing.length === 0 && colorless === 0;
  };

  private readonly ATTACK_TYPE_COLORS: Record<string, string> = {
    GRASS: '#166534', FIRE: '#991b1b', WATER: '#1e3a8a',
    LIGHTNING: '#854d0e', PSYCHIC: '#6b21a8', FIGHTING: '#92400e',
    DARKNESS: '#2d0a3a', METAL: '#475569', FAIRY: '#9d174d',
    DRAGON: '#7c2d12', COLORLESS: '#475569',
  };

  protected attackBg(type: string): string | null {
    return this.ATTACK_TYPE_COLORS[type?.toUpperCase()] ?? null;
  }

  private readonly FLASH_COLORS: Record<string, string> = {
    GRASS: 'radial-gradient(circle, rgba(34,197,94,0.85) 0%, transparent 70%)',
    FIRE: 'radial-gradient(circle, rgba(239,68,68,0.85) 0%, transparent 70%)',
    WATER: 'radial-gradient(circle, rgba(59,130,246,0.85) 0%, transparent 70%)',
    LIGHTNING: 'radial-gradient(circle, rgba(234,179,8,0.85) 0%, transparent 70%)',
    PSYCHIC: 'radial-gradient(circle, rgba(168,85,247,0.85) 0%, transparent 70%)',
    FIGHTING: 'radial-gradient(circle, rgba(217,119,6,0.85) 0%, transparent 70%)',
    DARKNESS: 'radial-gradient(circle, rgba(74,4,78,0.85) 0%, transparent 70%)',
    METAL: 'radial-gradient(circle, rgba(156,163,175,0.85) 0%, transparent 70%)',
    FAIRY: 'radial-gradient(circle, rgba(244,114,182,0.85) 0%, transparent 70%)',
    DRAGON: 'radial-gradient(circle, rgba(245,158,11,0.85) 0%, transparent 70%)',
    COLORLESS: 'radial-gradient(circle, rgba(209,213,219,0.5) 0%, transparent 70%)',
  };

  protected flashBg(type: string | null): string {
    if (!type) return 'transparent';
    return this.FLASH_COLORS[type?.toUpperCase()] ?? 'transparent';
  }

  readonly showAttackBar = computed(() => {
    const state = this.matchState.publicState();
    if (!state) return false;
    if (state.status !== 'ACTIVE' || state.phase !== 'MAIN') return false;
    if (!this.matchState.isMyTurn()) return false;
    if (this.needsKOReplacement()) return false;
    if (state.turnNumber === 1 && state.currentPlayerId === state.firstPlayerId) return false;
    const active = this.matchState.myActivePokemon();
    if (!active) return false;
    const cardDef = this.cardRepo.getFromCache(active.cardId);
    if (!cardDef?.attacks?.length) return false;
    return true;
  });

  readonly attacksWithAvailability = computed(() => {
    const active = this.matchState.myActivePokemon();
    if (!active) return [];
    const cardDef = this.cardRepo.getFromCache(active.cardId);
    if (!cardDef?.attacks) return [];
    const energies = this.matchState.activePokemonEnergyTypes();
    const pokemonTypes = cardDef.types ?? [];
    return cardDef.attacks.map(attack => ({
      index: attack.index,
      name: attack.name,
      cost: attack.cost ?? [],
      damage: attack.damage,
      text: attack.text,
      available: this.checkCost(attack.cost ?? [], energies),
      pokemonType: pokemonTypes[0] ?? 'COLORLESS',
    }));
  });

  readonly anySelectionPending = computed(() =>
    this.benchDamagePending() || this.benchAttachPending() || this.benchHealPending() ||
    this.energyDiscardPending() || this.selfEnergyDiscardPending() ||
    this.switchPending() || this.moveEnergyPending() || this.moveBenchPending() ||
    this.optionalBonusPending() || this.attackRestrictionPending()
  );

  readonly retreatInfo = computed(() => {
    const active = this.matchState.myActivePokemon();
    if (!active) return null;
    const cardDef = this.cardRepo.getFromCache(active.cardId);
    if (!cardDef?.retreatCost) return null;
    const attached = this.matchState.activePokemonEnergyTypes();

    const state = this.matchState.publicState();
    // Already retreated this turn
    if (state?.hasRetreated) {
      return { cost: cardDef.retreatCost, canRetreat: false };
    }
    const stadiumDefId = state?.stadiumCardDefinitionId ?? null;
    if (stadiumDefId) {
      const stadiumDef = this.cardRepo.getFromCache(stadiumDefId);
      if (stadiumDef?.name === 'Fairy Garden' && attached.includes('FAIRY')) {
        return { cost: [] as string[], canRetreat: true };
      }
    }

    const blockedByCondition = active.specialConditions?.includes('ASLEEP')
      || active.specialConditions?.includes('PARALYZED');
    const canRetreat = !blockedByCondition && this.checkCost(cardDef.retreatCost, attached);
    return { cost: cardDef.retreatCost, canRetreat };
  });

  readonly activeAbilityInfo = computed(() => {
    const active = this.matchState.myActivePokemon();
    if (!active) return null;
    const cardDef = this.cardRepo.getFromCache(active.cardId);
    const ability = cardDef?.abilities?.[0];
    if (!ability) return null;
    const canUse = ability.isActivable
      && this.matchState.isMyTurn()
      && this.matchState.currentPhase() === 'MAIN'
      && !active.specialConditions?.includes('ASLEEP')
      && !active.specialConditions?.includes('PARALYZED');
    return {
      name: ability.name,
      text: ability.text,
      isActivable: ability.isActivable,
      canUse,
    };
  });

  protected onAbilityButtonClick(): void {
    if (this.pendingAttack()) return;
    if (this.interactionService.isSelecting()) return;
    const active = this.matchState.myActivePokemon();
    if (!active) return;
    this.onPokemonAbilityClicked({ instanceId: active.instanceId });
  }

  readonly mulliganMessages = computed(() => {
    const state = this.matchState.publicState();
    if (!state || state.status !== 'SETUP') return [];
    const myId = this.matchState.myPlayerId();
    if (!myId) return [];
    const msgs: string[] = [];
    for (const player of state.players) {
      const count = player.mulliganCount ?? 0;
      if (count <= 0) continue;
      if (player.playerId === myId) {
        msgs.push(`Hiciste mulligan — tu mano fue reemplazada`);
      } else {
        msgs.push(`El oponente hizo mulligan`);
      }
    }
    return msgs;
  });

  protected readonly _boardSplash = signal(false);
  readonly coinFlipPhase = signal<'spinning' | 'result' | null>(null);
  readonly coinFlipWinnerIsMe = signal(false);
  readonly attackCoinFlip = signal<'HEADS' | 'TAILS' | null>(null);
  readonly pendingAttack = signal<PendingAttackData | null>(null);
  readonly benchDamagePending = signal(false);
  readonly benchAttachPending = signal(false);
  readonly benchHealPending = signal(false);
  readonly energyDiscardPending = signal(false);
  readonly selfEnergyDiscardPending = signal(false);
  readonly selfSelectedEnergyDiscard = signal<string[]>([]);
  readonly switchPending = signal(false);
  readonly selectedSwitchTarget = signal<string | null>(null);
  readonly selectedBenchTargets = signal<string[]>([]);
  readonly selectedHealTarget = signal<string | null>(null);
  readonly selectedEnergyDiscard = signal<string[]>([]);
  readonly moveEnergyPending = signal(false);
  readonly isMoveFromAttacker = signal(false);
  readonly moveSelectedEnergy = signal<string[]>([]);
  readonly moveBenchPending = signal(false);
  readonly moveBenchTarget = signal<string | null>(null);
  readonly optionalBonusPending = signal(false);
  readonly useOptionalBonus = signal(true);

  readonly attackRestrictionPending = signal(false);
  readonly selectedRestrictedAttack = signal<string | null>(null);
  protected readonly _koModalDismissed = signal(false);
  protected readonly _opponentKoModalDismissed = signal(false);

  // Ability signals
  protected readonly _pendingAbility = signal<{
    pokemonInstanceId: string;
    abilityName: string;
  } | null>(null);

  protected readonly _pendingAbilityFlow = signal<{
    pokemonInstanceId: string;
    abilityName: string;
    selectedEnergyInstanceId?: string;
    step: 'select_energy' | 'select_target';
  } | null>(null);

  protected readonly _abilityMoveEnergyPending = signal(false);
  protected readonly _abilityMoveBenchPending = signal(false);
  protected readonly _abilityMoveSelectedSource = signal<string | null>(null);
  protected readonly _abilityMoveSourcePkmId = signal<string | null>(null);
  protected readonly _abilityMoveBenchTarget = signal<string | null>(null);

  protected readonly _isMatchMenuOpen = signal(false);

  protected readonly _screenShake = signal(false);
  protected readonly _attackFlash = signal<'own' | 'opponent' | null>(null);
  protected readonly _attackFlashType = signal<string | null>(null);
  protected readonly _koDarken = signal(false);
  protected readonly _flyingCard = signal<{ name: string; imageUrl: string } | null>(null);

  protected openMatchMenu(): void {
    this._isMatchMenuOpen.set(true);
  }

  protected closeMatchMenu(): void {
    this._isMatchMenuOpen.set(false);
  }

  protected onMenuToggle(): void {
    this._isMatchMenuOpen.update(v => !v);
  }

  protected onConcede(matchId: string | undefined): void {
    if (!matchId) return;
    const myId = this.matchState.myPlayerId();
    if (!myId) return;
    this.matchApi.concedeMatch(matchId, myId).subscribe({
      next: () => this.onReturnToLobby(),
      error: () => this.onReturnToLobby(),
    });
  }

  readonly mulliganDrawCount = computed(() => {
    return this.matchState.privateState()?.pendingMulliganDrawCount ?? 0;
  });

  readonly showInitialMulliganDialog = computed(() => {
    const state = this.matchState.publicState();
    const myId = this.matchState.myPlayerId();
    if (!state || state.status !== 'SETUP' || !myId) return false;
    return state.pendingInitialMulliganPlayers?.includes(myId) ?? false;
  });

  readonly showMulliganDrawDialog = computed(() => {
    const state = this.matchState.publicState();
    if (!state || state.status !== 'SETUP') return false;
    return state.mulliganDrawPending === true && this.mulliganDrawCount() > 0;
  });

  readonly handCardCount = computed(() => {
    return this.matchState.privateState()?.hand?.length ?? 0;
  });

  readonly mulliganDrawDeadlineMs = computed(() => {
    const deadline = this.matchState.publicState()?.mulliganDrawDeadline;
    if (!deadline) return 0;
    return new Date(deadline).getTime();
  });

  readonly mulliganDrawTimeLeft = signal(0);

  readonly mulliganDrawTimeLeftFormatted = computed(() => {
    const ms = this.mulliganDrawTimeLeft();
    if (ms <= 0) return '—';
    const secs = Math.ceil(ms / 1000);
    return `${secs}s`;
  });

  private previousStatus: string | null = null;
  private previousPendingPrize: string | null | undefined = null;

  readonly needsKOReplacement = computed(() => {
    const state = this.matchState.publicState();
    const myId = this.matchState.myPlayerId();
    if (!state || !myId) return false;
    return state.pendingKOReplacement === true && state.knockedOutPlayerId === myId;
  });

  readonly opponentNeedsKOReplacement = computed(() => {
    const state = this.matchState.publicState();
    const myId = this.matchState.myPlayerId();
    if (!state || !myId) return false;
    return state.pendingKOReplacement === true && state.knockedOutPlayerId !== myId;
  });

  readonly needsPrizeTake = computed(() => {
    const state = this.matchState.publicState();
    const myId = this.matchState.myPlayerId();
    if (!state || !myId) return false;
    return state.pendingPrizeOwnerPlayerId != null && state.pendingPrizeOwnerPlayerId === myId;
  });

  readonly koModeTargets = computed(() => {
    if (!this.needsKOReplacement()) return [];
    return this.myPlayerState()?.bench
      .filter((p): p is NonNullable<typeof p> => p != null)
      .map(p => p.instanceId) ?? [];
  });

  protected countHeads(flips: { result: 'HEADS' | 'TAILS'; flipIndex: number; totalFlips: number }[]): number {
    return flips.filter(f => f.result === 'HEADS').length;
  }

  /**
   * Returns instance IDs of Pokemon in play (active + bench) whose cardDef.name
   * matches the `evolvesFrom` of the card at the given handIndex.
   * Used for EVOLVE_POKEMON target selection.
   */
  private getEligiblePokemonInstanceIds(handIndex: number): string[] {
    const hand = this.matchState.privateState()?.hand;
    if (!hand) return [];
    const card = hand[handIndex];
    if (!card) return [];

    const cardDef = this.cardRepo.getFromCache(card.cardId);
    if (!cardDef?.evolvesFrom || !cardDef.stage || cardDef.stage === 'BASIC') return [];

    const myState = this.myPlayerState();
    if (!myState) return [];

    // No evolution on the player's first turn
    if (!myState.firstTurnCompleted) return [];

    const state = this.matchState.publicState();
    if (!state) return [];

    const eligibleIds: string[] = [];

    // Check active Pokemon
    if (myState.activePokemon) {
      const activeDef = this.cardRepo.getFromCache(myState.activePokemon.cardId);
      if (activeDef?.name === cardDef.evolvesFrom
        && !myState.activePokemon.evolvedThisTurn
        && myState.activePokemon.enteredTurnNumber !== state.turnNumber) {
        eligibleIds.push(myState.activePokemon.instanceId);
      }
    }

    // Check bench Pokemon
    for (const poke of myState.bench) {
      if (!poke) continue;
      const benchDef = this.cardRepo.getFromCache(poke.cardId);
      if (benchDef?.name === cardDef.evolvesFrom
        && !poke.evolvedThisTurn
        && poke.enteredTurnNumber !== state.turnNumber) {
        eligibleIds.push(poke.instanceId);
      }
    }

    return eligibleIds;
  }

  constructor() {
    effect(() => {
      const publicState = this.matchState.publicState();
      if (!publicState) return;

      const cardIds = new Set<string>();
      for (const player of publicState.players) {
        if (player.activePokemon) cardIds.add(player.activePokemon.cardId);
        for (const poke of player.bench) {
          if (poke) cardIds.add(poke.cardId);
        }
      }

      // Preload hand card cardDefs (needed for evolution stage check, trainer subtypes, etc.)
      const privateState = this.matchState.privateState();
      if (privateState?.hand) {
        for (const card of privateState.hand) {
          cardIds.add(card.cardId);
        }
      }
      // Preload deck card definitions (needed for Evosoda validation, deck viewer, etc.)
      if (privateState?.deck) {
        for (const card of privateState.deck) {
          cardIds.add(card.cardId);
        }
      }

      this.cardRepo.preload(Array.from(cardIds));
    });

    effect(() => {
      const publicState = this.matchState.publicState();
      if (!publicState) return;

      const status = publicState.status;
      const previous = this.previousStatus;

      if (previous === 'SETUP' && status === 'ACTIVE' && !this.coinFlipPhase()) {
        const myId = this.matchState.myPlayerId();
        if (publicState.firstPlayerId && myId) {
          this._boardSplash.set(true);
          this.audioService.playFirstStartSound();
          this.coinFlipWinnerIsMe.set(publicState.firstPlayerId === myId);
          this.coinFlipPhase.set('spinning');
          setTimeout(() => {
            this.coinFlipPhase.set('result');
            setTimeout(() => {
              this.coinFlipPhase.set(null);
              this._boardSplash.set(false);
            }, 1500);
          }, 1500);
        }
      }

      this.previousStatus = status;
    });

    effect(() => {
      const shouldTake = this.needsPrizeTake();
      const myId = this.matchState.myPlayerId();

      // Leemos status y actionInProgress DENTRO de untracked() para NO
      // agregarlos como dependencias del effect. Si publicState() o
      // actionInProgress() fueran dependencias directas, el effect se
      // re-dispararía en cada update de estado (aunque needsPrizeTake
      // no haya cambiado), causando un loop de re-intentos.
      const { status, inProgress } = untracked(() => ({
        status: this.matchState.publicState()?.status,
        inProgress: this.interactionService.actionInProgress(),
      }));

      console.warn(`[DEBUG] needsPrizeTake effect: shouldTake=${shouldTake}, status=${status}, inProgress=${inProgress}, myId=${myId}`);
      if (shouldTake && this.matchId) {
        if (myId && status === 'ACTIVE' && !inProgress) {
          console.warn('[DEBUG] → DISPATCHING TAKE_PRIZE_CARD');
          // Envolvemos dispatchAction en untracked() porque internamente
          // lee señales (connectionStatus, actionInProgress, publicState)
          // que cambiarían durante el dispatch y provocarían que el effect
          // se re-dispara inmediatamente —> loop infinito.
          const matchId = this.matchId; // type narrowing para TS fuera del untracked
          untracked(() => this.dispatcher.dispatchAction(matchId, myId, 'TAKE_PRIZE_CARD', {}));
        } else {
          console.warn(`[DEBUG] → BLOQUEADO por: ${!myId ? 'no myId' : status !== 'ACTIVE' ? `status=${status}` : 'actionInProgress'}`);
        }
      }
    });

    effect(() => {
      const deadlineMs = this.mulliganDrawDeadlineMs();
      if (deadlineMs > 0) {
        if (this.mulliganTimerInterval) clearInterval(this.mulliganTimerInterval);
        const tick = () => this.mulliganDrawTimeLeft.set(Math.max(0, deadlineMs - Date.now()));
        tick();
        this.mulliganTimerInterval = setInterval(tick, 200);
      } else {
        if (this.mulliganTimerInterval) {
          clearInterval(this.mulliganTimerInterval);
          this.mulliganTimerInterval = null;
        }
        this.mulliganDrawTimeLeft.set(0);
      }
    });

    effect(() => {
      const mode = this.selectionState().mode;
      if (mode === 'NONE' && (this._pendingAbility() !== null || this._pendingAbilityFlow() !== null)) {
        this._pendingAbility.set(null);
        this._pendingAbilityFlow.set(null);
        this._abilityMoveEnergyPending.set(false);
        this._abilityMoveBenchPending.set(false);
        this._abilityMoveSelectedSource.set(null);
        this._abilityMoveSourcePkmId.set(null);
        this._abilityMoveBenchTarget.set(null);
      }
    });

    effect(() => {
      if (!this.needsKOReplacement()) {
        this._koModalDismissed.set(false);
      }
    });

    effect(() => {
      if (!this.opponentNeedsKOReplacement()) {
        this._opponentKoModalDismissed.set(false);
      }
    });

    effect(() => {
      const events = this.matchState.events();
      if (events.length > 0) {
        console.warn('[DEBUG] events effect: total=' + events.length + ' last type=' + events[events.length - 1].type);
      }
    });

    effect(() => {
      const ownerId = this.matchState.koTrigger();
      if (ownerId) {
        this.audioService.playKoSound();
        if (ownerId === this.matchState.myPlayerId()) {
          this._koDarken.set(true);
          setTimeout(() => this._koDarken.set(false), 2500);
        }
      }
    });

  }

  ngOnInit(): void {
    const playerId = this.authService.playerId();
    if (!playerId) {
      this.router.navigate(['/lobby']);
      return;
    }
    this.localPlayerId = playerId;

    this.matchId = this.route.snapshot.params['id'] ?? null;
    if (!this.matchId) {
      this.router.navigate(['/lobby']);
      return;
    }

    this.matchState.initialize(this.matchId);

    effect(() => {
      const error = this.matchState.lastError();
      if (!error) return;
      switch (error.code) {
        case 'SUPPORTER_ALREADY_PLAYED':
          this.toastService.show('Ya usaste un Partidario este turno', 'hostile');
          break;
        case 'STADIUM_ALREADY_PLAYED':
          this.toastService.show('Ya usaste un Estadio este turno', 'hostile');
          break;
      }
    });

    const isResume = this.route.snapshot.queryParamMap.get('rejoin') === 'true';
    if (isResume) {
      this.reconnecting.set(true);

      const timer5s = setTimeout(() => this.reconnecting.set(false), 5000);
      const timer30s = setTimeout(() => this.reconnecting.set(false), 30000);

      this.destroyRef.onDestroy(() => {
        clearTimeout(timer5s);
        clearTimeout(timer30s);
      });
    }
  }

  ngOnDestroy(): void {
    if (this.mulliganTimerInterval) {
      clearInterval(this.mulliganTimerInterval);
    }
    this.matchState.reset();
  }

  protected onPokemonAbilityClicked(event: { instanceId: string }): void {
    const me = this.myPlayerState();
    const pokemon = me?.activePokemon?.instanceId === event.instanceId
      ? me.activePokemon
      : me?.bench.find(p => p?.instanceId === event.instanceId);
    if (!pokemon) return;
    const cardDef = this.cardRepo.getFromCache(pokemon.cardId);
    const activable = cardDef?.abilities?.filter(a => a.isActivable) ?? [];
    if (activable.length === 0) return;

    this.startAbilityResolution(event.instanceId, activable[0].name, activable[0].text);
  }

  private startAbilityResolution(pokemonInstanceId: string, abilityName: string, abilityText: string): void {
    const myId = this.matchState.myPlayerId();
    if (!this.matchId || !myId) return;

    this._pendingAbility.set({ pokemonInstanceId, abilityName });

    switch (abilityName) {
      case 'Mystical Fire':
      case 'Stance Change':
      case "Upside-Down Evolution":
        this.dispatcher.useAbility(this.matchId, myId, pokemonInstanceId, abilityName).subscribe();
        this._pendingAbility.set(null);
        break;
      case 'Drive Off':
        this.interactionService.enterSelectTargetPokemon(-1, this.getOpponentBenchInstanceIds());
        break;
      case 'Water Shuriken': {
        const priv = this.matchState.privateState();
        const hand = priv?.hand ?? [];
        const waterEnergies: { instanceId: string; cardId: string }[] = [];
        for (const card of hand) {
          const def = this.cardRepo.getFromCache(card.cardId);
          if (def?.supertype === 'ENERGY' && def?.name === 'Water Energy') {
            waterEnergies.push({ instanceId: card.instanceId, cardId: card.cardId });
          }
        }
        if (waterEnergies.length === 0) {
          this.toastService.show('No tenés Energía Water en la mano', 'hostile', 3000);
          this._pendingAbility.set(null);
          return;
        }
        if (waterEnergies.length >= 1) {
          if (waterEnergies.length > 1) {
            this.toastService.show('Se usará la primera Energía Water de tu mano', 'info', 2000);
          }
          this._pendingAbilityFlow.set({
            pokemonInstanceId, abilityName,
            selectedEnergyInstanceId: waterEnergies[0].instanceId,
            step: 'select_target',
          });
          this.interactionService.enterSelectTargetPokemon(-1, this.opponentPokemonInstanceIds());
        }
        break;
      }
      case "Fairy Transfer": {
        const allPokemon = this.allPokemonInstanceIds();
        if (allPokemon.length < 2) {
          this.toastService.show('Necesitás al menos 2 Pokémon para transferir energía', 'hostile', 3000);
          this._pendingAbility.set(null);
          return;
        }
        const me = this.myPlayerState();
        const hasFairy = me?.activePokemon?.attachedCards.includes('FAIRY')
          || me?.bench.some(p => p?.attachedCards.includes('FAIRY'));
        if (!hasFairy) {
          this.toastService.show('Ningún Pokémon tiene Energía Fairy para transferir', 'hostile', 3000);
          this._pendingAbility.set(null);
          return;
        }
        this._abilityMoveEnergyPending.set(true);
        break;
      }
    }
  }

  private onAbilityTargetSelected(targetInstanceId: string): void {
    const myId = this.matchState.myPlayerId();
    if (!this.matchId || !myId) return;

    const pending = this._pendingAbility();
    const flow = this._pendingAbilityFlow();
    if (!pending && !flow) return;

    if (flow && flow.step === 'select_target') {
      this.dispatcher.useAbility(this.matchId, myId, flow.pokemonInstanceId, flow.abilityName, {
        energyCardInstanceId: flow.selectedEnergyInstanceId,
        targetPokemonInstanceId: targetInstanceId,
      }).subscribe();
      this._pendingAbilityFlow.set(null);
      this._pendingAbility.set(null);
      this.interactionService.cancelSelection();
      return;
    }

    if (pending) {
      console.warn(`[ABILITY] Dispatching ${pending.abilityName} from ${pending.pokemonInstanceId} targeting ${targetInstanceId}`);
      this.dispatcher.useAbility(this.matchId, myId, pending.pokemonInstanceId, pending.abilityName, {
        targetPokemonInstanceId: targetInstanceId,
      }).subscribe({
        next: (res) => console.warn(`[ABILITY] Response: success=${res.success}`, res),
        error: (err) => console.warn(`[ABILITY] Error:`, err),
      });
      this._pendingAbility.set(null);
      this.interactionService.cancelSelection();
    }
  }

  private getOpponentBenchInstanceIds(): string[] {
    return this.opponentPlayerState()?.bench.filter(p => p != null).map(p => p!.instanceId) ?? [];
  }

  protected onPokemonClicked(event: PokemonClickedEvent): void {
    const state = this.matchState.publicState();
    if (!state || state.status === 'SETUP') return;

    const mode = this.selectionState().mode;
    console.warn(`[DEBUG] onPokemonClicked: mode=${mode}, event=`, event);

    if (this.needsKOReplacement()) {
      const myId = this.matchState.myPlayerId();
      if (!this.matchId || !myId) return;
      const targetInstanceId = 'instanceId' in event ? event.instanceId : null;
      if (!targetInstanceId) return;
      this.dispatcher.dispatchAction(this.matchId, myId, 'CHOOSE_KO_REPLACEMENT', {
        benchPokemonInstanceId: targetInstanceId,
      });
      return;
    }

    const myId = this.matchState.myPlayerId();
    if (!this.matchId || !myId) return;

    if (mode === 'SELECT_BENCH_SLOT') {
      const benchIndex = 'benchIndex' in event ? event.benchIndex : -1;
      if (benchIndex < 0) return;

      const benchSlot = this.myPlayerState()?.bench[benchIndex];
      if (benchSlot !== null && benchSlot !== undefined) return;

      const selectedHandIndex = this.selectionState().selectedHandIndex;
      if (selectedHandIndex === null) return;

      console.warn(`[DEBUG] onPokemonClicked SELECT_BENCH_SLOT: handIndex=${selectedHandIndex}, benchIndex=${benchIndex}`);
      this.matchState.optimisticallyRemoveCardFromHand(selectedHandIndex);
      this.dispatcher.putBasicOnBench(this.matchId, myId, selectedHandIndex, benchIndex);
      return;
    }

    if (mode === 'SELECT_RETREAT_TARGET') {
      const targetInstanceId = 'instanceId' in event ? event.instanceId : null;
      if (!targetInstanceId) return;

      const bench = this.myPlayerState()?.bench ?? [];
      const benchIndex = bench.findIndex(p => p?.instanceId === targetInstanceId);
      if (benchIndex < 0) return;

      const myId = this.matchState.myPlayerId();
      if (!this.matchId || !myId) return;
      this.dispatcher.retreatActive(this.matchId, myId, benchIndex);
      return;
    }

    if (mode === 'SELECT_TARGET_POKEMON') {
      const state = this.matchState.publicState();
      if (!state || state.status !== 'ACTIVE' || state.phase !== 'MAIN' || !this.matchState.isMyTurn()) {
        console.warn(`[DEBUG] onPokemonClicked SELECT_TARGET_POKEMON: blocked by state/phase/turn`);
        return;
      }

      const targetInstanceId = 'instanceId' in event ? event.instanceId : null;
      console.warn(`[DEBUG] SELECT_TARGET_POKEMON: target instanceId=${targetInstanceId}`);

      if (!targetInstanceId) {
        console.warn(`[DEBUG] onPokemonClicked SELECT_TARGET_POKEMON: no targetInstanceId`);
        return;
      }

      // Ability targeting
      const pendingAbility = this._pendingAbility();
      const pendingAbilityFlow = this._pendingAbilityFlow();
      if (pendingAbilityFlow && pendingAbilityFlow.step === 'select_target') {
        this.onAbilityTargetSelected(targetInstanceId);
        return;
      }
      if (pendingAbility && pendingAbility.abilityName === 'Drive Off') {
        this.onAbilityTargetSelected(targetInstanceId);
        return;
      }

      const selectedHandIndex = this.selectionState().selectedHandIndex;
      if (selectedHandIndex === null) {
        console.warn(`[DEBUG] onPokemonClicked SELECT_TARGET_POKEMON: no selectedHandIndex`);
        return;
      }

      const hand = this.matchState.privateState()?.hand;
      const card = hand?.[selectedHandIndex];
      if (!card) {
        console.warn(`[DEBUG] onPokemonClicked SELECT_TARGET_POKEMON: card not found at handIndex ${selectedHandIndex}`);
        return;
      }

      const supertype = card.supertype;
      const cardDef = this.cardRepo.getFromCache(card.cardId);
      const pendingTrainer = this._pendingTrainerPlay();
      console.warn(`[DEBUG] onPokemonClicked SELECT_TARGET_POKEMON: supertype=${supertype}, target=${targetInstanceId}, handIndex=${selectedHandIndex}, pendingTrainer=${pendingTrainer?.effectCode ?? 'none'}`);
      if (pendingTrainer && pendingTrainer.handIndex === selectedHandIndex) {
        if (pendingTrainer.effectCode === 'HEAL_60_DISCARD_1') {
          const targetPkm = this.findPublicPokemon(targetInstanceId);
          const energyIds = targetPkm?.attachedEnergyInstanceIds ?? [];
          if (energyIds.length === 0) {
      this._pendingTrainerPlay.set(null);
      this.showFlyingCard(selectedHandIndex);
      this.matchState.optimisticallyRemoveCardFromHand(selectedHandIndex);
            this.dispatcher.playTrainer(this.matchId, myId, selectedHandIndex, {
              targetPokemonInstanceId: targetInstanceId,
            });
            return;
          }
          this.interactionService.enterSelectEnergyForSuperPotion(selectedHandIndex, targetInstanceId, energyIds);
          return;
        }
        if (pendingTrainer.effectCode === 'CASSIUS') {
          const myState = this.myPlayerState();
          const bench = myState?.bench ?? [];
          const isTargetActive = myState?.activePokemon?.instanceId === targetInstanceId;
          if (isTargetActive && bench.length > 0) {
            const benchIds = bench.filter(p => p != null).map(p => p!.instanceId);
            this.interactionService.enterSelectNewActive(selectedHandIndex, targetInstanceId, benchIds);
            this.toastService.show('Elegí un Pokémon de Banca para que sea tu nuevo Activo', 'info', 3000);
            return;
          }
        }
        if (pendingTrainer.effectCode === 'ATTACH_TOOL') {
          this._pendingTrainerPlay.set(null);
          this.showFlyingCard(selectedHandIndex);
          this.matchState.optimisticallyRemoveCardFromHand(selectedHandIndex);
          console.warn(`[DEBUG] Dispatching ATTACH_TOOL via pendingTrainer: handIndex=${selectedHandIndex}, target=${targetInstanceId}`);
          this.dispatcher.attachTool(this.matchId, myId, selectedHandIndex, targetInstanceId);
          return;
        }
        this._pendingTrainerPlay.set(null);
        if (pendingTrainer.effectCode === 'EVOSODA') {
          const myState = this.myPlayerState();
          if (myState && !myState.firstTurnCompleted) {
            this.toastService.show('No podés evolucionar en tu primer turno', 'hostile', 3000);
            return;
          }
          const targetPoke = myState?.activePokemon?.instanceId === targetInstanceId
            ? myState?.activePokemon
            : myState?.bench.find(p => p?.instanceId === targetInstanceId);
          if (targetPoke && (targetPoke.evolvedThisTurn || targetPoke.enteredTurnNumber === this.matchState.publicState()?.turnNumber)) {
            this.toastService.show('Este Pokémon no puede evolucionar este turno', 'hostile', 3000);
            return;
          }
        }
        this.showFlyingCard(selectedHandIndex);
        this.matchState.optimisticallyRemoveCardFromHand(selectedHandIndex);
        this.dispatcher.playTrainer(this.matchId, myId, selectedHandIndex, {
          targetPokemonInstanceId: targetInstanceId,
        });
        return;
      }
      this.matchState.optimisticallyRemoveCardFromHand(selectedHandIndex);
      if (supertype === 'ENERGY') {
        this.dispatcher.attachEnergy(this.matchId, myId, selectedHandIndex, targetInstanceId).subscribe({
          error: () => this.matchState.clearPendingRemovals(),
        });
      } else if (supertype === 'TRAINER' && (card.effectCode === 'ATTACH_TOOL' || (cardDef && normalizeCardSubtypes(cardDef.subtypes).includes('POKEMON_TOOL')))) {
        this.showFlyingCard(selectedHandIndex);
        console.warn(`[DEBUG] Dispatching ATTACH_TOOL (fallthrough): handIndex=${selectedHandIndex}, target=${targetInstanceId}`);
        this.dispatcher.attachTool(this.matchId, myId, selectedHandIndex, targetInstanceId);
      } else {
        this.audioService.playEvolutionSound();
        this.dispatcher.evolvePokemon(this.matchId, myId, selectedHandIndex, targetInstanceId);
      }
      return;
    }

    if (mode === 'SELECT_ENERGY_FOR_SUPER_POTION') {
      const energyEvent = 'instanceId' in event ? event.instanceId : null;
      if (!energyEvent) return;

      const selectedHandIndex = this.selectionState().selectedHandIndex;
      const targetPokemonInstanceId = this.selectionState().selectedInstanceId;
      if (selectedHandIndex === null || !targetPokemonInstanceId) return;

      const myId = this.matchState.myPlayerId();
      if (!this.matchId || !myId) return;

      const energyIds = this.selectionState().validTargets;
      const energyIndex = energyIds.indexOf(energyEvent);
      if (energyIndex < 0) return;

      this._pendingTrainerPlay.set(null);
      this.matchState.optimisticallyRemoveCardFromHand(selectedHandIndex);
      this.dispatcher.playTrainer(this.matchId, myId, selectedHandIndex, {
        targetPokemonInstanceId,
        energyIndex,
      });
      return;
    }

    if (mode === 'SELECT_NEW_ACTIVE') {
      const newActiveInstanceId = 'instanceId' in event ? event.instanceId : null;
      if (!newActiveInstanceId) return;

      const selectedHandIndex = this.selectionState().selectedHandIndex;
      const cassiusTargetId = this.selectionState().selectedInstanceId;
      if (selectedHandIndex === null || !cassiusTargetId) return;

      const myId = this.matchState.myPlayerId();
      if (!this.matchId || !myId) return;

      this._pendingTrainerPlay.set(null);
      this.matchState.optimisticallyRemoveCardFromHand(selectedHandIndex);
      this.dispatcher.playTrainer(this.matchId, myId, selectedHandIndex, {
        targetPokemonInstanceId: cassiusTargetId,
        newActiveInstanceId,
      });
      return;
    }
  }

  protected onEnergyClickedForSuperPotion(event: { instanceId: string; index: number }): void {
    const mode = this.selectionState().mode;
    if (mode !== 'SELECT_ENERGY_FOR_SUPER_POTION') return;

    const selectedHandIndex = this.selectionState().selectedHandIndex;
    const targetPokemonInstanceId = this.selectionState().selectedInstanceId;
    if (selectedHandIndex === null || !targetPokemonInstanceId) return;

    const myId = this.matchState.myPlayerId();
    if (!this.matchId || !myId) return;

    const energyIds = this.selectionState().validTargets;
    const energyIndex = energyIds.indexOf(event.instanceId);
    if (energyIndex < 0) return;

    this._pendingTrainerPlay.set(null);
    this.matchState.optimisticallyRemoveCardFromHand(selectedHandIndex);
    this.dispatcher.playTrainer(this.matchId, myId, selectedHandIndex, {
      targetPokemonInstanceId,
      energyIndex,
    });
  }

  protected onHandCardClicked(card: PrivateHandCardModel, handIndex: number): void {
    const state = this.matchState.publicState();
    if (!state || state.status === 'SETUP') return;

    const mode = this.selectionState().mode;
    if (mode !== 'NONE') return;

    const supertype = card.supertype;

    if (supertype === 'POKEMON') {
      const cardDef = this.cardRepo.getFromCache(card.cardId);

      // If cardDef has a stage other than BASIC, it's an evolution card
      if (cardDef?.stage && cardDef.stage !== 'BASIC') {
        const eligibleIds = this.getEligiblePokemonInstanceIds(handIndex);
        if (eligibleIds.length === 0) {
          const myState = this.myPlayerState();
          if (myState && !myState.firstTurnCompleted) {
            this.toastService.show('No podés evolucionar en tu primer turno', 'hostile', 3000);
            return;
          }
          if (myState) {
            const allMatchName: string[] = [];
            if (myState.activePokemon) {
              const activeDef = this.cardRepo.getFromCache(myState.activePokemon.cardId);
              if (activeDef?.name === cardDef.evolvesFrom) allMatchName.push(myState.activePokemon.instanceId);
            }
            for (const poke of myState.bench) {
              if (!poke) continue;
              const benchDef = this.cardRepo.getFromCache(poke.cardId);
              if (benchDef?.name === cardDef.evolvesFrom) allMatchName.push(poke.instanceId);
            }
            if (allMatchName.length > 0) {
              this.toastService.show('Este Pokémon no puede evolucionar este turno', 'hostile', 3000);
              return;
            }
          }
          this.toastService.show('No hay Pokémon disponibles para evolucionar', 'hostile', 3000);
          return;
        }
        this.interactionService.enterSelectTargetPokemon(handIndex, eligibleIds);
        return;
      }

      // BASIC (or unknown cardDef) → put on bench
      this.interactionService.enterSelectBenchSlot(handIndex, []);
      return;
    }

    if (supertype === 'ENERGY') {
      if (!state || state.status !== 'ACTIVE' || state.phase !== 'MAIN' || !this.matchState.isMyTurn()) return;
      this.interactionService.enterSelectTargetPokemon(handIndex, this.allPokemonInstanceIds());
      return;
    }

    if (supertype === 'TRAINER') {
      if (!state || state.status !== 'ACTIVE' || state.phase !== 'MAIN' || !this.matchState.isMyTurn()) return;
      const cardDef = this.cardRepo.getFromCache(card.cardId);
      const isPokemonTool = card.effectCode === 'ATTACH_TOOL'
                         || (cardDef && normalizeCardSubtypes(cardDef.subtypes).includes('POKEMON_TOOL'));
      if (isPokemonTool) {
        console.warn(`[DEBUG] Tool card clicked: handIndex=${handIndex}, cardId=${card.cardId}, name=${card.name}`);
        this._pendingTrainerPlay.set({ handIndex, effectCode: 'ATTACH_TOOL' });
        this.interactionService.enterSelectTargetPokemon(handIndex, this.pokemonWithoutToolInstanceIds());
        return;
      }
      const effectCode = card.effectCode ?? this.getTrainerEffectCode(cardDef?.name);
      if (effectCode === 'HEAL_60_DISCARD_1') {
        this.toastService.show('Arrastrá la carta a un Pokémon para usarla', 'info', 3000);
        return;
      }
      if (effectCode === 'CASSIUS') {
        const bench = this.myPlayerState()?.bench ?? [];
        if (bench.length === 0) {
          this.toastService.show('No podés jugar Cassius sin Pokémon en Banca', 'hostile', 3000);
          return;
        }
      }
      if (effectCode === 'DISCARD_HAND_DRAW_7' || effectCode === 'SHUFFLE_DRAW_5') {
        this._pendingTrainerConfirm.set({ handIndex, cardName: cardDef?.name ?? '' });
        return;
      }
      if (effectCode && this.needsPokemonTarget(effectCode)) {
        this._pendingTrainerPlay.set({ handIndex, effectCode });
        const targets = effectCode === 'TEAM_FLARE_GRUNT' ? this.opponentPokemonInstanceIds() : this.allPokemonInstanceIds();
        this.interactionService.enterSelectTargetPokemon(handIndex, targets);
        return;
      }
      if (effectCode === 'MAX_REVIVE') {
        const myId = this.matchState.myPlayerId();
        if (!myId) return;
        this._pendingDiscardSelection.set({ handIndex });
        this.openDiscardViewer(myId);
        return;
      }
      if (effectCode === 'GREAT_BALL') {
        const priv = this.matchState.privateState();
        if (!priv?.deck) return;
        const top7 = priv.deck.slice(0, 7);
        if (top7.length === 0) return;
        this._pendingDeckSelection.set({ handIndex, effectCode });
        this._showDeckViewer.set({
          cards: top7,
          title: 'Seleccioná un Pokémon de las primeras 7 cartas',
          selectionMode: 'single',
          allowedSupertype: 'POKEMON',
        });
        return;
      }
      if (effectCode === 'PROFESSORS_LETTER') {
        const priv = this.matchState.privateState();
        if (!priv?.deck) return;
        const energyCards = priv.deck.filter(c => c.supertype === 'ENERGY');
        if (energyCards.length === 0) return;
        this._pendingDeckSelection.set({ handIndex, effectCode });
        this._showDeckViewer.set({
          cards: energyCards,
          title: 'Seleccioná hasta 2 energías básicas',
          selectionMode: 'multi',
          allowedSupertype: 'ENERGY',
        });
        return;
      }
      const myId = this.matchState.myPlayerId();
      if (this.matchId && myId) {
        this.showFlyingCard(handIndex);
        this.matchState.optimisticallyRemoveCardFromHand(handIndex);
        this.dispatcher.playTrainer(this.matchId, myId, handIndex);
      }
    }
  }

  protected onActiveDropped(cardInstanceId: string): void {
    const myId = this.matchState.myPlayerId();
    if (this.matchId && myId) {
      this.matchState.optimisticallyRemoveCardByInstanceId(cardInstanceId);
      this.dispatcher.placeActive(this.matchId, myId, cardInstanceId);
    }
  }

  protected onBenchDropped(event: { cardInstanceId: string; benchIndex: number }): void {
    const myId = this.matchState.myPlayerId();
    if (this.matchId && myId) {
      this.matchState.optimisticallyRemoveCardByInstanceId(event.cardInstanceId);
      this.dispatcher.placeBench(this.matchId, myId, event.cardInstanceId, event.benchIndex);
    }
  }

  protected onHandToBenchDropped(event: { handIndex: number; benchIndex: number }): void {
    const state = this.matchState.publicState();
    if (!state || state.status !== 'ACTIVE' || state.phase !== 'MAIN') return;

    const myId = this.matchState.myPlayerId();
    if (!this.matchId || !myId) return;

    const benchSlot = this.myPlayerState()?.bench[event.benchIndex];
    if (benchSlot !== null && benchSlot !== undefined) return;

    console.warn(`[DEBUG] onHandToBenchDropped: handIndex=${event.handIndex}, benchIndex=${event.benchIndex}`);
    this.matchState.optimisticallyRemoveCardFromHand(event.handIndex);
    this.dispatcher.putBasicOnBench(this.matchId, myId, event.handIndex, event.benchIndex);
  }

  protected onEnergyDropped(event: { handIndex: number; targetInstanceId: string }): void {
    const myId = this.matchState.myPlayerId();
    if (!this.matchId || !myId) return;
    const state = this.matchState.publicState();
    if (!state || state.status !== 'ACTIVE' || state.phase !== 'MAIN' || !this.matchState.isMyTurn()) return;
    this.matchState.optimisticallyRemoveCardFromHand(event.handIndex);
    this.dispatcher.attachEnergy(this.matchId, myId, event.handIndex, event.targetInstanceId).subscribe({
      error: () => this.matchState.clearPendingRemovals(),
    });
  }

  protected onEvolutionDropped(event: { handIndex: number; targetInstanceId: string }): void {
    const myId = this.matchState.myPlayerId();
    if (!this.matchId || !myId) return;
    const state = this.matchState.publicState();
    if (!state || state.status !== 'ACTIVE' || state.phase !== 'MAIN' || !this.matchState.isMyTurn()) return;
    const hand = this.matchState.privateState()?.hand;
    const card = hand?.[event.handIndex];
    if (!card || card.supertype !== 'POKEMON') return;
    const cardDef = this.cardRepo.getFromCache(card.cardId);
    if (!cardDef?.stage || cardDef.stage === 'BASIC') return;

    const myState = this.myPlayerState();

    if (!myState?.firstTurnCompleted) {
      this.toastService.show('No podés evolucionar en tu primer turno', 'hostile', 3000);
      return;
    }

    const targetPoke = myState.activePokemon?.instanceId === event.targetInstanceId
      ? myState.activePokemon
      : myState?.bench.find(p => p?.instanceId === event.targetInstanceId);

    if (!targetPoke) return;

    const targetDef = this.cardRepo.getFromCache(targetPoke.cardId);
    if (targetDef?.name !== cardDef.evolvesFrom) {
      this.toastService.show('No hay Pokémon disponibles para evolucionar', 'hostile', 3000);
      return;
    }

    if (targetPoke.evolvedThisTurn || targetPoke.enteredTurnNumber === state.turnNumber) {
      this.toastService.show('Este Pokémon no puede evolucionar este turno', 'hostile', 3000);
      return;
    }

    this.matchState.optimisticallyRemoveCardFromHand(event.handIndex);
    this.audioService.playEvolutionSound();
    this.dispatcher.evolvePokemon(this.matchId, myId, event.handIndex, event.targetInstanceId);
  }

  protected onTrainerDropped(event: { handIndex: number; targetInstanceId: string }): void {
    const myId = this.matchState.myPlayerId();
    if (!this.matchId || !myId) return;
    const state = this.matchState.publicState();
    if (!state || state.status !== 'ACTIVE' || state.phase !== 'MAIN' || !this.matchState.isMyTurn()) return;
    const hand = this.matchState.privateState()?.hand;
    const card = hand?.[event.handIndex];
    if (!card || card.supertype !== 'TRAINER') return;
    const cardDef = this.cardRepo.getFromCache(card.cardId);
    const isTool = card.effectCode === 'ATTACH_TOOL'
                || (cardDef && normalizeCardSubtypes(cardDef.subtypes).includes('POKEMON_TOOL'));
    const isEvosoda = card.effectCode === 'EVOSODA' || cardDef?.name === 'Evosoda';
    if (isEvosoda) {
      const myState = this.myPlayerState();
      if (myState && !myState.firstTurnCompleted) {
        this.toastService.show('No podés evolucionar en tu primer turno', 'hostile', 3000);
        return;
      }
      const targetPoke = myState?.activePokemon?.instanceId === event.targetInstanceId
        ? myState?.activePokemon
        : myState?.bench.find(p => p?.instanceId === event.targetInstanceId);
      if (targetPoke && (targetPoke.evolvedThisTurn || targetPoke.enteredTurnNumber === state.turnNumber)) {
        this.toastService.show('Este Pokémon no puede evolucionar este turno', 'hostile', 3000);
        return;
      }
    }
    this.showFlyingCard(event.handIndex);
    this.matchState.optimisticallyRemoveCardFromHand(event.handIndex);
    if (isTool) {
      this.dispatcher.attachTool(this.matchId, myId, event.handIndex, event.targetInstanceId);
    } else {
      this.dispatcher.playTrainer(this.matchId, myId, event.handIndex, {
        targetPokemonInstanceId: event.targetInstanceId,
      });
    }
  }

  protected onDiscardCardSelected(event: { cardIndex: number; cardId: string; instanceId: string }): void {
    const pending = this._pendingDiscardSelection();
    if (!pending) return;
    this._pendingDiscardSelection.set(null);
    this.closeDiscardViewer();
    const myId = this.matchState.myPlayerId();
    if (!this.matchId || !myId) return;
    this.showFlyingCard(pending.handIndex);
    this.matchState.optimisticallyRemoveCardFromHand(pending.handIndex);
    this.dispatcher.playTrainer(this.matchId, myId, pending.handIndex, {
      targetCardIndex: event.cardIndex,
    });
  }

  protected onDeckCardSelected(event: { cardIndex: number; cardId: string; instanceId: string }): void {
    const pending = this._pendingDeckSelection();
    if (!pending) return;
    this._pendingDeckSelection.set(null);
    this._showDeckViewer.set(null);
    const myId = this.matchState.myPlayerId();
    if (!this.matchId || !myId) return;
    this.showFlyingCard(pending.handIndex);
    this.matchState.optimisticallyRemoveCardFromHand(pending.handIndex);
    this.dispatcher.playTrainer(this.matchId, myId, pending.handIndex, {
      targetCardIndex: event.cardIndex,
    });
  }

  protected onDeckCardsSelected(event: { cardIndexes: number[]; cardIds: string[] }): void {
    const pending = this._pendingDeckSelection();
    if (!pending) return;
    this._pendingDeckSelection.set(null);
    this._showDeckViewer.set(null);
    const myId = this.matchState.myPlayerId();
    if (!this.matchId || !myId) return;
    this.showFlyingCard(pending.handIndex);
    this.matchState.optimisticallyRemoveCardFromHand(pending.handIndex);
    this.dispatcher.playTrainer(this.matchId, myId, pending.handIndex, {
      targetCardIndexes: event.cardIndexes,
    });
  }

  protected onFieldDragStarted(instanceId: string): void {
    const myId = this.matchState.myPlayerId();
    if (!this.matchId || !myId) return;
    const me = this.myPlayerState();
    if (!me) return;
    if (me.activePokemon?.instanceId === instanceId) {
      this.dispatcher.removeActive(this.matchId, myId);
    } else {
      this.dispatcher.removeBench(this.matchId, myId, instanceId);
    }
  }

  protected onConfirmSetup(): void {
    const myId = this.matchState.myPlayerId();
    if (this.matchId && myId) {
      this.dispatcher.confirmSetup(this.matchId, myId);
    }
  }

  protected onInitialMulliganDecision(decision: 'MULLIGAN' | 'KEEP'): void {
    const myId = this.matchState.myPlayerId();
    if (this.matchId && myId) {
      this.dispatcher.resolveInitialMulligan(this.matchId, myId, decision);
    }
  }

  protected onMulliganDrawDecision(drawCards: boolean): void {
    const myId = this.matchState.myPlayerId();
    if (this.matchId && myId) {
      this.dispatcher.resolveMulliganDraw(this.matchId, myId, drawCards);
    }
  }

  protected onAttackClicked(attackIndex: number): void {
    const myActive = this.myPlayerState()?.activePokemon;
    if (!myActive) return;
    const cardDef = this.cardRepo.getFromCache(myActive.cardId);
    if (!cardDef?.attacks) return;
    const attackDef = cardDef.attacks.find(a => a.index === attackIndex);
    if (!attackDef) return;
    const opponentActive = this.matchState.opponentActivePokemon();
    // Reset all sub-action flags before setting a new pending attack
    this.benchDamagePending.set(false);
    this.benchAttachPending.set(false);
    this.benchHealPending.set(false);
    this.energyDiscardPending.set(false);
    this.selfEnergyDiscardPending.set(false);
    this.switchPending.set(false);
    this.moveEnergyPending.set(false);
    this.moveBenchPending.set(false);
    this.optionalBonusPending.set(false);
    this.selectedBenchTargets.set([]);
    this.selectedHealTarget.set(null);
    this.selectedEnergyDiscard.set([]);
    this.selfSelectedEnergyDiscard.set([]);
    this.selectedSwitchTarget.set(null);
    this.moveSelectedEnergy.set([]);
    this.moveBenchTarget.set(null);
    console.warn('[DEBUG] onAttackClicked: attackIndex=%d, attackName=%s, targetId=%s', attackIndex, attackDef.name, opponentActive?.instanceId);

    let conditionOptions: string[] | undefined;
    let chosenCondition: string | undefined;
    const lowerText = (attackDef.text ?? '').toLowerCase();
    if (lowerText.includes('choose either') || lowerText.includes('choose between')) {
      if (lowerText.includes('asleep') && lowerText.includes('poisoned')) {
        conditionOptions = ['ASLEEP', 'POISONED'];
        chosenCondition = 'ASLEEP';
      }
    }

    this.pendingAttack.set({
      attackIndex,
      attackName: attackDef.name,
      cost: attackDef.cost ?? [],
      targetId: opponentActive?.instanceId,
      conditionOptions,
      chosenCondition,
    });
  }

  protected onChooseCondition(condition: string): void {
    const pending = this.pendingAttack();
    if (pending) {
      this.pendingAttack.set({ ...pending, chosenCondition: condition });
    }
  }

  protected onRetreatInitiated(): void {
    if (!this.matchId) return;
    const myId = this.matchState.myPlayerId();
    if (!myId) return;
    this.interactionService.enterSelectRetreatTarget(this.benchInstanceIds());
  }

  protected onActionSelected(action: { type: GameActionType; payload?: Record<string, unknown> }): void {
    if (!this.matchId) return;

    if (action.type === 'RETREAT_ACTIVE') {
      this.interactionService.enterSelectRetreatTarget(this.benchInstanceIds());
      return;
    }

    if (action.type === 'DECLARE_ATTACK') {
      const myActive = this.myPlayerState()?.activePokemon;
      if (!myActive) return;
      const cardDef = this.cardRepo.getFromCache(myActive.cardId);
      const attackIndex = action.payload?.['attackIndex'] as number | undefined;
      if (attackIndex == null || !cardDef?.attacks) return;
      const attackDef = cardDef.attacks.find(a => a.index === attackIndex);
      if (!attackDef) return;
      this.pendingAttack.set({
        attackIndex,
        attackName: attackDef.name,
        cost: attackDef.cost ?? [],
        targetId: action.payload?.['targetPokemonInstanceId'] as string | undefined,
      });
      return;
    }

    this.dispatcher.dispatchAction(this.matchId, undefined, action.type, action.payload);
  }

  protected onConfirmAttack(): void {
    const pending = this.pendingAttack();
    if (!pending || !this.matchId) {
      console.warn('[DEBUG] onConfirmAttack blocked: pending=%o, matchId=%s', pending, this.matchId);
      return;
    }
    console.warn('[DEBUG] onConfirmAttack: attackIndex=%d, attackName=%s, targetId=%s', pending.attackIndex, pending.attackName, pending.targetId);

    const myActive = this.myPlayerState()?.activePokemon;
    const cardDef = myActive ? this.cardRepo.getFromCache(myActive.cardId) : null;
    const attackDef = cardDef?.attacks?.find(a => a.index === pending.attackIndex);

    const text = attackDef?.text ?? '';
    const lower = text.toLowerCase();
    const hasBenchDamage = /damage\s+to\s+.+?benched/i.test(lower) && !lower.includes('heal') && !lower.includes('your benched');
    const hasBenchHeal = /heal.*benched/i.test(lower);
    const hasDiscardEnergy = /discard.*(?:energy|energies).*(?:opponent|defending|defender|their|active)/i.test(lower);
    const hasSelfDiscard = /discard.*(?:energy|energies).*(?:this|attached to this)/i.test(lower) && !lower.includes('the top card');
    const hasSelfSwitch = /switch\s+this\s+(?:pok[eé]mon|pokemon)/i.test(lower);
    const hasOppSwitch = /opponent\s+switches/i.test(lower);
    const hasMoveEnergy = /move\s+(?:an\s+)?energy/i.test(lower);
    const isMoveFromAttacker = !lower.includes('your opponent');
    const hasOptionalBonus = /you\s+may\s+(?:do|discard)/i.test(lower) && /more\s+damage/i.test(lower);
    const hasAttackRestriction = /choose\s+1\s+of\s+(?:your\s+opponent's|your\s+opponent's)\s+active/i.test(lower) && /can't\s+use/i.test(lower);

    if (hasAttackRestriction) {
      this.attackRestrictionPending.set(true);
      this.selectedRestrictedAttack.set(null);
      this.pendingAttack.set({ ...pending, needsAttackRestriction: true });
      return;
    }

    if (hasBenchDamage) {
      this.benchDamagePending.set(true);
      this.selectedBenchTargets.set([]);
      return;
    }

    const hasBenchAttach = /attach.*benched/i.test(lower) && /flip/i.test(lower);
    if (hasBenchAttach) {
      this.benchAttachPending.set(true);
      this.selectedBenchTargets.set([]);
      return;
    }

    if (hasBenchHeal) {
      this.benchHealPending.set(true);
      this.selectedHealTarget.set(null);
      return;
    }

    // Thunderbolt style: "Discard all Energy attached to this Pokémon." — skip selection, auto-discard all.
    if (hasSelfDiscard && lower.includes('discard all')) {
      this.selfSelectedEnergyDiscard.set([]);
      this.doDispatchAttack(pending.attackIndex, pending.targetId, []);
      return;
    }

    if (hasSelfDiscard) {
      const myActivePkm = this.myPlayerState()?.activePokemon;
      if (myActivePkm?.attachedCards && myActivePkm.attachedCards.length > 0) {
        this.selfEnergyDiscardPending.set(true);
        this.selfSelectedEnergyDiscard.set([]);
        this.pendingAttack.set({ ...pending, isOptionalDiscard: lower.includes('you may') });
        return;
      }
    }

    if (hasDiscardEnergy) {
      const opponentActive = this.matchState.opponentActivePokemon();
      if (opponentActive?.attachedCards && opponentActive.attachedCards.length > 0) {
        this.energyDiscardPending.set(true);
        this.selectedEnergyDiscard.set([]);
        return;
      }
    }

    if (hasSelfSwitch || hasOppSwitch) {
      this.pendingAttack.set({ ...pending, isSelfSwitch: hasSelfSwitch });
      this.switchPending.set(true);
      this.selectedSwitchTarget.set(null);
      return;
    }

    if (hasMoveEnergy) {
      const sourcePkm = isMoveFromAttacker ? this.myPlayerState()?.activePokemon : this.matchState.opponentActivePokemon();
      if (sourcePkm?.attachedCards && sourcePkm.attachedCards.length > 0) {
        this.isMoveFromAttacker.set(isMoveFromAttacker);
        this.moveEnergyPending.set(true);
        this.moveSelectedEnergy.set([]);
        return;
      }
    }

    if (hasOptionalBonus) {
      this.optionalBonusPending.set(true);
      this.useOptionalBonus.set(true);
      return;
    }

    this.doDispatchAttack(pending.attackIndex, pending.targetId, []);
  }

  private doDispatchAttack(attackIndex: number, targetId: string | undefined, benchTargets: string[]): void {
    if (!this.matchId) {
      console.warn('[DEBUG] doDispatchAttack: no matchId');
      return;
    }
    console.warn('[DEBUG] doDispatchAttack: attackIndex=%d, targetId=%s, benchTargets=%o', attackIndex, targetId, benchTargets);

    // Trigger screen shake and attack flash
    this._screenShake.set(true);
    setTimeout(() => this._screenShake.set(false), 500);
    const active = this.matchState.myActivePokemon();
    if (active) {
      const cardDef = this.cardRepo.getFromCache(active.cardId);
      const type = cardDef?.types?.[0] ?? 'COLORLESS';
      this._attackFlash.set('own');
      this._attackFlashType.set(type);
      this.audioService.playTypeSound(type);
      setTimeout(() => this._attackFlash.set(null), 600);
    }

    const payload: Record<string, unknown> = {
      attackIndex,
      targetPokemonInstanceId: targetId,
    };
    if (benchTargets.length > 0) {
      payload['benchTargets'] = benchTargets.map(id => ({ instanceId: id, damageCounters: 1 }));
    }
    const healId = this.selectedHealTarget();
    if (healId) {
      payload['healTargetId'] = healId;
    }
    const selfDiscardIds = this.selfSelectedEnergyDiscard();
    if (selfDiscardIds.length > 0) {
      payload['energyCardInstanceIdsToDiscard'] = selfDiscardIds;
    } else {
      const discardIds = this.selectedEnergyDiscard();
      if (discardIds.length > 0) {
        payload['energyCardInstanceIdsToDiscard'] = discardIds;
      }
    }
    const moveEnergyIds = this.moveSelectedEnergy();
    if (moveEnergyIds.length > 0) {
      payload['energyCardInstanceIdsToMove'] = moveEnergyIds;
    }
    const pending = this.pendingAttack();
    if (pending?.useOptionalBonus != null) {
      payload['useOptionalBonus'] = pending.useOptionalBonus;
    }
    if (pending?.needsAttackRestriction) {
      const restricted = this.selectedRestrictedAttack();
      if (restricted) {
        payload['restrictedAttackName'] = restricted;
      }
    }
    if (pending?.chosenCondition) {
      payload['specialCondition'] = pending.chosenCondition;
    }
    this.dispatcher.dispatchAction(this.matchId, undefined, 'DECLARE_ATTACK', payload).subscribe();
    this.pendingAttack.set(null);
    this.benchDamagePending.set(false);
    this.benchAttachPending.set(false);
    this.benchHealPending.set(false);
    this.energyDiscardPending.set(false);
    this.selfEnergyDiscardPending.set(false);
    this.switchPending.set(false);
    this.moveEnergyPending.set(false);
    this.moveBenchPending.set(false);
    this.optionalBonusPending.set(false);
    this.attackRestrictionPending.set(false);
    this.selectedRestrictedAttack.set(null);
    this.selectedBenchTargets.set([]);
    this.selectedHealTarget.set(null);
    this.selectedEnergyDiscard.set([]);
    this.selfSelectedEnergyDiscard.set([]);
    this.selectedSwitchTarget.set(null);
    this.moveSelectedEnergy.set([]);
    this.moveBenchTarget.set(null);
  }

  protected onSelectBenchTarget(instanceId: string): void {
    const current = this.selectedBenchTargets();
    if (current.includes(instanceId)) {
      this.selectedBenchTargets.set(current.filter(id => id !== instanceId));
    } else {
      this.selectedBenchTargets.set([...current, instanceId]);
    }
  }

  protected onSelectHealTarget(instanceId: string): void {
    this.selectedHealTarget.set(instanceId);
  }

  protected onToggleEnergyDiscard(energyInstanceId: string): void {
    const current = this.selectedEnergyDiscard();
    if (current.includes(energyInstanceId)) {
      this.selectedEnergyDiscard.set(current.filter(id => id !== energyInstanceId));
    } else {
      this.selectedEnergyDiscard.set([...current, energyInstanceId]);
    }
  }

  protected onConfirmBenchTargets(): void {
    const pending = this.pendingAttack();
    if (!pending) return;
    this.doDispatchAttack(pending.attackIndex, pending.targetId, this.selectedBenchTargets());
  }

  protected onConfirmBenchAttach(): void {
    const pending = this.pendingAttack();
    if (!pending) return;
    this.doDispatchAttack(pending.attackIndex, pending.targetId, this.selectedBenchTargets());
  }

  protected onConfirmHealTarget(): void {
    const pending = this.pendingAttack();
    if (!pending) return;
    this.doDispatchAttack(pending.attackIndex, pending.targetId, []);
  }

  protected onConfirmEnergyDiscard(): void {
    const pending = this.pendingAttack();
    if (!pending) return;
    this.doDispatchAttack(pending.attackIndex, pending.targetId, []);
  }

  protected onToggleSelfEnergyDiscard(energyInstanceId: string): void {
    const current = this.selfSelectedEnergyDiscard();
    if (current.includes(energyInstanceId)) {
      this.selfSelectedEnergyDiscard.set(current.filter(id => id !== energyInstanceId));
    } else {
      this.selfSelectedEnergyDiscard.set([...current, energyInstanceId]);
    }
  }

  protected onSelectRestrictedAttack(attackName: string): void {
    this.selectedRestrictedAttack.set(attackName);
  }

  protected onConfirmRestrictedAttack(): void {
    const pending = this.pendingAttack();
    if (!pending || !this.selectedRestrictedAttack()) return;
    this.attackRestrictionPending.set(false);
    this.doDispatchAttack(pending.attackIndex, pending.targetId, []);
  }

  protected onConfirmSelfEnergyDiscard(): void {
    const pending = this.pendingAttack();
    if (!pending) return;
    this.doDispatchAttack(pending.attackIndex, pending.targetId, []);
  }

  protected onSkipSelfEnergyDiscard(): void {
    const pending = this.pendingAttack();
    if (!pending) return;
    this.selfSelectedEnergyDiscard.set([]);
    this.doDispatchAttack(pending.attackIndex, pending.targetId, []);
  }

  protected onSelectSwitchTarget(instanceId: string): void {
    this.selectedSwitchTarget.set(instanceId);
  }

  protected onConfirmSwitchTarget(): void {
    const pending = this.pendingAttack();
    if (!pending || !this.selectedSwitchTarget()) return;
    this.doDispatchAttack(pending.attackIndex, pending.targetId, [this.selectedSwitchTarget()!]);
  }

  protected onToggleMoveEnergy(energyInstanceId: string): void {
    const current = this.moveSelectedEnergy();
    if (current.includes(energyInstanceId)) {
      this.moveSelectedEnergy.set(current.filter(id => id !== energyInstanceId));
    } else {
      this.moveSelectedEnergy.set([...current, energyInstanceId]);
    }
  }

  protected onConfirmMoveEnergy(): void {
    this.moveEnergyPending.set(false);
    this.moveBenchPending.set(true);
    this.moveBenchTarget.set(null);
  }

  protected onSelectMoveBenchTarget(instanceId: string): void {
    this.moveBenchTarget.set(instanceId);
  }

  protected onConfirmMoveBench(): void {
    const pending = this.pendingAttack();
    if (!pending || !this.moveBenchTarget()) return;
    this.doDispatchAttack(pending.attackIndex, pending.targetId, [this.moveBenchTarget()!]);
  }

  protected onConfirmOptionalBonus(useBonus: boolean): void {
    const pending = this.pendingAttack();
    if (!pending) return;
    this.pendingAttack.set({ ...pending, useOptionalBonus: useBonus });
    this.optionalBonusPending.set(false);
    this.doDispatchAttack(pending.attackIndex, pending.targetId, []);
  }

  protected getPlayerPokemonList(): PublicPokemonSlotModel[] {
    const me = this.myPlayerState();
    if (!me) return [];
    const list: PublicPokemonSlotModel[] = [];
    if (me.activePokemon) list.push(me.activePokemon);
    for (const p of me.bench) {
      if (p) list.push(p);
    }
    return list;
  }

  protected onSelectAbilitySourceSlot(instanceId: string): void {
    this._abilityMoveSelectedSource.set(instanceId);
  }

  protected onConfirmAbilitySourceSlot(): void {
    const sourceId = this._abilityMoveSelectedSource();
    if (!sourceId) return;
    this._abilityMoveSourcePkmId.set(sourceId);
    this._abilityMoveEnergyPending.set(false);
    this._abilityMoveBenchPending.set(true);
    this._abilityMoveBenchTarget.set(null);
  }

  protected onSelectAbilityMoveBenchTarget(instanceId: string): void {
    this._abilityMoveBenchTarget.set(instanceId);
  }

  protected onConfirmAbilityMoveBench(): void {
    const myId = this.matchState.myPlayerId();
    if (!this.matchId || !myId) return;
    const pending = this._pendingAbility();
    if (!pending) return;
    const sourceId = this._abilityMoveSourcePkmId();
    const targetId = this._abilityMoveBenchTarget();
    if (!sourceId || !targetId) return;

    const sourcePokemon = this.getPlayerPokemonList().find(p => p.instanceId === sourceId);
    if (!sourcePokemon?.attachedEnergyInstanceIds?.length) return;
    const firstEnergyId = sourcePokemon.attachedEnergyInstanceIds[0];

    this.dispatcher.useAbility(this.matchId, myId, pending.pokemonInstanceId, pending.abilityName, {
      sourceEnergyInstanceId: firstEnergyId,
      targetPokemonInstanceId: targetId,
    }).subscribe();
    this.onCancelAbility();
  }

  protected onCancelAbility(): void {
    this._pendingAbility.set(null);
    this._pendingAbilityFlow.set(null);
    this._abilityMoveEnergyPending.set(false);
    this._abilityMoveBenchPending.set(false);
    this._abilityMoveSelectedSource.set(null);
    this._abilityMoveSourcePkmId.set(null);
    this._abilityMoveBenchTarget.set(null);
    this.interactionService.cancelSelection();
  }

  protected onCancelBenchTargets(): void {
    this.benchDamagePending.set(false);
    this.benchAttachPending.set(false);
    this.benchHealPending.set(false);
    this.energyDiscardPending.set(false);
    this.selfEnergyDiscardPending.set(false);
    this.switchPending.set(false);
    this.moveEnergyPending.set(false);
    this.moveBenchPending.set(false);
    this.optionalBonusPending.set(false);
    this.attackRestrictionPending.set(false);
    this.selectedRestrictedAttack.set(null);
    this.selectedBenchTargets.set([]);
    this.selectedHealTarget.set(null);
    this.selectedEnergyDiscard.set([]);
    this.selfSelectedEnergyDiscard.set([]);
    this.selectedSwitchTarget.set(null);
    this.moveSelectedEnergy.set([]);
    this.moveBenchTarget.set(null);
    this.onCancelAbility();
    this.pendingAttack.set(null);
  }

  protected onCancelAttack(): void {
    this.pendingAttack.set(null);
  }

  protected onDrawCard(): void {
    if (!this.matchId) return;
    const myId = this.matchState.myPlayerId();
    if (!myId) return;
    this.dispatcher.dispatchAction(this.matchId, myId, 'DRAW_CARD', {});
  }

  /** Cleanup antes de volver a la página anterior via Location.back() */
  protected onBackFromWaiting(): void {
    this.leaveNormally = true;
    this.matchState.reset();
  }

  protected onReturnToLobby(): void {
    this.leaveNormally = true;
    this.matchState.reset();
    this.router.navigate(['/lobby'], { replaceUrl: true });
  }

  protected onReturnToDeckBuilder(): void {
    this.matchState.reset();
    this.router.navigate(['/decks'], { replaceUrl: true });
  }

  protected onCancelMatch(): void {
    if (!this.localPlayerId || !this.matchId) return;
    this.leaveNormally = true;
    this.matchApi.deleteMatch(this.matchId, this.localPlayerId).subscribe({
      next: () => this.onReturnToLobby(),
      error: () => this.onReturnToLobby(),
    });
  }

  protected onEscapeKey(): void {
    if (this._showDeckViewer()) {
      this._showDeckViewer.set(null);
      this._pendingDeckSelection.set(null);
      return;
    }
    if (this._pendingDiscardSelection()) {
      this.closeDiscardViewer();
      return;
    }
    if (this.pendingAttack()) {
      this.pendingAttack.set(null);
      return;
    }
    if (this._pendingTrainerPlay()) {
      this.onCancelTrainerPlay();
      return;
    }
    if (this._pendingAbility() || this._pendingAbilityFlow()) {
      this.onCancelAbility();
      return;
    }
    if (this.interactionService.isSelecting()) {
      this.interactionService.cancelSelection();
      return;
    }
    if (this._isMatchMenuOpen()) {
      this.closeMatchMenu();
      return;
    }
    this.openMatchMenu();
  }

  protected onCancelTrainerPlay(): void {
    this._pendingTrainerPlay.set(null);
    this._pendingTrainerConfirm.set(null);
    this.interactionService.cancelSelection();
  }

  private showFlyingCard(handIndex: number): void {
    const hand = this.matchState.privateState()?.hand;
    const card = hand?.[handIndex];
    if (!card) return;
    const cardDef = this.cardRepo.getFromCache(card.cardId);
    const url = cardDef?.imageSmallUrl ?? cardDef?.imageLargeUrl;
    if (!url) return;
    this._flyingCard.set({ name: cardDef?.name ?? card.name, imageUrl: url });
    setTimeout(() => this._flyingCard.set(null), 900);
  }

  protected onConfirmTrainerPlay(confirm: boolean): void {
    const pending = this._pendingTrainerConfirm();
    this._pendingTrainerConfirm.set(null);
    if (!confirm || !pending) return;
    const myId = this.matchState.myPlayerId();
    if (this.matchId && myId) {
      this.showFlyingCard(pending.handIndex);
      this.matchState.optimisticallyRemoveCardFromHand(pending.handIndex);
      this.dispatcher.playTrainer(this.matchId, myId, pending.handIndex);
    }
  }

  private getTrainerEffectCode(cardName: string | undefined): string | null {
    if (!cardName) return null;
    const map: Record<string, string> = {
      'Evosoda': 'EVOSODA',
      'Great Ball': 'GREAT_BALL',
      'Max Revive': 'MAX_REVIVE',
      "Professor's Letter": 'PROFESSORS_LETTER',
      'Red Card': 'RED_CARD',
      'Roller Skates': 'COIN_FLIP_DRAW_3',
      'Super Potion': 'HEAL_60_DISCARD_1',
      'Cassius': 'CASSIUS',
      'Professor Sycamore': 'DISCARD_HAND_DRAW_7',
      'Shauna': 'SHUFFLE_DRAW_5',
      'Team Flare Grunt': 'TEAM_FLARE_GRUNT',
    };
    return map[cardName] ?? null;
  }

  private needsPokemonTarget(effectCode: string): boolean {
    return [
      'EVOSODA',
      'CASSIUS',
      'HEAL_20',
      'HEAL_30',
      'HEAL_60',
      'TEAM_FLARE_GRUNT',
    ].includes(effectCode);
  }
}
