import { computed, inject, Injectable, signal } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { CardRepositoryService } from '../../../core/services/card-repository.service';
import { MatchApiService } from '../../../core/api/match-api.service';
import { MatchSocketService } from '../../../core/websocket/match-socket.service';
import {
  GameErrorModel,
  GameEventDto,
} from '../../../shared/models/game-action.models';
import {
  PrivateHandCardModel,
  PrivatePlayerStateModel,
  PublicGameStateModel,
  PublicPokemonSlotModel,
} from '../../../shared/models/game-state.models';
import { EnergyType } from '../../../shared/models/card.models';
import { ToastService } from '../../../shared/services/toast.service';
import { AudioService } from '../../../core/audio/audio.service';
import { filter, Subscription } from 'rxjs';

export type ConnectionStatus = 'DISCONNECTED' | 'CONNECTED' | 'RECONNECTING';

@Injectable({ providedIn: 'root' })
export class MatchStateService {
  private readonly matchApi = inject(MatchApiService);
  private readonly matchSocket = inject(MatchSocketService);
  private readonly authService = inject(AuthService);
  private readonly cardRepo = inject(CardRepositoryService);
  private readonly toastService = inject(ToastService);
  private readonly audioService = inject(AudioService);

  private readonly _matchId = signal<string | null>(null);
  readonly matchId = this._matchId.asReadonly();

  private readonly _publicState = signal<PublicGameStateModel | null>(null);
  readonly publicState = this._publicState.asReadonly();

  private readonly _privateState = signal<PrivatePlayerStateModel | null>(null);
  readonly privateState = this._privateState.asReadonly();

  private readonly _events = signal<GameEventDto[]>([]);
  readonly events = this._events.asReadonly();

  private readonly _pendingRemovals = signal<Set<string>>(new Set());

  private readonly _damagePopups = signal<Map<string, number>>(new Map());
  readonly damagePopups = this._damagePopups.asReadonly();

  private readonly _weaknessPopups = signal<Map<string, number>>(new Map());
  readonly weaknessPopups = this._weaknessPopups.asReadonly();

  private readonly _resistancePopups = signal<Map<string, number>>(new Map());
  readonly resistancePopups = this._resistancePopups.asReadonly();

  private readonly _energyAttachFlashes = signal<Map<string, string>>(new Map());
  readonly energyAttachFlashes = this._energyAttachFlashes.asReadonly();

  private readonly _lastError = signal<GameErrorModel | null>(null);
  readonly lastError = this._lastError.asReadonly();

  private readonly _connectionStatus = signal<ConnectionStatus>('DISCONNECTED');
  readonly connectionStatus = this._connectionStatus.asReadonly();

  private readonly _opponentReconnected = signal(false);
  readonly opponentReconnected = this._opponentReconnected.asReadonly();

  private readonly _opponentDisconnected = signal(false);
  readonly opponentDisconnected = this._opponentDisconnected.asReadonly();

  private readonly _attackCoinFlip = signal<'HEADS' | 'TAILS' | null>(null);
  readonly attackCoinFlip = this._attackCoinFlip.asReadonly();

  private readonly _multiCoinFlips = signal<{ result: 'HEADS' | 'TAILS'; flipIndex: number; totalFlips: number }[]>([]);
  readonly multiCoinFlips = this._multiCoinFlips.asReadonly();
  private multiCoinTimer: ReturnType<typeof setTimeout> | null = null;

  private readonly popupTimers = new Map<string, ReturnType<typeof setTimeout>>();
  private readonly energyFlashTimers = new Map<string, ReturnType<typeof setTimeout>>();

  private readonly _koTrigger = signal<string | null>(null);
  readonly koTrigger = this._koTrigger.asReadonly();

  private readonly _activeSlotFlash = signal<string | null>(null);
  readonly activeSlotFlash = this._activeSlotFlash.asReadonly();

  private readonly _stadiumBg = signal<string | null>(null);
  readonly stadiumBg = this._stadiumBg.asReadonly();

  readonly isMyTurn = computed(() => {
    const currentId = this._publicState()?.currentPlayerId ?? null;
    const myId = this.myPlayerId();
    return currentId !== null && myId !== null && currentId === myId;
  });

  readonly currentPhase = computed(() => {
    const phase = this._publicState()?.phase;
    if (phase === 'DRAW' || phase === 'MAIN' || phase === 'ATTACK' || phase === 'BETWEEN_TURNS') {
      return phase;
    }
    return null;
  });

