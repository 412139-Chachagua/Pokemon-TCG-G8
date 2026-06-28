import { ChangeDetectionStrategy, Component, computed, inject, output } from '@angular/core';
import { MatchStateService } from '../../services/match-state.service';
import { MatchInteractionService } from '../../services/match-interaction.service';
import { CardRepositoryService } from '../../../../core/services/card-repository.service';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner/loading-spinner.component';
import { GameActionType } from '../../../../shared/models/game-action.models';

@Component({
  selector: 'app-action-panel',
  imports: [LoadingSpinnerComponent],
  template: `
    <div class="flex flex-col gap-2 p-3 border-t border-[var(--pk-btn-border)]" [style.background]="'color-mix(in oklch, var(--pk-surface) 75%, transparent)'">
      @if (!isMyTurn() && !actionInProgress()) {
        <div class="text-center text-[var(--pk-text-dim)] text-sm p-2">
          Esperando al oponente...
        </div>
      }

      @if (actionInProgress()) {
        <div class="flex items-center justify-center gap-2 text-[var(--pk-text-dim)] text-sm p-2">
          <app-loading-spinner />
          <span>Enviando acción...</span>
        </div>
      }

      @if (isMyTurn() && currentPhase() === 'MAIN' && !actionInProgress()) {
        <div class="flex flex-wrap gap-2 justify-center">
          <button
            class="px-4 py-2 border border-blue-600 rounded-md bg-blue-600 text-white text-sm font-semibold cursor-pointer font-inherit transition-[background,border-color] duration-150 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-blue-500"
            [disabled]="isSelecting()"
            (click)="emitAction('END_TURN')"
          >
            Finalizar turno
          </button>
        </div>
      }

      @if (isSelecting() && !actionInProgress()) {
        <div class="flex justify-center">
          <button
            class="px-4 py-2 border border-red-500 rounded-md bg-transparent text-red-500 text-sm font-semibold cursor-pointer font-inherit transition-[background,border-color] duration-150 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-red-950"
            (click)="onCancel()"
          >
            Cancelar
          </button>
        </div>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActionPanelComponent {
  private readonly matchState = inject(MatchStateService);
  protected readonly interactionService = inject(MatchInteractionService);
  private readonly cardRepo = inject(CardRepositoryService);

  readonly actionSelected = output<{ type: GameActionType; payload?: Record<string, unknown> }>();

  protected readonly isMyTurn = this.matchState.isMyTurn;
  protected readonly currentPhase = this.matchState.currentPhase;
  protected readonly isSelecting = this.interactionService.isSelecting;
  protected readonly actionInProgress = this.interactionService.actionInProgress;
  protected readonly canDraw = this.matchState.canDraw;

  protected readonly myActiveCardDef = computed(() => {
    const active = this.matchState.myActivePokemon();
    if (!active) return null;
    return this.cardRepo.getFromCache(active.cardId);
  });

  protected readonly opponentActiveInstanceId = computed(() => {
    return this.matchState.opponentActivePokemon()?.instanceId ?? null;
  });

  protected emitAction(type: GameActionType, payload?: Record<string, unknown>): void {
    // For DECLARE_ATTACK, inject opponent active as the default target (MVP)
    if (type === 'DECLARE_ATTACK') {
      const targetId = this.opponentActiveInstanceId();
      if (!targetId) return;
      this.actionSelected.emit({ type, payload: { ...payload, targetPokemonInstanceId: targetId } });
      return;
    }
    this.actionSelected.emit({ type, payload });
  }

  protected onCancel(): void {
    this.interactionService.cancelSelection();
  }
}
