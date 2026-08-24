import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';
import { GraphQlClientService } from './graphql-client.service';

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
  private readonly graphql = inject(GraphQlClientService);

  list(type: CatalogResourceType | '', query: string) {
    return this.graphql.query<{ catalogTranslations: Array<Omit<CatalogTranslationEntry, 'translations'> & { translations: Array<{ locale: string; value: string }> }> }>(
      'query CatalogTranslations($type: CatalogResourceType, $query: String) { catalogTranslations(type: $type, query: $query) { resourceType resourceId parentId canonicalName translations { locale value } customizedLocales missingLocales } }',
      { type: type || null, query: query.trim() || null },
    ).pipe(map(({ catalogTranslations }) => catalogTranslations.map((entry) => ({ ...entry, translations: Object.fromEntries(entry.translations.map(({ locale, value }) => [locale, value])) }))));
  }

  replace(entry: CatalogTranslationEntry, translations: CatalogTranslations) {
    return this.graphql.query<{ replaceCatalogTranslations: Omit<CatalogTranslationEntry, 'translations'> & { translations: Array<{ locale: string; value: string }> } }>(
      'mutation ReplaceCatalogTranslations($type: CatalogResourceType!, $id: ID!, $translations: [CatalogTranslationMutationInput!]!) { replaceCatalogTranslations(type: $type, id: $id, translations: $translations) { resourceType resourceId parentId canonicalName translations { locale value } customizedLocales missingLocales } }',
      { type: entry.resourceType, id: entry.resourceId, translations: Object.entries(translations).map(([locale, value]) => ({ locale, value })) },
    ).pipe(map(({ replaceCatalogTranslations }) => ({ ...replaceCatalogTranslations, translations: Object.fromEntries(replaceCatalogTranslations.translations.map(({ locale, value }) => [locale, value])) })));
  }
}
