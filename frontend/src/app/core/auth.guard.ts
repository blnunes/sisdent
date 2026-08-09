import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Permission } from './models';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.authenticated() ? true : inject(Router).createUrlTree(['/login']);
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAdmin() ? true : inject(Router).createUrlTree(['/not-found']);
};

export const platformAdministrationGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isPlatformAdministrator() ? true : inject(Router).createUrlTree(['/not-found']);
};

export const permissionsGuard = (...permissions: Permission[]): CanActivateFn => () => {
  const auth = inject(AuthService);
  return auth.hasAllPermissions(...permissions)
    ? true
    : inject(Router).createUrlTree(['/not-found']);
};

export const anyPermissionsGuard = (...permissions: Permission[]): CanActivateFn => () => {
  const auth = inject(AuthService);
  return auth.hasAnyPermission(...permissions)
    ? true
    : inject(Router).createUrlTree(['/not-found']);
};

export const organizationAdministrationGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.canAdministerOrganization()
    ? true
    : inject(Router).createUrlTree(['/not-found']);
};

export const practitionerManagementGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.canManagePractitioners()
    ? true
    : inject(Router).createUrlTree(['/not-found']);
};
