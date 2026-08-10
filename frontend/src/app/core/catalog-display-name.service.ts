import { Injectable, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

export type CatalogRecord = Readonly<Record<string, unknown>>;

@Injectable({ providedIn: 'root' })
export class CatalogDisplayNameService {
  private readonly translate = inject(TranslateService);

  speciality(record: CatalogRecord): string {
    return this.translatedCatalogName('SPECIALITIES', record);
  }

  procedure(record: CatalogRecord): string {
    return this.translatedCatalogName('PROCEDURES', record);
  }

  country(record: CatalogRecord): string {
    const fallback = this.fallback(record);
    const code = String(record['code'] ?? '').toUpperCase();
    if (!/^[A-Z]{2}$/.test(code)) return fallback;
    try {
      return (
        new Intl.DisplayNames([this.translate.getCurrentLang() ?? 'en'], { type: 'region' }).of(
          code,
        ) ?? fallback
      );
    } catch {
      return fallback;
    }
  }

  private translatedCatalogName(
    group: 'SPECIALITIES' | 'PROCEDURES',
    record: CatalogRecord,
  ): string {
    const fallback = this.fallback(record);
    const canonical = String(record['name'] ?? '');
    if (record['displayName'] && fallback !== canonical) return fallback;
    const key = `CATALOG.${group}.${this.slug(canonical)}`;
    const translated = this.translate.instant(key);
    return translated === key ? fallback : translated;
  }

  private fallback(record: CatalogRecord): string {
    return String(record['displayName'] ?? record['name'] ?? '—');
  }

  private slug(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)/g, '');
  }
}
