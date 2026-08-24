import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { distinctUntilChanged } from 'rxjs';
import { Appointment, Practitioner } from '../../core/models';
import { AppointmentGraphqlService } from '../../core/appointment-graphql.service';
import { AuthService } from '../../core/auth.service';
import { OrganizationReadGraphqlService } from '../../core/organization-read-graphql.service';
import { PatientApiService } from '../patients/patient-api.service';
import { AppHeaderComponent } from '../../core/layout/app-header/app-header.component';
import { ModuleNavigationComponent } from '../../core/layout/module-navigation/module-navigation.component';

type ClinicUnit = { id: string; organizationId: string; name: string; active: boolean };

@Component({
  selector: 'app-appointments',
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatCardModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSidenavModule,
    MatTooltipModule,
    TranslatePipe,
    AppHeaderComponent,
    ModuleNavigationComponent,
  ],
  templateUrl: './appointments.component.html',
  styleUrl: './appointments.component.scss',
})
export class AppointmentsComponent {
  readonly auth = inject(AuthService);
  private readonly appointmentApi = inject(AppointmentGraphqlService);
  private readonly organizationReads = inject(OrganizationReadGraphqlService);
  private readonly patientApi = inject(PatientApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly translate = inject(TranslateService);

  readonly membership = this.auth.activeMembership;
  readonly appointments = signal<Appointment[]>([]);
  readonly practitioners = signal<Practitioner[]>([]);
  readonly patients = signal<{ globalId: string; name: string }[]>([]);
  readonly clinicUnits = signal<ClinicUnit[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly page = signal(0);
  readonly totalPages = signal(0);

  clinicUnitId = this.membership()?.clinicUnitId ?? '';
  clinicSearch = '';
  patientId = '';
  practitionerId = '';
  appointmentDate: Date | null = null;
  startTime = '';
  endTime = '';
  editingAppointment: Appointment | null = null;
  readonly minimumDate = new Date();
  readonly timeSlots = Array.from({ length: 21 }, (_, index) => {
    const minutes = 8 * 60 + index * 30;
    return `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`;
  });

  constructor() {
    toObservable(this.auth.activeMembership)
      .pipe(
        distinctUntilChanged((previous, current) => previous?.id === current?.id),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((membership) => {
        this.clinicUnitId = membership?.clinicUnitId ?? '';
        this.clinicSearch = '';
        this.patientId = '';
        this.practitionerId = '';
        this.appointmentDate = null;
        this.startTime = '';
        this.endTime = '';
        this.appointments.set([]);
        this.patients.set([]);
        this.practitioners.set([]);
        this.error.set('');
        this.page.set(0);
        this.load();
      });
  }

  scheduledCount(): number {
    return this.appointments().filter(({ status }) => status === 'SCHEDULED').length;
  }

  statusLabel(status: Appointment['status']): string {
    return this.translate.instant(`APPOINTMENTS.STATUS.${status}`);
  }

  scrollToSchedule(): void {
    document
      .querySelector('#schedule-appointment')
      ?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  filteredClinicUnits(): ClinicUnit[] {
    const query = this.clinicSearch.trim().toLowerCase();
    return this.clinicUnits().filter(
      (clinic) => !query || clinic.name.toLowerCase().includes(query),
    );
  }

  selectClinicUnit(clinic: ClinicUnit): void {
    this.clinicUnitId = clinic.id;
    this.clinicSearch = clinic.name;
    this.patientId = '';
    this.loadPatients(clinic.id);
  }

  selectStartTime(): void {
    const nextTime = this.timeSlots[this.timeSlots.indexOf(this.startTime) + 1] ?? '';
    if (!this.endTime || this.endTime <= this.startTime) this.endTime = nextTime;
  }

  endTimeSlots(): string[] {
    return this.timeSlots.filter((time) => time > this.startTime);
  }

  load(page = this.page()): void {
    const membership = this.membership();
    if (!membership) return;
    this.loading.set(true);
    this.error.set('');
    const from = new Date();
    from.setHours(0, 0, 0, 0);
    this.appointmentApi
      .list(membership.organizationId, membership.clinicUnitId, from.toISOString(), undefined, page, 10)
      .subscribe({
        next: (page) => {
          this.appointments.set(page.content);
          this.page.set(page.page);
          this.totalPages.set(page.totalPages);
          this.loading.set(false);
        },
        error: () => {
          this.error.set(this.translate.instant('APPOINTMENTS.ERROR.LOAD_SCHEDULE'));
          this.loading.set(false);
        },
      });
    this.organizationReads.listPractitioners(membership.organizationId, membership.clinicUnitId).subscribe({
      next: (records) =>
        this.practitioners.set(records.filter((practitioner) => practitioner.active)),
      error: () => this.error.set(this.translate.instant('APPOINTMENTS.ERROR.LOAD_OPTIONS')),
    });
    this.organizationReads
      .listClinicUnits(membership.organizationId, membership.clinicUnitId)
      .subscribe({
        next: (units) => {
          this.clinicUnits.set(units);
          const selected = units.find((unit) => unit.id === this.clinicUnitId);
          if (selected) this.clinicSearch = selected.name;
          if (this.clinicUnitId) this.loadPatients(this.clinicUnitId);
        },
        error: () => this.error.set(this.translate.instant('APPOINTMENTS.ERROR.LOAD_CLINICS')),
      });
  }

  previousPage(): void {
    if (this.page() > 0) this.load(this.page() - 1);
  }
  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) this.load(this.page() + 1);
  }

  create(): void {
    const membership = this.membership();
    if (!membership || !this.appointmentDate || !this.startTime || !this.endTime) return;
    const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    const request = {
      clinicUnitId: this.clinicUnitId,
      patientId: this.patientId,
      practitionerId: this.practitionerId,
      startAt: this.localDateTime(this.startTime).toISOString(),
      endAt: this.localDateTime(this.endTime).toISOString(),
      schedulingTimezone: timezone,
    };
    const response = this.editingAppointment
      ? this.appointmentApi.reschedule(membership.organizationId, this.editingAppointment.globalId, request)
      : this.appointmentApi.create(membership.organizationId, request);
    response.subscribe({
      next: () => {
        this.patientId = '';
        this.practitionerId = '';
        this.appointmentDate = null;
        this.startTime = '';
        this.endTime = '';
        this.editingAppointment = null;
        this.load();
      },
      error: (response) =>
        this.error.set(
          isSchedulingConflict(response)
            ? this.translate.instant('APPOINTMENTS.ERROR.CONFLICT')
            : this.translate.instant(
                this.editingAppointment
                  ? 'APPOINTMENTS.ERROR.RESCHEDULE'
                  : 'APPOINTMENTS.ERROR.CREATE',
              ),
        ),
    });
  }

  reschedule(appointment: Appointment): void {
    if (appointment.status !== 'SCHEDULED') return;
    this.editingAppointment = appointment;
    this.clinicUnitId = appointment.clinicUnitId;
    this.clinicSearch =
      this.clinicUnits().find((unit) => unit.id === appointment.clinicUnitId)?.name ?? '';
    this.patientId = appointment.patientId;
    this.practitionerId = appointment.practitionerId;
    this.appointmentDate = new Date(appointment.startAt);
    this.startTime = this.timeOf(appointment.startAt);
    this.endTime = this.timeOf(appointment.endAt);
    this.loadPatients(appointment.clinicUnitId);
    this.scrollToSchedule();
  }

  transition(appointment: Appointment, action: 'cancel' | 'complete' | 'no-show'): void {
    const membership = this.membership();
    if (!membership || appointment.status !== 'SCHEDULED') return;
    const statuses: Record<'cancel' | 'complete' | 'no-show', Appointment['status']> = {
      cancel: 'CANCELLED',
      complete: 'COMPLETED',
      'no-show': 'NO_SHOW',
    };
    this.appointmentApi
      .transition(membership.organizationId, appointment.clinicUnitId, appointment.globalId, statuses[action])
      .subscribe({
        next: () => this.load(),
        error: (response) =>
          this.error.set(
            isSchedulingConflict(response)
              ? this.translate.instant('APPOINTMENTS.ERROR.CONFLICT')
              : this.translate.instant('APPOINTMENTS.ERROR.TRANSITION'),
          ),
      });
  }

  cancelEdit(): void {
    this.editingAppointment = null;
    this.patientId = '';
    this.practitionerId = '';
    this.appointmentDate = null;
    this.startTime = '';
    this.endTime = '';
  }

  private loadPatients(clinicUnitId: string): void {
    const membership = this.membership();
    if (!membership || !clinicUnitId) {
      this.patients.set([]);
      return;
    }
    this.patientApi
      .list({ ...membership, clinicUnitId }, {
        page: { page: 0, size: 20, sort: 'name', direction: 'ASC' },
        filter: {},
      })
      .subscribe({
        next: (page) => this.patients.set(page.content.map((patient) => ({
          globalId: String(patient['globalId']),
          name: String(patient['name']),
        }))),
        error: () => this.error.set(this.translate.instant('APPOINTMENTS.ERROR.LOAD_OPTIONS')),
      });
  }

  private timeOf(value: string): string {
    return new Intl.DateTimeFormat('en-GB', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    }).format(new Date(value));
  }

  private localDateTime(time: string): Date {
    const [hours, minutes] = time.split(':').map(Number);
    const date = new Date(this.appointmentDate!);
    date.setHours(hours, minutes, 0, 0);
    return date;
  }
}

function isSchedulingConflict(response: { code?: string; status?: number }): boolean {
  return response.code === 'SCHEDULING.PRACTITIONER_UNAVAILABLE'
    || response.code === 'CONFLICT'
    || response.status === 409;
}
