export type GameActionType =
  | 'PUT_BASIC_ON_BENCH'
  | 'ATTACH_ENERGY'
  | 'EVOLVE_POKEMON'
  | 'PLAY_TRAINER'
  | 'DECLARE_ATTACK'
  | 'RETREAT_ACTIVE'
  | 'END_TURN'
  | 'DRAW_CARD'
  | 'CHOOSE_KO_REPLACEMENT'
  | 'TAKE_PRIZE_CARD'
  | 'ATTACH_TOOL'
  | 'SETUP_PLACE_ACTIVE'
  | 'SETUP_PLACE_BENCH'
  | 'SETUP_REMOVE_ACTIVE'
  | 'SETUP_REMOVE_BENCH'
  | 'CONFIRM_SETUP'
  | 'RESOLVE_MULLIGAN_DRAW'
  | 'RESOLVE_INITIAL_MULLIGAN'
  | 'USE_ABILITY';

export type GameEventType =
  | 'CARD_DRAWN'
  | 'VICTORY_DECIDED'
  | 'POKEMON_PLACED_ON_BENCH'
  | 'ENERGY_ATTACHED'
  | 'POKEMON_EVOLVED'
  | 'TRAINER_PLAYED'
  | 'RETREAT_EXECUTED'
  | 'DAMAGE_APPLIED'
  | 'KNOCKOUT_OCCURRED'
  | 'ATTACK_DECLARED'
  | 'PHASE_CHANGED'
  | 'STATE_UPDATED'
  | 'PRIZE_TAKEN'
  | 'MULLIGAN_REVEALED'
  | 'INITIAL_MULLIGAN_NEEDED'
  | 'INITIAL_MULLIGAN_RESOLVED'
  | 'MULLIGAN_DRAW_OPPORTUNITY'
  | 'MULLIGAN_DRAW_RESOLVED'
  | 'BENCH_DAMAGE'
  | 'STATUS_APPLIED'
  | 'ATTACK_EFFECT_RESOLVED'
  | 'POKEMON_HEALED'
  | 'ABILITY_USED'
  | 'ABILITY_BLOCKED'
  | 'CARDS_DRAWN'
  | 'STADIUM_PLAYED'
  | 'STADIUM_REMOVED'
  | 'TOOL_ATTACHED'
  | 'ENERGY_DISCARDED'
  | 'KO_REPLACEMENT_REQUIRED'
  | 'KO_REPLACEMENT_DONE'
  | 'TRAINER_EFFECT_RESOLVED'
  | 'CONFUSION_SELF_HIT'
  | 'SETUP_ACTIVE_PLACED'
  | 'SETUP_ACTIVE_REMOVED'
  | 'SETUP_BENCH_PLACED'
  | 'SETUP_BENCH_REMOVED'
  | 'SETUP_CONFIRMED'
  | 'SETUP_COMPLETED'
  | 'COIN_FLIP_RESULT'
  | 'SUDDEN_DEATH_STARTED'
  | 'ATTACK_CANCELED'
  | 'RECOIL_OCCURRED'
  | 'SWITCH_EXECUTED'
  | 'POKEMON_SEARCHED'
  | 'STATUS_REMOVED';

export interface GameActionRequest {
  type: GameActionType;
  playerId: string;
  payload: Record<string, unknown>;
  clientRequestId: string;
}

export interface GameErrorModel {
  code: string;
  message: string;
  details?: Record<string, unknown>;
}

export interface GameActionResponse {
  success: boolean;
  clientRequestId: string;
  publicState: import('./game-state.models').PublicGameStateModel | null;
  privateState: import('./game-state.models').PrivatePlayerStateModel | null;
  events: GameEventDto[];
  error: GameErrorModel | null;
}

export interface GameEventDto {
  type: string;
  message: string;
  payload?: Record<string, unknown>;
  turnNumber?: number;
}
