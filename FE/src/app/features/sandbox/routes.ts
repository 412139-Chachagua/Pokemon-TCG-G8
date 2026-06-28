import { Routes } from '@angular/router';

export const sandboxRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/sandbox-page/sandbox-page').then((m) => m.SandboxPage),
  },
];
