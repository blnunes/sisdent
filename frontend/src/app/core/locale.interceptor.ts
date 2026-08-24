import { HttpInterceptorFn } from '@angular/common/http';

const STORAGE_KEY = 'sisdent.language';
const DEFAULT_LANGUAGE = 'en';
const SUPPORTED_LANGUAGES = new Set(['en', 'nl', 'pt-PT']);

/** Adds the UI locale to API calls while keeping locale selection independent of authentication. */
export const localeInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.url !== '/graphql') return next(request);

  const savedLanguage = localStorage.getItem(STORAGE_KEY);
  const language =
    savedLanguage && SUPPORTED_LANGUAGES.has(savedLanguage) ? savedLanguage : DEFAULT_LANGUAGE;
  return next(request.clone({ setHeaders: { 'Accept-Language': language } }));
};
