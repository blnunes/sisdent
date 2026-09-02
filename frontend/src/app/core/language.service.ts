import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { TranslateService } from '@ngx-translate/core';
import { Router } from '@angular/router';
import { DateAdapter } from '@angular/material/core';

export type Language = 'pt-PT' | 'en' | 'nl';

const STORAGE_KEY = 'sisdent.language';
export const SUPPORTED_LANGUAGES: readonly Language[] = ['pt-PT', 'en', 'nl'];
export const LANGUAGE_OPTIONS: readonly { value: Language; label: string }[] = [
  { value: 'pt-PT', label: 'Português (Portugal)' },
  { value: 'en', label: 'English' },
  { value: 'nl', label: 'Nederlands' },
];
export const LANGUAGE_CHANGED_EVENT = 'sisdent-language-changed';

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

  set(language: string): void {
    const selected = this.isSupported(language) ? language : 'en';
    const changed = this.current() !== selected;
    this.current.set(selected);
    localStorage.setItem(STORAGE_KEY, selected);
    this.document.documentElement.lang = selected;
    this.dateAdapter.setLocale(selected === 'en' ? 'en-US' : selected);
    this.translate.use(selected).subscribe({
      next: () => {
        this.setTitle();
        if (changed) window.dispatchEvent(new Event(LANGUAGE_CHANGED_EVENT));
      },
      error: (error) => this.reportLoadFailure(selected, error),
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
    return this.isSupported(saved) ? saved : 'en';
  }

  isSupported(value: string | null | undefined): value is Language {
    return SUPPORTED_LANGUAGES.includes(value as Language);
  }
}
