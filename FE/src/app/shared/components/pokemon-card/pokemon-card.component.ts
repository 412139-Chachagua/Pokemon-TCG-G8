import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CardDetailResponse } from '../../models/card.models';
import { CardImagePipe } from '../../pipes/card-image.pipe';

const TYPE_BG: Record<string, string> = {
  GRASS: '#22c55e',
  FIRE: '#ef4444',
  WATER: '#3b82f6',
  LIGHTNING: '#eab308',
  PSYCHIC: '#a855f7',
  FIGHTING: '#f97316',
  DARKNESS: '#1f2937',
  METAL: '#9ca3af',
  FAIRY: '#f472b6',
  COLORLESS: '#d1d5db',
};

const TYPE_ABBR: Record<string, string> = {
  GRASS: 'G',
  FIRE: 'R',
  WATER: 'W',
  LIGHTNING: 'L',
  PSYCHIC: 'P',
  FIGHTING: 'F',
  DARKNESS: 'D',
  METAL: 'M',
  FAIRY: 'Y',
  COLORLESS: 'C',
};

const STAGE_BG: Record<string, string> = {
  BASIC: '#22c55e',
  STAGE_1: '#3b82f6',
  STAGE_2: '#a855f7',
  MEGA: '#dc2626',
};

const TRAINER_SUBTYPE_BG: Record<string, string> = {
  ITEM: '#3B82F6',
  SUPPORTER: '#F59E0B',
  STADIUM: '#10B981',
  POKEMON_TOOL: '#8B5CF6',
  ACE_SPEC: '#EC4899',
};

