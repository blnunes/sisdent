import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  if (
    !token ||
    request.url.endsWith('/api/auth/login') ||
    request.url.endsWith('/api/auth/email-verification')
  ) {
    return next(request);
  }
  let scopedRequest = request;
  const membership = auth.activeMembership();
  if (membership && request.url.startsWith('/api/patients')) {
    const suffix = request.url.slice('/api/patients'.length);
    const separator = suffix.includes('?') ? '&' : '?';
    const clinic = membership.clinicUnitId
      ? `${separator}clinicUnitId=${encodeURIComponent(membership.clinicUnitId)}`
      : '';
    scopedRequest = request.clone({
      url: `/api/organizations/${membership.organizationId}/patients${suffix}${clinic}`,
    });
  }
  return next(scopedRequest.clone({ setHeaders: { Authorization: `Bearer ${token}` } })).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        auth.logout();
      }
      return throwError(() => error);
    }),
  );
};
