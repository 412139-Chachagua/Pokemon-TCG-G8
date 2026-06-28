import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

import { MatchHeaderComponent } from '../../../match/components/match-header/match-header.component';
import { PlayerAreaComponent } from '../../../match/components/player-area/player-area.component';
import { OpponentAreaComponent } from '../../../match/components/opponent-area/opponent-area.component';
import { HandZoneComponent } from '../../../match/components/hand-zone/hand-zone.component';
import { GameLogComponent } from '../../../match/components/game-log/game-log.component';
import { ActionPanelComponent } from '../../../match/components/action-panel/action-panel.component';

import { CardRepositoryService } from '../../../../core/services/card-repository.service';

import {
  PublicGameStateModel,
  PublicPlayerStateModel,
  PublicPokemonSlotModel,
  PrivateHandCardModel,
} from '../../../../shared/models/game-state.models';
import { GameEventDto } from '../../../../shared/models/game-action.models';

// ---------------------------------------------------------------------------
// Mock data — IDs reales de Pokémon TCG para que las imágenes carguen del CDN
// ---------------------------------------------------------------------------

const MY_PLAYER_ID = 'sandbox-p1';
const OPPONENT_PLAYER_ID = 'sandbox-p2';

// ── Jugador 1 (vos) ────────────────────────────────────────────────────────

const MY_ACTIVE: PublicPokemonSlotModel = {
  instanceId: 's-p1-active',
  cardId: 'base1-4', // Charizard
  damageCounters: 2,
  specialConditions: [],
  attachedCards: ['FIRE', 'FIRE', 'COLORLESS'],
};

const MY_BENCH: PublicPokemonSlotModel[] = [
  {
    instanceId: 's-p1-bench-0',
    cardId: 'base1-17', // Hitmonchan
    damageCounters: 0,
    specialConditions: [],
    attachedCards: ['FIGHTING'],
  },
  {
    instanceId: 's-p1-bench-1',
    cardId: 'base1-6', // Gyarados
    damageCounters: 0,
    specialConditions: ['PARALYZED'],
    attachedCards: ['WATER'],
  },
];

const MY_PLAYER_STATE: PublicPlayerStateModel = {
  playerId: MY_PLAYER_ID,
  side: 'PLAYER_ONE',
  activePokemon: MY_ACTIVE,
  bench: MY_BENCH,
  prizes: ['▰', '▰', '▰', '▰', '', ''],
  totalPrizeCount: 6,
  setupConfirmed: true,
};

// ── Jugador 2 (oponente) ──────────────────────────────────────────────────

const OPPONENT_ACTIVE: PublicPokemonSlotModel = {
  instanceId: 's-p2-active',
  cardId: 'base1-12', // Mewtwo
  damageCounters: 1,
  specialConditions: ['CONFUSED'],
  attachedCards: ['PSYCHIC', 'PSYCHIC'],
};

const OPPONENT_BENCH: PublicPokemonSlotModel[] = [
  {
    instanceId: 's-p2-bench-0',
    cardId: 'base1-11', // Raichu
    damageCounters: 3,
    specialConditions: [],
    attachedCards: ['LIGHTNING'],
  },
  {
    instanceId: 's-p2-bench-1',
    cardId: 'base1-10', // Ninetales
    damageCounters: 0,
    specialConditions: ['ASLEEP'],
    attachedCards: [],
  },
];

const OPPONENT_PLAYER_STATE: PublicPlayerStateModel = {
  playerId: OPPONENT_PLAYER_ID,
  side: 'PLAYER_TWO',
  activePokemon: OPPONENT_ACTIVE,
  bench: OPPONENT_BENCH,
  prizes: ['▰', '▰', '▰', '▰', '▰', ''],
  totalPrizeCount: 6,
  setupConfirmed: true,
};

// ── Estado global de la partida ───────────────────────────────────────────

const MOCK_PUBLIC_STATE: PublicGameStateModel = {
  matchId: 'sandbox-match',
  status: 'ACTIVE',
  phase: 'MAIN',
  turnNumber: 3,
  currentPlayerId: MY_PLAYER_ID,
  firstPlayerId: MY_PLAYER_ID,
  players: [MY_PLAYER_STATE, OPPONENT_PLAYER_STATE],
  winnerPlayerId: null,
  finishReason: null,
};

// ── Mano del jugador ──────────────────────────────────────────────────────

const MOCK_HAND: PrivateHandCardModel[] = [
  { instanceId: 's-hand-0', cardId: 'base1-17', name: 'Hitmonchan', supertype: 'POKEMON' },
  { instanceId: 's-hand-1', cardId: 'base1-99', name: 'Fire Energy', supertype: 'ENERGY' },
  { instanceId: 's-hand-2', cardId: 'base1-63', name: 'Energy Removal', supertype: 'TRAINER' },
  { instanceId: 's-hand-3', cardId: 'base1-81', name: 'Potion', supertype: 'TRAINER' },
  { instanceId: 's-hand-4', cardId: 'base1-98', name: 'Fighting Energy', supertype: 'ENERGY' },
];

// ── Eventos del log ───────────────────────────────────────────────────────