  readonly canDraw = computed(() => {
    const state = this._publicState();
    if (!state || state.status !== 'ACTIVE' || state.phase !== 'DRAW') return false;
    if (!this.isMyTurn()) return false;
    // First player doesn't draw on their first turn (TCG rule)
    if (state.turnNumber === 1 && state.currentPlayerId === state.firstPlayerId) return false;
    return true;
  });

  readonly canAttachEnergy = computed(() => {
    const state = this._publicState();
    if (!state || state.status !== 'ACTIVE' || state.phase !== 'MAIN') return false;
    if (!this.isMyTurn()) return false;
    if (state.hasAttachedEnergy) return false;
    return true;
  });

  readonly myPlayerId = computed(() => this.authService.playerId());

  readonly myActivePokemon = computed(() => {
    const publicState = this._publicState();
    const myId = this.myPlayerId();
    if (!publicState || !myId) {
      return null;
    }
    const playerState = publicState.players.find(p => p.playerId === myId);
    return playerState?.activePokemon ?? null;
  });

  readonly myBench = computed(() => {
    const publicState = this._publicState();
    const myId = this.myPlayerId();
    if (!publicState || !myId) return [];
    const playerState = publicState.players.find(p => p.playerId === myId);
    return playerState?.bench ?? [];
  });

  readonly opponentActivePokemon = computed(() => {
    const publicState = this._publicState();
    const myId = this.myPlayerId();
    if (!publicState || !myId) {
      return null;
    }
    const opponentState = publicState.players.find(p => p.playerId !== myId);
    return opponentState?.activePokemon ?? null;
  });

  readonly activePokemonRetreatCost = computed<EnergyType[]>(() => {
    const active = this.myActivePokemon();
    if (!active) return [];
    const def = this.cardRepo.getFromCache(active.cardId);
    return (def?.retreatCost as EnergyType[]) ?? [];
  });

  private resolveToEnergyType(value: string): string | null {
    const upper = value.toUpperCase();
    const KNOWN = new Set(['GRASS','FIRE','WATER','LIGHTNING','PSYCHIC','FIGHTING','DARKNESS','METAL','FAIRY','COLORLESS']);
    if (KNOWN.has(upper)) return upper;
    console.warn(`[DEBUG] resolveToEnergyType: '${value}' not a known type, trying card cache`);
    const def = this.cardRepo.getFromCache(value);
    if (def?.providesEnergyTypes?.length) {
      return def.providesEnergyTypes[0].toUpperCase();
    }
    if (def?.types?.length) {
      return def.types[0].toUpperCase();
    }
    return null;
  }

  readonly activePokemonEnergyTypes = computed<EnergyType[]>(() => {
    const active = this.myActivePokemon();
    if (!active) {
      console.warn(`[DEBUG] activePokemonEnergyTypes: no active Pokemon`);
      return [];
    }
    if (!active.attachedCards || active.attachedCards.length === 0) {
      console.warn(`[DEBUG] activePokemonEnergyTypes: active=${active.instanceId} has NO attachedCards`);
      return [];
    }
    const result: EnergyType[] = [];
    for (const c of active.attachedCards) {
      const resolved = this.resolveToEnergyType(c);
      if (resolved) result.push(resolved as EnergyType);
    }
    console.warn(`[DEBUG] activePokemonEnergyTypes: active=${active.instanceId} cards=[${active.attachedCards.join(',')}] resolved=[${result.join(',')}]`);
    return result;
  });

  private wsSubscription: Subscription | null = null;

