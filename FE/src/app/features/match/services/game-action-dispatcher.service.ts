import { inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { MatchApiService } from '../../../core/api/match-api.service';
import { MatchSocketService } from '../../../core/websocket/match-socket.service';
import { AuthService } from '../../../core/services/auth.service';
import { MatchStateService } from './match-state.service';
import { MatchInteractionService } from './match-interaction.service';
import { GameActionRequest, GameActionResponse, GameActionType } from '../../../shared/models/game-action.models';
import { MatchStatus, TurnPhase } from '../../../shared/models/game-state.models';

interface ActionRule {
  allowedStatuses: MatchStatus[];
  allowedPhases?: TurnPhase[];
  needsMyTurn: boolean;
  requiresCondition?: string;
}

@Injectable({ providedIn: 'root' })
export class GameActionDispatcherService {
  private readonly matchApi = inject(MatchApiService);
  private readonly matchSocket = inject(MatchSocketService);
  private readonly authService = inject(AuthService);
  private readonly matchState = inject(MatchStateService);
  private readonly interactionService = inject(MatchInteractionService);

  private readonly actionRules: Record<string, ActionRule> = {
    SETUP_PLACE_ACTIVE:            { allowedStatuses: ['SETUP'], needsMyTurn: false },
    SETUP_PLACE_BENCH:             { allowedStatuses: ['SETUP'], needsMyTurn: false },
    SETUP_REMOVE_ACTIVE:           { allowedStatuses: ['SETUP'], needsMyTurn: false },
    SETUP_REMOVE_BENCH:            { allowedStatuses: ['SETUP'], needsMyTurn: false },
    CONFIRM_SETUP:                 { allowedStatuses: ['SETUP'], needsMyTurn: false },
    RESOLVE_MULLIGAN_DRAW:         { allowedStatuses: ['SETUP'], needsMyTurn: false },
    RESOLVE_INITIAL_MULLIGAN:      { allowedStatuses: ['SETUP'], needsMyTurn: false },
    DRAW_CARD:                     { allowedStatuses: ['ACTIVE'], allowedPhases: ['DRAW'], needsMyTurn: true },
    PUT_BASIC_ON_BENCH:            { allowedStatuses: ['ACTIVE'], allowedPhases: ['DRAW', 'MAIN'], needsMyTurn: true },
    ATTACH_ENERGY:                 { allowedStatuses: ['ACTIVE'], allowedPhases: ['MAIN'], needsMyTurn: true },
    EVOLVE_POKEMON:                { allowedStatuses: ['ACTIVE'], allowedPhases: ['MAIN'], needsMyTurn: true },
    PLAY_TRAINER:                 { allowedStatuses: ['ACTIVE'], allowedPhases: ['MAIN'], needsMyTurn: true },
    RETREAT_ACTIVE:                { allowedStatuses: ['ACTIVE'], allowedPhases: ['MAIN'], needsMyTurn: true },
    ATTACH_TOOL:                   { allowedStatuses: ['ACTIVE'], allowedPhases: ['MAIN'], needsMyTurn: true },
    USE_ABILITY:                   { allowedStatuses: ['ACTIVE'], allowedPhases: ['MAIN'], needsMyTurn: true },
    DECLARE_ATTACK:                { allowedStatuses: ['ACTIVE'], allowedPhases: ['MAIN'], needsMyTurn: true },
    // TODO: Backend only allows END_TURN in DRAW|MAIN (RuleValidator.validateEndTurn).
    // Frontend action-panel also shows "Fin del turno sin atacar" during ATTACK,
    // so we include ATTACK here for frontend consistency until backend is aligned.
    END_TURN:                      { allowedStatuses: ['ACTIVE'], allowedPhases: ['DRAW', 'MAIN', 'ATTACK'], needsMyTurn: true },
    TAKE_PRIZE_CARD:               { allowedStatuses: ['ACTIVE'], needsMyTurn: false, requiresCondition: 'PENDING_PRIZE' },
    CHOOSE_KO_REPLACEMENT:   { allowedStatuses: ['ACTIVE'], needsMyTurn: false, requiresCondition: 'PENDING_KO_REPLACEMENT' },
  };

  private readonly conditionCheckers: Record<string, () => boolean> = {
    PENDING_PRIZE: () => {
      const pub = this.matchState.publicState();
      return pub?.pendingPrizeOwnerPlayerId != null;
    },
    PENDING_KO_REPLACEMENT: () => {
      const pub = this.matchState.publicState();
      return pub?.pendingKOReplacement ?? false;
    },
  };

  private validateAction(actionType: GameActionType): GameActionResponse | null {
    const rule = this.actionRules[actionType];

    if (!rule) {
      console.warn(`[GameActionDispatcher] No rule defined for action type: ${actionType} — rejecting`);
      return {
        success: false,
        clientRequestId: '',
        publicState: null,
        privateState: null,
        events: [],
        error: { code: 'ACTION_NOT_RECOGNIZED', message: `Action '${actionType}' is not registered in the validation matrix` },
      };
    }

    const publicState = this.matchState.publicState();
    if (!publicState) {
      return null;
    }

    const status = publicState.status as MatchStatus;
    if (!rule.allowedStatuses.includes(status)) {
      return {
        success: false,
        clientRequestId: '',
        publicState: null,
        privateState: null,
        events: [],
        error: { code: 'ACTION_NOT_ALLOWED_IN_STATUS', message: `'${actionType}' is not allowed during match status '${status}'` },
      };
    }

    if (rule.allowedPhases) {
      const phase = publicState.phase as TurnPhase;
      if (!rule.allowedPhases.includes(phase)) {
        return {
          success: false,
          clientRequestId: '',
          publicState: null,
          privateState: null,
          events: [],
          error: { code: 'ACTION_NOT_ALLOWED_IN_PHASE', message: `'${actionType}' is not allowed during '${phase}' phase` },
        };
      }
    }

    if (rule.needsMyTurn && !this.matchState.isMyTurn()) {
      return {
        success: false,
        clientRequestId: '',
        publicState: null,
        privateState: null,
        events: [],
        error: { code: 'NOT_YOUR_TURN', message: `'${actionType}' requires your turn` },
      };
    }

    if (rule.requiresCondition) {
      const checker = this.conditionCheckers[rule.requiresCondition];
      if (!checker || !checker()) {
        return {
          success: false,
          clientRequestId: '',
          publicState: null,
          privateState: null,
          events: [],
          error: { code: 'CONDITION_NOT_MET', message: `'${actionType}' requires condition '${rule.requiresCondition}' which is not satisfied` },
        };
      }
    }

    return null;
  }

  dispatchAction(
    matchId: string,
    playerId?: string,
    actionType?: GameActionType,
    payload?: Record<string, unknown>,
    clientRequestId?: string,
  ): Observable<GameActionResponse> {
    const resolvedPlayerId = playerId ?? this.authService.playerId();
    if (!resolvedPlayerId) {
      return of({
        success: false,
        clientRequestId: '',
        publicState: null,
        privateState: null,
        events: [],
        error: { code: 'AUTH_REQUIRED', message: 'Player not authenticated' },
      });
    }

    const validationError = this.validateAction(actionType!);
    if (validationError) {
      console.warn(`[GameActionDispatcher] Action rejected by validation: ${actionType}`, validationError.error);
      return of(validationError);
    }

    if (this.interactionService.actionInProgress()) {
      return of({
        success: false,
        clientRequestId: '',
        publicState: null,
        privateState: null,
        events: [],
        error: { code: 'ACTION_IN_PROGRESS', message: 'An action is already in progress' },
      });
    }

    const requestId = clientRequestId ?? crypto.randomUUID();
    this.interactionService.startAction(requestId);

    const request: GameActionRequest = {
      type: actionType!,
      playerId: resolvedPlayerId,
      payload: payload ?? {},
      clientRequestId: requestId,
    };

    const connStatus = this.matchState.connectionStatus();
    console.warn(`[DEBUG] dispatchAction(${actionType}): connectionStatus=${connStatus}`);
    if (connStatus === 'CONNECTED') {
      const sent = this.matchSocket.sendAction(request);
      this.interactionService.completeAction();
      if (sent) {
        console.warn('[DEBUG] → enviado por WS');
        return of({
          success: true,
          clientRequestId: requestId,
          publicState: null,
          privateState: null,
          events: [],
          error: null,
        });
      }
      console.warn('[GameActionDispatcher] WebSocket.sendAction devolvió false, fallback a HTTP');
    } else {
      console.warn(`[DEBUG] → connectionStatus=${connStatus}, usando HTTP`);
    }

    return this.sendViaHttp(matchId, request);
  }

  private sendViaHttp(matchId: string, request: GameActionRequest): Observable<GameActionResponse> {
    const observable = this.matchApi.sendAction(matchId, request);
    observable.subscribe({
      next: (response) => {
        this.interactionService.completeAction();
        console.warn(`[DEBUG] sendViaHttp response: success=${response.success} hasPublic=${!!response.publicState} hasPrivate=${!!response.privateState}`);
        if (response.success) {
          if (response.publicState) {
            this.matchState.updatePublicState(response.publicState);
          }
          if (response.privateState) {
            this.matchState.updatePrivateState(response.privateState);
          }
          if (response.events) {
            response.events.forEach(event => this.matchState.addEvent(event));
          }
        } else {
          this.matchState.setError(response.error);
          this.matchState.clearPendingRemovals();
        }
      },
      error: (err) => {
        this.interactionService.completeAction();
        this.matchState.clearPendingRemovals();
        this.matchState.setError({
          code: 'HTTP_ERROR',
          message: err.message ?? 'Request failed',
        });
      },
    });
    return observable;
  }

  endTurn(matchId: string, playerId?: string): Observable<GameActionResponse> {
    return this.dispatchAction(matchId, playerId, 'END_TURN', {});
  }

  putBasicOnBench(matchId: string, playerId: string, handIndex: number, benchIndex: number): Observable<GameActionResponse> {
    return this.dispatchAction(matchId, playerId, 'PUT_BASIC_ON_BENCH', { handIndex, benchIndex });
  }

  attachEnergy(
    matchId: string,
    playerId: string,
    handIndex: number,
    targetPokemonInstanceId: string,
  ): Observable<GameActionResponse> {
    return this.dispatchAction(matchId, playerId, 'ATTACH_ENERGY', {
      handIndex,
      targetPokemonInstanceId,
    });
  }

  evolvePokemon(
    matchId: string,
    playerId: string,
    handIndex: number,
    targetPokemonInstanceId: string,
  ): Observable<GameActionResponse> {
    return this.dispatchAction(matchId, playerId, 'EVOLVE_POKEMON', {
      handIndex,
      targetPokemonInstanceId,
    });
  }

  playTrainer(matchId: string, playerId: string, handIndex: number, extraPayload?: Record<string, unknown>): Observable<GameActionResponse> {
    return this.dispatchAction(matchId, playerId, 'PLAY_TRAINER', { handIndex, ...extraPayload });
  }

  retreatActive(matchId: string, playerId: string, benchIndex: number, energyCardInstanceIdsToDiscard?: string[]): Observable<GameActionResponse> {
    const payload: Record<string, unknown> = { benchIndex };
    if (energyCardInstanceIdsToDiscard && energyCardInstanceIdsToDiscard.length > 0) {
      payload['energyCardInstanceIdsToDiscard'] = energyCardInstanceIdsToDiscard;
    }
    return this.dispatchAction(matchId, playerId, 'RETREAT_ACTIVE', payload);
  }

  attachTool(
    matchId: string,
    playerId: string,
    handIndex: number,
    targetPokemonInstanceId: string,
  ): Observable<GameActionResponse> {
    return this.dispatchAction(matchId, playerId, 'ATTACH_TOOL', {
      handIndex,
      targetPokemonInstanceId,
    });
  }

  declareAttack(
    matchId: string,
    playerId: string,
    attackIndex: number,
    targetPokemonInstanceId: string,
  ): Observable<GameActionResponse> {
    return this.dispatchAction(matchId, playerId, 'DECLARE_ATTACK', {
      attackIndex,
      targetPokemonInstanceId,
    });
  }

  placeActive(matchId: string, playerId: string, cardInstanceId: string): Observable<GameActionResponse> {
    return this.dispatchAction(matchId, playerId, 'SETUP_PLACE_ACTIVE', { cardInstanceId });
  }

  placeBench(matchId: string, playerId: string, cardInstanceId: string, benchIndex: number): Observable<GameActionResponse> {
    return this.dispatchAction(matchId, playerId, 'SETUP_PLACE_BENCH', { cardInstanceId, benchIndex });
  }

  removeActive(matchId: string, playerId: string): Observable<GameActionResponse> {
    return this.dispatchAction(matchId, playerId, 'SETUP_REMOVE_ACTIVE', {});
  }

  removeBench(matchId: string, playerId: string, cardInstanceId: string): Observable<GameActionResponse> {
    return this.dispatchAction(matchId, playerId, 'SETUP_REMOVE_BENCH', { cardInstanceId });
  }

  confirmSetup(matchId: string, playerId: string): Observable<GameActionResponse> {
    return this.dispatchAction(matchId, playerId, 'CONFIRM_SETUP', {});
  }

  resolveMulliganDraw(matchId: string, playerId: string, drawCards: boolean): Observable<GameActionResponse> {
    return this.dispatchAction(matchId, playerId, 'RESOLVE_MULLIGAN_DRAW', { drawCards });
  }

  resolveInitialMulligan(matchId: string, playerId: string, decision: 'MULLIGAN' | 'KEEP'): Observable<GameActionResponse> {
    return this.dispatchAction(matchId, playerId, 'RESOLVE_INITIAL_MULLIGAN', { decision });
  }

  useAbility(
    matchId: string,
    playerId: string,
    pokemonInstanceId: string,
    abilityName: string,
    extraPayload?: Record<string, unknown>,
  ): Observable<GameActionResponse> {
    return this.dispatchAction(matchId, playerId, 'USE_ABILITY', {
      pokemonInstanceId,
      abilityName,
      ...extraPayload,
    });
  }
}
