import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatTooltipModule } from '@angular/material/tooltip';
import { distinctUntilChanged } from 'rxjs';
import { Appointment, PageResponse, Practitioner } from '../../core/models';
import { AuthService } from '../../core/auth.service';
import { AppHeaderComponent } from '../../shared/app-header.component';
import { ModuleNavigationComponent } from '../../shared/module-navigation.component';

@Component({
  selector: 'app-appointments',
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSidenavModule,
    MatTooltipModule,
    AppHeaderComponent,
    ModuleNavigationComponent,
  ],
  templateUrl: './appointments.component.html',
  styleUrl: './appointments.component.scss',
})
export class AppointmentsComponent {
  readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  readonly membership = this.auth.activeMembership;
  readonly appointments = signal<Appointment[]>([]);
  readonly practitioners = signal<Practitioner[]>([]);
  readonly patients = signal<{ globalId: string; name: string }[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');

  clinicUnitId = this.membership()?.clinicUnitId ?? '';
  patientId = '';
  practitionerId = '';
  start = '';
  end = '';

  constructor() {
    toObservable(this.auth.activeMembership)
      .pipe(
        distinctUntilChanged((previous, current) => previous?.id === current?.id),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((membership) => {
        this.clinicUnitId = membership?.clinicUnitId ?? '';
        this.patientId = '';
        this.practitionerId = '';
        this.start = '';
        this.end = '';
        this.appointments.set([]);
        this.patients.set([]);
        this.practitioners.set([]);
        this.error.set('');
        this.load();
      });
  }

  scheduledCount(): number {
    return this.appointments().filter(({ status }) => status === 'SCHEDULED').length;
  }

  statusLabel(status: Appointment['status']): string {
    return status.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
  }

  scrollToSchedule(): void {
    document.querySelector('#schedule-appointment')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  load(): void {
    const membership = this.membership();
    if (!membership) return;
    this.loading.set(true);
    this.error.set('');
    const now = new Date();
    const to = new Date(now.getTime() + 31 * 86_400_000);
    const query = `from=${encodeURIComponent(now.toISOString())}&to=${encodeURIComponent(to.toISOString())}`
      + (membership.clinicUnitId ? `&clinicUnitId=${encodeURIComponent(membership.clinicUnitId)}` : '');
    this.http.get<PageResponse<Appointment>>(`/api/organizations/${membership.organizationId}/appointments?${query}`).subscribe({
      next: (page) => {
        this.appointments.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load the scoped schedule.');
        this.loading.set(false);
      },
    });
    this.http.get<Practitioner[]>(`/api/organizations/${membership.organizationId}/practitioners`).subscribe({
      next: (records) => this.practitioners.set(records.filter((practitioner) => practitioner.active)),
      error: () => this.error.set('Unable to load the appointment options.'),
    });
    this.http.get<PageResponse<{ globalId: string; name: string }>>(`/api/organizations/${membership.organizationId}/patients`).subscribe({
      next: (page) => this.patients.set(page.content),
      error: () => this.error.set('Unable to load the appointment options.'),
    });
  }

  create(): void {
    const membership = this.membership();
    if (!membership) return;
    const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    this.http.post<Appointment>(`/api/organizations/${membership.organizationId}/appointments`, {
      clinicUnitId: this.clinicUnitId,
      patientId: this.patientId,
      practitionerId: this.practitionerId,
      startAt: new Date(this.start).toISOString(),
      endAt: new Date(this.end).toISOString(),
      schedulingTimezone: timezone,
    }).subscribe({
      next: () => {
        this.patientId = '';
        this.practitionerId = '';
        this.start = '';
        this.end = '';
        this.load();
      },
      error: (response) => this.error.set(response.status === 409
        ? 'The practitioner is unavailable for this interval.'
        : 'Unable to schedule this appointment.'),
    });
  }
}
