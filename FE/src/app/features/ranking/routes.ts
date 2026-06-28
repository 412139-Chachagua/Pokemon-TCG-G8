import { Routes } from '@angular/router';

export const rankingRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/ranking-page/ranking-page').then((m) => m.RankingPage),
  },
];
