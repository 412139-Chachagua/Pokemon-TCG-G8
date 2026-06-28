import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';

export const homeRoutes: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/home-page/home-page').then((m) => m.HomePage),
  },
];
