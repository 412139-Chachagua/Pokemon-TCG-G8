export type CardSupertype = 'POKEMON' | 'ENERGY' | 'TRAINER';

export type TrainerSubtype = 'ITEM' | 'SUPPORTER' | 'STADIUM' | 'ACE_SPEC' | 'POKEMON_TOOL';

export type PokemonStage = 'BASIC' | 'STAGE_1' | 'STAGE_2' | 'MEGA' | 'RESTORED';

export type EnergyType = 'GRASS' | 'FIRE' | 'WATER' | 'LIGHTNING' | 'PSYCHIC' | 'FIGHTING' | 'DARKNESS' | 'METAL' | 'FAIRY' | 'COLORLESS';

export interface CardAbilityResponse {
  name: string;
  text: string;
  type: string;
  isActivable: boolean;
}

export interface AttackModel {
  index: number;
  name: string;
  cost: EnergyType[];
  convertedEnergyCost: number;
  damage: string;
  text: string;
}

export interface WeaknessModel {
  type: EnergyType;
  value: string;
}

export interface ResistanceModel {
  type: EnergyType;
  value: string;
}

export interface CardModel {
  id: string;
  name: string;
  supertype: CardSupertype;
  subtypes: string[];
  setCode: string;
  number: string;
  imageSmallUrl: string | null;
  imageLargeUrl: string | null;
  rulesText: string[];
  hp?: number;
  stage?: PokemonStage;
  evolvesFrom?: string;
  types?: EnergyType[];
  attacks?: AttackModel[];
  weaknesses?: WeaknessModel[];
  resistances?: ResistanceModel[];
  retreatCost?: EnergyType[];
  isEx?: boolean;
  isMega?: boolean;
}

export interface CardSummaryResponse {
  id: string;
  name: string;
  supertype: string;
  setCode: string;
  number: string;
  imageSmallUrl: string;
  subtypes: string[];
  stage?: string;
}

export function normalizeCardSubtypes(subtypes: string[] | undefined | null): string[] {
  if (!subtypes) return [];
  return subtypes.map(s => {
    const upper = s.toUpperCase().replace(/É/g, 'E').replace(/\s+/g, '_');
    return upper === 'TOOL' ? 'POKEMON_TOOL' : upper;
  });
}

export interface CardDetailResponse {
  id: string;
  name: string;
  supertype: string;
  subtypes: string[];
  setCode: string;
  number: string;
  imageSmallUrl: string | null;
  imageLargeUrl: string | null;
  rulesText: string[];
  hp?: number;
  stage?: string;
  evolvesFrom?: string;
  types?: string[];
  attacks?: AttackModel[];
  weaknesses?: WeaknessModel[];
  resistances?: ResistanceModel[];
  retreatCost?: string[];
  isEx?: boolean;
  isMega?: boolean;
  abilities?: CardAbilityResponse[];
  providesEnergyTypes?: string[];
}

export interface PaginatedCardsResponse {
  items: CardSummaryResponse[];
  page: number;
  size: number;
  totalItems: number;
}
