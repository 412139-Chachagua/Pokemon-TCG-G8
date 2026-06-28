import { Routes } from '@angular/router';

export const lobbyRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/lobby-page/lobby-page').then((m) => m.LobbyPage),
  },
];
