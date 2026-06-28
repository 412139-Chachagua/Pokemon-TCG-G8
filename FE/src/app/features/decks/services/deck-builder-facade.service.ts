import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { DeckApiService } from '../../../core/api/deck-api.service';
import { DeckValidationModel } from '../../../shared/models/deck.models';
import { AuthService } from '../../../core/services/auth.service';

export interface DeckCardEntry {
  cardId: string;
  name: string;
  supertype: string;
  subtypes: string[];
  stage?: string;
  isBasicEnergy: boolean;
  quantity: number;
}

@Injectable({ providedIn: 'root' })
export class DeckBuilderFacadeService {
  private readonly deckApi = inject(DeckApiService);
  private readonly authService = inject(AuthService);

  private readonly _cards = signal<DeckCardEntry[]>([]);
  readonly cards = this._cards.asReadonly();
  readonly totalCards = computed(() =>
    this.cards().reduce((sum, c) => sum + c.quantity, 0),
  );
  readonly isEmpty = computed(() => this.totalCards() === 0);
  readonly aceSpecCount = computed(() =>
    this.cards()
      .filter((c) => c.subtypes.includes('ACE_SPEC'))
      .reduce((sum, c) => sum + c.quantity, 0),
  );
  readonly basicPokemonCount = computed(() =>
    this.cards()
      .filter((c) => c.stage === 'BASIC')
      .reduce((sum, c) => sum + c.quantity, 0),
  );
  readonly hasBasicPokemon = computed(() => this.basicPokemonCount() > 0);
  readonly pokemonCount = computed(() =>
    this.cards()
      .filter((c) => c.supertype === 'POKEMON')
      .reduce((sum, c) => sum + c.quantity, 0),
  );
  readonly trainerCount = computed(() =>
    this.cards()
      .filter((c) => c.supertype === 'TRAINER')
      .reduce((sum, c) => sum + c.quantity, 0),
  );
  readonly energyCount = computed(() =>
    this.cards()
      .filter((c) => c.supertype === 'ENERGY')
      .reduce((sum, c) => sum + c.quantity, 0),
  );

  addCard(cardId: string, name: string, supertype: string, subtypes: string[] = [], stage?: string, isBasicEnergy = false): void {
    this._cards.update((prev) => {
      const existing = prev.find((c) => c.cardId === cardId);
      if (existing) {
        return prev.map((c) =>
          c.cardId === cardId ? { ...c, quantity: c.quantity + 1 } : c,
        );
      }
      return [...prev, { cardId, name, supertype, subtypes, stage, isBasicEnergy, quantity: 1 }];
    });
  }

  removeCard(cardId: string): void {
    this._cards.update((prev) => {
      const existing = prev.find((c) => c.cardId === cardId);
      if (!existing) return prev;
      if (existing.quantity <= 1) return prev.filter((c) => c.cardId !== cardId);
      return prev.map((c) =>
        c.cardId === cardId ? { ...c, quantity: c.quantity - 1 } : c,
      );
    });
  }

  setCards(cards: DeckCardEntry[]): void {
    this._cards.set(cards);
  }

  reset(): void {
    this._cards.set([]);
  }

  validate(): Observable<DeckValidationModel> {
    return this.deckApi.validateCards(this._cards());
  }

  createDeck(name: string): Observable<unknown> {
    const playerId = this.authService.playerId();
    return this.deckApi
      .create({ name, playerId: playerId ?? '', cards: this._cards() })
      .pipe(tap(() => this.reset()));
  }
}