  initialize(matchId: string): void {
    if (this._matchId() === matchId) return;

    this.wsSubscription?.unsubscribe();
    this.wsSubscription = null;
    this.matchSocket.disconnect();
    this._matchId.set(matchId);

    const playerId = this.myPlayerId();
    if (!playerId) {
      this._lastError.set({ code: 'AUTH_REQUIRED', message: 'Player not authenticated' });
      return;
    }

    this.matchSocket.connect(matchId, playerId);
    // NOTA: NO setear CONNECTED acá — el status real llega por
    // matchSocket.connectionStatus$ (abajo). Si lo seteamos antes de que
    // el WebSocket esté listo, dispatchAction manda acciones por WS que
    // sendAction() descarta silenciosamente porque client.active es false,
    // pero el caller cree que se enviaron. Esto genera un loop de re-intentos.

    const publicSub = this.matchSocket.publicEvents$.subscribe(event => {
      if (event.type === 'STATE_UPDATED') {
        const publicStatePayload = event.payload?.['publicState'];
        if (publicStatePayload) {
          this.updatePublicState(publicStatePayload as PublicGameStateModel);
        }
      } else {
        this.addEvent(event);
      }
    });

    const privateSub = this.matchSocket.privateState$.subscribe(state => {
      this.updatePrivateState(state);
    });

    const errorSub = this.matchSocket.actionErrors$.subscribe(error => {
      this.clearPendingRemovals();
      console.error('[MatchState] Action error:', error);
      this._lastError.set(error);
    });

    const connectionSub = this.matchSocket.connectionStatus$.subscribe(status => {
      console.warn(`[DEBUG] connectionStatus cambió a: ${status}`);
      this._connectionStatus.set(status as ConnectionStatus);
    });

    this.wsSubscription = new Subscription();
    this.wsSubscription.add(publicSub);
    this.wsSubscription.add(privateSub);
    this.wsSubscription.add(errorSub);
    this.wsSubscription.add(connectionSub);

    this.pollMatchState(matchId, playerId);
  }

  private pollMatchState(matchId: string, playerId: string, attempt: number = 0): void {
    if (this._matchId() !== matchId || attempt >= 60) return;

    this.matchApi.getMatchState(matchId, playerId).subscribe({
      next: (response) => {
        this._publicState.set(response.publicState);
        this._privateState.set(response.privateState);
      },
      error: () => {
        setTimeout(() => this.pollMatchState(matchId, playerId, attempt + 1), 2000);
      },
    });
  }

  updatePublicState(state: PublicGameStateModel): void {
    const myId = this.myPlayerId();
    const me = myId ? state.players.find(p => p.playerId === myId) : null;
    const active = me?.activePokemon;
    console.warn(`[DEBUG] updatePublicState: status=${state.status} myActive=${active?.instanceId} attachedCards=${JSON.stringify(active?.attachedCards)}`);
    this._publicState.set(state);
    if (state.stadiumCardDefinitionId) {
      this.cardRepo.resolve(state.stadiumCardDefinitionId).then(cardDef => {
        if (this._publicState()?.stadiumCardDefinitionId !== state.stadiumCardDefinitionId) return;
        const name = cardDef?.name;
        if (name) this._stadiumBg.set(`assets/images/${name}.png`);
      });
    }
  }

  clearPendingRemovals(): void {
    if (this._pendingRemovalCards.size > 0) {
      this._privateState.update(state => {
        if (!state) return state;
        const cardsToAdd: PrivateHandCardModel[] = [];
        for (const [, card] of this._pendingRemovalCards) {
          if (!state.hand.some(c => c.instanceId === card.instanceId)) {
            cardsToAdd.push(card);
          }
        }
        if (cardsToAdd.length === 0) return state;
        console.warn(`[DEBUG] clearPendingRemovals: re-adding ${cardsToAdd.length} cards to hand on error`);
        return { ...state, hand: [...state.hand, ...cardsToAdd] };
      });
      this._pendingRemovalCards.clear();
    }
    this._pendingRemovals.set(new Set());
  }

  private _pendingRemovalCards = new Map<string, PrivateHandCardModel>();

  private trackPendingRemoval(instanceId: string): void {
    this._pendingRemovals.update(set => {
      const next = new Set(set);
      next.add(instanceId);
      return next;
    });
  }

  optimisticallyRemoveCardFromHand(handIndex: number): void {
    this._privateState.update(state => {
      if (!state) {
        console.warn('[DEBUG] optimisticallyRemoveCardFromHand: state is null');
        return state;
      }
      if (handIndex < 0 || handIndex >= state.hand.length) {
        console.warn(`[DEBUG] optimisticallyRemoveCardFromHand: handIndex ${handIndex} out of bounds (hand size ${state.hand.length})`);
        return state;
      }
      const removed = state.hand[handIndex];
      const newHand = state.hand.filter((_, i) => i !== handIndex);
      console.warn(`[DEBUG] optimisticallyRemoveCardFromHand: removed ${removed?.name} (idx ${handIndex}), hand size ${state.hand.length} -> ${newHand.length}`);
      this.trackPendingRemoval(removed.instanceId);
      this._pendingRemovalCards.set(removed.instanceId, removed);
      return { ...state, hand: newHand };
    });
  }

