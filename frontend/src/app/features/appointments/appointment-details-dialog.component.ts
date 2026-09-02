import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DateTime } from 'luxon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  AppointmentDetail,
  AppointmentGraphqlService,
  AppointmentStatus,
  PerformedProcedure,
  PerformedProcedureOption,
} from '../../core/appointment-graphql.service';
import { AppointmentConfirmationDialogComponent } from './appointment-confirmation-dialog.component';

export interface AppointmentDetailsDialogData {
  organizationId: string;
  clinicUnitId: string;
  appointmentId: string;
  clinicName: string;
  timezone: string;
  canManageAppointments: boolean;
  onUnavailable: () => void;
  onReschedule: (detail: AppointmentDetail, onSaved: (updated: AppointmentDetail) => void) => void;
  onLifecycleFinished: () => void;
}

@Component({
  selector: 'app-appointment-details-dialog',
  imports: [
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatFormFieldModule,
    MatMenuModule,
    MatSelectModule,
    TranslatePipe,
  ],
  templateUrl: './appointment-details-dialog.component.html',
  styleUrl: './appointment-details-dialog.component.scss',
})
export class AppointmentDetailsDialogComponent {
  readonly data = inject<AppointmentDetailsDialogData>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<AppointmentDetailsDialogComponent>);
  private readonly dialog = inject(MatDialog);
  private readonly appointments = inject(AppointmentGraphqlService);
  private readonly translate = inject(TranslateService);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly detail = signal<AppointmentDetail | null>(null);
  readonly transitioning = signal(false);
  readonly lifecycleError = signal(false);
  readonly procedures = signal<PerformedProcedure[]>([]);
  readonly procedureOptions = signal<PerformedProcedureOption[]>([]);
  readonly procedureError = signal(false);
  selectedProcedureId = '';

  constructor() {
    this.ref.keydownEvents().subscribe((event) => {
      if (event.key !== 'Escape') return;
      event.preventDefault();
      this.close();
    });
    this.appointments
      .detail(this.data.organizationId, this.data.clinicUnitId, this.data.appointmentId)
      .subscribe({
        next: (appointment) => {
          this.detail.set(appointment);
          this.loading.set(false);
          this.loadProcedures();
        },
        error: () => {
          this.error.set(true);
          this.loading.set(false);
          this.data.onUnavailable();
        },
      });
  }

  appointmentTime(): string {
    const appointment = this.detail();
    if (!appointment) return '';
    const formatter = new Intl.DateTimeFormat(this.translate.getCurrentLang() ?? 'en', {
      dateStyle: 'medium',
      timeStyle: 'short',
      timeZone: this.data.timezone,
    });
    return formatter.formatRange(new Date(appointment.startAt), new Date(appointment.endAt));
  }

  close(): void {
    this.ref.close();
  }

  reschedule(): void {
    const detail = this.detail();
    if (detail) this.data.onReschedule(detail, (updated) => this.detail.set(updated));
  }
  loadProcedures(): void {
    this.appointments
      .performedProcedures(
        this.data.organizationId,
        this.data.clinicUnitId,
        this.data.appointmentId,
      )
      .subscribe({
        next: (value) => this.procedures.set(value),
        error: () => this.procedureError.set(true),
      });
  }
  loadProcedureOptions(): void {
    if (this.procedureOptions().length) {
      return;
    }
    this.appointments
      .eligiblePerformedProcedureOptions(
        this.data.organizationId,
        this.data.clinicUnitId,
        this.data.appointmentId,
      )
      .subscribe({
        next: (value) => this.procedureOptions.set(value),
        error: () => this.procedureError.set(true),
      });
  }
  addProcedure(): void {
    if (!this.selectedProcedureId) {
      this.procedureError.set(true);
      return;
    }
    const performedAt = DateTime.now().setZone(this.data.timezone).toUTC().toISO();
    if (!performedAt) return;
    this.procedureError.set(false);
    this.appointments
      .createPerformedProcedure(
        this.data.organizationId,
        this.data.clinicUnitId,
        this.data.appointmentId,
        this.selectedProcedureId,
        performedAt,
      )
      .subscribe({
        next: () => {
          this.selectedProcedureId = '';
          this.loadProcedures();
          this.data.onLifecycleFinished();
        },
        error: () => {
          this.procedureError.set(true);
          this.data.onLifecycleFinished();
        },
      });
  }
  requestVoid(procedure: PerformedProcedure): void {
    this.dialog
      .open(ProcedureVoidDialog)
      .afterClosed()
      .subscribe((reason) => {
        if (!reason) {
          return;
        }
        this.appointments
          .voidPerformedProcedure(
            this.data.organizationId,
            this.data.clinicUnitId,
            procedure.globalId,
            reason,
          )
          .subscribe({
            next: () => {
              this.loadProcedures();
              this.data.onLifecycleFinished();
            },
            error: () => {
              this.procedureError.set(true);
              this.data.onLifecycleFinished();
            },
          });
      });
  }

  requestTransition(status: AppointmentStatus): void {
    if (this.transitioning()) return;
    this.dialog
      .open(AppointmentConfirmationDialogComponent, {
        data: {
          titleKey: 'APPOINTMENTS.CONFIRM.TITLE',
          messageKey: 'APPOINTMENTS.CONFIRM.TEXT',
          cancelKey: 'APPOINTMENTS.CONFIRM.KEEP',
          confirmKey: 'APPOINTMENTS.CONFIRM.PROCEED',
        },
        autoFocus: 'dialog',
        width: 'min(400px, calc(100vw - 32px))',
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) this.transition(status);
      });
  }

  private transition(status: AppointmentStatus): void {
    this.lifecycleError.set(false);
    this.transitioning.set(true);
    this.appointments
      .transition(this.data.organizationId, this.data.clinicUnitId, this.data.appointmentId, status)
      .subscribe({
        next: (appointment) => {
          const current = this.detail();
          if (current) this.detail.set({ ...current, status: appointment.status });
          this.transitioning.set(false);
          this.data.onLifecycleFinished();
        },
        error: () => {
          this.lifecycleError.set(true);
          this.transitioning.set(false);
          this.data.onLifecycleFinished();
        },
      });
  }
}

@Component({
  imports: [
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    TranslatePipe,
  ],
  template: `<h2 mat-dialog-title>{{ 'APPOINTMENTS.PROCEDURES.VOID_TITLE' | translate }}</h2>
    <mat-dialog-content
      ><mat-form-field
        ><mat-label>{{ 'APPOINTMENTS.PROCEDURES.REASON' | translate }}</mat-label
        ><input
          matInput
          [(ngModel)]="reason"
          maxlength="500"
          required /></mat-form-field></mat-dialog-content
    ><mat-dialog-actions align="end"
      ><button mat-button (click)="ref.close()">
        {{ 'APPOINTMENTS.CONFIRM.KEEP' | translate }}</button
      ><button mat-flat-button [disabled]="!reason.trim()" (click)="ref.close(reason.trim())">
        {{ 'APPOINTMENTS.PROCEDURES.VOID' | translate }}
      </button></mat-dialog-actions
    >`,
})
export class ProcedureVoidDialog {
  readonly ref = inject(MatDialogRef<ProcedureVoidDialog, string>);
  reason = '';
}
