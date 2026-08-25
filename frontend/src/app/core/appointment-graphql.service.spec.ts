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
      to: '2030-01-02T00:00:00Z', practitionerIds: null, page: 1, size: 10,
    });
    request.flush({ data: { appointments: { content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 } } });
    expect(result).toHaveBeenCalledWith({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 });
  });

  it('includes an explicit practitioner filter when one is selected', () => {
    TestBed.inject(AppointmentGraphqlService)
      .list('organization-1', 'clinic-1', '2030-01-01T00:00:00Z', '2030-01-02T00:00:00Z', 0, 100, ['practitioner-1'])
      .subscribe();

    const request = http.expectOne('/graphql');
    expect(request.request.body.variables.practitionerIds).toEqual(['practitioner-1']);
    request.flush({ data: { appointments: { content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 } } });
  });

  it('requests only scoped, read-only fields for appointment details', () => {
    const result = vi.fn();
    TestBed.inject(AppointmentGraphqlService)
      .detail('organization-1', 'clinic-1', '11111111-1111-4111-8111-111111111111')
      .subscribe(result);

    const request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain('query Appointment');
    expect(request.request.body.query).toContain('patientId');
    expect(request.request.body.query).not.toContain('globalId');
    expect(request.request.body.variables).toEqual({
      organizationId: 'organization-1', clinicUnitId: 'clinic-1', appointmentId: '11111111-1111-4111-8111-111111111111',
    });
    request.flush({ data: { appointment: { patientName: 'Patient', practitionerName: 'Practitioner', startAt: '2030-01-01T09:00:00Z', endAt: '2030-01-01T10:00:00Z', status: 'SCHEDULED' } } });
    expect(result).toHaveBeenCalledWith(expect.objectContaining({ practitionerName: 'Practitioner' }));
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

  it('queries availability intervals with scoped range and practitioner variables', () => {
    const result = vi.fn();
    TestBed.inject(AppointmentGraphqlService)
      .availabilityIntervals('organization-1', 'clinic-1', '2030-01-01T09:00:00Z', '2030-01-01T10:00:00Z', ['practitioner-1'])
      .subscribe(result);

    const request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain('appointmentAvailabilityIntervals');
    expect(request.request.body.variables).toEqual({
      organizationId: 'organization-1', clinicUnitId: 'clinic-1', from: '2030-01-01T09:00:00Z',
      to: '2030-01-01T10:00:00Z', practitionerIds: ['practitioner-1'],
    });
    request.flush({ data: { appointmentAvailabilityIntervals: [{ practitionerId: 'practitioner-1', startAt: '2030-01-01T09:00:00Z', endAt: '2030-01-01T10:00:00Z', availability: 'AVAILABLE', category: 'WORKING_HOURS' }] } });
    expect(result).toHaveBeenCalledWith([{ practitionerId: 'practitioner-1', startAt: '2030-01-01T09:00:00Z', endAt: '2030-01-01T10:00:00Z', availability: 'AVAILABLE', category: 'WORKING_HOURS' }]);
  });

  it('sends create and reschedule mutations with the supplied scoped timezone-safe input', () => {
    const service = TestBed.inject(AppointmentGraphqlService);
    const input = { clinicUnitId: 'clinic-1', patientId: 'patient-1', practitionerId: 'practitioner-1', startAt: '2026-03-29T01:30:00.000Z', endAt: '2026-03-29T02:00:00.000Z', schedulingTimezone: 'Europe/Lisbon' };
    service.create('organization-1', input).subscribe();
    let request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain('createAppointment');
    expect(request.request.body.variables).toEqual({ organizationId: 'organization-1', input });
    request.flush({ data: { createAppointment: {} } });
    service.reschedule('organization-1', 'appointment-1', input).subscribe();
    request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain('rescheduleAppointment');
    expect(request.request.body.variables).toEqual({ organizationId: 'organization-1', appointmentId: 'appointment-1', input });
    request.flush({ data: { rescheduleAppointment: {} } });
  });

  it('uses POST GraphQL only for blocked-period optimistic-lock mutations', () => {
    const service = TestBed.inject(AppointmentGraphqlService);
    service.updateBlockedPeriod('organization-1', 'block-1', 4, { clinicUnitId: 'clinic-1', practitionerId: null, startAt: '2026-03-29T09:00:00Z', endAt: '2026-03-29T10:00:00Z' }).subscribe();
    const request = http.expectOne('/graphql');
    expect(request.request.method).toBe('POST');
    expect(request.request.body.query).toContain('updateAppointmentBlockedPeriod');
    expect(request.request.body.variables).toMatchObject({ organizationId: 'organization-1', blockedPeriodId: 'block-1', version: 4 });
    request.flush({ data: { updateAppointmentBlockedPeriod: {} } });
  });

  it('uses appointment-scoped eligible options and procedure mutations without raw error fields', () => {
    const service = TestBed.inject(AppointmentGraphqlService);
    service.eligiblePerformedProcedureOptions('organization-1', 'clinic-1', 'appointment-1').subscribe();
    let request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain('eligiblePerformedProcedureOptions');
    expect(request.request.body.variables).toEqual({ organizationId: 'organization-1', clinicUnitId: 'clinic-1', appointmentId: 'appointment-1' });
    request.flush({ data: { eligiblePerformedProcedureOptions: [{ id: '7', displayName: 'Cleaning' }] } });
    service.voidPerformedProcedure('organization-1', 'clinic-1', 'performed-1', 'Duplicate').subscribe();
    request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain('voidPerformedProcedure');
    expect(request.request.body.variables).toEqual({ organizationId: 'organization-1', clinicUnitId: 'clinic-1', performedProcedureId: 'performed-1', input: { reason: 'Duplicate' } });
    request.flush({ data: { voidPerformedProcedure: {} } });
  });
});
