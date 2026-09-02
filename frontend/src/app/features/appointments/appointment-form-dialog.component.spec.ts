import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { EMPTY, of, throwError } from 'rxjs';
import { AppointmentGraphqlService } from '../../core/appointment-graphql.service';
import {
  AppointmentFormDialogComponent,
  AppointmentFormDialogData,
} from './appointment-form-dialog.component';
import { provideNativeDateAdapter } from '@angular/material/core';
import { provideTranslateService } from '@ngx-translate/core';
import { GraphQlUserError } from '../../core/graphql-client.service';

describe('AppointmentFormDialogComponent', () => {
  const api = { create: vi.fn(), reschedule: vi.fn(), availabilityIntervals: vi.fn() };
  const ref = { close: vi.fn(), keydownEvents: () => EMPTY };
  const data: AppointmentFormDialogData = {
    organizationId: 'organization-1',
    clinicUnitId: 'clinic-1',
    timezone: 'Europe/Lisbon',
    patients: [{ id: 'patient-1', name: 'Patient' }],
    practitioners: [{ id: 'practitioner-1', name: 'Practitioner' }],
    onMutationFinished: vi.fn(),
  };

  beforeEach(() => {
    Object.values(api).forEach((mock) => mock.mockReset());
    api.create.mockReturnValue(
      of({
        patientId: 'patient-1',
        practitionerId: 'practitioner-1',
        patientName: 'Patient',
        practitionerName: 'Practitioner',
        startAt: '2026-03-29T01:30:00.000Z',
        endAt: '2026-03-29T02:00:00.000Z',
        status: 'SCHEDULED',
      }),
    );
    api.reschedule.mockReturnValue(api.create());
    api.availabilityIntervals.mockReturnValue(of([]));
    TestBed.configureTestingModule({
      imports: [AppointmentFormDialogComponent],
      providers: [
        { provide: AppointmentGraphqlService, useValue: api },
        { provide: MatDialogRef, useValue: ref },
        { provide: MAT_DIALOG_DATA, useValue: data },
        provideNativeDateAdapter(),
        provideTranslateService(),
      ],
    });
  });

  afterEach(() => vi.useRealTimers());

  it('creates with selected organization, clinic timezone, and UTC instants', () => {
    const component = TestBed.createComponent(AppointmentFormDialogComponent).componentInstance;
    component.form.setValue({
      patientId: 'patient-1',
      practitionerId: 'practitioner-1',
      startDate: new Date(2026, 9, 25),
      startTime: '01:30',
      endDate: new Date(2026, 9, 25),
      endTime: '02:00',
    });
    component.save();
    expect(api.create).toHaveBeenCalledWith(
      'organization-1',
      expect.objectContaining({
        clinicUnitId: 'clinic-1',
        schedulingTimezone: 'Europe/Lisbon',
        startAt: expect.stringMatching(/Z$/),
        endAt: expect.stringMatching(/Z$/),
      }),
    );
    expect(data.onMutationFinished).toHaveBeenCalled();
    expect(api.availabilityIntervals).toHaveBeenCalledWith(
      'organization-1',
      'clinic-1',
      expect.stringMatching(/Z$/),
      expect.stringMatching(/Z$/),
      ['practitioner-1'],
    );
  });

  it('reschedules and safely reports conflicts while refreshing datasets', () => {
    TestBed.resetTestingModule();
    const rescheduleData = {
      ...data,
      appointmentId: 'appointment-1',
      detail: {
        patientId: 'patient-1',
        practitionerId: 'practitioner-1',
        patientName: 'Patient',
        practitionerName: 'Practitioner',
        startAt: '2026-10-29T00:30:00Z',
        endAt: '2026-10-29T01:00:00Z',
        status: 'SCHEDULED',
      },
    };
    api.reschedule.mockReturnValue(
      throwError(() => new GraphQlUserError('SCHEDULING.PRACTITIONER_UNAVAILABLE', 'secret')),
    );
    TestBed.configureTestingModule({
      imports: [AppointmentFormDialogComponent],
      providers: [
        { provide: AppointmentGraphqlService, useValue: api },
        { provide: MatDialogRef, useValue: ref },
        { provide: MAT_DIALOG_DATA, useValue: rescheduleData },
        provideNativeDateAdapter(),
        provideTranslateService(),
      ],
    });
    const component = TestBed.createComponent(AppointmentFormDialogComponent).componentInstance;
    component.save();
    expect(api.reschedule).toHaveBeenCalledWith(
      'organization-1',
      'appointment-1',
      expect.objectContaining({ clinicUnitId: 'clinic-1' }),
    );
    expect(component.error()).toBe('APPOINTMENTS.ERROR.PRACTITIONER_UNAVAILABLE');
    expect(rescheduleData.onMutationFinished).toHaveBeenCalled();
  });

  it('uses distinct named mutation errors for create and reschedule failures', () => {
    const component = TestBed.createComponent(AppointmentFormDialogComponent).componentInstance;
    expect(
      (component as unknown as { mutationErrorKey: (code: string) => string }).mutationErrorKey(
        'OTHER',
      ),
    ).toBe('APPOINTMENTS.ERROR.CREATE');
    expect(
      (component as unknown as { mutationErrorKey: (code: string) => string }).mutationErrorKey(
        'SCHEDULING.PRACTITIONER_UNAVAILABLE',
      ),
    ).toBe('APPOINTMENTS.ERROR.PRACTITIONER_UNAVAILABLE');
  });

  it('rejects required, non-increasing, malformed, and DST-gap local times', () => {
    const component = TestBed.createComponent(AppointmentFormDialogComponent).componentInstance;
    component.save();
    expect(component.error()).toBe('APPOINTMENTS.FORM.REQUIRED_ERROR');
    component.form.setValue({
      patientId: 'patient-1',
      practitionerId: 'practitioner-1',
      startDate: new Date(2026, 9, 25),
      startTime: '02:00',
      endDate: new Date(2026, 9, 25),
      endTime: '01:30',
    });
    component.save();
    expect(component.error()).toBe('APPOINTMENTS.FORM.END_AFTER_START');
    component.form.patchValue({
      startDate: new Date(2027, 2, 28),
      startTime: 'invalid',
      endDate: new Date(2027, 2, 28),
      endTime: '03:30',
    });
    component.save();
    expect(component.error()).toBe('APPOINTMENTS.FORM.INVALID_LOCAL_TIME');
    component.form.patchValue({
      startDate: new Date(2027, 2, 28),
      startTime: '01:30',
      endDate: new Date(2027, 2, 28),
      endTime: '03:30',
    });
    component.save();
    expect(component.error()).toBe('APPOINTMENTS.FORM.INVALID_LOCAL_TIME');
  });

  it('rejects typed invalid dates, calendar dates before today, and past times today in the clinic timezone', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-08-25T10:00:00Z'));
    const component = TestBed.createComponent(AppointmentFormDialogComponent).componentInstance;

    component.form.setValue({
      patientId: 'patient-1',
      practitionerId: 'practitioner-1',
      startDate: new Date(2026, 7, 25),
      startTime: '10:30',
      endDate: new Date(2026, 7, 25),
      endTime: '11:30',
    });
    component.save();
    expect(component.error()).toBe('APPOINTMENTS.FORM.PAST_DATE_TIME');
    expect(api.create).not.toHaveBeenCalledWith('organization-1', expect.anything());

    component.form.patchValue({
      startDate: new Date(2026, 7, 24),
      startTime: '12:00',
      endDate: new Date(2026, 7, 24),
      endTime: '13:00',
    });
    component.save();
    expect(component.error()).toBe('APPOINTMENTS.FORM.PAST_DATE');

    component.form.controls.startDate.setErrors({ matDatepickerParse: true });
    component.save();
    expect(component.error()).toBe('APPOINTMENTS.FORM.INVALID_DATE');
  });

  it('defaults new appointments to today and renders editable Material date pickers bounded to today', () => {
    const fixture = TestBed.createComponent(AppointmentFormDialogComponent);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.form.controls.startDate.value).toEqual(component.minimumDate);
    expect(component.form.controls.endDate.value).toEqual(component.minimumDate);
    const element = fixture.nativeElement as HTMLElement;
    const dateInputs = element.querySelectorAll<HTMLInputElement>('input[aria-haspopup="dialog"]');
    expect(dateInputs).toHaveLength(2);
    expect(dateInputs[0].readOnly).toBe(false);
    for (const inputId of [
      'appointment-start-date',
      'appointment-start-time',
      'appointment-end-date',
      'appointment-end-time',
    ]) {
      expect(element.querySelector(`input#${inputId}`)).not.toBeNull();
      expect(element.querySelector(`label[for="${inputId}"]`)).not.toBeNull();
    }
    const yesterday = new Date(component.minimumDate);
    yesterday.setDate(yesterday.getDate() - 1);
    component.form.controls.startDate.setValue(yesterday);
    fixture.detectChanges();
    expect(component.form.controls.startDate.hasError('matDatepickerMin')).toBe(true);
  });
});
