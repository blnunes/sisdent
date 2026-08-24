import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { PageResponse } from './models';
import { GraphQlClientService } from './graphql-client.service';

export interface AdministrativeDivisionItem extends Record<string, unknown> {
  id: string;
  name: string;
  code: string;
  type: string;
  country: { code: string; name: string };
}

export type AdministrativeDivisionWrite = { name: string; code: string; type: string; countryCode: string };

@Injectable({ providedIn: 'root' })
export class AdministrativeDivisionGraphqlService {
  private readonly graphql = inject(GraphQlClientService);

  list(page: number, size: number, sort: string, direction: 'asc' | 'desc'): Observable<PageResponse<AdministrativeDivisionItem>> {
    return this.graphql.query<{ administrativeDivisions: PageResponse<AdministrativeDivisionItem> }>(
      'query AdministrativeDivisions($page: CataloguePageInput) { administrativeDivisions(page: $page) { content { id name code type country { code name } } page size totalElements totalPages } }',
      { page: { page, size, sort, direction: direction.toUpperCase() } },
    ).pipe(map(({ administrativeDivisions }) => administrativeDivisions));
  }

  save(record: AdministrativeDivisionItem | undefined, input: AdministrativeDivisionWrite): Observable<AdministrativeDivisionItem> {
    const operation = record ? 'updateAdministrativeDivision' : 'createAdministrativeDivision';
    return this.graphql.query<Record<string, AdministrativeDivisionItem>>(
      `mutation SaveAdministrativeDivision($id: ID, $input: AdministrativeDivisionMutationInput!) { ${operation}${record ? '(id: $id, input: $input)' : '(input: $input)'} { id name code type country { code name } } }`,
      { id: record?.id, input },
    ).pipe(map((response) => response[operation]));
  }

  delete(id: string): Observable<boolean> {
    return this.graphql.query<{ deleteAdministrativeDivision: boolean }>(
      'mutation DeleteAdministrativeDivision($id: ID!) { deleteAdministrativeDivision(id: $id) }', { id },
    ).pipe(map(({ deleteAdministrativeDivision }) => deleteAdministrativeDivision));
  }
}
