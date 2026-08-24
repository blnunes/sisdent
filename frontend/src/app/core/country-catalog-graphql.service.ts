import { Injectable, inject } from '@angular/core';
import { Observable, map, throwError } from 'rxjs';
import { PageResponse } from './models';
import { GraphQlClientService, GraphQlUserError } from './graphql-client.service';
import { LanguageService } from './language.service';

export interface CountryCatalogueItem extends Record<string, unknown> {
  id: string;
  code: string;
  name: string;
  displayName: string;
  continent: string;
}

export interface CountryCatalogueQuery {
  page: number;
  size: number;
  sort: string;
  direction: 'asc' | 'desc';
}

const COUNTRIES_QUERY = `query Countries($page: CataloguePageInput, $locale: String) {
  countries(page: $page, locale: $locale) {
    content { id code name displayName continent }
    page
    size
    totalElements
    totalPages
  }
}`;
const CONTINENTS_QUERY = 'query Continents { continents }';

export function supportsCatalogueLocale(locale: string): boolean {
  try {
    return ['en', 'pt', 'nl'].includes(new Intl.Locale(locale).language);
  } catch {
    return false;
  }
}

@Injectable({ providedIn: 'root' })
export class CountryCatalogGraphqlService {
  private readonly graphql = inject(GraphQlClientService);
  private readonly language = inject(LanguageService);

  list(
    query: CountryCatalogueQuery,
    locale: string = this.language.current(),
  ): Observable<PageResponse<CountryCatalogueItem>> {
    if (!supportsCatalogueLocale(locale)) {
      return throwError(
        () =>
          new GraphQlUserError(
            'CATALOG.UNSUPPORTED_LOCALE',
            'The selected language is not supported for the country catalogue.',
          ),
      );
    }
    return this.graphql
      .query<{ countries: PageResponse<CountryCatalogueItem> }>(COUNTRIES_QUERY, {
        page: {
          page: query.page,
          size: query.size,
          sort: query.sort,
          direction: query.direction.toUpperCase(),
        },
        locale,
      })
      .pipe(map(({ countries }) => countries));
  }

  continents(): Observable<readonly string[]> {
    return this.graphql.query<{ continents: readonly string[] }>(CONTINENTS_QUERY, {})
      .pipe(map(({ continents }) => continents));
  }
}
