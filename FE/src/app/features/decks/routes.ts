import { Routes } from '@angular/router';

export const deckRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/deck-list-page/deck-list-page').then(
        (m) => m.DeckListPage,
      ),
  },
  {
    path: 'new',
    loadComponent: () =>
      import('./pages/deck-builder-page/deck-builder-page').then(
        (m) => m.DeckBuilderPage,
      ),
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./pages/deck-builder-page/deck-builder-page').then(
        (m) => m.DeckBuilderPage,
      ),
  },
];
