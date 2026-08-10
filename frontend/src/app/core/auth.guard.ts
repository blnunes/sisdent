import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';
import { Permission } from './models';
import { markSystemUnavailable, wasSystemUnavailable } from './system-availability';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.authenticated()) return router.createUrlTree([wasSystemUnavailable() ? '/unavailable' : '/login']);
  return auth.loadSession().pipe(
    map(() => true),
    catchError((error: unknown) => {
      if (isSystemUnavailable(error)) markSystemUnavailable();
      auth.clearSession();
      return of(router.createUrlTree([isSystemUnavailable(error) ? '/unavailable' : '/login']));
    }),
  );
};

function isSystemUnavailable(error: unknown): error is { status: number } {
  if (typeof error !== 'object' || error === null || !('status' in error)) return false;
  const status = error.status;
  return typeof status === 'number' && (status === 0 || status >= 500);
}

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAdmin() ? true : inject(Router).createUrlTree(['/not-found']);
};

export const platformAdministrationGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isPlatformAdministrator() ? true : inject(Router).createUrlTree(['/not-found']);
};

export const permissionsGuard =
  (...permissions: Permission[]): CanActivateFn =>
  () => {
    const auth = inject(AuthService);
    return auth.hasAllPermissions(...permissions)
      ? true
      : inject(Router).createUrlTree(['/not-found']);
  };

export const anyPermissionsGuard =
  (...permissions: Permission[]): CanActivateFn =>
  () => {
    const auth = inject(AuthService);
    return auth.hasAnyPermission(...permissions)
      ? true
      : inject(Router).createUrlTree(['/not-found']);
  };

export const organizationAdministrationGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.canAdministerOrganization() ? true : inject(Router).createUrlTree(['/not-found']);
};

export const practitionerManagementGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.canManagePractitioners() ? true : inject(Router).createUrlTree(['/not-found']);
};
