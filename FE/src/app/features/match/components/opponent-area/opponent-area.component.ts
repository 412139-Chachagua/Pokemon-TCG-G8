import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output } from '@angular/core';
import { PublicDiscardCardModel, PublicPlayerStateModel, PublicPokemonSlotModel } from '../../../../shared/models/game-state.models';
import { CardDetailResponse } from '../../../../shared/models/card.models';
import { CardRepositoryService } from '../../../../core/services/card-repository.service';
import { PokemonSlotComponent } from '../pokemon-slot/pokemon-slot.component';
import { BenchZoneComponent } from '../bench-zone/bench-zone.component';
import { PrizeZoneComponent } from '../prize-zone/prize-zone.component';
import { PokemonClickedEvent, SelectionMode } from '../../../../shared/models/ui-state.models';

@Component({
  selector: 'app-opponent-area',
  standalone: true,
  imports: [PokemonSlotComponent, BenchZoneComponent, PrizeZoneComponent],
  template: `
    @if (playerState(); as player) {
      <div class="flex flex-row gap-2 p-3 border border-red-700/60 rounded-lg flex-1 min-w-0 min-h-[440px]" [style.background]="'color-mix(in oklch, color-mix(in oklch, var(--pk-surface) 75%, transparent), #ef4444 30%)'">
        <div class="flex-[3] min-w-0 flex flex-col">
          <span class="block text-[0.625rem] font-bold text-[var(--pk-text-dim)] uppercase tracking-wider mb-1">Activo</span>
          @if (player.activePokemon; as active) {
            <app-pokemon-slot
              [pokemon]="active"
              [cardDef]="cardDefs().get(active.instanceId) ?? null"
              [isActive]="true"
              [isOwn]="false"
              [isHighlighted]="validTargets().includes(active.instanceId)"
              (slotClicked)="onPokemonClicked($event)"
            />
          } @else {
            <div class="p-4 text-center text-[var(--pk-text-dim)] border border-dashed border-[var(--pk-panel)] rounded-lg">Sin Pokémon activo</div>
          }
        </div>
        <div class="flex-[2] min-w-0 flex flex-col">
          <span class="block text-[0.625rem] font-bold text-[var(--pk-text-dim)] uppercase tracking-wider mb-1">Banca</span>
          <app-bench-zone
            [bench]="normalizedBench()"
            [cardDefs]="cardDefs()"
            [isOwn]="false"
            [discardCount]="discardCount()"
            [discard]="discard()"
            [validTargets]="validTargets()"
            [selectionMode]="selectionMode()"
            (slotClicked)="pokemonClicked.emit($event)"
            (viewDiscard)="viewDiscard.emit()"
          />
        </div>
        <div class="w-10 shrink-0">
          <app-prize-zone [prizeCount]="prizeCount()" [totalPrizeCount]="player.totalPrizeCount ?? 6" [isOwn]="false" [columns]="1" />
        </div>
      </div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OpponentAreaComponent {
  private readonly cardRepo = inject(CardRepositoryService);

  readonly playerState = input.required<PublicPlayerStateModel>();
  readonly validTargets = input<string[]>([]);
  readonly selectionMode = input<SelectionMode>('NONE');

  readonly pokemonClicked = output<PokemonClickedEvent>();
  readonly viewDiscard = output<void>();

  constructor() {
    effect(() => {
      const player = this.playerState();
      if (!player) return;
      const ids = new Set<string>();
      const addSlot = (poke: PublicPokemonSlotModel | null) => {
        if (!poke) return;
        ids.add(poke.cardId);
        if (poke.attachedToolCardDefinitionId) ids.add(poke.attachedToolCardDefinitionId);
      };
      addSlot(player.activePokemon);
      for (const poke of player.bench) addSlot(poke);
      for (const id of ids) {
        if (!this.cardRepo.getFromCache(id)) this.cardRepo.resolve(id);
      }
    });
  }

  readonly cardDefs = computed(() => {
    const player = this.playerState();
    const map = new Map<string, CardDetailResponse | null>();
    const addCard = (poke: PublicPokemonSlotModel | null) => {
      if (!poke) return;
      map.set(poke.instanceId, this.cardRepo.getFromCache(poke.cardId));
    };
    addCard(player.activePokemon);
    for (const poke of player.bench) addCard(poke);
    return map;
  });

  readonly prizeCount = computed(() => this.playerState().prizes.length);

  readonly discardCount = computed(() => this.playerState().discardCount ?? 0);

  readonly discard = computed<PublicDiscardCardModel[]>(() => this.playerState().discard ?? []);

  readonly normalizedBench = computed<(PublicPokemonSlotModel | null)[]>(() => {
    const bench = this.playerState().bench;
    const result: (PublicPokemonSlotModel | null)[] = [...bench];
    while (result.length < 5) result.push(null);
    return result;
  });

  protected onPokemonClicked(pokemon: PublicPokemonSlotModel): void {
    this.pokemonClicked.emit({ instanceId: pokemon.instanceId });
  }
}
