import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { Appointment, PageResponse, Practitioner } from '../../core/models';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-appointments', standalone: true, imports: [FormsModule, DatePipe],
  template: `<main><h1>Appointments</h1><p>Schedule for the active organization membership.</p>
  @if (!membership()) { <p>Select an organization membership to view appointments.</p> } @else {
    <button (click)="load()">Refresh</button>
    @if (error()) { <p role="alert">{{ error() }}</p> }
    <ul>@for (appointment of appointments(); track appointment.globalId) { <li>{{ appointment.startAt | date:'medium' }} — {{ appointment.patientName }} with {{ appointment.practitionerName }} ({{ appointment.status }})</li> }</ul>
    @if (auth.canManageAppointments()) { <form (ngSubmit)="create()"><h2>Schedule appointment</h2>
      <label>Clinic unit UUID <input required [(ngModel)]="clinicUnitId" name="clinicUnitId"></label>
      <label>Patient <select required [(ngModel)]="patientId" name="patientId"><option value="">Select</option>@for (patient of patients(); track patient.globalId) { <option [value]="patient.globalId">{{patient.name}}</option> }</select></label>
      <label>Practitioner <select required [(ngModel)]="practitionerId" name="practitionerId"><option value="">Select</option>@for (p of practitioners(); track p.globalId) { <option [value]="p.globalId">{{p.displayName}}</option> }</select></label>
      <label>Start (local) <input required type="datetime-local" [(ngModel)]="start" name="start"></label><label>End (local) <input required type="datetime-local" [(ngModel)]="end" name="end"></label><button type="submit">Schedule</button></form> }
  }</main>`,
})
export class AppointmentsComponent {
  readonly auth = inject(AuthService); private readonly http = inject(HttpClient);
  readonly membership = this.auth.activeMembership; readonly appointments = signal<Appointment[]>([]); readonly practitioners = signal<Practitioner[]>([]); readonly patients = signal<{globalId: string; name: string}[]>([]); readonly error = signal('');
  clinicUnitId = this.membership()?.clinicUnitId ?? ''; patientId = ''; practitionerId = ''; start = ''; end = '';
  constructor() { this.load(); }
  load(): void { const membership = this.membership(); if (!membership) return; const now = new Date(); const to = new Date(now.getTime() + 31 * 86400000); const query = `from=${encodeURIComponent(now.toISOString())}&to=${encodeURIComponent(to.toISOString())}` + (membership.clinicUnitId ? `&clinicUnitId=${membership.clinicUnitId}` : ''); this.http.get<PageResponse<Appointment>>(`/api/organizations/${membership.organizationId}/appointments?${query}`).subscribe({next: page => this.appointments.set(page.content), error: () => this.error.set('Unable to load the scoped schedule.')}); this.http.get<Practitioner[]>(`/api/organizations/${membership.organizationId}/practitioners`).subscribe({next: records => this.practitioners.set(records.filter(p => p.active))}); this.http.get<PageResponse<{globalId: string; name: string}>>(`/api/organizations/${membership.organizationId}/patients`).subscribe({next: page => this.patients.set(page.content)}); }
  create(): void { const membership = this.membership(); if (!membership) return; const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone; this.http.post<Appointment>(`/api/organizations/${membership.organizationId}/appointments`, {clinicUnitId:this.clinicUnitId, patientId:this.patientId, practitionerId:this.practitionerId, startAt:new Date(this.start).toISOString(), endAt:new Date(this.end).toISOString(), schedulingTimezone:timezone}).subscribe({next: () => { this.error.set(''); this.load(); }, error: response => this.error.set(response.status === 409 ? 'The practitioner is unavailable for this interval.' : 'Unable to schedule this appointment.')}); }
}
