import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { DateTime } from 'luxon';
import { AppointmentDetail, AppointmentGraphqlService, AppointmentInput } from '../../core/appointment-graphql.service';
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
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, TranslatePipe],
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
  readonly form = this.fb.nonNullable.group({
    patientId: ['', Validators.required],
    practitionerId: ['', Validators.required],
    startLocal: ['', Validators.required],
    endLocal: ['', Validators.required],
  });

  constructor() {
    this.ref.keydownEvents().subscribe((event) => {
      if (event.key !== 'Escape') return;
      event.preventDefault();
      if (this.confirmDiscard()) this.confirmDiscard.set(false); else this.close();
    });
    if (this.data.detail) {
      const start = DateTime.fromISO(this.data.detail.startAt, { zone: 'utc' }).setZone(this.data.timezone);
      const end = DateTime.fromISO(this.data.detail.endAt, { zone: 'utc' }).setZone(this.data.timezone);
      this.form.setValue({
        patientId: this.data.detail.patientId,
        practitionerId: this.data.detail.practitionerId,
        startLocal: start.toFormat("yyyy-LL-dd'T'HH:mm"),
        endLocal: end.toFormat("yyyy-LL-dd'T'HH:mm"),
      });
    } else if (this.data.prefill) {
      this.form.patchValue(this.data.prefill);
    }
    this.form.valueChanges.subscribe(() => this.refreshAvailability());
  }

  titleKey(): string { return this.data.appointmentId ? 'APPOINTMENTS.RESCHEDULE' : 'APPOINTMENTS.SCHEDULE'; }
  saveKey(): string { return this.data.appointmentId ? 'APPOINTMENTS.SAVE_RESCHEDULE' : 'APPOINTMENTS.SCHEDULE'; }
  close(): void {
    if (this.form.dirty && !this.submitting()) this.confirmDiscard.set(true);
    else this.ref.close();
  }
  discard(): void { this.ref.close(); }

  save(): void {
    this.error.set('');
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
        this.error.set(this.translate.instant(code === 'SCHEDULING.PRACTITIONER_UNAVAILABLE'
          ? 'APPOINTMENTS.ERROR.PRACTITIONER_UNAVAILABLE'
          : this.data.appointmentId ? 'APPOINTMENTS.ERROR.RESCHEDULE' : 'APPOINTMENTS.ERROR.CREATE'));
        this.submitting.set(false);
      },
    });
  }

  private input(): AppointmentInput | null {
    const start = this.localInstant(this.form.controls.startLocal.value);
    const end = this.localInstant(this.form.controls.endLocal.value);
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
    const start = this.localInstant(this.form.controls.startLocal.value);
    const end = this.localInstant(this.form.controls.endLocal.value);
    if (!practitionerId || !start || !end || end <= start) {
      this.availabilityLoaded.set(false);
      return;
    }
    this.api.availabilityIntervals(this.data.organizationId, this.data.clinicUnitId, start, end, [practitionerId])
      .subscribe({ next: () => this.availabilityLoaded.set(true), error: () => this.availabilityLoaded.set(false) });
  }

  private localInstant(value: string): string | null {
    const local = DateTime.fromFormat(value, "yyyy-LL-dd'T'HH:mm", { zone: this.data.timezone, setZone: true });
    if (!local.isValid || local.toFormat("yyyy-LL-dd'T'HH:mm") !== value) return null;
    // Luxon resolves repeated local times using its zone rules; the resulting instant is deterministic.
    return local.toUTC().toISO({ suppressMilliseconds: false });
  }
}
