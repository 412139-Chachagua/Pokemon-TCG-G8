import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { CardApiService } from '../api/card-api.service';
import { CardDetailResponse } from '../../shared/models/card.models';

@Injectable({ providedIn: 'root' })
export class CardRepositoryService {
  private readonly cardApi = inject(CardApiService);
  private readonly _cache = signal<Map<string, CardDetailResponse>>(new Map());
  private readonly _loading = signal<Set<string>>(new Set());

  readonly cache = this._cache.asReadonly();

  async resolve(cardId: string): Promise<CardDetailResponse> {
    const cached = this._cache().get(cardId);
    if (cached) return cached;

    if (this._loading().has(cardId)) {
      await this.waitForLoad(cardId);
      return this._cache().get(cardId)!;
    }

    this._loading.update(s => new Set(s).add(cardId));
    try {
      const card = await firstValueFrom(this.cardApi.getCardById(cardId));
      this._cache.update(cache => {
        const newCache = new Map(cache);
        newCache.set(cardId, card);
        return newCache;
      });
      return card;
    } finally {
      this._loading.update(s => {
        const newSet = new Set(s);
        newSet.delete(cardId);
        return newSet;
      });
    }
  }

  async preload(cardIds: string[]): Promise<void> {
    const uncached = cardIds.filter(id => !this._cache().has(id));
    if (uncached.length === 0) return;
    await Promise.all(uncached.map(id => this.resolve(id)));
  }

  getFromCache(cardId: string): CardDetailResponse | null {
    return this._cache().get(cardId) ?? null;
  }

  private waitForLoad(cardId: string): Promise<void> {
    return new Promise((resolve) => {
      const interval = setInterval(() => {
        if (!this._loading().has(cardId)) {
          clearInterval(interval);
          resolve();
        }
      }, 50);
    });
  }
}
