import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AppointmentGraphqlService } from './appointment-graphql.service';

describe('AppointmentGraphqlService', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends a scoped appointment query with typed range and pagination variables', () => {
    const result = vi.fn();
    TestBed.inject(AppointmentGraphqlService)
      .list('organization-1', 'clinic-1', '2030-01-01T00:00:00Z', '2030-01-02T00:00:00Z', 1, 10)
      .subscribe(result);

    const request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain('query Appointments');
    expect(request.request.body.variables).toEqual({
      organizationId: 'organization-1', clinicUnitId: 'clinic-1', from: '2030-01-01T00:00:00Z',
      to: '2030-01-02T00:00:00Z', page: 1, size: 10,
    });
    request.flush({ data: { appointments: { content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 } } });
    expect(result).toHaveBeenCalledWith({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 });
  });

  it('uses the lifecycle mutation and maps a safe GraphQL authorization error', () => {
    const failure = vi.fn();
    TestBed.inject(AppointmentGraphqlService)
      .transition('organization-1', 'clinic-1', 'appointment-1', 'COMPLETED')
      .subscribe({ error: failure });

    const request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain('transitionAppointment');
    expect(request.request.body.variables).toEqual({
      organizationId: 'organization-1', clinicUnitId: 'clinic-1', appointmentId: 'appointment-1', status: 'COMPLETED',
    });
    request.flush({ errors: [{ message: 'Denied', extensions: { code: 'AUTHORIZATION.DENIED', correlationId: 'correlation-1' } }] });
    expect(failure).toHaveBeenCalledWith(expect.objectContaining({ code: 'AUTHORIZATION.DENIED', correlationId: 'correlation-1' }));
  });

  it('queries practitioner availability with scoped interval variables', () => {
    const result = vi.fn();
    TestBed.inject(AppointmentGraphqlService)
      .availability('organization-1', 'clinic-1', 'practitioner-1', '2030-01-01T09:00:00Z', '2030-01-01T10:00:00Z')
      .subscribe(result);

    const request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain('appointmentAvailability');
    expect(request.request.body.variables).toEqual({
      organizationId: 'organization-1', clinicUnitId: 'clinic-1', practitionerId: 'practitioner-1',
      startAt: '2030-01-01T09:00:00Z', endAt: '2030-01-01T10:00:00Z',
    });
    request.flush({ data: { appointmentAvailability: { available: true } } });
    expect(result).toHaveBeenCalledWith({ available: true });
  });
});
