import { computed, inject, Injectable, signal } from '@angular/core';
import { CardApiService } from '../../../core/api/card-api.service';
import { CardSummaryResponse } from '../../../shared/models/card.models';

@Injectable({ providedIn: 'root' })
export class CardCatalogFacadeService {
  private readonly cardApi = inject(CardApiService);

  private readonly _query = signal('');
  private readonly _supertype = signal('');
  private readonly _stage = signal('');
  private readonly _page = signal(0);
  private readonly _pageSize = signal(24);
  private readonly _cards = signal<CardSummaryResponse[]>([]);
  private readonly _totalItems = signal(0);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly query = this._query.asReadonly();
  readonly supertype = this._supertype.asReadonly();
  readonly stage = this._stage.asReadonly();
  readonly page = this._page.asReadonly();
  readonly pageSize = this._pageSize.asReadonly();
  readonly cards = this._cards.asReadonly();
  readonly totalItems = this._totalItems.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  readonly totalPages = computed(() => {
    const total = this._totalItems();
    const size = this._pageSize();
    return total > 0 ? Math.max(1, Math.ceil(total / size)) : 0;
  });

  setQuery(query: string): void {
    this._query.set(query);
    this._page.set(0);
    this.search();
  }

  setSupertype(supertype: string): void {
    this._supertype.set(supertype);
    if (supertype !== 'POKEMON') {
      this._stage.set('');
    }
    this._page.set(0);
    this.search();
  }

  setStage(stage: string): void {
    this._stage.set(stage);
    this._page.set(0);
    this.search();
  }

  setPage(page: number): void {
    this._page.set(page);
    this.search();
  }

  search(): void {
    this._loading.set(true);
    this._error.set(null);

    this.cardApi.searchCards({
      query: this._query() || undefined,
      supertype: this._supertype() || undefined,
      stage: this._stage() || undefined,
      page: this._page(),
      size: this._pageSize(),
    }).subscribe({
      next: (response) => {
        this._cards.set(response.items);
        this._totalItems.set(response.totalItems);
        this._loading.set(false);
      },
      error: () => {
        this._loading.set(false);
        this._error.set('Error al cargar las cartas');
      },
    });
  }
}
