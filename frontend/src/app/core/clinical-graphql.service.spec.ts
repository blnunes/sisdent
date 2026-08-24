import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ClinicalGraphqlService } from './clinical-graphql.service';

describe('ClinicalGraphqlService', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('does not declare an unused encounter ID variable when it creates a clinical encounter', () => {
    TestBed.inject(ClinicalGraphqlService)
      .createEncounter('organization-1', {
        clinicUnitId: 'clinic-1', patientId: 'patient-1', careAt: '2030-01-01T09:00:00Z',
        careTimezone: 'Europe/Lisbon', narrative: 'Initial note',
      })
      .subscribe();

    const request = http.expectOne('/graphql');
    expect(request.request.body.query).not.toContain('$encounterId');
    expect(request.request.body.variables).toEqual({
      organizationId: 'organization-1',
      input: {
        clinicUnitId: 'clinic-1', patientId: 'patient-1', careAt: '2030-01-01T09:00:00Z',
        careTimezone: 'Europe/Lisbon', narrative: 'Initial note',
      },
    });
    request.flush({ data: { createClinicalEncounter: { globalId: 'encounter-1' } } });
  });
});
