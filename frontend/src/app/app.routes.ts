import { Routes } from '@angular/router';
import {
  anyPermissionsGuard,
  authGuard,
  organizationAdministrationGuard,
  platformAdministrationGuard,
  practitionerManagementGuard,
} from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'clinic-units',
    canActivate: [authGuard, organizationAdministrationGuard],
    loadComponent: () =>
      import('./features/organization/clinic-units.component').then((m) => m.ClinicUnitsComponent),
  },
  {
    path: 'practitioners',
    canActivate: [authGuard, practitionerManagementGuard],
    loadComponent: () =>
      import('./features/organization/practitioners.component').then(
        (m) => m.PractitionersComponent,
      ),
  },
  {
    path: 'appointments',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/appointments/appointments.component').then((m) => m.AppointmentsComponent),
  },
  {
    path: 'clinical',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/clinical/clinical-workspace.component').then(
        (m) => m.ClinicalWorkspaceComponent,
      ),
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
    path: 'accounts',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/accounts/accounts.component').then((m) => m.AccountsComponent),
  },
  {
    path: 'settings/translations',
    canActivate: [authGuard, platformAdministrationGuard],
    loadComponent: () =>
      import('./features/catalog-translations/catalog-translations.component').then(
        (m) => m.CatalogTranslationsComponent,
      ),
  },
  {
    path: 'patients',
    canActivate: [authGuard, anyPermissionsGuard('READ_PATIENTS', 'MAINTAIN_PATIENTS')],
    loadComponent: () =>
      import('./features/patients/patients.component').then((m) => m.PatientsComponent),
  },
  {
    path: 'specialities',
    canActivate: [authGuard, anyPermissionsGuard('READ_SPECIALITIES', 'MAINTAIN_SPECIALITIES')],
    loadComponent: () =>
      import('./features/specialities/specialities.component').then((m) => m.SpecialitiesComponent),
  },
  {
    path: 'addresses',
    canActivate: [authGuard, anyPermissionsGuard('READ_ADDRESSES', 'MAINTAIN_ADDRESSES')],
    loadComponent: () =>
      import('./features/addresses/addresses.component').then((m) => m.AddressesComponent),
  },
  {
    path: 'countries',
    canActivate: [authGuard, anyPermissionsGuard('READ_COUNTRIES', 'MAINTAIN_COUNTRIES')],
    loadComponent: () =>
      import('./features/countries/countries.component').then((m) => m.CountriesComponent),
  },
  {
    path: 'administrative-divisions',
    canActivate: [
      authGuard,
      anyPermissionsGuard('READ_ADMINISTRATIVE_DIVISIONS', 'MAINTAIN_ADMINISTRATIVE_DIVISIONS'),
    ],
    loadComponent: () =>
      import('./features/administrative-divisions/administrative-divisions.component').then(
        (m) => m.AdministrativeDivisionsComponent,
      ),
  },
  {
    path: 'not-found',
    loadComponent: () =>
      import('./features/not-found/not-found.component').then((m) => m.NotFoundComponent),
  },
  {
    path: 'translation-error',
    loadComponent: () =>
      import('./features/translation-error/translation-error.component').then(
        (m) => m.TranslationErrorComponent,
      ),
  },
  { path: '', pathMatch: 'full', redirectTo: 'home' },
  { path: '**', redirectTo: 'not-found' },
];
