import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { PublicPokemonSlotModel } from '../../../../shared/models/game-state.models';
import { CardDetailResponse, CardAbilityResponse } from '../../../../shared/models/card.models';
import { CardImagePipe } from '../../../../shared/pipes/card-image.pipe';
import { ConditionIconPipe } from '../../../../shared/pipes/condition-icon.pipe';
import { EnergyIconPipe } from '../../../../shared/pipes/energy-icon.pipe';
import { MatchStateService } from '../../services/match-state.service';
import { CardPreviewService } from '../../services/card-preview.service';
import { CardRepositoryService } from '../../../../core/services/card-repository.service';

const ENERGY_COLORS: Record<string, string> = {
  FIRE: '#ef4444',
  WATER: '#3b82f6',
  GRASS: '#4ade80',
  LIGHTNING: '#facc15',
  PSYCHIC: '#a855f7',
  FIGHTING: '#f97316',
  DARKNESS: '#6b21a8',
  METAL: '#9ca3af',
  FAIRY: '#f472b6',
  DRAGON: '#f59e0b',
  COLORLESS: '#e5e7eb',
};

@Component({
  selector: 'app-pokemon-slot',
  standalone: true,
  imports: [CardImagePipe, ConditionIconPipe, EnergyIconPipe],
  host: { '(click)': 'onClick()' },
  template: `
    @let card = cardDef();
    @let currentHp = card?.hp != null ? (card!.hp! - pokemon().damageCounters * 10) : 0;
    @let maxHp = card?.hp ?? 0;
    <div
      class="border-2 border-[var(--pk-btn-border)] rounded-lg transition-[border-color] duration-150 flex flex-col h-full relative"
      [class.p-2]="!isBenchSlot()"
      [class.p-1]="isBenchSlot()"
      [style.background]="isOwn() ? 'color-mix(in oklch, var(--pk-surface), #3b82f6 25%)' : 'color-mix(in oklch, var(--pk-surface), #ef4444 25%)'"
      [class.!border-amber-500]="isActive() && !isHighlighted()"
      [class.!border-3]="isActive()"
      [class.!border-cyan-500]="isHighlighted() && !isActive()"
      [class.!border-teal-400]="isHighlighted() && isActive()"
      [class.!border-4]="isHighlighted() && isActive()"
      [class.!border-dashed]="isHighlighted() && !isActive()"
      [class.!border-2]="isHighlighted() && !isActive()"
      [class.opacity-85]="!isOwn()"
      [class.opacity-60]="!card"
      [class.flex]="!card"
      [class.items-center]="!card"
      [class.justify-center]="!card"
      [class.!border-violet-700]="pokemon().evolvedThisTurn === true"
      [class.!border-3]="pokemon().evolvedThisTurn === true"

    >
      @if (conditionTint(); as tint) {
        <div class="absolute inset-0 rounded-lg pointer-events-none z-5"
             [class.bg-sky-400/25]="tint === 'bg-sky-400/25'"
             [class.bg-orange-500/30]="tint === 'bg-orange-500/30'"
             [class.bg-purple-500/25]="tint === 'bg-purple-500/25'"
             [class.bg-yellow-400/25]="tint === 'bg-yellow-400/25'"
             [class.bg-green-600/30]="tint === 'bg-green-600/30'">
        </div>
      }
      @if (energyFlash(); as type) {
        <div class="absolute inset-0 rounded-lg pointer-events-none z-10 animate-energy-flash"
             [style.background-color]="ENERGY_COLORS[type] ?? '#d1d5db'"></div>
      }
      @if (slotFlash()) {
        <div class="absolute inset-0 pointer-events-none z-10 flex items-center justify-center animate-active-flash">
          <div class="w-3/4 h-3/4 bg-red-600/60 rounded-full"></div>
        </div>
      }
      @if (pokemon().evolvedThisTurn === true) {
        <div class="absolute inset-0 rounded-lg pointer-events-none z-[6] bg-violet-600 animate-evo-flash"></div>
        @for (pos of EVO_PARTICLE_POSITIONS; track $index) {
          <div
            class="absolute z-[5] pointer-events-none w-2 h-2 rounded-full animate-evo-particle"
            [class.bg-violet-400]="isOwn()"
            [class.bg-fuchsia-400]="!isOwn()"
            [style.left.%]="pos.x"
            [style.top.%]="pos.y"
            [style.animation-delay.s]="$index * 0.03"
          ></div>
        }
      }
      @if (canUseAbility()) {
        <div class="relative group z-20">
          <button
            class="absolute top-0.5 right-7 w-5 h-5 flex items-center justify-center text-xs bg-purple-600/80 rounded-full text-white cursor-pointer hover:bg-purple-500 z-10 border-none leading-none shadow"
            (mousedown)="$event.stopPropagation()"
            (click)="$event.stopPropagation(); onAbilityClick()"
            title="Usar habilidad"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="currentColor"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
          </button>
          <div class="absolute top-full left-1/2 -translate-x-1/2 mt-1 hidden group-hover:block bg-black/90 text-white text-[0.625rem] p-2 rounded w-48 z-50 whitespace-normal leading-tight pointer-events-none">
            @for (ability of usableAbilities(); track ability.name) {
              <div class="mb-1 last:mb-0">
                <strong>{{ ability.name }}</strong><br>
                {{ ability.text }}
              </div>
            }
          </div>
        </div>
      }
      @if (hasPassiveAbility()) {
        <div class="relative group z-20">
          <span
            class="absolute top-0.5 right-0.5 w-4 h-4 flex items-center justify-center text-[0.5rem] bg-slate-600/80 rounded-full text-white cursor-default z-10 border-none leading-none shadow"
          >✦</span>
          <div class="absolute top-full left-1/2 -translate-x-1/2 mt-1 hidden group-hover:block bg-black/90 text-white text-[0.625rem] p-2 rounded w-48 z-50 whitespace-normal leading-tight pointer-events-none">
            @for (ability of passiveAbilities(); track ability.name) {
              <div class="mb-1 last:mb-0">
                <strong>{{ ability.name }}</strong><br>
                {{ ability.text }}
              </div>
            }
          </div>
        </div>
      }
      @if (isBenchSlot()) {
        <div class="flex flex-col items-center h-full">
          <div class="relative w-full mx-auto group transition-transform duration-200 flex-1 min-h-0 flex items-center justify-center rounded-[2px] hover:scale-[1.3] hover:z-30">
            <img [src]="pokemon().cardId | cardImage" alt="{{ card?.name ?? pokemon().cardId }}"
                 class="w-full h-full object-cover rounded bg-[var(--pk-panel)]" />
            <button
              class="absolute top-0.5 right-0.5 w-4 h-4 flex items-center justify-center rounded-full bg-black/50 text-white text-[0.625rem] opacity-0 group-hover:opacity-100 transition-opacity duration-150 cursor-pointer hover:bg-black/70 z-10 border-none leading-none shadow"
              (mousedown)="$event.stopPropagation()"
              (click)="$event.stopPropagation(); previewSvc.open({ cardId: pokemon().cardId, name: card?.name ?? pokemon().cardId })"
              title="Ver detalle"
            >+</button>
            @if (pokemon().evolvedThisTurn === true) {
              <div class="absolute -top-0.5 -right-0.5 bg-violet-700 text-white text-[0.4375rem] font-extrabold px-1 py-[1px] rounded border border-violet-400 leading-none">EVO</div>
            }
              @if (weaknessBadge() || resistanceBadge()) {
                <div
                  class="absolute top-0.5 left-0.5 text-[0.4375rem] font-extrabold px-1 py-px rounded-[0.1875rem] leading-tight pointer-events-none animate-[badge-pop_1.5s_ease-out_forwards]"
                  [class.bg-red-500/85]="!!weaknessBadge()"
                  [class.text-white]="!!weaknessBadge()"
                  [class.border]="!!weaknessBadge()"
                  [class.border-white/30]="!!weaknessBadge()"
                  [class.bg-blue-500/85]="!!resistanceBadge()"
                  [class.text-white]="!!resistanceBadge()"
                  [class.border]="!!resistanceBadge()"
                  [class.border-white/30]="!!resistanceBadge()"
                >
                  {{ weaknessBadge() !== false ? '\u00d7' + weaknessBadge() : resistanceBadge() }}
                </div>
              }
              @if (damagePopup(); as dmg) {
                <div class="absolute -top-2 -right-2 bg-red-500/90 text-white text-[0.875rem] font-black px-[5px] py-px rounded-lg leading-tight pointer-events-none shadow-lg animate-[damage-float-up_1.5s_ease-out_forwards]">+{{ dmg }}</div>
              }
          </div>
          <div class="flex gap-1 flex-wrap justify-center shrink-0 py-[1px] min-h-[18px]">
            @if (pokemon().attachedCards.length > 0) {
              @for (group of groupedEnergies(); track group.type) {
                <div class="group/energy relative">
                  <div class="w-3.5 h-3.5 rounded-full" [style.background-color]="group.color">
                    @if (group.count > 1) {
                      <span class="absolute inset-0 flex items-center justify-center text-[0.375rem] font-bold text-white leading-none drop-shadow-[0_0_1px_black]">{{ group.count }}</span>
                    }
                  </div>
                  <div class="absolute bottom-full left-1/2 -translate-x-1/2 mb-1 hidden group-hover/energy:block bg-gray-900 border border-gray-700 rounded px-3 py-2 z-50 shadow-xl pointer-events-none text-[0.65rem] text-gray-300 leading-tight whitespace-nowrap">
                    {{ group.type }} x{{ group.count }}
                  </div>
                </div>
              }
            }
            @if (pokemon().attachedToolCardInstanceId) {
              <img src="assets/icons/tool.svg" alt="Tool" class="w-3.5 h-3.5" [title]="toolCardDef()?.name ?? 'Tool'" />
            }
          </div>
          <div class="w-full min-w-0 shrink-0 leading-none">
            <p class="m-0 text-[var(--pk-text-bright)] text-[0.5rem] font-bold whitespace-nowrap overflow-hidden text-ellipsis">{{ card?.name ?? pokemon().cardId }}</p>
            @if (card?.hp != null) {
              <span class="text-[var(--pk-text-dim)] text-[0.4375rem] whitespace-nowrap leading-none">{{ currentHp }}/{{ maxHp }}</span>
            }
          </div>
        </div>
      } @else {
        <div class="flex flex-col items-center h-full">
          <div class="flex-1 min-h-0 flex items-center justify-center">
             <div class="relative group transition-transform duration-200 h-full flex items-center justify-center hover:scale-[1.5] hover:z-20">
              <img [src]="pokemon().cardId | cardImage" alt="{{ card?.name ?? pokemon().cardId }}"
                   class="h-full w-auto max-w-full object-contain rounded bg-[var(--pk-panel)] block" />
              <button
                class="absolute top-1 right-1 w-6 h-6 flex items-center justify-center rounded-full bg-black/60 text-white text-sm opacity-0 group-hover:opacity-100 transition-opacity duration-150 cursor-pointer hover:bg-black/80 z-10 border-none leading-none shadow-lg"
                (mousedown)="$event.stopPropagation()"
                (click)="$event.stopPropagation(); previewSvc.open({ cardId: pokemon().cardId, name: card?.name ?? pokemon().cardId })"
                title="Ver detalle"
              >+</button>
              @if (pokemon().evolvedThisTurn === true) {
                <div class="absolute -top-1 -right-1 bg-violet-700 text-white text-[0.5625rem] font-extrabold px-1.5 py-0.5 rounded border border-violet-400 leading-none">EVO</div>
              }
              @if (damagePopup(); as dmg) {
                <div class="absolute -top-2 -right-2 bg-red-500/90 text-white text-[1.375rem] font-black px-[7px] py-px rounded-lg leading-tight pointer-events-none shadow-lg animate-[damage-float-up_1.5s_ease-out_forwards]">+{{ dmg }}</div>
              }
              @if (weaknessBadge() || resistanceBadge()) {
                <div
                  class="absolute top-0.5 left-0.5 text-[0.5625rem] font-extrabold px-1 py-px rounded-[0.25rem] leading-tight pointer-events-none animate-[badge-pop_1.5s_ease-out_forwards]"
                  [class.bg-red-500/85]="!!weaknessBadge()"
                  [class.text-white]="!!weaknessBadge()"
                  [class.border]="!!weaknessBadge()"
                  [class.border-white/30]="!!weaknessBadge()"
                  [class.bg-blue-500/85]="!!resistanceBadge()"
                  [class.text-white]="!!resistanceBadge()"
                  [class.border]="!!resistanceBadge()"
                  [class.border-white/30]="!!resistanceBadge()"
                >
                  {{ weaknessBadge() !== false ? '\u00d7' + weaknessBadge() : resistanceBadge() }}
                </div>
              }
            </div>
          </div>
          <div class="w-full shrink-0 min-h-[24px]">
            @if (pokemon().attachedCards.length > 0) {
              <span class="block text-[0.625rem] font-bold text-[var(--pk-text-dim)] uppercase tracking-wider mb-1">Energ\u00edas</span>
              <div class="flex gap-1.5 flex-wrap">
                @for (energy of pokemon().attachedCards; track $index) {
                  <div class="relative group/energy">
                    <div class="w-12 h-12 rounded-md border border-[var(--pk-panel)] flex items-center justify-center cursor-pointer hover:border-amber-400 transition-colors" [style.background]="isOwn() ? 'color-mix(in oklch, var(--pk-surface), #3b82f6 25%)' : 'color-mix(in oklch, var(--pk-surface), #ef4444 25%)'" (click)="onEnergyClick($index)">
                      <img [src]="energy | energyIcon" alt="{{ energy }}" class="w-7 h-7" />
                    </div>
                    <div class="absolute bottom-full left-1/2 -translate-x-1/2 mb-1 hidden group-hover/energy:block bg-gray-900 border border-gray-700 rounded px-3 py-2 z-50 shadow-xl pointer-events-none text-[0.65rem] text-gray-300 leading-tight whitespace-nowrap">
                      {{ energy }}
                    </div>
                  </div>
                }
              </div>
            }
            @if (pokemon().attachedToolCardInstanceId) {
              <div class="flex items-center gap-1 mt-1">
                <img src="assets/icons/tool.svg" alt="Tool" class="w-5 h-5" />
                <span class="text-[0.625rem] font-bold text-[var(--pk-text-dim)] uppercase tracking-wider">{{ toolCardDef()?.name ?? pokemon().attachedToolCardDefinitionId }}</span>
              </div>
            }
          </div>
          <div class="w-full min-w-0 shrink-0">
            <p class="m-0 text-[var(--pk-text-bright)] text-xs font-semibold whitespace-nowrap overflow-hidden text-ellipsis">{{ card?.name ?? pokemon().cardId }}</p>
            @if (card?.hp != null) {
              <div class="flex items-center gap-1 mt-0.5">
                <span class="text-[var(--pk-text-dim)] text-xs whitespace-nowrap">HP: {{ currentHp }}/{{ maxHp }}</span>
                <div class="flex-1 h-1.5 bg-[var(--pk-dark)] rounded-[0.1875rem] overflow-hidden">
                  <div
                    class="h-full rounded-[0.1875rem] transition-[width] duration-200"
                    [style.width.%]="hpPercent()"
                    [class.bg-green-500]="hpPercent() > 50"
                    [class.bg-yellow-500]="hpPercent() > 25 && hpPercent() <= 50"
                    [class.bg-red-500]="hpPercent() <= 25"
                  ></div>
                </div>
              </div>
            }
            <div class="flex gap-1 mt-0.5 flex-wrap">
              @for (condition of pokemon().specialConditions; track $index) {
                <img [src]="condition | conditionIcon" alt="{{ condition }}" class="w-4 h-4" />
              }
            </div>
          </div>
        </div>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PokemonSlotComponent {
  private readonly matchState = inject(MatchStateService);
  protected readonly previewSvc = inject(CardPreviewService);
  private readonly cardRepo = inject(CardRepositoryService);
  protected readonly ENERGY_COLORS = ENERGY_COLORS;

  protected readonly EVO_PARTICLE_POSITIONS = [
    { x: 50, y: 5 }, { x: 80, y: 15 }, { x: 20, y: 20 },
    { x: 70, y: 35 }, { x: 30, y: 45 }, { x: 50, y: 55 },
    { x: 85, y: 55 }, { x: 15, y: 60 }, { x: 65, y: 70 },
    { x: 35, y: 75 }, { x: 50, y: 85 }, { x: 90, y: 40 },
    { x: 10, y: 40 }, { x: 75, y: 50 }, { x: 25, y: 35 },
    { x: 50, y: 30 },
    { x: 10, y: 10 }, { x: 90, y: 10 }, { x: 50, y: 15 },
    { x: 40, y: 20 }, { x: 60, y: 25 }, { x: 80, y: 30 },
    { x: 20, y: 35 }, { x: 45, y: 40 }, { x: 55, y: 45 },
    { x: 75, y: 50 }, { x: 25, y: 55 }, { x: 40, y: 60 },
    { x: 60, y: 65 }, { x: 80, y: 70 }, { x: 20, y: 75 },
    { x: 45, y: 80 }, { x: 55, y: 85 }, { x: 70, y: 90 },
    { x: 30, y: 10 }, { x: 70, y: 10 }, { x: 15, y: 25 },
    { x: 85, y: 25 }, { x: 10, y: 50 }, { x: 90, y: 50 },
    { x: 15, y: 75 }, { x: 85, y: 75 }, { x: 40, y: 90 },
    { x: 60, y: 90 }, { x: 30, y: 30 }, { x: 70, y: 30 },
  ];

  readonly pokemon = input.required<PublicPokemonSlotModel>();
  readonly cardDef = input<CardDetailResponse | null>(null);
  readonly isActive = input(false);
  readonly isOwn = input(true);
  readonly isHighlighted = input(false);
  readonly isBenchSlot = input(false);

  readonly slotClicked = output<PublicPokemonSlotModel>();
  readonly energyClicked = output<{ instanceId: string; index: number }>();
  readonly abilityClicked = output<{ instanceId: string }>();

  readonly usableAbilities = computed<CardAbilityResponse[]>(() => {
    return this.cardDef()?.abilities?.filter(a => a.isActivable) ?? [];
  });

  readonly passiveAbilities = computed<CardAbilityResponse[]>(() => {
    return this.cardDef()?.abilities?.filter(a => !a.isActivable) ?? [];
  });

  readonly hasUsableAbility = computed(() => this.usableAbilities().length > 0);
  readonly hasPassiveAbility = computed(() => this.passiveAbilities().length > 0);

  readonly canUseAbility = computed(() => {
    if (!this.isOwn()) return false;
    if (!this.matchState.isMyTurn()) return false;
    if (this.matchState.currentPhase() !== 'MAIN') return false;
    return this.usableAbilities().length > 0;
  });

  readonly groupedEnergies = computed<{ type: string; color: string; count: number }[]>(() => {
    const energies = this.pokemon().attachedCards;
    const groups = new Map<string, number>();
    for (const energy of energies) {
      const key = energy.toUpperCase();
      groups.set(key, (groups.get(key) ?? 0) + 1);
    }
    return Array.from(groups.entries()).map(([type, count]) => ({
      type,
      color: ENERGY_COLORS[type] ?? '#d1d5db',
      count,
    }));
  });

  readonly hpPercent = computed(() => {
    const card = this.cardDef();
    const poke = this.pokemon();
    if (!card?.hp) return 100;
    const maxHp = card.hp;
    const currentHp = maxHp - poke.damageCounters * 10;
    return Math.max(0, (currentHp / maxHp) * 100);
  });

  readonly damagePopup = computed(() => {
    return this.matchState.damagePopups().get(this.pokemon().instanceId) ?? null;
  });

  readonly weaknessBadge = computed((): number | false => {
    const val = this.matchState.weaknessPopups().get(this.pokemon().instanceId);
    return val != null ? val : false;
  });

  readonly resistanceBadge = computed((): number | false => {
    const val = this.matchState.resistancePopups().get(this.pokemon().instanceId);
    return val != null ? val : false;
  });

  readonly energyFlash = computed((): string | null => {
    return this.matchState.energyAttachFlashes().get(this.pokemon().instanceId) ?? null;
  });

  readonly slotFlash = computed(() => {
    const ownerId = this.matchState.activeSlotFlash();
    if (!ownerId || !this.isActive()) return false;
    return this.isOwn() === (ownerId === this.matchState.myPlayerId());
  });

  readonly toolCardDef = computed(() => {
    const defId = this.pokemon().attachedToolCardDefinitionId;
    return defId ? this.cardRepo.getFromCache(defId) : null;
  });

  readonly conditionTint = computed(() => {
    const conditions = this.pokemon().specialConditions;
    if (!conditions || conditions.length === 0) return null;
    switch (conditions[0]) {
      case 'ASLEEP': return 'bg-sky-400/25';
      case 'BURNED': return 'bg-orange-500/30';
      case 'CONFUSED': return 'bg-purple-500/25';
      case 'PARALYZED': return 'bg-yellow-400/25';
      case 'POISONED': return 'bg-green-600/30';
      default: return null;
    }
  });

  onClick(): void {
    if (!this.cardDef()) {
      console.warn(`[DEBUG] pokemon-slot onClick: cardDef null for ${this.pokemon().cardId}, emitting anyway`);
    }
    this.slotClicked.emit(this.pokemon());
  }

  onAbilityClick(): void {
    this.abilityClicked.emit({ instanceId: this.pokemon().instanceId });
  }

  onEnergyClick(index: number): void {
    const energyIds = this.pokemon().attachedEnergyInstanceIds;
    if (energyIds && energyIds[index]) {
      this.energyClicked.emit({ instanceId: energyIds[index], index });
    }
  }
}
