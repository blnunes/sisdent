import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { provideRouter } from '@angular/router';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import defaultEnglish from '../../public/i18n/en.json';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth.interceptor';
import { localeInterceptor } from './core/locale.interceptor';
import { csrfInterceptor } from './core/csrf.interceptor';
import { CsrfService } from './core/csrf.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([localeInterceptor, csrfInterceptor, authInterceptor])),
    provideNativeDateAdapter(),
    { provide: MAT_DATE_LOCALE, useValue: 'pt-PT' },
    provideTranslateService({
      loader: provideTranslateHttpLoader({ prefix: '/i18n/', suffix: '.json' }),
      fallbackLang: 'en',
      lang: 'en',
    }),
    provideAppInitializer(() => {
      const translate = inject(TranslateService);
      translate.setTranslation('en', defaultEnglish, true);
      translate.setFallbackLang('en');
      translate.use('en');
    }),
    provideAppInitializer(() => firstValueFrom(inject(CsrfService).initialize())),
  ],
};
