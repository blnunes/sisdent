import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export type CatalogResourceType = 'SPECIALITY' | 'PROCEDURE';
export type CatalogTranslations = Readonly<Record<string, string>>;

export interface CatalogTranslationEntry {
  resourceType: CatalogResourceType;
  resourceId: number;
  parentId?: number;
  canonicalName: string;
  translations: CatalogTranslations;
  customizedLocales: string[];
  missingLocales: string[];
}

@Injectable({ providedIn: 'root' })
export class CatalogTranslationApiService {
  private readonly http = inject(HttpClient);

  list(type: CatalogResourceType | '', query: string) {
    let params = new HttpParams();
    if (type) params = params.set('type', type);
    if (query.trim()) params = params.set('query', query.trim());
    return this.http.get<CatalogTranslationEntry[]>('/api/platform/catalog-translations', {
      params,
    });
  }

  replace(entry: CatalogTranslationEntry, translations: CatalogTranslations) {
    return this.http.put<CatalogTranslationEntry>(
      `/api/platform/catalog-translations/${entry.resourceType}/${entry.resourceId}`,
      { translations },
    );
  }
}
