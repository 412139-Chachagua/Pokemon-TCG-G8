import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { DragDropModule, CdkDragDrop } from '@angular/cdk/drag-drop';
import { PublicDiscardCardModel, PublicPokemonSlotModel } from '../../../../shared/models/game-state.models';
import { CardDetailResponse } from '../../../../shared/models/card.models';
import { PokemonSlotComponent } from '../pokemon-slot/pokemon-slot.component';
import { DiscardPileComponent } from '../discard-pile/discard-pile.component';
import { PokemonClickedEvent, SelectionMode } from '../../../../shared/models/ui-state.models';

@Component({
  selector: 'app-bench-zone',
  standalone: true,
  imports: [DragDropModule, PokemonSlotComponent, DiscardPileComponent],
  template: `
    <div class="grid grid-cols-3 gap-2 flex-1 h-full grid-rows-[1fr_1fr]">
      @for (slot of bench(); track slot?.instanceId ?? $index; let idx = $index) {
        <div
          class="flex flex-col"
          [style.grid-column]="isOwn() && idx >= 3 ? idx - 1 : null"
          cdkDropList
          [id]="'bench-slot-' + idx"
          [cdkDropListConnectedTo]="connectedDropListIds()"
          [cdkDropListDisabled]="!dragEnabled()"
          (cdkDropListDropped)="onDrop($event, idx, slot)"
        >
          @if (slot; as poke) {
            <app-pokemon-slot
              [pokemon]="poke"
              [cardDef]="cardDefs().get(poke.instanceId) ?? null"
              [isOwn]="isOwn()"
              [isHighlighted]="validTargets().includes(poke.instanceId)"
              [class.cursor-pointer]="selectionMode() === 'SELECT_BENCH_SLOT' || selectionMode() === 'SELECT_NEW_ACTIVE'"
              [isBenchSlot]="true"
              (slotClicked)="onOccupiedClick(idx, $event)"
              (abilityClicked)="abilityClicked.emit($event)"
            />
          } @else {
            <div
              class="border-2 border-dashed border-[var(--pk-panel)] rounded-lg flex items-center justify-center h-full text-[var(--pk-text-dim)] text-lg"
              [class.!border-cyan-500]="selectionMode() === 'SELECT_BENCH_SLOT'"
              [class.cursor-pointer]="selectionMode() === 'SELECT_BENCH_SLOT'"
              [class.hover:border-cyan-300]="selectionMode() === 'SELECT_BENCH_SLOT'"
              [class.hover:bg-[var(--pk-surface)]]="selectionMode() === 'SELECT_BENCH_SLOT'"
              (click)="onEmptyClick(idx)"
            >
              —
            </div>
          }
        </div>
      }
      <div class="flex flex-col h-full" [style.grid-column]="isOwn() ? 1 : 3" [style.grid-row]="2">
        <app-discard-pile
          [discardCount]="discardCount()"
          [discard]="discard()"
          (openViewer)="viewDiscard.emit()" />
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BenchZoneComponent {
  readonly bench = input.required<(PublicPokemonSlotModel | null)[]>();
  readonly cardDefs = input<Map<string, CardDetailResponse | null>>(new Map());
  readonly isOwn = input(false);
  readonly discardCount = input(0);
  readonly discard = input<PublicDiscardCardModel[]>([]);
  readonly validTargets = input<string[]>([]);
  readonly selectionMode = input<SelectionMode>('NONE');
  readonly dragEnabled = input(false);
  readonly connectedDropListIds = input<string[]>([]);

  readonly slotClicked = output<PokemonClickedEvent>();
  readonly abilityClicked = output<{ instanceId: string }>();
  readonly benchDropped = output<{ handIndex: number; benchIndex: number }>();
  readonly energyDropped = output<{ handIndex: number; targetInstanceId: string }>();
  readonly evolutionDropped = output<{ handIndex: number; targetInstanceId: string }>();
  readonly trainerDropped = output<{ handIndex: number; targetInstanceId: string }>();
  readonly viewDiscard = output<void>();

  protected onDrop(event: CdkDragDrop<unknown>, benchIndex: number, slot: PublicPokemonSlotModel | null): void {
    const data = event.item.data as { handIndex: number; supertype: string } | undefined;
    if (!data || typeof data.handIndex !== 'number') return;
    if (data.supertype === 'ENERGY') {
      if (!slot) return;
      this.energyDropped.emit({ handIndex: data.handIndex, targetInstanceId: slot.instanceId });
    } else if (data.supertype === 'POKEMON') {
      if (slot) {
        this.evolutionDropped.emit({ handIndex: data.handIndex, targetInstanceId: slot.instanceId });
      } else {
        this.benchDropped.emit({ handIndex: data.handIndex, benchIndex });
      }
    } else if (data.supertype === 'TRAINER') {
      if (!slot) return;
      this.trainerDropped.emit({ handIndex: data.handIndex, targetInstanceId: slot.instanceId });
    }
  }

  protected onOccupiedClick(_index: number, pokemon: PublicPokemonSlotModel): void {
    console.warn(`[DEBUG] bench-zone onOccupiedClick: instanceId=${pokemon.instanceId}, cardId=${pokemon.cardId}`);
    this.slotClicked.emit({ instanceId: pokemon.instanceId });
  }

  protected onEmptyClick(index: number): void {
    if (this.selectionMode() === 'SELECT_BENCH_SLOT') {
      this.slotClicked.emit({ benchIndex: index });
    }
  }
}
