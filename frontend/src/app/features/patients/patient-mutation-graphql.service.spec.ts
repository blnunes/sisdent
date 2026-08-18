import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PatientMutationGraphqlService } from './patient-mutation-graphql.service';

describe('PatientMutationGraphqlService', () => {
  let http: HttpTestingController;
  beforeEach(() => { TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] }); http = TestBed.inject(HttpTestingController); });
  afterEach(() => http.verify());

  it('sends the active organization and clinic scope with the exact typed patient update', () => {
    const result = vi.fn();
    const input = { name: 'Ada Patient', birthDate: '1990-01-01', active: true, gender: 'FEMALE', taxId: null, identificationType: 'PASSPORT', identificationNumber: 'A123', documentIssuerCountryCode: 'PT', nationalityCode: 'PT', addressId: 12, specialityIds: [3] };
    TestBed.inject(PatientMutationGraphqlService).update({ organizationId: 'org-1', clinicUnitId: 'clinic-2' } as never, 'patient-3', input).subscribe(result);
    const request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain('mutation UpdatePatient');
    expect(request.request.body.variables).toEqual({ organizationId: 'org-1', clinicUnitId: 'clinic-2', patientId: 'patient-3', input });
    request.flush({ data: { updatePatient: { globalId: 'patient-3', name: 'Ada Patient', active: true } } });
    expect(result).toHaveBeenCalledWith({ globalId: 'patient-3', name: 'Ada Patient', active: true });
  });
});
