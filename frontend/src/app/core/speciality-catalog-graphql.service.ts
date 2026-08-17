import { Injectable, inject } from '@angular/core';
import { Observable, map, throwError } from 'rxjs';
import { PageResponse } from './models';
import { GraphQlClientService, GraphQlUserError } from './graphql-client.service';
import { LanguageService } from './language.service';
import { supportsCatalogueLocale } from './country-catalog-graphql.service';

export interface SpecialityCatalogueItem extends Record<string, unknown> {
  id: string;
  name: string;
  displayName: string;
  status: 'ACTIVE' | 'INACTIVE';
  procedures: readonly { id: string; name: string; displayName: string }[];
}

export interface SpecialityCatalogueQuery {
  page: number;
  size: number;
  sort: string;
  direction: 'asc' | 'desc';
  filter?: { name?: string; procedure?: string };
}

const SPECIALITIES_QUERY = `query Specialities($page: CataloguePageInput, $filter: SpecialityFilterInput, $locale: String) {
  specialities(page: $page, filter: $filter, locale: $locale) {
    content { id name displayName status procedures { id name displayName } }
    page size totalElements totalPages
  }
}`;

@Injectable({ providedIn: 'root' })
export class SpecialityCatalogGraphqlService {
  private readonly graphql = inject(GraphQlClientService);
  private readonly language = inject(LanguageService);

  list(query: SpecialityCatalogueQuery, locale: string = this.language.current()): Observable<PageResponse<SpecialityCatalogueItem>> {
    if (!supportsCatalogueLocale(locale)) {
      return throwError(() => new GraphQlUserError(
        'CATALOG.UNSUPPORTED_LOCALE',
        'The selected language is not supported for the speciality catalogue.',
      ));
    }
    return this.graphql.query<{ specialities: PageResponse<SpecialityCatalogueItem> }>(SPECIALITIES_QUERY, {
      page: { page: query.page, size: query.size, sort: query.sort, direction: query.direction.toUpperCase() },
      filter: query.filter ?? {},
      locale,
    }).pipe(map(({ specialities }) => specialities));
  }
}
