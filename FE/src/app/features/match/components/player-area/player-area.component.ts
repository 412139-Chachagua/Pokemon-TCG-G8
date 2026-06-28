import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output } from '@angular/core';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { PublicDiscardCardModel, PublicPlayerStateModel, PublicPokemonSlotModel } from '../../../../shared/models/game-state.models';
import { CardDetailResponse } from '../../../../shared/models/card.models';
import { CardRepositoryService } from '../../../../core/services/card-repository.service';
import { PokemonSlotComponent } from '../pokemon-slot/pokemon-slot.component';
import { BenchZoneComponent } from '../bench-zone/bench-zone.component';
import { PrizeZoneComponent } from '../prize-zone/prize-zone.component';
import { PokemonClickedEvent, SelectionMode } from '../../../../shared/models/ui-state.models';

@Component({
  selector: 'app-player-area',
  standalone: true,
  imports: [DragDropModule, PokemonSlotComponent, BenchZoneComponent, PrizeZoneComponent],
  template: `
    @if (playerState(); as player) {
      <div class="flex flex-row gap-2 p-3 border border-blue-700/60 rounded-lg flex-1 min-w-0 min-h-[440px]" [style.background]="'color-mix(in oklch, color-mix(in oklch, var(--pk-surface) 75%, transparent), #3b82f6 30%)'">
        <div class="w-10 shrink-0">
          <app-prize-zone [prizeCount]="prizeCount()" [totalPrizeCount]="player.totalPrizeCount ?? 6" [isOwn]="true" [columns]="1" />
        </div>
        <div class="flex-[2] min-w-0 flex flex-col">
          <span class="block text-[0.625rem] font-bold text-[var(--pk-text-dim)] uppercase tracking-wider mb-1">Banca</span>
          <app-bench-zone
            [bench]="normalizedBench()"
            [cardDefs]="cardDefs()"
            [isOwn]="true"
            [discardCount]="discardCount()"
            [discard]="discard()"
            [validTargets]="validTargets()"
            [selectionMode]="selectionMode()"
            [dragEnabled]="dragEnabled()"
            [connectedDropListIds]="connectedDropListIds()"
            (slotClicked)="pokemonClicked.emit($event)"
            (benchDropped)="benchDropped.emit($event)"
            (energyDropped)="onEnergyDropped($event)"
            (evolutionDropped)="onEvolutionDropped($event)"
            (trainerDropped)="onTrainerDropped($event)"
            (abilityClicked)="abilityClicked.emit($event)"
            (viewDiscard)="viewDiscard.emit()"
          />
        </div>
        <div class="flex-[3] min-w-0 flex flex-col">
          <span class="block text-[0.625rem] font-bold text-[var(--pk-text-dim)] uppercase tracking-wider mb-1">Activo</span>
          @if (player.activePokemon; as active) {
            <div
              cdkDropList
              id="active-slot"
              [cdkDropListConnectedTo]="connectedDropListIds()"
              [cdkDropListDisabled]="!dragEnabled()"
              (cdkDropListDropped)="onActiveDrop($event)"
              class="flex-1 flex flex-col"
            >
              <app-pokemon-slot
                [pokemon]="active"
                [cardDef]="cardDefs().get(active.instanceId) ?? null"
                [isActive]="true"
                [isOwn]="true"
                [isHighlighted]="validTargets().includes(active.instanceId)"
                (slotClicked)="onPokemonClicked($event)"
                (energyClicked)="energyClicked.emit($event)"
                (abilityClicked)="abilityClicked.emit($event)"
              />
            </div>
          } @else {
            <span class="block p-4 text-center text-[var(--pk-text-dim)]">Sin Pokémon activo</span>
          }
        </div>
      </div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlayerAreaComponent {
  private readonly cardRepo = inject(CardRepositoryService);

  readonly playerState = input.required<PublicPlayerStateModel>();
  readonly validTargets = input<string[]>([]);
  readonly selectionMode = input<SelectionMode>('NONE');
  readonly dragEnabled = input(false);
  readonly connectedDropListIds = input<string[]>([]);

  readonly pokemonClicked = output<PokemonClickedEvent>();
  readonly abilityClicked = output<{ instanceId: string }>();
  readonly energyClicked = output<{ instanceId: string; index: number }>();
  readonly benchDropped = output<{ handIndex: number; benchIndex: number }>();
  readonly energyDropped = output<{ handIndex: number; targetInstanceId: string }>();
  readonly evolutionDropped = output<{ handIndex: number; targetInstanceId: string }>();
  readonly trainerDropped = output<{ handIndex: number; targetInstanceId: string }>();
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

  protected onEnergyDropped(event: { handIndex: number; targetInstanceId: string }): void {
    this.energyDropped.emit(event);
  }

  protected onEvolutionDropped(event: { handIndex: number; targetInstanceId: string }): void {
    this.evolutionDropped.emit(event);
  }

  protected onActiveDrop(event: CdkDragDrop<unknown>): void {
    const data = event.item.data as { handIndex: number; supertype: string } | undefined;
    if (!data) return;
    const active = this.playerState()?.activePokemon;
    if (!active) return;
    if (data.supertype === 'ENERGY') {
      this.energyDropped.emit({ handIndex: data.handIndex, targetInstanceId: active.instanceId });
    } else if (data.supertype === 'POKEMON') {
      this.evolutionDropped.emit({ handIndex: data.handIndex, targetInstanceId: active.instanceId });
    } else if (data.supertype === 'TRAINER') {
      this.trainerDropped.emit({ handIndex: data.handIndex, targetInstanceId: active.instanceId });
    }
  }

  protected onTrainerDropped(event: { handIndex: number; targetInstanceId: string }): void {
    this.trainerDropped.emit(event);
  }
}