  optimisticallyRemoveCardByInstanceId(cardInstanceId: string): void {
    this._privateState.update(state => {
      if (!state) return state;
      const index = state.hand.findIndex(c => c.instanceId === cardInstanceId);
      if (index < 0) {
        console.warn(`[DEBUG] optimisticallyRemoveCardByInstanceId: instanceId ${cardInstanceId} not found in hand`);
        return state;
      }
      const removed = state.hand[index];
      const newHand = state.hand.filter((_, i) => i !== index);
      console.warn(`[DEBUG] optimisticallyRemoveCardByInstanceId: removed idx ${index}, hand size ${state.hand.length} -> ${newHand.length}`);
      this.trackPendingRemoval(cardInstanceId);
      this._pendingRemovalCards.set(cardInstanceId, removed);
      return { ...state, hand: newHand };
    });
  }

  updatePrivateState(state: PrivatePlayerStateModel): void {
    const pending = this._pendingRemovals();
    if (pending.size > 0) {
      const confirmedRemoved = Array.from(pending).filter(
        id => !state.hand.some(c => c.instanceId === id)
      );
      // Filter out ALL pending removals (both confirmed and unconfirmed) from incoming state
      const filtered = state.hand.filter(c => !pending.has(c.instanceId));
      this._privateState.set({ ...state, hand: filtered });
      if (confirmedRemoved.length > 0) {
        this._pendingRemovals.update(set => {
          const next = new Set(set);
          for (const id of confirmedRemoved) next.delete(id);
          return next;
        });
        for (const id of confirmedRemoved) this._pendingRemovalCards.delete(id);
      }
      return;
    }
    this._privateState.set(state);
  }

