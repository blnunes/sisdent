import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { Membership, PageResponse } from '../../core/models';
import { GraphQlClientService } from '../../core/graphql-client.service';
import { SpecialityCatalogGraphqlService } from '../../core/speciality-catalog-graphql.service';
import { AdministrativeDivisionGraphqlService } from '../../core/administrative-division-graphql.service';
import { AddressGraphqlService } from '../../core/address-graphql.service';
import { AddressOption, AdministrativeDivisionOption, PatientRecord, SpecialityOption } from './patient.models';
import { FilterOption } from '../../shared/filters/filter.models';

@Injectable({ providedIn: 'root' })
export class PatientApiService {
  private readonly graphql = inject(GraphQlClientService);
  private readonly specialitiesGraphql = inject(SpecialityCatalogGraphqlService);
  private readonly divisionsGraphql = inject(AdministrativeDivisionGraphqlService);
  private readonly addressesGraphql = inject(AddressGraphqlService);
  list(membership: Membership, query: PatientListQuery): Observable<PageResponse<PatientRecord>> {
    return this.graphql.query<{ patients: PageResponse<PatientRecord> }>(
      `query Patients($organizationId: ID!, $clinicUnitId: ID, $page: CataloguePageInput, $filter: PatientFilterInput) {
        patients(organizationId: $organizationId, clinicUnitId: $clinicUnitId, page: $page, filter: $filter) {
          content { id globalId name birthDate active gender taxId identificationType identificationNumber documentIssuerCountry { code } nationality { code } address { id street district city additionalInfo block postalCode administrativeDivision { name code type } country { code } } specialities { id name displayName } }
          page size totalElements totalPages
        }
      }`,
      { organizationId: membership.organizationId, clinicUnitId: membership.clinicUnitId, page: query.page, filter: query.filter },
    ).pipe(map(({ patients }) => patients));
  }
  filterOptions(membership: Membership, field: string, query = ''): Observable<FilterOption[]> {
    const normalizedQuery = query.trim().toLowerCase();
    if (field === 'specialityId') {
      return this.specialities().pipe(map((response) => response.content.filter(({ name }) => name.toLowerCase().includes(normalizedQuery)).slice(0, 10).map(({ id, name }) => ({ value: String(id), label: name }))));
    }
    if (field === 'addressId') {
      return this.addressesGraphql.list(0, 100, 'street', 'asc').pipe(map((response) => response.content.filter((address) => [address.street, address.city, address.postalCode].some((value) => value?.toLowerCase().includes(normalizedQuery))).slice(0, 10).map((address) => ({ value: String(address.id), label: `${address.street} · ${address.postalCode ?? ''} · ${address.city}` }))));
    }
    if (field === 'taxId') {
      return this.list(membership, { page: patientNamePage(), filter: { taxId: query } }).pipe(
        map((response) => response.content.flatMap((patient) => patient['taxId'] ? [{ value: String(patient['taxId']), label: String(patient['taxId']) }] : [])),
      );
    }
    return this.graphql.query<{ patientFilterOptions: FilterOption[] }>(
      `query PatientFilterOptions($organizationId: ID!, $clinicUnitId: ID, $field: String!, $query: String) {
        patientFilterOptions(organizationId: $organizationId, clinicUnitId: $clinicUnitId, field: $field, query: $query) { value label }
      }`,
      { organizationId: membership.organizationId, clinicUnitId: membership.clinicUnitId, field, query },
    ).pipe(map(({ patientFilterOptions }) => patientFilterOptions));
  }
  create(membership: Membership, input: Record<string, unknown>): Observable<PatientRecord> {
    return this.graphql.query<{ createPatient: PatientRecord }>(
      `mutation CreatePatient($organizationId: ID!, $clinicUnitId: ID, $input: PatientMutationInput!) {
        createPatient(organizationId: $organizationId, clinicUnitId: $clinicUnitId, input: $input) { globalId name active }
      }`,
      { organizationId: membership.organizationId, clinicUnitId: membership.clinicUnitId, input },
    ).pipe(map(({ createPatient }) => createPatient));
  }
  deactivate(membership: Membership, patientId: string): Observable<boolean> {
    return this.graphql.query<{ deactivatePatient: boolean }>(
      `mutation DeactivatePatient($organizationId: ID!, $clinicUnitId: ID, $patientId: ID!) {
        deactivatePatient(organizationId: $organizationId, clinicUnitId: $clinicUnitId, patientId: $patientId)
      }`,
      { organizationId: membership.organizationId, clinicUnitId: membership.clinicUnitId, patientId },
    ).pipe(map(({ deactivatePatient }) => deactivatePatient));
  }
  specialities(): Observable<PageResponse<SpecialityOption>> {
    return this.specialitiesGraphql.list({ page: 0, size: 100, sort: 'name', direction: 'asc' }).pipe(
      map((response) => ({
        ...response,
        content: response.content.map(({ id, name, displayName }) => ({
          id: Number(id),
          name,
          displayName,
        })),
      })),
    );
  }
  administrativeDivisions(): Observable<PageResponse<AdministrativeDivisionOption>> {
    return this.divisionsGraphql.list(0, 100, 'name', 'asc').pipe(map((response) => ({
      ...response,
      content: response.content.map((division) => ({
        ...division,
        id: Number(division.id),
      })),
    })));
  }
  postalCodeSuggestions(countryCode: string, query: string): Observable<AddressOption[]> { return this.addressesGraphql.postalCodeSuggestions(countryCode, query).pipe(map((addresses) => addresses.map((address) => ({ ...address, id: Number(address.id) })))); }
}

export type PatientListQuery = {
  page: { page: number; size: number; sort: string; direction: 'ASC' | 'DESC' };
  filter: Record<string, unknown>;
};

function patientNamePage(): PatientListQuery['page'] {
  return { page: 0, size: 10, sort: 'name', direction: 'ASC' };
}
