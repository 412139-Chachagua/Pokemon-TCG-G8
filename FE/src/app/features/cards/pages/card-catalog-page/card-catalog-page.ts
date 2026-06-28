import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { BackButtonComponent } from '../../../../shared/components/back-button/back-button.component';
import { CardCatalogFacadeService } from '../../services/card-catalog-facade.service';
import { SearchBarComponent } from '../../../../shared/components/search-bar/search-bar.component';
import { CardFilterComponent, FilterOption } from '../../../../shared/components/card-filter/card-filter.component';
import { CardViewComponent } from '../../../../shared/components/card-view/card-view.component';
import { CardPreviewOverlayComponent } from '../../../match/components/card-preview-overlay/card-preview-overlay.component';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination.component';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-card-catalog-page',
  imports: [
    BackButtonComponent,
    SearchBarComponent,
    CardFilterComponent,
    CardViewComponent,
    CardPreviewOverlayComponent,
    PaginationComponent,
    LoadingSpinnerComponent,
  ],
  template: `
    <div class="relative min-h-dvh">
      <app-back-button />
      <main class="min-h-dvh bg-[var(--pk-bg)] p-8 max-w-5xl mx-auto">
        <h1 class="m-0 mb-4 text-lg text-[var(--pk-text)]">CATÁLOGO DE CARTAS</h1>

        <div class="mb-6 flex flex-col gap-4 sm:flex-row">
          <div class="flex-1">
            <app-search-bar
              placeholder="Buscar por nombre..."
              (queryChange)="facade.setQuery($event)"
            />
          </div>
          <div class="w-full sm:w-48">
            <app-card-filter
              [options]="filterOptions"
              [selected]="facade.supertype()"
              (filterChange)="facade.setSupertype($event)"
            />
          </div>
          <div class="w-full sm:w-48">
            <app-card-filter
              [options]="stageFilterOptions"
              [selected]="facade.stage()"
              [disabled]="facade.supertype() !== 'POKEMON'"
              (filterChange)="facade.setStage($event)"
            />
          </div>
        </div>

        @if (facade.loading()) {
          <app-loading-spinner />
        } @else if (facade.error()) {
          <div class="flex flex-col items-center gap-4 py-12">
            <p class="text-red-600">{{ facade.error() }}</p>
            <button class="pk-btn pk-btn--danger" (click)="facade.search()">Reintentar</button>
          </div>
        } @else if (facade.cards().length === 0) {
          <div class="py-12 text-center">
            <p class="text-gray-500">No se encontraron cartas</p>
          </div>
        } @else {
          <div class="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
            @for (card of facade.cards(); track card.id) {
              <div (click)="goToCard(card.id)" class="cursor-pointer">
                <app-card-view [card]="card" />
              </div>
            }
          </div>

          <div class="mt-8 flex justify-center">
            <app-pagination
              [currentPage]="facade.page()"
              [totalPages]="facade.totalPages()"
              (pageChange)="facade.setPage($event)"
            />
          </div>
        }
      </main>
      <app-card-preview-overlay />
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardCatalogPage {
  private readonly router = inject(Router);
  protected readonly facade = inject(CardCatalogFacadeService);

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

  constructor() {
    this.facade.search();
  }

  goToCard(cardId: string): void {
    this.router.navigate(['/cards', cardId]);
  }
}
