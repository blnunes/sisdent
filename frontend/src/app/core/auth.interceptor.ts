import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { clearSystemUnavailable, markSystemUnavailable } from './system-availability';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  // Translation/assets are loaded while LanguageService can still be constructing.
  // Avoid resolving AuthService for non-API requests, which would create a DI cycle.
  if (!isApplicationRequest(request.url)) return next(request);
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token();
  const isAuthenticationRequest = request.url === '/api/auth/login';
  const outgoing = token && !isAuthenticationRequest
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;
  return next(outgoing).pipe(
    catchError((error: unknown) => {
      if (isUnauthorized(error) && !isAuthenticationRequest && auth.token() === token) {
        clearSystemUnavailable();
        auth.clearSession();
        void router.navigateByUrl('/login');
      } else if (isSystemUnavailable(error)) {
        markSystemUnavailable();
        auth.clearSession();
      }
      return throwError(() => error);
    }),
  );
};

function isApplicationRequest(url: string): boolean {
  return (
    url === '/graphql'
    || url === '/api/auth/login'
    || url === '/api/session'
    || url === '/api/csrf'
  );
}

function isUnauthorized(error: unknown): error is { status: number } {
  return typeof error === 'object' && error !== null && 'status' in error && error.status === 401;
}

function isSystemUnavailable(error: unknown): error is { status: number } {
  if (typeof error !== 'object' || error === null || !('status' in error)) return false;
  const status = error.status;
  return typeof status === 'number' && (status === 0 || status >= 500);
}