@Component({
  selector: 'app-pokemon-card',
  imports: [CardImagePipe],
  template: `
    <div class="mx-auto max-w-2xl">
      <div class="overflow-hidden rounded-xl bg-white shadow-xl">

        <div class="flex flex-col sm:flex-row">
          <div class="flex shrink-0 items-start justify-center bg-gray-50 p-4 sm:w-48">
            <img
              [src]="card().id | cardImage:'large'"
              [alt]="card().name"
              class="h-auto w-full max-w-[200px] rounded-lg object-contain shadow-md"
              loading="lazy"
              (error)="onImageError($event)"
            />
          </div>

          <div class="flex flex-1 flex-col gap-3 p-5 overflow-hidden">
            <div>
              <div class="flex items-start justify-between gap-2">
                <h2 class="text-2xl font-bold text-gray-900">{{ card().name }}</h2>
                @if (card().hp && card().supertype !== 'TRAINER') {
                  <span class="shrink-0 rounded-full bg-red-100 px-2.5 py-0.5 text-[0.75rem] font-bold text-red-600">
                    {{ card().hp }} HP
                  </span>
                }
              </div>

              @if (card().supertype) {
                <p class="mt-1 text-sm text-gray-500">
                  @if (card().supertype === 'TRAINER') {
                    {{ card().supertype }}
                    @if (card().subtypes.length > 0) {
                      <span class="mx-1">·</span>
                      <span
                        class="inline-block rounded px-2 py-0.5 text-xs font-semibold uppercase text-white"
                        [style.background]="trainerSubtypeBg(card().subtypes[0])"
                      >{{ card().subtypes[0] }}</span>
                    }
                  } @else {
                    {{ card().supertype }}
                    @if (card().stage) {
                      <span class="mx-1">·</span>
                      <span
                        class="inline-block rounded px-2 py-0.5 text-xs font-semibold uppercase text-white"
                        [style.background]="stageBg"
                      >{{ card().stage }}</span>
                    }
                  }
                </p>
              }
            </div>

            @if (card().supertype !== 'TRAINER' && (card().types ?? []).length > 0) {
              <div>
                <span class="text-xs font-semibold uppercase tracking-wide text-gray-400">Tipo</span>
                <div class="mt-1 flex flex-wrap gap-1">
                  @for (type of card().types; track $index) {
                    <span
                      class="inline-block rounded px-2 py-0.5 text-xs font-semibold text-white"
                      [style.background]="typeBg(type)"
                    >{{ type }}</span>
                  }
                </div>
              </div>
            }

            @if (card().supertype !== 'TRAINER' && card().evolvesFrom) {
              <div>
                <span class="text-xs font-semibold uppercase tracking-wide text-gray-400">Evoluciona de</span>
                <p class="text-sm text-gray-700">{{ card().evolvesFrom }}</p>
              </div>
            }

            <div class="flex flex-wrap gap-3 text-xs text-gray-500">
              <span>{{ card().setCode }} · #{{ card().number }}</span>
              @if (card().subtypes.length > 0) {
                <span>{{ card().subtypes.join(', ') }}</span>
              }
              @if (card().isEx) {
                <span class="rounded bg-yellow-400 px-2 py-0.5 text-xs font-bold text-black">EX</span>
              }
              @if (card().isMega) {
                <span class="rounded bg-purple-400 px-2 py-0.5 text-xs font-bold text-white">MEGA</span>
              }
            </div>
          </div>
        </div>

        @if (card().rulesText.length > 0) {
          <div class="border-t border-gray-100 px-5 py-4">
            <span class="text-xs font-semibold uppercase tracking-wide text-gray-400">Reglas</span>
            @for (rule of card().rulesText; track $index) {
              <p class="mt-1 text-sm italic text-gray-600">{{ rule }}</p>
            }
          </div>
        }

        @if (card().supertype !== 'TRAINER' && (card().attacks ?? []).length > 0) {
          <div class="border-t border-gray-100 px-5 py-4">
            <span class="text-xs font-semibold uppercase tracking-wide text-gray-400">Ataques</span>
            <div class="mt-2 space-y-2">
              @for (attack of card().attacks; track attack.index) {
                <div class="rounded-lg bg-gray-50 p-3">
                  <div class="flex items-center justify-between gap-2">
                    <div class="flex items-center gap-2">
                      <span class="font-semibold text-gray-900">{{ attack.name }}</span>
                      @if (attack.cost && attack.cost.length > 0) {
                        <span class="flex items-center gap-0.5">
                          @for (c of attack.cost; track $index) {
                            <span
                              class="inline-flex h-5 w-5 items-center justify-center rounded-full text-[10px] font-bold text-white"
                              [style.background]="typeBg(c)"
                              [title]="c"
                            >{{ typeAbbr(c) }}</span>
                          }
                        </span>
                      }
                    </div>
                    @if (attack.damage) {
                      <span class="shrink-0 font-mono text-lg font-bold text-gray-800">{{ attack.damage }}</span>
                    }
                  </div>
                  @if (attack.text) {
                    <p class="mt-1 text-xs text-gray-500">{{ attack.text }}</p>
                  }
                </div>
              }
            </div>
          </div>
        }

        @if (hasStats() && card().supertype !== 'TRAINER') {
          <div class="flex flex-wrap gap-6 border-t border-gray-100 px-5 py-4 text-sm">
            @if ((card().weaknesses ?? []).length > 0) {
              <div>
                <span class="text-xs font-semibold uppercase tracking-wide text-gray-400">Debilidad</span>
                <div class="mt-1 flex items-center gap-1">
                @for (w of card().weaknesses; track $index) {
                  <span
                    class="inline-flex h-5 w-5 items-center justify-center rounded-full text-[10px] font-bold text-white"
                    [style.background]="typeBg(w.type)"
                    [title]="w.type"
                  >{{ typeAbbr(w.type) }}</span>
                  <span class="text-xs text-gray-600">×{{ w.value }}</span>
                }
                </div>
              </div>
            }

            @if ((card().resistances ?? []).length > 0) {
              <div>
                <span class="text-xs font-semibold uppercase tracking-wide text-gray-400">Resistencia</span>
                <div class="mt-1 flex items-center gap-1">
                @for (r of card().resistances; track $index) {
                  <span
                    class="inline-flex h-5 w-5 items-center justify-center rounded-full text-[10px] font-bold text-white"
                    [style.background]="typeBg(r.type)"
                    [title]="r.type"
                  >{{ typeAbbr(r.type) }}</span>
                  <span class="text-xs text-gray-600">-{{ r.value }}</span>
                }
                </div>
              </div>
            }

            @if ((card().retreatCost ?? []).length > 0) {
              <div>
                <span class="text-xs font-semibold uppercase tracking-wide text-gray-400">Retiro</span>
                <div class="mt-1 flex items-center gap-0.5">
                @for (c of card().retreatCost; track $index) {
                  <span
                    class="inline-flex h-5 w-5 items-center justify-center rounded-full text-[10px] font-bold text-white"
                    [style.background]="typeBg(c)"
                    [title]="c"
                  >{{ typeAbbr(c) }}</span>
                }
                </div>
              </div>
            }
          </div>
        }

      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PokemonCardComponent {
  card = input.required<CardDetailResponse>();

  typeBg(type: string): string {
    return TYPE_BG[type.toUpperCase()] ?? '#d1d5db';
  }

  typeAbbr(type: string): string {
    return TYPE_ABBR[type.toUpperCase()] ?? type.charAt(0).toUpperCase();
  }

  get stageBg(): string {
    return STAGE_BG[this.card().stage ?? ''] ?? '#d1d5db';
  }

  trainerSubtypeBg(subtype: string): string {
    const key = subtype.toUpperCase().replace(/\s+/g, '_') === 'TOOL' ? 'POKEMON_TOOL' : subtype.toUpperCase().replace(/\s+/g, '_');
    return TRAINER_SUBTYPE_BG[key] ?? '#6b7280';
  }

  hasStats(): boolean {
    const c = this.card();
    return !!(c.weaknesses?.length || c.resistances?.length || c.retreatCost?.length);
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.style.display = 'none';
  }
}
