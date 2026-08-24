import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Membership } from '../../core/models';
import { LanguageService } from '../../core/language.service';
import { PatientApiService } from './patient-api.service';

describe('PatientApiService', () => {
  let api: PatientApiService;
  let http: HttpTestingController;
  const membership: Membership = { id: 'member-1', organizationId: 'org / one', organizationName: 'Clinic', clinicUnitId: 'unit / a', role: 'MANAGER', version: 1 };

  beforeEach(() => { TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), { provide: LanguageService, useValue: { current: () => 'en' } }] }); api = TestBed.inject(PatientApiService); http = TestBed.inject(HttpTestingController); });
  afterEach(() => http.verify());

  it('sends scoped typed GraphQL variables for every patient filter', () => {
    const filter = { name: 'Ana', active: true, specialityId: '3' };
    api.list(membership, { page: { page: 0, size: 10, sort: 'name', direction: 'ASC' }, filter }).subscribe();
    const request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain('query Patients');
    expect(request.request.body.variables).toEqual({ organizationId: 'org / one', clinicUnitId: 'unit / a', page: { page: 0, size: 10, sort: 'name', direction: 'ASC' }, filter });
    request.flush({ data: { patients: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 } } });
  });

  it('creates and deactivates patients through typed GraphQL mutations', () => {
    api.create(membership, { name: 'Ana' }).subscribe();
    const creation = http.expectOne('/graphql');
    expect(creation.request.body.query).toContain('mutation CreatePatient');
    creation.flush({ data: { createPatient: { globalId: 'patient-global-id', name: 'Ana', active: true } } });
    api.deactivate(membership, 'patient-global-id').subscribe();
    const deactivation = http.expectOne('/graphql');
    expect(deactivation.request.body.variables.patientId).toBe('patient-global-id');
    deactivation.flush({ data: { deactivatePatient: true } });
  });

  it('maps speciality, address, and tax-id autocomplete sources to patient filter options', () => {
    api.filterOptions(membership, 'specialityId', 'pediatric').subscribe((options) => expect(options).toEqual([{ value: '2', label: 'Pediatric Dentistry' }]));
    const specialitiesRequest = http.expectOne('/graphql');
    expect(specialitiesRequest.request.body.variables).toEqual({
      page: { page: 0, size: 100, sort: 'name', direction: 'ASC' },
      filter: {},
      locale: 'en',
    });
    specialitiesRequest.flush({ data: { specialities: { content: [{ id: '1', name: 'Surgery', displayName: 'Surgery' }, { id: '2', name: 'Pediatric Dentistry', displayName: 'Pediatric Dentistry' }], page: 0, size: 100, totalElements: 2, totalPages: 1 } } });

    api.filterOptions(membership, 'addressId', 'maple').subscribe((options) => expect(options).toEqual([{ value: '7', label: 'Maple Grove · 1000 · Lisbon' }]));
    const addressesRequest = http.expectOne('/graphql');
    expect(addressesRequest.request.body.query).toContain('query Addresses');
    addressesRequest.flush({ data: { addresses: { content: [{ id: '7', street: 'Maple Grove', postalCode: '1000', city: 'Lisbon', country: { code: 'PT' } }, { id: '8', street: 'Oak Road', city: 'Porto', country: { code: 'PT' } }], page: 0, size: 100, totalElements: 2, totalPages: 1 } } });

    api.filterOptions(membership, 'taxId', '123').subscribe((options) => expect(options).toEqual([{ value: '123', label: '123' }]));
    const patientsRequest = http.expectOne('/graphql');
    expect(patientsRequest.request.body.variables.filter).toEqual({ taxId: '123' });
    patientsRequest.flush({ data: { patients: { content: [{ taxId: '123' }], page: 0, size: 10, totalElements: 1, totalPages: 1 } } });
  });
});