  addEvent(event: GameEventDto): void {
    this._events.update(events => [...events, event]);

    if (event.type === 'DAMAGE_APPLIED') {
      const targetId = (event.payload?.['defenderPokemonInstanceId']
        ?? event.payload?.['attackerPokemonInstanceId']) as string | undefined;
      const defenderId = event.payload?.['defenderPokemonInstanceId'] as string | undefined;
      const finalDmg = event.payload?.['finalDamage'] as number | undefined;
      const countersDmg = event.payload?.['damageCountersAdded'] as number | undefined;
      const damage = finalDmg ?? (countersDmg != null ? countersDmg * 10 : undefined);
      const weakness = event.payload?.['weaknessApplied'] as boolean | undefined;
      const resistance = event.payload?.['resistanceApplied'] as boolean | undefined;
      if (targetId && damage) {
        this.schedulePopup(this._damagePopups, targetId, damage, this.popupTimers);
      }
      if (weakness && defenderId) {
        const mult = (event.payload?.['weaknessMultiplier'] as number) ?? 2;
        this.schedulePopup(this._weaknessPopups, defenderId, mult, this.popupTimers);
      }
      if (resistance && defenderId) {
        const val = (event.payload?.['resistanceValue'] as number) ?? -20;
        this.schedulePopup(this._resistancePopups, defenderId, val, this.popupTimers);
      }
    }

    if (event.type === 'RECOIL_OCCURRED') {
      const recoilTarget = event.payload?.['attackerPokemonInstanceId'] as string | undefined;
      const recoilDmg = event.payload?.['damage'] as number | undefined;
      if (recoilTarget && recoilDmg) {
        this.schedulePopup(this._damagePopups, recoilTarget, recoilDmg, this.popupTimers);
        this.toastService.show(`¡${recoilDmg} de daño por retroceso!`, 'hostile', 3000);
      }
    }

    if (event.type === 'ENERGY_DISCARDED') {
      this.toastService.show('Energía descartada', 'energy', 2000);
    }

    if (event.type === 'ENERGY_ATTACHED') {
      const targetId = event.payload?.['pokemonInstanceId'] as string | undefined;
      const energyCardId = event.payload?.['energyCardId'] as string | undefined;
      if (targetId && energyCardId) {
        this.audioService.playEnergyAttachSound();
        this.cardRepo.resolve(energyCardId).then(cardDef => {
          const energyType = (cardDef?.providesEnergyTypes?.[0] ?? cardDef?.types?.[0])?.toUpperCase() ?? 'COLORLESS';
          this._energyAttachFlashes.update(map => {
            const next = new Map(map);
            next.set(targetId, energyType);
            return next;
          });
          const existing = this.energyFlashTimers.get(targetId);
          if (existing) clearTimeout(existing);
          this.energyFlashTimers.set(targetId, setTimeout(() => {
            this._energyAttachFlashes.update(map => {
              const next = new Map(map);
              next.delete(targetId);
              return next;
            });
            this.energyFlashTimers.delete(targetId);
          }, 1000));
        });
      }
    }

    if (event.type === 'CARDS_DRAWN') {
      const owner = event.payload?.['playerId'] as string | undefined;
      if (owner === this.myPlayerId()) {
        this.toastService.show('Robaste cartas por efecto', 'info', 2000);
      }
    }

    if (event.type === 'BENCH_DAMAGE') {
      this.toastService.show('Daño a Pokémon en banca', 'hostile', 2500);
    }

    if (event.type === 'POKEMON_PLACED_ON_BENCH' || event.type === 'SETUP_BENCH_PLACED') {
      this.audioService.playBenchSound();
    }

    if (event.type === 'ATTACK_CANCELED') {
      const reason = event.payload?.['reason'] as string | undefined;
      if (reason === 'asleep') this.toastService.show('¡Está Dormido! No puede atacar', 'hostile', 3000);
      else if (reason === 'paralyzed') this.toastService.show('¡Está Paralizado! No puede atacar', 'hostile', 3000);
      else this.toastService.show('El ataque falló', 'hostile', 3000);
    }

    if (event.type === 'COIN_FLIP_RESULT') {
      const source = event.payload?.['source'] as string | undefined;
      const result = event.payload?.['result'] as 'HEADS' | 'TAILS' | undefined;
      if (source === 'multi_coin_flip') {
        const flipIndex = event.payload?.['flipIndex'] as number | undefined;
        const totalFlips = event.payload?.['totalFlips'] as number | undefined;
        if (result && flipIndex != null) {
          this._multiCoinFlips.update(arr => [...arr, { result, flipIndex, totalFlips: totalFlips ?? 0 }]);
          if (this.multiCoinTimer != null) clearTimeout(this.multiCoinTimer);
          this.multiCoinTimer = setTimeout(() => this._multiCoinFlips.set([]), 3000);
        }
      } else if (source === 'sleep_check') {
        if (result) {
          this._attackCoinFlip.set(result);
          setTimeout(() => this._attackCoinFlip.set(null), 2000);
        }
        if (result === 'HEADS') {
          this.toastService.show('¡El Pokémon despertó!', 'heal', 3000);
        } else {
          this.toastService.show('El Pokémon sigue dormido...', 'hostile', 3000);
        }
      } else if (source === 'mental_panic') {
        if (result) {
          this._attackCoinFlip.set(result);
          setTimeout(() => this._attackCoinFlip.set(null), 2000);
        }
        if (result === 'TAILS') {
          this.toastService.show('¡El ataque falló por Pánico Mental!', 'hostile', 3000);
        } else {
          this.toastService.show('El Pokémon superó el Pánico Mental.', 'heal', 3000);
        }
      } else if (source === 'attack_effect' || source === 'attack_cancel') {
        if (result) {
          this._attackCoinFlip.set(result);
          setTimeout(() => this._attackCoinFlip.set(null), 2000);
        }
      }
    }

    if (event.type === 'KNOCKOUT_OCCURRED') {
      const owner = event.payload?.['ownerPlayerId'] as string | undefined;
      if (owner === this.myPlayerId()) {
        this.toastService.show('¡Debilitaron a tu Pokémon!', 'hostile', 4000);
      } else {
        this.toastService.show('¡Debilitaste a un Pokémon del oponente!', 'reward', 4000);
      }
      this._koTrigger.set(owner ?? null);
      setTimeout(() => this._koTrigger.set(null), 2500);
    }

    if (event.type === 'RETREAT_EXECUTED' || event.type === 'SWITCH_EXECUTED') {
      const oldActive = (event.payload?.['oldActivePokemonInstanceId']
        ?? event.payload?.['previousActiveInstanceId']
        ?? event.payload?.['oldActiveInstanceId']) as string | undefined;
      if (oldActive) {
        const state = this._publicState();
        const owner = state?.players.find(p =>
          p.bench.some(b => b.instanceId === oldActive)
        );
        const ownerId = owner?.playerId ?? null;
        this._activeSlotFlash.set(ownerId);
        this.audioService.playActivePokemonSound();
        setTimeout(() => this._activeSlotFlash.set(null), 1000);
      }
    }

    if (event.type === 'ATTACK_DECLARED') {
      const myId = this.myPlayerId();
      const currentPlayerId = this._publicState()?.currentPlayerId;
      if (currentPlayerId && currentPlayerId !== myId) {
        const opponent = this._publicState()?.players.find(p => p.playerId !== myId);
        const active = opponent?.activePokemon;
        if (active) {
          this.cardRepo.resolve(active.cardId).then(cardDef => {
            const type = cardDef?.types?.[0];
            if (type) {
              this.audioService.playTypeSound(type);
            }
          });
        }
      }
    }

    if (event.type === 'PRIZE_TAKEN') {
      const count = (event.payload?.['prizeCount'] as number) ?? 1;
      const owner = event.payload?.['playerId'] as string | undefined;
      if (owner === this.myPlayerId()) {
        this.toastService.show(`Tomaste ${count} carta de premio`, 'reward', 3000);
      }
    }

    if (event.type === 'STADIUM_PLAYED') {
      const cardDefId = event.payload?.['cardDefinitionId'] as string | undefined;
      if (cardDefId) {
        this.cardRepo.resolve(cardDefId).then(cardDef => {
          const name = cardDef?.name;
          if (name) this._stadiumBg.set(`assets/images/${name}.png`);
        });
      }
    }

    if (event.type === 'STADIUM_REMOVED') {
      this._stadiumBg.set(null);
    }

    if (event.type === 'STATUS_APPLIED' && event.payload?.['blocked'] !== true) {
      const condition = event.payload?.['condition'] as string | undefined;
      if (condition) {
        const translated: Record<string, string> = {
          ASLEEP: 'Dormido', BURNED: 'Quemado', CONFUSED: 'Confundido',
          PARALYZED: 'Paralizado', POISONED: 'Envenenado',
        };
        this.toastService.show(`¡${translated[condition] ?? condition}!`, 'hostile', 3000);
      }
    }

    if (event.type === 'STATUS_REMOVED') {
      const cond = event.payload?.['condition'] as string | undefined;
      if (cond) {
        const translated: Record<string, string> = {
          ASLEEP: 'Dormido', BURNED: 'Quemado', CONFUSED: 'Confundido',
          PARALYZED: 'Paralizado', POISONED: 'Envenenado',
        };
        this.toastService.show(`${translated[cond] ?? cond} removido`, 'heal', 3000);
      } else {
        this.toastService.show('Condición eliminada', 'heal', 3000);
      }
    }

    if (event.type === 'ENERGY_SEARCHED') {
      const count = (event.payload?.['count'] as number) ?? 0;
      if (count > 0) {
        this.toastService.show(`Buscaste ${count} energía(s) básicas en tu mazo`, 'info', 2500);
      }
    }

    if (event.type === 'POKEMON_EVOLVED') {
      const owner = event.payload?.['playerId'] as string | undefined;
      if (!owner || owner === this.myPlayerId()) {
        this.toastService.show('¡Pokémon evolucionado!', 'info', 2500);
      }
    }

    if (event.type === 'POKEMON_HEALED') {
      const counters = (event.payload?.['countersRemoved'] as number ?? event.payload?.['healedCounters'] as number | undefined) ?? 0;
      if (counters > 0) {
        const owner = event.payload?.['playerId'] as string | undefined;
        if (owner === this.myPlayerId()) {
          this.toastService.show(`Recuperaste ${counters * 10} PS`, 'heal', 3000);
        }
      }
    }

    if (event.type === 'CONFUSION_SELF_HIT') {
      this.toastService.show('¡Se golpeó a sí mismo por confusión!', 'hostile', 3000);
    }

    if (event.type === 'DECK_PEEKED') {
      const cardName = event.payload?.['cardName'] as string | undefined;
      if (cardName) {
        this.toastService.show(`La carta del tope del mazo rival es: "${cardName}"`, 'info', 5000);
      }
    }

    if (event.type === 'DECK_ORDERED') {
      const peekCount = event.payload?.['count'] as number | undefined;
      if (peekCount && this.myPlayerId()) {
        this.toastService.show(`Mirá las ${peekCount} cartas del tope de tu mazo. Podés reordenarlas.`, 'info', 5000);
      }
    }

    if (event.type === 'OPPONENT_RANDOM_DISCARD') {
      const cardName = event.payload?.['cardName'] as string | undefined;
      if (cardName) {
        this.toastService.show(`"${cardName}" de la mano rival fue barajado al mazo.`, 'hostile', 5000);
      }
    }

    if (event.type === 'POKEMON_SEARCHED') {
      const owner = event.payload?.['playerId'] as string | undefined;
      const found = (event.payload?.['foundCount'] as number) ?? 0;
      if (owner === this.myPlayerId()) {
        this.toastService.show(`Buscaste ${found} carta(s) en tu mazo`, 'info', 2500);
      }
    }

    if (event.type === 'TOOL_ATTACHED') {
      const owner = event.payload?.['playerId'] as string | undefined;
      if (owner === this.myPlayerId()) {
        this.toastService.show('¡Herramienta equipada!', 'info', 2500);
      }
    }

    if (event.type === 'PLAYER_RECONNECTED') {
      const pid = event.payload?.['playerId'] as string | undefined;
      if (pid && pid !== this.myPlayerId()) {
        this._opponentReconnected.set(true);
        this._opponentDisconnected.set(false);
        this.toastService.show('El oponente se reconectó', 'info', 3000);
      }
    }

    if (event.type === 'PLAYER_DISCONNECTED') {
      const pid = event.payload?.['playerId'] as string | undefined;
      if (pid && pid !== this.myPlayerId()) {
        this._opponentDisconnected.set(true);
        this.toastService.show('El oponente se desconectó. Esperando reconexión...', 'info', 5000);
      }
    }

    if (event.type === 'ABILITY_USED') {
      const bonus = event.payload?.['bonus'] as number | undefined;
      const preventEffects = event.payload?.['preventEffects'] as boolean | undefined;
      const targetPlayerId = event.payload?.['targetPlayerId'] as string | undefined;
      const hasAbilityName = event.payload?.['abilityName'] != null;
      const hasPokemonId = event.payload?.['pokemonInstanceId'] != null;

      if (bonus != null) {
        if (event.payload?.['playerId'] === this.myPlayerId()) {
          this.toastService.show(`¡Tu Pokémon hará +${bonus} de daño el próximo turno!`, 'info', 3000);
        }
      } else if (preventEffects) {
        this.toastService.show('¡El Pokémon se protegió! No recibirá daño el próximo turno.', 'heal', 3000);
      } else if (targetPlayerId && !hasAbilityName) {
        if (targetPlayerId === this.myPlayerId()) {
          this.toastService.show('¡No podés jugar Partidarios el próximo turno!', 'hostile', 3000);
        }
      } else if (hasPokemonId && !hasAbilityName) {
        this.toastService.show('¡El Pokémon no puede atacar el próximo turno!', 'hostile', 3000);
      }
    }
  }

