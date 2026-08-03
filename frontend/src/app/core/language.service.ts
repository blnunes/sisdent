import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { TranslateService } from '@ngx-translate/core';
import { Router } from '@angular/router';
import { DateAdapter } from '@angular/material/core';

export type Language = 'pt-PT' | 'en' | 'nl';

const STORAGE_KEY = 'sisdent.language';
const SUPPORTED_LANGUAGES: readonly Language[] = ['pt-PT', 'en', 'nl'];

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly translate = inject(TranslateService);
  private readonly document = inject(DOCUMENT);
  private readonly title = inject(Title);
  private readonly router = inject(Router);
  private readonly dateAdapter = inject<DateAdapter<Date>>(DateAdapter);
  readonly current = signal<Language>(this.savedLanguage());

  constructor() {
    this.set(this.current());
  }

  set(language: Language): void {
    this.current.set(language);
    localStorage.setItem(STORAGE_KEY, language);
    this.document.documentElement.lang = language;
    this.dateAdapter.setLocale(language === 'en' ? 'en-US' : language);
    this.translate.use(language).subscribe({
      next: () => this.setTitle(),
      error: (error) => this.reportLoadFailure(language, error),
    });
  }

  private reportLoadFailure(language: Language, error: unknown): void {
    const message =
      error instanceof Error ? error.message : 'HTTP translation resource could not be loaded';
    void this.router.navigate(['/translation-error'], {
      queryParams: { language, resource: `/i18n/${language}.json`, message },
      replaceUrl: true,
    });
  }

  private setTitle(): void {
    this.title.setTitle(this.translate.instant('APP.TITLE'));
  }

  private savedLanguage(): Language {
    const saved = localStorage.getItem(STORAGE_KEY);
    return SUPPORTED_LANGUAGES.includes(saved as Language) ? (saved as Language) : 'en';
  }
}
