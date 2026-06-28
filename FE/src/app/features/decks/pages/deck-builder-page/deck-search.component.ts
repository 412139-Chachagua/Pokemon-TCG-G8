import { ChangeDetectionStrategy, Component, computed, inject, output, signal } from '@angular/core';
import { Subject, switchMap, debounceTime, distinctUntilChanged, catchError, of, merge, map, tap, startWith } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { CardApiService } from '../../../../core/api/card-api.service';
import { CardViewComponent } from '../../../../shared/components/card-view/card-view.component';
import { CardFilterComponent, FilterOption } from '../../../../shared/components/card-filter/card-filter.component';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-deck-search',
  imports: [DragDropModule, CardViewComponent, CardFilterComponent, PaginationComponent],
  template: `
    <div class="flex flex-col gap-3">
      <div class="flex gap-2">
        <input
          #searchInput
          type="text"
          placeholder="Buscar cartas..."
          (input)="onSearchInput(searchInput.value)"
          class="flex-1 rounded-md border border-[var(--pk-btn-border)] bg-[var(--pk-surface)] px-3 py-2 text-[length:var(--pk-fz-base)] text-[var(--pk-text)] placeholder-[var(--pk-text-dim)] focus:border-[var(--pk-accent)] focus:outline-none focus:ring-1 focus:ring-[var(--pk-accent)]"
        />
        <div class="w-40 shrink-0">
          <app-card-filter
            [options]="filterOptions"
            [selected]="supertype()"
            (filterChange)="onFilterChange($event)"
          />
        </div>
      </div>
      @if (searchResult() && totalPages() > 0) {
        <div class="flex items-center justify-between gap-2">
          <app-pagination [currentPage]="page()" [totalPages]="totalPages()" (pageChange)="onPageChange($event)" />
          <div class="w-40 shrink-0">
            <app-card-filter
              [options]="stageFilterOptions"
              [selected]="stage()"
              [disabled]="supertype() !== 'POKEMON'"
              (filterChange)="onStageFilterChange($event)"
            />
          </div>
        </div>
      }
      <div
        cdkDropList
        id="search-grid"
        [cdkDropListConnectedTo]="['deck-list']"
        [cdkDropListSortingDisabled]="true"
        (cdkDropListDropped)="noop()"
        class="grid grid-cols-3 gap-2"
      >
        @for (card of searchResult()?.items ?? []; track card.id) {
          <button
            cdkDrag
            [cdkDragData]="{ cardId: card.id, name: card.name, supertype: card.supertype, subtypes: card.subtypes, stage: card.stage }"
            class="cursor-pointer text-left"
            (click)="selectCard(card.id, card.name, card.supertype, card.subtypes, card.stage)"
          >
            <app-card-view [card]="card" />
          </button>
        } @empty {
          <p class="col-span-3 py-8 text-center text-sm text-[var(--pk-text-dim)]">
            No se encontraron cartas
          </p>
        }
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeckSearchComponent {
  private readonly cardApi = inject(CardApiService);

  protected readonly filterOptions: FilterOption[] = [
    { label: 'Todas', value: '' },
    { label: 'Pokemon', value: 'POKEMON' },
    { label: 'Energía', value: 'ENERGY' },
    { label: 'Entrenador', value: 'TRAINER' },
  ];

  protected readonly stageFilterOptions: FilterOption[] = [
    { label: 'Todas', value: '' },
    { label: 'Básico', value: 'BASIC' },
    { label: 'Stage 1', value: 'STAGE_1' },
    { label: 'Stage 2', value: 'STAGE_2' },
    { label: 'MEGA', value: 'MEGA' },
  ];

  protected readonly supertype = signal('');
  protected readonly stage = signal('');
  protected readonly page = signal(0);
  protected readonly totalItems = signal(0);
  protected readonly totalPages = computed(() => {
    const total = this.totalItems();
    return total > 0 ? Math.max(1, Math.ceil(total / 12)) : 0;
  });

  private readonly searchSubject = new Subject<string>();
  private readonly filterSubject = new Subject<string>();
  private readonly stageFilterSubject = new Subject<string>();
  private readonly pageSubject = new Subject<number>();
  private currentQuery = '';

  private readonly query$ = this.searchSubject.pipe(
    debounceTime(300),
    distinctUntilChanged(),
    map((query) => ({ query, supertype: this.supertype(), stage: this.stage(), page: 0 })),
  );

  private readonly filter$ = this.filterSubject.pipe(
    map((supertype) => {
      this.supertype.set(supertype);
      if (supertype !== 'POKEMON') {
        this.stage.set('');
      }
      return { query: this.currentQuery, supertype, stage: this.stage(), page: 0 };
    }),
  );

  private readonly stageFilter$ = this.stageFilterSubject.pipe(
    map((stage) => {
      this.stage.set(stage);
      return { query: this.currentQuery, supertype: this.supertype(), stage, page: 0 };
    }),
  );

  private readonly pageChange$ = this.pageSubject.pipe(
    map((page) => ({ query: this.currentQuery, supertype: this.supertype(), stage: this.stage(), page })),
  );

  private readonly PAGE_SIZE = 12;

  private readonly search$ = merge(this.query$, this.filter$, this.stageFilter$, this.pageChange$).pipe(
    startWith({ query: undefined, supertype: undefined, stage: undefined, page: 0 }),
    switchMap(({ query, supertype, stage, page }) =>
      this.cardApi.searchCards({
        query: query || undefined,
        supertype: supertype || undefined,
        stage: stage || undefined,
        page,
        size: this.PAGE_SIZE,
      }).pipe(
        tap((response) => {
          this.page.set(response.page);
          this.totalItems.set(response.totalItems);
        }),
        catchError(() => of({ items: [], page: 0, size: this.PAGE_SIZE, totalItems: 0 })),
      ),
    ),
  );

  protected readonly searchResult = toSignal(this.search$);

  readonly cardSelected = output<{ cardId: string; name: string; supertype: string; subtypes: string[]; stage?: string }>();

  onSearchInput(value: string): void {
    this.currentQuery = value;
    this.page.set(0);
    this.searchSubject.next(value);
  }

  onFilterChange(value: string): void {
    this.page.set(0);
    this.filterSubject.next(value);
  }

  onStageFilterChange(value: string): void {
    this.page.set(0);
    this.stageFilterSubject.next(value);
  }

  onPageChange(page: number): void {
    this.pageSubject.next(page);
  }

  selectCard(cardId: string, name: string, supertype: string, subtypes: string[], stage?: string): void {
    this.cardSelected.emit({ cardId, name, supertype, subtypes, stage });
  }

  noop(): void {}
}
