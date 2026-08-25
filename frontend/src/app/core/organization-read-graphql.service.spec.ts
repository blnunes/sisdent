import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { OrganizationReadGraphqlService } from './organization-read-graphql.service';

describe('OrganizationReadGraphqlService', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('sends exact scoped clinic-unit variables and maps the result', () => {
    const result = vi.fn();
    TestBed.inject(OrganizationReadGraphqlService).listClinicUnits('organization-1', 'clinic-2').subscribe(result);
    const request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain('query ClinicUnits($organizationId: ID!, $clinicUnitId: ID)');
    expect(request.request.body.variables).toEqual({ organizationId: 'organization-1', clinicUnitId: 'clinic-2' });
    request.flush({ data: { clinicUnits: [{ id: 'clinic-2', organizationId: 'organization-1', name: 'Central', active: true, timezone: 'Europe/Lisbon' }] } });
    expect(result).toHaveBeenCalledWith([{ id: 'clinic-2', organizationId: 'organization-1', name: 'Central', active: true, timezone: 'Europe/Lisbon' }]);
  });

  it('sends the practitioner operation without internal fields and maps safe errors', () => {
    const failure = vi.fn();
    TestBed.inject(OrganizationReadGraphqlService).listPractitioners('organization-1').subscribe({ error: failure });
    const request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain('query Practitioners($organizationId: ID!, $clinicUnitId: ID)');
    expect(request.request.body.query).not.toContain('password');
    expect(request.request.body.variables).toEqual({ organizationId: 'organization-1', clinicUnitId: null });
    request.flush({ errors: [{ message: 'You are not allowed to access this resource.', extensions: { code: 'AUTHORIZATION.DENIED', correlationId: 'correlation-42' } }] });
    expect(failure).toHaveBeenCalledWith(expect.objectContaining({ code: 'AUTHORIZATION.DENIED', correlationId: 'correlation-42' }));
  });
});
