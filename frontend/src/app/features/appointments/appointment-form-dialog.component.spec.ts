import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { EMPTY, of, throwError } from 'rxjs';
import { AppointmentGraphqlService } from '../../core/appointment-graphql.service';
import { AppointmentFormDialogComponent, AppointmentFormDialogData } from './appointment-form-dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { GraphQlUserError } from '../../core/graphql-client.service';

describe('AppointmentFormDialogComponent', () => {
  const api = { create: vi.fn(), reschedule: vi.fn(), availabilityIntervals: vi.fn() };
  const ref = { close: vi.fn(), keydownEvents: () => EMPTY };
  const data: AppointmentFormDialogData = {
    organizationId: 'organization-1', clinicUnitId: 'clinic-1', timezone: 'Europe/Lisbon',
    patients: [{ id: 'patient-1', name: 'Patient' }], practitioners: [{ id: 'practitioner-1', name: 'Practitioner' }],
    onMutationFinished: vi.fn(),
  };

  beforeEach(() => {
    api.create.mockReturnValue(of({ patientId: 'patient-1', practitionerId: 'practitioner-1', patientName: 'Patient', practitionerName: 'Practitioner', startAt: '2026-03-29T01:30:00.000Z', endAt: '2026-03-29T02:00:00.000Z', status: 'SCHEDULED' }));
    api.reschedule.mockReturnValue(api.create());
    api.availabilityIntervals.mockReturnValue(of([]));
    TestBed.configureTestingModule({ imports: [AppointmentFormDialogComponent], providers: [
      { provide: AppointmentGraphqlService, useValue: api }, { provide: MatDialogRef, useValue: ref },
      { provide: MAT_DIALOG_DATA, useValue: data }, { provide: TranslateService, useValue: { instant: (key: string) => key } },
    ] });
  });

  it('creates with selected organization, clinic timezone, and UTC instants', () => {
    const component = TestBed.createComponent(AppointmentFormDialogComponent).componentInstance;
    component.form.setValue({ patientId: 'patient-1', practitionerId: 'practitioner-1', startLocal: '2026-10-25T01:30', endLocal: '2026-10-25T02:00' });
    component.save();
    expect(api.create).toHaveBeenCalledWith('organization-1', expect.objectContaining({ clinicUnitId: 'clinic-1', schedulingTimezone: 'Europe/Lisbon', startAt: expect.stringMatching(/Z$/), endAt: expect.stringMatching(/Z$/) }));
    expect(data.onMutationFinished).toHaveBeenCalled();
    expect(api.availabilityIntervals).toHaveBeenCalledWith('organization-1', 'clinic-1', expect.stringMatching(/Z$/), expect.stringMatching(/Z$/), ['practitioner-1']);
  });

  it('reschedules and safely reports conflicts while refreshing datasets', () => {
    TestBed.resetTestingModule();
    const rescheduleData = { ...data, appointmentId: 'appointment-1', detail: { patientId: 'patient-1', practitionerId: 'practitioner-1', patientName: 'Patient', practitionerName: 'Practitioner', startAt: '2026-03-29T00:30:00Z', endAt: '2026-03-29T01:00:00Z', status: 'SCHEDULED' } };
    api.reschedule.mockReturnValue(throwError(() => new GraphQlUserError('SCHEDULING.PRACTITIONER_UNAVAILABLE', 'secret')));
    TestBed.configureTestingModule({ imports: [AppointmentFormDialogComponent], providers: [
      { provide: AppointmentGraphqlService, useValue: api }, { provide: MatDialogRef, useValue: ref }, { provide: MAT_DIALOG_DATA, useValue: rescheduleData },
      { provide: TranslateService, useValue: { instant: (key: string) => key } },
    ] });
    const component = TestBed.createComponent(AppointmentFormDialogComponent).componentInstance;
    component.save();
    expect(api.reschedule).toHaveBeenCalledWith('organization-1', 'appointment-1', expect.objectContaining({ clinicUnitId: 'clinic-1' }));
    expect(component.error()).toBe('APPOINTMENTS.ERROR.PRACTITIONER_UNAVAILABLE');
    expect(rescheduleData.onMutationFinished).toHaveBeenCalled();
  });

  it('rejects required, non-increasing, malformed, and DST-gap local times', () => {
    const component = TestBed.createComponent(AppointmentFormDialogComponent).componentInstance;
    component.save();
    expect(component.error()).toBe('APPOINTMENTS.FORM.REQUIRED_ERROR');
    component.form.setValue({ patientId: 'patient-1', practitionerId: 'practitioner-1', startLocal: '2026-10-25T02:00', endLocal: '2026-10-25T01:30' });
    component.save();
    expect(component.error()).toBe('APPOINTMENTS.FORM.END_AFTER_START');
    component.form.patchValue({ startLocal: 'invalid', endLocal: '2026-03-29T03:30' }); component.save();
    expect(component.error()).toBe('APPOINTMENTS.FORM.INVALID_LOCAL_TIME');
    component.form.patchValue({ startLocal: '2026-03-29T01:30', endLocal: '2026-03-29T03:30' }); component.save();
    expect(component.error()).toBe('APPOINTMENTS.FORM.INVALID_LOCAL_TIME');
  });
});
