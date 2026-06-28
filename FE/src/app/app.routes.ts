import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { alreadyAuthGuard } from './core/guards/already-auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/welcome', pathMatch: 'full' },
  {
    path: 'cards',
    canActivate: [authGuard],
    loadChildren: () => import('./features/cards/routes').then((m) => m.cardRoutes),
  },
  {
    path: 'decks',
    canActivate: [authGuard],
    loadChildren: () => import('./features/decks/routes').then((m) => m.deckRoutes),
  },
  {
    path: 'lobby',
    canActivate: [authGuard],
    loadChildren: () => import('./features/lobby/routes').then((m) => m.lobbyRoutes),
  },
  {
    path: 'match',
    canActivate: [authGuard],
    loadChildren: () => import('./features/match/routes').then((m) => m.matchRoutes),
  },
  {
    path: 'auth',
    canActivate: [alreadyAuthGuard],
    loadChildren: () => import('./features/auth/routes').then((m) => m.authRoutes),
  },
  {
    path: 'home',
    canActivate: [authGuard],
    loadChildren: () => import('./features/home/routes').then((m) => m.homeRoutes),
  },
  {
    path: 'ranking',
    canActivate: [authGuard],
    loadChildren: () => import('./features/ranking/routes').then((m) => m.rankingRoutes),
  },
  {
    path: 'history',
    canActivate: [authGuard],
    loadChildren: () => import('./features/history/routes/routes').then((m) => m.historyRoutes),
  },
  {
    path: 'profile',
    loadChildren: () => import('./features/profile/routes').then((m) => m.profileRoutes),
  },
  {
    path: 'welcome',
    loadComponent: () =>
      import('./features/home/components/splash/splash.component').then((m) => m.SplashComponent),
  },
  {
    path: 'sandbox',
    loadChildren: () => import('./features/sandbox/routes').then((m) => m.sandboxRoutes),
  },
  {
    path: 'rules',
    canActivate: [authGuard],
    loadChildren: () => import('./features/rules/routes').then((m) => m.rulesRoutes),
  },
  {
    path: 'quick-match',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/lobby/pages/quick-match-page/quick-match-page').then((m) => m.QuickMatchPage),
  },
  { path: 's', redirectTo: '/sandbox', pathMatch: 'full' },
  { path: '**', redirectTo: '/home' },
];
