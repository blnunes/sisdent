import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Appointment, PageResponse } from './models';
import { GraphQlClientService } from './graphql-client.service';

export type AppointmentInput = {
  clinicUnitId: string;
  patientId: string;
  practitionerId: string;
  startAt: string;
  endAt: string;
  schedulingTimezone: string;
};

export type AppointmentAvailability = { available: boolean };

type AppointmentStatus = Appointment['status'];

/** Typed scheduling transport. Authorization and scheduling validation remain server-owned. */
@Injectable({ providedIn: 'root' })
export class AppointmentGraphqlService {
  private readonly graphql = inject(GraphQlClientService);

  list(organizationId: string, clinicUnitId: string | undefined, from: string, to: string | undefined, page: number, size: number): Observable<PageResponse<Appointment>> {
    return this.graphql.query<{ appointments: PageResponse<Appointment> }>(
      `query Appointments($organizationId: ID!, $clinicUnitId: ID, $from: String!, $to: String, $page: Int, $size: Int) {
        appointments(organizationId: $organizationId, clinicUnitId: $clinicUnitId, from: $from, to: $to, page: $page, size: $size) {
          content { globalId clinicUnitId patientId patientName practitionerId practitionerName startAt endAt schedulingTimezone status }
          page size totalElements totalPages
        }
      }`,
      { organizationId, clinicUnitId: clinicUnitId ?? null, from, to: to ?? null, page, size },
    ).pipe(map(({ appointments }) => appointments));
  }

  create(organizationId: string, input: AppointmentInput): Observable<Appointment> {
    return this.graphql.query<{ createAppointment: Appointment }>(
      `mutation CreateAppointment($organizationId: ID!, $input: AppointmentMutationInput!) {
        createAppointment(organizationId: $organizationId, input: $input) {
          globalId clinicUnitId patientId patientName practitionerId practitionerName startAt endAt schedulingTimezone status
        }
      }`,
      { organizationId, input },
    ).pipe(map(({ createAppointment }) => createAppointment));
  }

  reschedule(organizationId: string, appointmentId: string, input: AppointmentInput): Observable<Appointment> {
    return this.graphql.query<{ rescheduleAppointment: Appointment }>(
      `mutation RescheduleAppointment($organizationId: ID!, $appointmentId: ID!, $input: AppointmentMutationInput!) {
        rescheduleAppointment(organizationId: $organizationId, appointmentId: $appointmentId, input: $input) {
          globalId clinicUnitId patientId patientName practitionerId practitionerName startAt endAt schedulingTimezone status
        }
      }`,
      { organizationId, appointmentId, input },
    ).pipe(map(({ rescheduleAppointment }) => rescheduleAppointment));
  }

  transition(organizationId: string, clinicUnitId: string, appointmentId: string, status: AppointmentStatus): Observable<Appointment> {
    return this.graphql.query<{ transitionAppointment: Appointment }>(
      `mutation TransitionAppointment($organizationId: ID!, $clinicUnitId: ID!, $appointmentId: ID!, $status: AppointmentStatus!) {
        transitionAppointment(organizationId: $organizationId, clinicUnitId: $clinicUnitId, appointmentId: $appointmentId, status: $status) {
          globalId clinicUnitId patientId patientName practitionerId practitionerName startAt endAt schedulingTimezone status
        }
      }`,
      { organizationId, clinicUnitId, appointmentId, status },
    ).pipe(map(({ transitionAppointment }) => transitionAppointment));
  }

  availability(organizationId: string, clinicUnitId: string, practitionerId: string, startAt: string, endAt: string): Observable<AppointmentAvailability> {
    return this.graphql.query<{ appointmentAvailability: AppointmentAvailability }>(
      `query AppointmentAvailability($organizationId: ID!, $clinicUnitId: ID!, $practitionerId: ID!, $startAt: String!, $endAt: String!) {
        appointmentAvailability(organizationId: $organizationId, clinicUnitId: $clinicUnitId, practitionerId: $practitionerId, startAt: $startAt, endAt: $endAt) { available }
      }`,
      { organizationId, clinicUnitId, practitionerId, startAt, endAt },
    ).pipe(map(({ appointmentAvailability }) => appointmentAvailability));
  }
}
