import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { PageResponse } from './models';
import { GraphQlClientService } from './graphql-client.service';

export interface AddressGraphqlItem extends Record<string, unknown> {
  id: string;
  street: string;
  district?: string;
  city: string;
  additionalInfo?: string;
  block?: string;
  postalCode: string;
  administrativeDivision?: { name: string; code: string; type: string };
  country: { code: string; name: string };
}

export type AddressWrite = { street: string; district?: string | null; city: string; additionalInfo?: string | null; block?: string | null; postalCode: string; administrativeDivision?: { name: string; code: string; type: string } | null; countryCode: string };

const ADDRESS_FIELDS = 'id street district city additionalInfo block postalCode administrativeDivision { name code type } country { code name }';

@Injectable({ providedIn: 'root' })
export class AddressGraphqlService {
  private readonly graphql = inject(GraphQlClientService);

  list(page: number, size: number, sort: string, direction: 'asc' | 'desc'): Observable<PageResponse<AddressGraphqlItem>> {
    return this.graphql.query<{ addresses: PageResponse<AddressGraphqlItem> }>(
      `query Addresses($page: CataloguePageInput) { addresses(page: $page) { content { ${ADDRESS_FIELDS} } page size totalElements totalPages } }`,
      { page: { page, size, sort, direction: direction.toUpperCase() } },
    ).pipe(map(({ addresses }) => addresses));
  }

  save(record: AddressGraphqlItem | undefined, input: AddressWrite): Observable<AddressGraphqlItem> {
    const operation = record ? 'updateAddress' : 'createAddress';
    return this.graphql.query<Record<string, AddressGraphqlItem>>(
      `mutation SaveAddress($id: ID, $input: AddressMutationInput!) { ${operation}${record ? '(id: $id, input: $input)' : '(input: $input)'} { ${ADDRESS_FIELDS} } }`,
      { id: record?.id, input },
    ).pipe(map((response) => response[operation]));
  }

  delete(id: string): Observable<boolean> {
    return this.graphql.query<{ deleteAddress: boolean }>('mutation DeleteAddress($id: ID!) { deleteAddress(id: $id) }', { id })
      .pipe(map(({ deleteAddress }) => deleteAddress));
  }

  postalCodeSuggestions(countryCode: string, query: string): Observable<AddressGraphqlItem[]> {
    return this.graphql.query<{ addressPostalCodeSuggestions: AddressGraphqlItem[] }>(
      `query AddressPostalCodeSuggestions($countryCode: String!, $query: String!) { addressPostalCodeSuggestions(countryCode: $countryCode, query: $query) { ${ADDRESS_FIELDS} } }`,
      { countryCode, query },
    ).pipe(map(({ addressPostalCodeSuggestions }) => addressPostalCodeSuggestions));
  }
}
