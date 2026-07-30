import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  if (!token || request.url.endsWith('/api/auth/login')) {
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
  return next(scopedRequest.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
