import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { CardRepositoryService } from '../../../../core/services/card-repository.service';
import { PrivateHandCardModel } from '../../../../shared/models/game-state.models';
import { normalizeCardSubtypes } from '../../../../shared/models/card.models';
import { SelectionMode } from '../../../../shared/models/ui-state.models';
import { CardImagePipe } from '../../../../shared/pipes/card-image.pipe';
import { CardPreviewService } from '../../services/card-preview.service';

const ENERGY_COLORS: Record<string, string> = {
  FIRE: '#ef4444',
  WATER: '#3b82f6',
  GRASS: '#22c55e',
  LIGHTNING: '#eab308',
  PSYCHIC: '#a855f7',
  FIGHTING: '#d97706',
  DARKNESS: '#4a044e',
  METAL: '#9ca3af',
  FAIRY: '#f472b6',
  DRAGON: '#f59e0b',
  COLORLESS: '#d1d5db',
};

@Component({
  selector: 'app-hand-zone',
  imports: [DragDropModule, CardImagePipe],
  template: `
    <div class="flex flex-col gap-1">
      <div class="flex justify-start">
        <button
          class="pk-btn pk-btn--ghost h-6 px-2"
          [title]="collapsed() ? 'Expandir mano' : 'Contraer mano'"
          (click)="collapsed.set(!collapsed())"
        >
          @if (collapsed()) {
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 9 6 6 6-6"/></svg>
          } @else {
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m18 15-6-6-6 6"/></svg>
          }
        </button>
      </div>

      <div class="flex items-center gap-2 px-1">
        <span class="text-[0.625rem] font-bold text-[var(--pk-text-dim)] uppercase tracking-wider">Mano ({{ hand().length }})</span>
        <div class="flex gap-1 ml-auto">
          <button class="text-[0.5rem] px-2 py-1 rounded font-bold uppercase tracking-wider border transition-colors"
            [class.bg-blue-700]="activeFilter() === 'ALL'"
            [class.bg-slate-700]="activeFilter() !== 'ALL'"
            [class.text-white]="activeFilter() === 'ALL'"
            [class.text-slate-400]="activeFilter() !== 'ALL'"
            [class.border-blue-500]="activeFilter() === 'ALL'"
            [class.border-slate-600]="activeFilter() !== 'ALL'"
            (click)="activeFilter.set('ALL')">Todas</button>
          <button class="text-[0.5rem] px-2 py-1 rounded font-bold uppercase tracking-wider border transition-colors"
            [class.bg-blue-700]="activeFilter() === 'POKEMON'"
            [class.bg-slate-700]="activeFilter() !== 'POKEMON'"
            [class.text-white]="activeFilter() === 'POKEMON'"
            [class.text-slate-400]="activeFilter() !== 'POKEMON'"
            [class.border-blue-500]="activeFilter() === 'POKEMON'"
            [class.border-slate-600]="activeFilter() !== 'POKEMON'"
            (click)="activeFilter.set('POKEMON')">Pokémon</button>
          <button class="text-[0.5rem] px-2 py-1 rounded font-bold uppercase tracking-wider border transition-colors"
            [class.bg-blue-700]="activeFilter() === 'TRAINER'"
            [class.bg-slate-700]="activeFilter() !== 'TRAINER'"
            [class.text-white]="activeFilter() === 'TRAINER'"
            [class.text-slate-400]="activeFilter() !== 'TRAINER'"
            [class.border-blue-500]="activeFilter() === 'TRAINER'"
            [class.border-slate-600]="activeFilter() !== 'TRAINER'"
            (click)="activeFilter.set('TRAINER')">Entrenadores</button>
          <button class="text-[0.5rem] px-2 py-1 rounded font-bold uppercase tracking-wider border transition-colors"
            [class.bg-blue-700]="activeFilter() === 'ENERGY'"
            [class.bg-slate-700]="activeFilter() !== 'ENERGY'"
            [class.text-white]="activeFilter() === 'ENERGY'"
            [class.text-slate-400]="activeFilter() !== 'ENERGY'"
            [class.border-blue-500]="activeFilter() === 'ENERGY'"
            [class.border-slate-600]="activeFilter() !== 'ENERGY'"
            (click)="activeFilter.set('ENERGY')">Energías</button>
        </div>
      </div>

      <div
        class="flex flex-row-reverse gap-2 p-3 rounded-lg border border-[var(--pk-btn-border)] min-h-[120px]" [style.background]="'color-mix(in oklch, var(--pk-surface) 75%, transparent)'"
        [class.flex-wrap]="!collapsed()"
        [class.max-h-[280px]]="!collapsed()"
        [class.overflow-y-visible]="!collapsed()"
        [class.flex-nowrap]="collapsed()"
        [class.overflow-x-auto]="collapsed()"
        [class.h-[160px]]="collapsed()"
        cdkDropList id="hand-zone-droplist"
        [cdkDropListConnectedTo]="connectedDropListIds()"
      >
        @for (entry of filteredHand(); track entry.card.instanceId; let i = $index) {
        <div
          class="flex flex-col items-center gap-1.5 p-2 border-2 border-[var(--pk-panel)] rounded-md cursor-pointer transition-[opacity,border-color] duration-150 min-w-[85px] w-[85px] text-[var(--pk-text-bright)] relative select-none touch-none group" [style.background]="'color-mix(in oklch, var(--pk-surface) 75%, transparent)'"
          [class.opacity-50]="isDimmed() || (entry.card.supertype === 'ENERGY' && !canAttachEnergy()) || isSupporterDisabled(entry.card) || isStadiumDisabled(entry.card)"
          [class.cursor-default]="isDimmed() || (entry.card.supertype === 'ENERGY' && !canAttachEnergy()) || isSupporterDisabled(entry.card) || isStadiumDisabled(entry.card)"
          [class.pointer-events-none]="isDimmed() || (entry.card.supertype === 'ENERGY' && !canAttachEnergy()) || isSupporterDisabled(entry.card) || isStadiumDisabled(entry.card)"
          [class.border-amber-400]="selectedHandIndex() === entry.originalIndex"
          [style.border-color]="energyBorder(entry.card, entry.originalIndex)"
          [class.shadow-[0_0_0_2px_#fbbf24]]="selectedHandIndex() === entry.originalIndex"
          [class.cursor-grab]="dragEnabled() && isDraggableCard(entry.card)"
          [class.cursor-pointer]="!dragEnabled() || !isDraggableCard(entry.card)"
          cdkDrag
          [cdkDragData]="{ handIndex: entry.originalIndex, supertype: entry.card.supertype }"
          [cdkDragDisabled]="!dragEnabled() || !isDraggableCard(entry.card)"
          (click)="onCardClick(entry.card, entry.originalIndex)"
        >
          <div class="flex flex-col items-center gap-1.5">
            <div class="relative hover:scale-[1.8] hover:z-20 transition-[transform] duration-150">
              <img [src]="entry.card.cardId | cardImage" alt="{{ entry.card.name }}" class="w-[50px] aspect-[3/4] object-cover rounded bg-[var(--pk-panel)]" />
              <button
                class="absolute top-0.5 right-0.5 w-5 h-5 flex items-center justify-center rounded-full bg-black/60 text-white text-xs opacity-0 group-hover:opacity-100 transition-opacity duration-150 cursor-pointer hover:bg-black/80 z-10 border-none leading-none"
                (mousedown)="$event.stopPropagation()"
                (click)="$event.stopPropagation(); previewSvc.open({ cardId: entry.card.cardId, name: entry.card.name })"
                title="Ver detalle"
              >+</button>
            </div>
              <span class="flex flex-col items-center gap-0.5 w-full">
              <span
                class="text-[0.5625rem] font-bold uppercase px-1 rounded text-white"
                [style.background]="badgeBg(entry.card)"
              >{{ badgeLabel(entry.card) }}</span>
              <span class="text-[0.6rem] font-semibold text-center leading-tight max-w-full overflow-hidden text-ellipsis line-clamp-2" title="{{ entry.card.name }}">
                @if (entry.card.supertype === 'ENERGY') {
                  {{ entry.card.name.replace(' Energy', '') }}
                } @else {
                  {{ entry.card.name }}
                }
              </span>
            </span>
          </div>
        </div>
      }
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HandZoneComponent {
  private readonly cardRepo = inject(CardRepositoryService);
  protected readonly previewSvc = inject(CardPreviewService);

  readonly hand = input<PrivateHandCardModel[]>([]);
  readonly selectionMode = input<SelectionMode>('NONE');
  readonly selectedHandIndex = input<number | null>(null);
  readonly dragEnabled = input(false);
  readonly connectedDropListIds = input<string[]>([]);
  readonly hasPlayedSupporter = input(false);
  readonly hasPlayedStadium = input(false);
  readonly canAttachEnergy = input(true);

  readonly cardClicked = output<{ card: PrivateHandCardModel; handIndex: number }>();

  protected readonly collapsed = signal(false);

  protected readonly activeFilter = signal<'ALL' | 'POKEMON' | 'TRAINER' | 'ENERGY'>('ALL');

  protected readonly filteredHand = computed(() => {
    const f = this.activeFilter();
    const cards = this.hand();
    if (f === 'ALL') return cards.map((card, i) => ({ card, originalIndex: i }));
    return cards
      .map((card, i) => ({ card, originalIndex: i }))
      .filter(entry => entry.card.supertype === f);
  });

  protected isDimmed(): boolean {
    return this.selectionMode() !== 'NONE';
  }

  protected isSupporterDisabled(card: PrivateHandCardModel): boolean {
    if (!this.hasPlayedSupporter()) return false;
    if (card.supertype !== 'TRAINER') return false;
    const cardDef = this.cardRepo.getFromCache(card.cardId);
    const subtypes = normalizeCardSubtypes(cardDef?.subtypes);
    return subtypes.includes('SUPPORTER');
  }

  protected isStadiumDisabled(card: PrivateHandCardModel): boolean {
    if (!this.hasPlayedStadium()) return false;
    if (card.supertype !== 'TRAINER') return false;
    const cardDef = this.cardRepo.getFromCache(card.cardId);
    const subtypes = normalizeCardSubtypes(cardDef?.subtypes);
    return subtypes.includes('STADIUM');
  }

  protected energyBorder(card: PrivateHandCardModel, i: number): string | null {
    if (card.supertype !== 'ENERGY') return null;
    if (this.selectedHandIndex() === i) return null;
    const cardDef = this.cardRepo.getFromCache(card.cardId);
    let energyType: string | null = null;
    const types = cardDef?.types;
    if (types?.length) {
      energyType = Object.keys(ENERGY_COLORS).find(
        k => k === types[0].toUpperCase()
      ) ?? null;
    }
    const cardName = cardDef?.name;
    if (!energyType && cardName) {
      const fromName = cardName.replace(/ Energy$/i, '').toUpperCase();
      energyType = Object.keys(ENERGY_COLORS).includes(fromName) ? fromName : null;
    }
    return energyType ? ENERGY_COLORS[energyType] : null;
  }

  protected badgeLabel(card: PrivateHandCardModel): string {
    if (card.supertype !== 'TRAINER') return card.supertype;
    const cardDef = this.cardRepo.getFromCache(card.cardId);
    const subtypes = normalizeCardSubtypes(cardDef?.subtypes);
    if (!subtypes.length) return 'TRAINER';
    const subtype = subtypes[0];
    if (subtype === 'ITEM' || subtype === 'SUPPORTER' || subtype === 'STADIUM' || subtype === 'ACE_SPEC') {
      return subtype === 'ACE_SPEC' ? 'ACE SPEC' : subtype;
    }
    return 'TRAINER';
  }

  protected badgeBg(card: PrivateHandCardModel): string {
    if (card.supertype === 'POKEMON') return '#3b82f6';
    if (card.supertype === 'ENERGY') return '#a855f7';
    if (card.supertype === 'TRAINER') {
      const cardDef = this.cardRepo.getFromCache(card.cardId);
      const subtypes = normalizeCardSubtypes(cardDef?.subtypes);
      if (subtypes.length) {
        const subtype = subtypes[0];
        if (subtype === 'ITEM') return '#3B82F6';
        if (subtype === 'SUPPORTER') return '#F59E0B';
        if (subtype === 'STADIUM') return '#10B981';
        if (subtype === 'POKEMON_TOOL') return '#8B5CF6';
      }
      return '#22c55e';
    }
    return '#6b7280';
  }

  protected isDraggableCard(card: PrivateHandCardModel): boolean {
    if (card.supertype === 'POKEMON' || card.supertype === 'ENERGY') return true;
    if (card.supertype === 'TRAINER') return this.isTrainerDraggable(card);
    return false;
  }

  private static readonly TRAINERS_NEEDING_TARGET = new Set([
    'Evosoda', 'Cassius', 'Super Potion',
  ]);

  private isTrainerDraggable(card: PrivateHandCardModel): boolean {
    const cardDef = this.cardRepo.getFromCache(card.cardId);
    if (card.effectCode === 'ATTACH_TOOL') return true;
    if (cardDef?.name) {
      const subtypes = normalizeCardSubtypes(cardDef.subtypes);
      if (subtypes.includes('POKEMON_TOOL')) return true;
      return HandZoneComponent.TRAINERS_NEEDING_TARGET.has(cardDef.name);
    }
    return true;
  }

  protected onCardClick(card: PrivateHandCardModel, handIndex: number): void {
    this.cardClicked.emit({ card, handIndex });
  }
}
