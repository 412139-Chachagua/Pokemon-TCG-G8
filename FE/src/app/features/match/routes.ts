import { Routes } from '@angular/router';

export const matchRoutes: Routes = [
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/match-page/match-page').then((m) => m.MatchPage),
  },
];
