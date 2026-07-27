import { Routes } from '@angular/router';
import { anyPermissionsGuard, authGuard, permissionsGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'home',
    canActivate: [authGuard],
    loadComponent: () => import('./features/home/home.component').then((m) => m.HomeComponent),
  },
  {
    path: 'users',
    canActivate: [authGuard, permissionsGuard('MAINTAIN_USERS')],
    loadComponent: () => import('./features/users/users.component').then((m) => m.UsersComponent),
  },
  {
    path: 'patients',
    canActivate: [authGuard, anyPermissionsGuard('READ_PATIENTS', 'MAINTAIN_PATIENTS')],
    loadComponent: () => import('./features/resources/resource-list.component').then((m) => m.ResourceListComponent),
    data: { key: 'patients', endpoint: '/api/patients', title: 'MODULES.PATIENTS', description: 'MODULES.PATIENTS_DESCRIPTION' },
  },
  {
    path: 'specialities',
    canActivate: [authGuard, anyPermissionsGuard('READ_SPECIALITIES', 'MAINTAIN_SPECIALITIES')],
    loadComponent: () => import('./features/resources/resource-list.component').then((m) => m.ResourceListComponent),
    data: { key: 'specialities', endpoint: '/api/specialities', title: 'MODULES.SPECIALITIES', description: 'MODULES.SPECIALITIES_DESCRIPTION' },
  },
  {
    path: 'addresses',
    canActivate: [authGuard, anyPermissionsGuard('READ_ADDRESSES', 'MAINTAIN_ADDRESSES')],
    loadComponent: () => import('./features/resources/resource-list.component').then((m) => m.ResourceListComponent),
    data: { key: 'addresses', endpoint: '/api/addresses', title: 'MODULES.ADDRESSES', description: 'MODULES.ADDRESSES_DESCRIPTION' },
  },
  {
    path: 'countries',
    canActivate: [authGuard, anyPermissionsGuard('READ_COUNTRIES', 'MAINTAIN_COUNTRIES')],
    loadComponent: () => import('./features/resources/resource-list.component').then((m) => m.ResourceListComponent),
    data: { key: 'countries', endpoint: '/api/countries', title: 'MODULES.COUNTRIES', description: 'MODULES.COUNTRIES_DESCRIPTION' },
  },
  {
    path: 'states',
    canActivate: [authGuard, anyPermissionsGuard('READ_STATES', 'MAINTAIN_STATES')],
    loadComponent: () => import('./features/resources/resource-list.component').then((m) => m.ResourceListComponent),
    data: { key: 'states', endpoint: '/api/states', title: 'MODULES.STATES', description: 'MODULES.STATES_DESCRIPTION' },
  },
  {
    path: 'permissions',
    canActivate: [authGuard, anyPermissionsGuard('READ_PERMISSIONS', 'MAINTAIN_PERMISSIONS')],
    loadComponent: () => import('./features/permissions/permissions.component').then((m) => m.PermissionsComponent),
  },
  {
    path: 'not-found',
    loadComponent: () =>
      import('./features/not-found/not-found.component').then((m) => m.NotFoundComponent),
  },
  {
    path: 'translation-error',
    loadComponent: () =>
      import('./features/translation-error/translation-error.component').then((m) => m.TranslationErrorComponent),
  },
  { path: '', pathMatch: 'full', redirectTo: 'home' },
  { path: '**', redirectTo: 'not-found' },
];
