import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { Membership, PageResponse } from '../../core/models';
import { AddressOption, AdministrativeDivisionOption, CountryOption, PatientRecord, SpecialityOption } from './patient.models';
import { FilterOption } from '../../shared/filters/filter.models';

@Injectable({ providedIn: 'root' })
export class PatientApiService {
  private readonly http = inject(HttpClient);
  endpoint(membership: Membership | null): string {
    if (!membership) return '';
    const base = `/api/organizations/${encodeURIComponent(membership.organizationId)}/patients`;
    return membership.clinicUnitId ? `${base}?clinicUnitId=${encodeURIComponent(membership.clinicUnitId)}` : base;
  }
  list(membership: Membership, params: HttpParams): Observable<PageResponse<PatientRecord>> { return this.http.get<PageResponse<PatientRecord>>(this.endpoint(membership), { params }); }
  filterOptions(membership: Membership, field: string, query = ''): Observable<FilterOption[]> {
    const normalizedQuery = query.trim().toLowerCase();
    if (field === 'specialityId') {
      return this.specialities().pipe(map((response) => response.content.filter(({ name }) => name.toLowerCase().includes(normalizedQuery)).slice(0, 10).map(({ id, name }) => ({ value: String(id), label: name }))));
    }
    if (field === 'addressId') {
      return this.http.get<PageResponse<AddressOption>>('/api/addresses', { params: { page: '0', size: '100', sort: 'street', direction: 'asc' } }).pipe(map((response) => response.content.filter((address) => [address.street, address.city, address.postalCode].some((value) => value?.toLowerCase().includes(normalizedQuery))).slice(0, 10).map((address) => ({ value: String(address.id), label: `${address.street} · ${address.postalCode ?? ''} · ${address.city}` }))));
    }
    if (field === 'taxId') {
      const params = new HttpParams().set('page', 0).set('size', 10).set('sort', 'name').set('direction', 'asc').set('taxId', query);
      return this.list(membership, params).pipe(map((response) => response.content.flatMap((patient) => patient['taxId'] ? [{ value: String(patient['taxId']), label: String(patient['taxId']) }] : [])));
    }
    return this.http.get<FilterOption[]>(`${this.endpoint(membership).split('?')[0]}/filter-options`, { params: { field, query } });
  }
  create(membership: Membership, body: unknown): Observable<unknown> { return this.http.post(this.endpoint(membership).split('?')[0], body); }
  update(membership: Membership, globalId: string, body: unknown): Observable<unknown> { return this.http.put(`${this.endpoint(membership).split('?')[0]}/${globalId}`, body); }
  deactivate(membership: Membership, globalId: string): Observable<unknown> { return this.http.delete(`${this.endpoint(membership).split('?')[0]}/${globalId}`); }
  specialities(): Observable<PageResponse<SpecialityOption>> { return this.http.get<PageResponse<SpecialityOption>>('/api/specialities', { params: { page: '0', size: '100', sort: 'name', direction: 'asc' } }); }
  countries(): Observable<PageResponse<CountryOption>> { return this.http.get<PageResponse<CountryOption>>('/api/countries', { params: { page: '0', size: '100', sort: 'name', direction: 'asc' } }); }
  administrativeDivisions(): Observable<PageResponse<AdministrativeDivisionOption>> { return this.http.get<PageResponse<AdministrativeDivisionOption>>('/api/administrative-divisions', { params: { page: '0', size: '100', sort: 'name', direction: 'asc' } }); }
  postalCodeSuggestions(countryCode: string, query: string): Observable<AddressOption[]> { return this.http.get<AddressOption[]>('/api/addresses/postal-code-suggestions', { params: { countryCode, query } }); }
}
