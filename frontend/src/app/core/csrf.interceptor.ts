import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { CsrfService } from './csrf.service';

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

export const csrfInterceptor: HttpInterceptorFn = (request, next) => {
  if (SAFE_METHODS.has(request.method) || request.url === '/api/csrf') return next(request);
  const csrf = inject(CsrfService);
  return next(request.clone({ setHeaders: csrf.header() }));
};
