import { Routes } from '@angular/router';

export const cardRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/card-catalog-page/card-catalog-page').then(
        (m) => m.CardCatalogPage,
      ),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/card-detail-page/card-detail-page').then(
        (m) => m.CardDetailPage,
      ),
  },
];
