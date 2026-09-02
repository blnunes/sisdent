import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { DateTime } from 'luxon';
import {
  AppointmentDetail,
  AppointmentGraphqlService,
  AppointmentInput,
} from '../../core/appointment-graphql.service';
import { GraphQlUserError } from '../../core/graphql-client.service';

export type AppointmentCandidate = { id: string; name: string };
export interface AppointmentFormDialogData {
  organizationId: string;
  clinicUnitId: string;
  timezone: string;
  patients: readonly AppointmentCandidate[];
  practitioners: readonly AppointmentCandidate[];
  appointmentId?: string;
  detail?: AppointmentDetail;
  onMutationFinished: () => void;
  onSaved?: (detail: AppointmentDetail) => void;
  prefill?: { startLocal: string; endLocal: string };
}

@Component({
  selector: 'app-appointment-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatNativeDateModule,
    MatSelectModule,
    TranslatePipe,
  ],
  templateUrl: './appointment-form-dialog.component.html',
  styleUrl: './appointment-form-dialog.component.scss',
})
export class AppointmentFormDialogComponent {
  readonly data = inject<AppointmentFormDialogData>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<AppointmentFormDialogComponent>);
  private readonly api = inject(AppointmentGraphqlService);
  private readonly translate = inject(TranslateService);
  private readonly fb = inject(FormBuilder);
  readonly submitting = signal(false);
  readonly error = signal('');
  readonly availabilityLoaded = signal(false);
  readonly confirmDiscard = signal(false);
  readonly minimumDate = this.calendarDate(DateTime.now().setZone(this.data.timezone));
  readonly form = this.fb.group({
    patientId: this.fb.nonNullable.control('', Validators.required),
    practitionerId: this.fb.nonNullable.control('', Validators.required),
    startDate: this.fb.control<Date | null>(null, Validators.required),
    startTime: this.fb.nonNullable.control('', Validators.required),
    endDate: this.fb.control<Date | null>(null, Validators.required),
    endTime: this.fb.nonNullable.control('', Validators.required),
  });

  constructor() {
    this.ref.keydownEvents().subscribe((event) => {
      if (event.key !== 'Escape') return;
      event.preventDefault();
      if (this.confirmDiscard()) this.confirmDiscard.set(false);
      else this.close();
    });
    if (this.data.detail) {
      const start = DateTime.fromISO(this.data.detail.startAt, { zone: 'utc' }).setZone(
        this.data.timezone,
      );
      const end = DateTime.fromISO(this.data.detail.endAt, { zone: 'utc' }).setZone(
        this.data.timezone,
      );
      this.form.setValue({
        patientId: this.data.detail.patientId,
        practitionerId: this.data.detail.practitionerId,
        startDate: this.calendarDate(start),
        startTime: start.toFormat('HH:mm'),
        endDate: this.calendarDate(end),
        endTime: end.toFormat('HH:mm'),
      });
    } else if (this.data.prefill) {
      const start = this.localDateTime(this.data.prefill.startLocal);
      const end = this.localDateTime(this.data.prefill.endLocal);
      this.form.patchValue({
        startDate: start && this.calendarDate(start),
        startTime: start?.toFormat('HH:mm') ?? '',
        endDate: end && this.calendarDate(end),
        endTime: end?.toFormat('HH:mm') ?? '',
      });
    } else {
      this.form.patchValue({ startDate: this.minimumDate, endDate: this.minimumDate });
    }
    this.form.valueChanges.subscribe(() => this.refreshAvailability());
  }

  titleKey(): string {
    return this.data.appointmentId ? 'APPOINTMENTS.RESCHEDULE' : 'APPOINTMENTS.SCHEDULE';
  }
  saveKey(): string {
    return this.data.appointmentId ? 'APPOINTMENTS.SAVE_RESCHEDULE' : 'APPOINTMENTS.SCHEDULE';
  }
  close(): void {
    if (this.form.dirty && !this.submitting()) this.confirmDiscard.set(true);
    else this.ref.close();
  }
  discard(): void {
    this.ref.close();
  }

  save(): void {
    this.error.set('');
    const temporalError = this.temporalError();
    if (temporalError) {
      this.form.markAllAsTouched();
      this.error.set(this.translate.instant(temporalError));
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set(this.translate.instant('APPOINTMENTS.FORM.REQUIRED_ERROR'));
      return;
    }
    const input = this.input();
    if (!input) return;
    this.submitting.set(true);
    const request = this.data.appointmentId
      ? this.api.reschedule(this.data.organizationId, this.data.appointmentId, input)
      : this.api.create(this.data.organizationId, input);
    request.subscribe({
      next: (appointment) => {
        this.data.onMutationFinished();
        this.data.onSaved?.({
          patientId: appointment.patientId,
          practitionerId: appointment.practitionerId,
          patientName: appointment.patientName,
          practitionerName: appointment.practitionerName,
          startAt: appointment.startAt,
          endAt: appointment.endAt,
          status: appointment.status,
        });
        this.ref.close(true);
      },
      error: (error: unknown) => {
        this.data.onMutationFinished();
        const code = error instanceof GraphQlUserError ? error.code : '';
        this.error.set(this.translate.instant(this.mutationErrorKey(code)));
        this.submitting.set(false);
      },
    });
  }

  private mutationErrorKey(code: string): string {
    if (code === 'SCHEDULING.PRACTITIONER_UNAVAILABLE') {
      return 'APPOINTMENTS.ERROR.PRACTITIONER_UNAVAILABLE';
    }
    return this.data.appointmentId ? 'APPOINTMENTS.ERROR.RESCHEDULE' : 'APPOINTMENTS.ERROR.CREATE';
  }

  private input(): AppointmentInput | null {
    const start = this.localInstant(
      this.form.controls.startDate.value,
      this.form.controls.startTime.value,
    );
    const end = this.localInstant(
      this.form.controls.endDate.value,
      this.form.controls.endTime.value,
    );
    if (!start || !end) {
      this.error.set(this.translate.instant('APPOINTMENTS.FORM.INVALID_LOCAL_TIME'));
      return null;
    }
    if (end <= start) {
      this.error.set(this.translate.instant('APPOINTMENTS.FORM.END_AFTER_START'));
      return null;
    }
    return {
      clinicUnitId: this.data.clinicUnitId,
      patientId: this.form.controls.patientId.value,
      practitionerId: this.form.controls.practitionerId.value,
      startAt: start,
      endAt: end,
      schedulingTimezone: this.data.timezone,
    };
  }

  private refreshAvailability(): void {
    const practitionerId = this.form.controls.practitionerId.value;
    const start = this.localInstant(
      this.form.controls.startDate.value,
      this.form.controls.startTime.value,
    );
    const end = this.localInstant(
      this.form.controls.endDate.value,
      this.form.controls.endTime.value,
    );
    if (!practitionerId || !start || !end || end <= start || this.temporalError()) {
      this.availabilityLoaded.set(false);
      return;
    }
    this.api
      .availabilityIntervals(this.data.organizationId, this.data.clinicUnitId, start, end, [
        practitionerId,
      ])
      .subscribe({
        next: () => this.availabilityLoaded.set(true),
        error: () => this.availabilityLoaded.set(false),
      });
  }

  private localInstant(date: Date | null, time: string): string | null {
    const local = this.localDateTimeFor(date, time);
    if (!local) return null;
    // Luxon resolves repeated local times using its zone rules; the resulting instant is deterministic.
    return local.toUTC().toISO({ suppressMilliseconds: false });
  }

  private temporalError(): string | null {
    const dateControls = [this.form.controls.startDate, this.form.controls.endDate];
    if (dateControls.some((control) => control.hasError('matDatepickerParse')))
      return 'APPOINTMENTS.FORM.INVALID_DATE';
    if (
      dateControls.some(
        (control) =>
          control.hasError('matDatepickerMin') || this.isBeforeMinimumDate(control.value),
      )
    ) {
      return 'APPOINTMENTS.FORM.PAST_DATE';
    }
    const start = this.localDateTimeFor(
      this.form.controls.startDate.value,
      this.form.controls.startTime.value,
    );
    if (start && Date.parse(start.toUTC().toISO() ?? '') < Date.now()) {
      return 'APPOINTMENTS.FORM.PAST_DATE_TIME';
    }
    return null;
  }

  private isBeforeMinimumDate(value: Date | null): boolean {
    if (!value) return false;
    return (
      new Date(value.getFullYear(), value.getMonth(), value.getDate()).getTime() <
      this.minimumDate.getTime()
    );
  }

  private localDateTimeFor(date: Date | null, time: string): DateTime | null {
    if (!date || !/^\d{2}:\d{2}$/.test(time)) return null;
    const value = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}T${time}`;
    return this.localDateTime(value);
  }

  private localDateTime(value: string): DateTime | null {
    const local = DateTime.fromFormat(value, "yyyy-LL-dd'T'HH:mm", {
      zone: this.data.timezone,
      setZone: true,
    });
    return local.isValid && local.toFormat("yyyy-LL-dd'T'HH:mm") === value ? local : null;
  }

  private calendarDate(value: DateTime): Date {
    const [year, month, day] = value.toFormat('yyyy-LL-dd').split('-').map(Number);
    return new Date(year, month - 1, day);
  }
}