  private schedulePopup(
    signalRef: ReturnType<typeof signal<Map<string, number>>>,
    key: string,
    value: number,
    timerMap: Map<string, ReturnType<typeof setTimeout>>,
    durationMs: number = 3000,
  ): void {
    const existing = timerMap.get(key);
    if (existing != null) clearTimeout(existing);
    signalRef.update(map => new Map(map).set(key, value));
    const timerId = setTimeout(() => {
      signalRef.update(map => {
        const next = new Map(map);
        next.delete(key);
        return next;
      });
      timerMap.delete(key);
    }, durationMs);
    timerMap.set(key, timerId);
  }

  setError(error: GameErrorModel | null): void {
    this._lastError.set(error);
  }

  reset(): void {
    this.wsSubscription?.unsubscribe();
    this.wsSubscription = null;
    this.matchSocket.disconnect();
    this._matchId.set(null);
    this._publicState.set(null);
    this._privateState.set(null);
    this._events.set([]);
    this._damagePopups.set(new Map());
    this._weaknessPopups.set(new Map());
    this._resistancePopups.set(new Map());
    this._lastError.set(null);
    this._connectionStatus.set('DISCONNECTED');
    this._opponentReconnected.set(false);
    this._opponentDisconnected.set(false);
    this._pendingRemovals.set(new Set());
    this._pendingRemovalCards.clear();
    if (this.multiCoinTimer != null) clearTimeout(this.multiCoinTimer);
    this.multiCoinTimer = null;
    this._multiCoinFlips.set([]);
    this._activeSlotFlash.set(null);
    this._stadiumBg.set(null);
  }
}
