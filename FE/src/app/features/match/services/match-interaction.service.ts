import { computed, inject, Injectable, signal } from '@angular/core';
import { SelectionState } from '../../../shared/models/ui-state.models';
import { MatchStateService } from './match-state.service';

const DEFAULT_SELECTION: SelectionState = {
  mode: 'NONE',
  selectedHandIndex: null,
  selectedInstanceId: null,
  validTargets: [],
};

@Injectable({ providedIn: 'root' })
export class MatchInteractionService {
  private readonly matchState = inject(MatchStateService);

  private readonly _selection = signal<SelectionState>(DEFAULT_SELECTION);
  readonly selection = this._selection.asReadonly();

  private readonly _status = computed(() => this.matchState.publicState()?.status);

  private readonly _hoveredCardInstanceId = signal<string | null>(null);
  readonly hoveredCardInstanceId = this._hoveredCardInstanceId.asReadonly();

  private readonly _actionInProgress = signal<boolean>(false);
  readonly actionInProgress = this._actionInProgress.asReadonly();

  private readonly _modalSignal = signal<{ open: boolean; content: unknown }>({
    open: false,
    content: null,
  });
  readonly modal = this._modalSignal.asReadonly();

  readonly isSelecting = computed(() => this._selection().mode !== 'NONE');
  readonly canInteract = computed(
    () => !this._actionInProgress() && !this._modalSignal().open,
  );

  enterSelectBenchSlot(handIndex: number, validTargets: string[]): void {
    if (this._status() !== 'ACTIVE') return;
    this._selection.set({
      mode: 'SELECT_BENCH_SLOT',
      selectedHandIndex: handIndex,
      selectedInstanceId: null,
      validTargets,
    });
  }

  enterSelectTargetPokemon(handIndex: number, targets: string[]): void {
    if (this._status() !== 'ACTIVE') return;
    this._selection.set({
      mode: 'SELECT_TARGET_POKEMON',
      selectedHandIndex: handIndex,
      selectedInstanceId: null,
      validTargets: targets,
    });
  }

  enterSelectRetreatTarget(validTargets: string[]): void {
    if (this._status() !== 'ACTIVE') return;
    this._selection.set({
      mode: 'SELECT_RETREAT_TARGET',
      selectedHandIndex: null,
      selectedInstanceId: null,
      validTargets,
    });
  }

  enterSelectEnergyForSuperPotion(handIndex: number, targetPokemonInstanceId: string, energyInstanceIds: string[]): void {
    if (this._status() !== 'ACTIVE') return;
    this._selection.set({
      mode: 'SELECT_ENERGY_FOR_SUPER_POTION',
      selectedHandIndex: handIndex,
      selectedInstanceId: targetPokemonInstanceId,
      validTargets: energyInstanceIds,
    });
  }

  enterSelectNewActive(handIndex: number, targetInstanceId: string, benchInstanceIds: string[]): void {
    if (this._status() !== 'ACTIVE') return;
    this._selection.set({
      mode: 'SELECT_NEW_ACTIVE',
      selectedHandIndex: handIndex,
      selectedInstanceId: targetInstanceId,
      validTargets: benchInstanceIds,
    });
  }

  enterSetupActive(handIndex: number): void {
    this._selection.set({
      mode: 'SETUP_ACTIVE',
      selectedHandIndex: handIndex,
      selectedInstanceId: null,
      validTargets: [],
    });
  }

  enterSetupBench(handIndex: number): void {
    this._selection.set({
      mode: 'SETUP_BENCH',
      selectedHandIndex: handIndex,
      selectedInstanceId: null,
      validTargets: [],
    });
  }

  cancelSelection(): void {
    this._selection.set(DEFAULT_SELECTION);
  }

  startAction(requestId: string): void {
    this._actionInProgress.set(true);
  }

  completeAction(): void {
    this._actionInProgress.set(false);
    this.cancelSelection();
  }

  setHoveredCard(instanceId: string | null): void {
    this._hoveredCardInstanceId.set(instanceId);
  }

  openModal(content: unknown): void {
    this._modalSignal.set({ open: true, content });
  }

  closeModal(): void {
    this._modalSignal.set({ open: false, content: null });
  }
}
