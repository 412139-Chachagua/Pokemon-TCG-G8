import { Routes } from '@angular/router';

export const rulesRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/rules-page/rules-page').then((m) => m.RulesPage),
  },
];
