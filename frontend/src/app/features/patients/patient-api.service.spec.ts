import { HttpParams, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Membership } from '../../core/models';
import { PatientApiService } from './patient-api.service';

describe('PatientApiService', () => {
  let api: PatientApiService;
  let http: HttpTestingController;
  const membership: Membership = { id: 'member-1', organizationId: 'org / one', organizationName: 'Clinic', clinicUnitId: 'unit / a', role: 'MANAGER', version: 1 };

  beforeEach(() => { TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] }); api = TestBed.inject(PatientApiService); http = TestBed.inject(HttpTestingController); });
  afterEach(() => http.verify());

  it('builds tenant endpoints with an optional encoded clinic unit', () => {
    expect(api.endpoint(membership)).toBe('/api/organizations/org%20%2F%20one/patients?clinicUnitId=unit%20%2F%20a');
    expect(api.endpoint({ ...membership, clinicUnitId: undefined })).toBe('/api/organizations/org%20%2F%20one/patients');
    expect(api.endpoint(null)).toBe('');
  });

  it('sends every patient filter in the list request', () => {
    const filters = ['name', 'birthDate', 'active', 'gender', 'taxId', 'identificationType', 'nationalityCode', 'addressId', 'specialityId'];
    let params = new HttpParams().set('page', 0).set('size', 10).set('sort', 'name').set('direction', 'asc');
    filters.forEach((key) => { params = params.set(key, `${key}-value`); });
    api.list(membership, params).subscribe();
    const request = http.expectOne((candidate) => candidate.url.startsWith('/api/organizations/org%20%2F%20one/patients'));
    expect(request.request.urlWithParams).toContain('clinicUnitId=unit%20%2F%20a');
    filters.forEach((key) => expect(request.request.params.get(key)).toBe(`${key}-value`));
    request.flush({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
  });

  it('uses globalId for update and deactivate while create targets the collection', () => {
    api.create(membership, { name: 'Ana' }).subscribe();
    expect(http.expectOne((request) => request.url.endsWith('/patients') && request.method === 'POST').request.body).toEqual({ name: 'Ana' });
    api.update(membership, 'patient-global-id', { name: 'Ana Maria' }).subscribe();
    expect(http.expectOne((request) => request.url.endsWith('/patients/patient-global-id') && request.method === 'PUT').request.body).toEqual({ name: 'Ana Maria' });
    api.deactivate(membership, 'patient-global-id').subscribe();
    expect(http.expectOne((request) => request.url.endsWith('/patients/patient-global-id') && request.method === 'DELETE')).toBeTruthy();
  });

  it('maps speciality, address, and tax-id autocomplete sources to patient filter options', () => {
    api.filterOptions(membership, 'specialityId', 'pediatric').subscribe((options) => expect(options).toEqual([{ value: '2', label: 'Pediatric Dentistry' }]));
    http.expectOne((request) => request.url === '/api/specialities').flush({ content: [{ id: 1, name: 'Surgery' }, { id: 2, name: 'Pediatric Dentistry' }], page: 0, size: 100, totalElements: 2, totalPages: 1 });

    api.filterOptions(membership, 'addressId', 'maple').subscribe((options) => expect(options).toEqual([{ value: '7', label: 'Maple Grove · 1000 · Lisbon' }]));
    http.expectOne((request) => request.url === '/api/addresses').flush({ content: [{ id: 7, street: 'Maple Grove', postalCode: '1000', city: 'Lisbon', country: { code: 'PT' } }, { id: 8, street: 'Oak Road', city: 'Porto', country: { code: 'PT' } }], page: 0, size: 100, totalElements: 2, totalPages: 1 });

    api.filterOptions(membership, 'taxId', '123').subscribe((options) => expect(options).toEqual([{ value: '123', label: '123' }]));
    http.expectOne((request) => request.url.startsWith('/api/organizations/') && request.params.get('taxId') === '123').flush({ content: [{ taxId: '123' }], page: 0, size: 10, totalElements: 1, totalPages: 1 });
  });
});
