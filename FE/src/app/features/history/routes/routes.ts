import { Routes } from '@angular/router';

export const historyRoutes: Routes = [
  { path: '', loadComponent: () => import('../pages/history-page/history-page').then(m => m.HistoryPage) },
];
