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
    data: {
      key: 'patients', endpoint: '/api/patients', title: 'MODULES.PATIENTS', description: 'MODULES.PATIENTS_DESCRIPTION', maintainPermission: 'MAINTAIN_PATIENTS',
      filters: [
        { key: 'name', label: 'RESOURCE.FILTER.NAME', type: 'autocomplete' },
        { key: 'birthDate', label: 'RESOURCE.FILTER.BIRTH_DATE', type: 'date', placement: 'advanced' },
        { key: 'active', label: 'RESOURCE.FILTER.STATUS', type: 'select', options: [{ value: 'true', label: 'RESOURCE.FILTER.ACTIVE' }, { value: 'false', label: 'RESOURCE.FILTER.INACTIVE' }] },
        { key: 'gender', label: 'RESOURCE.FILTER.GENDER', type: 'select', placement: 'advanced', options: [{ value: 'FEMALE', label: 'RESOURCE.FILTER.FEMALE' }, { value: 'MALE', label: 'RESOURCE.FILTER.MALE' }, { value: 'OTHER', label: 'RESOURCE.FILTER.OTHER' }] },
        { key: 'taxId', label: 'RESOURCE.FILTER.TAX_ID', type: 'autocomplete', placement: 'advanced' },
        { key: 'identificationType', label: 'RESOURCE.FILTER.IDENTIFICATION_TYPE', type: 'select', placement: 'advanced', options: [{ value: 'NATIONAL_ID_CARD', label: 'RESOURCE.FILTER.NATIONAL_ID' }, { value: 'PASSPORT', label: 'RESOURCE.FILTER.PASSPORT' }] },
        { key: 'nationalityCode', label: 'RESOURCE.FILTER.NATIONALITY', type: 'autocomplete', selectionRequired: true, placement: 'advanced' },
        { key: 'addressId', label: 'RESOURCE.FILTER.ADDRESS', type: 'autocomplete', selectionRequired: true, placement: 'advanced' },
        { key: 'specialityId', label: 'RESOURCE.FILTER.SPECIALITY', type: 'autocomplete', selectionRequired: true },
      ],
    },
  },
  {
    path: 'specialities',
    canActivate: [authGuard, anyPermissionsGuard('READ_SPECIALITIES', 'MAINTAIN_SPECIALITIES')],
    loadComponent: () => import('./features/resources/resource-list.component').then((m) => m.ResourceListComponent),
    data: {
      key: 'specialities', endpoint: '/api/specialities', title: 'MODULES.SPECIALITIES', description: 'MODULES.SPECIALITIES_DESCRIPTION', maintainPermission: 'MAINTAIN_SPECIALITIES',
      filters: [
        { key: 'name', label: 'RESOURCE.FILTER.NAME', type: 'autocomplete' },
        { key: 'procedure', label: 'Procedures', type: 'autocomplete' },
      ],
    },
  },
  {
    path: 'addresses',
    canActivate: [authGuard, anyPermissionsGuard('READ_ADDRESSES', 'MAINTAIN_ADDRESSES')],
    loadComponent: () => import('./features/resources/resource-list.component').then((m) => m.ResourceListComponent),
    data: { key: 'addresses', endpoint: '/api/addresses', title: 'MODULES.ADDRESSES', description: 'MODULES.ADDRESSES_DESCRIPTION', maintainPermission: 'MAINTAIN_ADDRESSES' },
  },
  {
    path: 'countries',
    canActivate: [authGuard, anyPermissionsGuard('READ_COUNTRIES', 'MAINTAIN_COUNTRIES')],
    loadComponent: () => import('./features/resources/resource-list.component').then((m) => m.ResourceListComponent),
    data: { key: 'countries', endpoint: '/api/countries', title: 'MODULES.COUNTRIES', description: 'MODULES.COUNTRIES_DESCRIPTION', maintainPermission: 'MAINTAIN_COUNTRIES' },
  },
  {
    path: 'administrative-divisions',
    canActivate: [authGuard, anyPermissionsGuard('READ_ADMINISTRATIVE_DIVISIONS', 'MAINTAIN_ADMINISTRATIVE_DIVISIONS')],
    loadComponent: () => import('./features/resources/resource-list.component').then((m) => m.ResourceListComponent),
    data: { key: 'administrativeDivisions', endpoint: '/api/administrative-divisions', title: 'MODULES.ADMINISTRATIVE_DIVISIONS', description: 'MODULES.ADMINISTRATIVE_DIVISIONS_DESCRIPTION', maintainPermission: 'MAINTAIN_ADMINISTRATIVE_DIVISIONS' },
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
