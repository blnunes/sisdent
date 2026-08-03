import { Routes } from '@angular/router';
import { authGuard, organizationAdministrationGuard, practitionerManagementGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'clinic-units',
    canActivate: [authGuard, organizationAdministrationGuard],
    loadComponent: () => import('./features/organization/clinic-units.component').then((m) => m.ClinicUnitsComponent),
  },
  {
    path: 'practitioners',
    canActivate: [authGuard, practitionerManagementGuard],
    loadComponent: () => import('./features/organization/practitioners.component').then((m) => m.PractitionersComponent),
  },
  {
    path: 'appointments',
    canActivate: [authGuard],
    loadComponent: () => import('./features/appointments/appointments.component').then((m) => m.AppointmentsComponent),
  },
  {
    path: 'clinical',
    canActivate: [authGuard],
    loadComponent: () => import('./features/clinical/clinical-workspace.component').then((m) => m.ClinicalWorkspaceComponent),
  },
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
    path: 'email-enrollment',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/email-enrollment/email-enrollment.component').then(
        (m) => m.EmailEnrollmentComponent,
      ),
  },
  {
    path: 'verify-email',
    loadComponent: () =>
      import('./features/email-verification/email-verification.component').then(
        (m) => m.EmailVerificationComponent,
      ),
  },
  {
    path: 'accounts',
    canActivate: [authGuard],
    loadComponent: () => import('./features/accounts/accounts.component').then((m) => m.AccountsComponent),
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