const MOCK_EVENTS: GameEventDto[] = [
  { type: 'GAME_START', message: '⚔️ Comienza la partida', payload: {} },
  { type: 'DRAW', message: '🃏 Robaste una carta', payload: {} },
  { type: 'ENERGY_ATTACH', message: '⚡ Energía Fuego unida a Charizard', payload: {} },
  {
    type: 'DAMAGE_APPLIED',
    message: '💥 Mewtwo recibe 20 de daño',
    payload: { damageCountersAdded: 2 },
  },
  { type: 'SWITCH', message: '🔄 Cambiaste a Hitmonchan', payload: {} },
  { type: 'ENERGY_ATTACH', message: '⚡ Energía Lucha unida a Hitmonchan', payload: {} },
  {
    type: 'DAMAGE_APPLIED',
    message: '💥 Charizard recibe 50 de daño',
    payload: { damageCountersAdded: 5 },
  },
  { type: 'HEAL', message: '💚 Pocion usada — recuperás 20 PS', payload: {} },
  { type: 'PRIZE_TAKEN', message: '🏆 Tomaste 1 carta de premio', payload: {} },
  { type: 'SWITCH', message: '🔄 Oponente cambia a Raichu', payload: {} },
  { type: 'STATUS_APPLIED', message: '💤 Ninetales se ha dormido', payload: {} },
  { type: 'STATUS_APPLIED', message: '💫 Mewtwo está confundido', payload: {} },
  { type: 'STATUS_APPLIED', message: '⚡ Gyarados ha quedado paralizado', payload: {} },
  { type: 'ENERGY_REMOVAL', message: '🗑️ Usaste Quitaenergía contra el oponente', payload: {} },
  { type: 'ENERGY_ATTACH', message: '⚡ Energía Psíquica unida a Mewtwo', payload: {} },
  { type: 'ENERGY_ATTACH', message: '⚡ Energía Psíquica unida a Mewtwo', payload: {} },
  { type: 'BENCH_POKEMON', message: '🃏 Oponente mandó a Ninetales a la banca', payload: {} },
  { type: 'BENCH_POKEMON', message: '🃏 Oponente mandó a Raichu a la banca', payload: {} },
  { type: 'BENCH_POKEMON', message: '🃏 Mandaste a Hitmonchan a la banca', payload: {} },
  { type: 'BENCH_POKEMON', message: '🃏 Mandaste a Gyarados a la banca', payload: {} },
];

// ── Banderas de selección (todo desactivado) ──────────────────────────────

const EMPTY_TARGETS: string[] = [];
const DEFAULT_SELECTION_MODE = 'NONE' as const;

// ---------------------------------------------------------------------------
// Componente
// ---------------------------------------------------------------------------

@Component({
  selector: 'app-sandbox-page',
  imports: [
    RouterLink,
    MatchHeaderComponent,
    PlayerAreaComponent,
    OpponentAreaComponent,
    HandZoneComponent,
    GameLogComponent,
    ActionPanelComponent,
  ],
  template: `
    <!-- Banner de sandbox -->
    <div
      class="sticky top-0 z-50 flex items-center justify-between px-4 py-1.5 bg-amber-600/20 border-b border-amber-500/40 text-amber-300 text-xs font-mono"
    >
      <span>🧪 SANDBOX — datos mockeados · sin lógica</span>
      <a routerLink="/lobby" class="underline hover:text-amber-100 transition-colors">Salir</a>
    </div>

    <main class="flex flex-col gap-4 p-6 min-h-screen">
      <!-- Header -->
      <app-match-header [publicState]="publicState" [myPlayerId]="myPlayerId" />

      <!-- Áreas de juego (player + opponent lado a lado) -->
      @if (opponentPlayerState; as opp) {
        <div class="flex flex-row gap-2 flex-1 min-h-0">
          <div class="flex-1 min-w-0">
            <app-player-area
              [playerState]="myPlayerState"
              [validTargets]="emptyTargets"
              [selectionMode]="selectionMode"
            />
          </div>
          <div class="flex-1 min-w-0">
            <app-opponent-area
              [playerState]="opp"
              [validTargets]="emptyTargets"
              [selectionMode]="selectionMode"
            />
          </div>
        </div>
      }

      <!-- Mano -->
      <app-hand-zone [hand]="hand" [selectionMode]="selectionMode" />

      <!-- Panel de acciones (solo muestra "Esperando oponente" sin estado real) -->
      <app-action-panel />

      <!-- Log de eventos -->
      <app-game-log [events]="events" [myPlayerId]="myPlayerId" />
    </main>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SandboxPage implements OnInit {
  private readonly cardRepo = inject(CardRepositoryService);

  // Datos expuestos al template
  protected readonly publicState = MOCK_PUBLIC_STATE;
  protected readonly myPlayerId = MY_PLAYER_ID;
  protected readonly myPlayerState = MY_PLAYER_STATE;
  protected readonly opponentPlayerState = OPPONENT_PLAYER_STATE;
  protected readonly hand = MOCK_HAND;
  protected readonly events = MOCK_EVENTS;
  protected readonly emptyTargets = EMPTY_TARGETS;
  protected readonly selectionMode = DEFAULT_SELECTION_MODE;

  ngOnInit(): void {
    // Precargar definiciones de cartas en el repositorio (best-effort).
    // Si el backend no está corriendo, las imágenes de las cartas igual
    // cargan del CDN de Pokémon TCG, pero no se verán HP/ataques.
    const mockCardIds = [
      'base1-4', // Charizard
      'base1-17', // Hitmonchan
      'base1-6', // Gyarados
      'base1-12', // Mewtwo
      'base1-11', // Raichu
      'base1-10', // Ninetales
      'base1-99', // Fire Energy
      'base1-98', // Fighting Energy
      'base1-95', // Water Energy
      'base1-96', // Lightning Energy
      'base1-94', // Psychic Energy
      'base1-63', // Energy Removal
      'base1-81', // Potion
    ];
    this.cardRepo.preload(mockCardIds);
  }
}
