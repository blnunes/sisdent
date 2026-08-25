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

export type AppointmentAvailabilityInterval = {
  practitionerId: string;
  startAt: string;
  endAt: string;
  availability: 'AVAILABLE' | 'UNAVAILABLE';
  category: 'WORKING_HOURS' | 'BREAK' | 'BLOCKED' | 'OCCUPIED';
};
export type AppointmentPatient = { globalId: string; name: string; active: boolean };

export type AppointmentStatus = Appointment['status'];
export type AppointmentDetail = Pick<
  Appointment,
  'patientId' | 'practitionerId' | 'patientName' | 'practitionerName' | 'startAt' | 'endAt' | 'status'
>;
export type BlockedPeriod = { globalId: string; clinicUnitId: string; practitionerId: string | null; startAt: string; endAt: string; version: number };
export type BlockedPeriodInput = { clinicUnitId: string; practitionerId: string | null; startAt: string; endAt: string };
export type PerformedProcedure = { globalId: string; dentalProcedureId: string; procedureNameSnapshot: string; performedAt: string; voidedAt: string | null };
export type PerformedProcedureOption = { id: string; displayName: string };

/** Typed scheduling transport. Authorization and scheduling validation remain server-owned. */
@Injectable({ providedIn: 'root' })
export class AppointmentGraphqlService {
  private readonly graphql = inject(GraphQlClientService);

  list(organizationId: string, clinicUnitId: string | undefined, from: string, to: string | undefined, page: number, size: number, practitionerIds?: readonly string[]): Observable<PageResponse<Appointment>> {
    return this.graphql.query<{ appointments: PageResponse<Appointment> }>(
      `query Appointments($organizationId: ID!, $clinicUnitId: ID, $from: String!, $to: String, $practitionerIds: [ID!], $page: Int, $size: Int) {
        appointments(organizationId: $organizationId, clinicUnitId: $clinicUnitId, from: $from, to: $to, practitionerIds: $practitionerIds, page: $page, size: $size) {
          content { globalId clinicUnitId patientId patientName practitionerId practitionerName startAt endAt schedulingTimezone status }
          page size totalElements totalPages
        }
      }`,
      { organizationId, clinicUnitId: clinicUnitId ?? null, from, to: to ?? null, practitionerIds: practitionerIds ?? null, page, size },
    ).pipe(map(({ appointments }) => appointments));
  }

  detail(organizationId: string, clinicUnitId: string, appointmentId: string): Observable<AppointmentDetail> {
    return this.graphql.query<{ appointment: AppointmentDetail }>(
      `query Appointment($organizationId: ID!, $clinicUnitId: ID!, $appointmentId: ID!) {
        appointment(organizationId: $organizationId, clinicUnitId: $clinicUnitId, appointmentId: $appointmentId) {
          patientId practitionerId patientName practitionerName startAt endAt status
        }
      }`,
      { organizationId, clinicUnitId, appointmentId },
    ).pipe(map(({ appointment }) => appointment));
  }

  patients(organizationId: string, clinicUnitId: string): Observable<AppointmentPatient[]> {
    return this.graphql.query<{ patients: PageResponse<AppointmentPatient> }>(
      `query AppointmentPatients($organizationId: ID!, $clinicUnitId: ID!, $page: CataloguePageInput, $filter: PatientFilterInput) {
        patients(organizationId: $organizationId, clinicUnitId: $clinicUnitId, page: $page, filter: $filter) {
          content { globalId name active } page size totalElements totalPages
        }
      }`,
      { organizationId, clinicUnitId, page: { page: 0, size: 100, sort: 'name', direction: 'ASC' }, filter: { active: true } },
    ).pipe(map(({ patients }) => patients.content.filter((patient) => patient.active)));
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

  availabilityIntervals(organizationId: string, clinicUnitId: string, from: string, to: string, practitionerIds?: readonly string[]): Observable<AppointmentAvailabilityInterval[]> {
    return this.graphql.query<{ appointmentAvailabilityIntervals: AppointmentAvailabilityInterval[] }>(
      `query AppointmentAvailabilityIntervals($organizationId: ID!, $clinicUnitId: ID!, $from: String!, $to: String!, $practitionerIds: [ID!]) {
        appointmentAvailabilityIntervals(organizationId: $organizationId, clinicUnitId: $clinicUnitId, from: $from, to: $to, practitionerIds: $practitionerIds) { practitionerId startAt endAt availability category }
      }`,
      { organizationId, clinicUnitId, from, to, practitionerIds: practitionerIds ?? null },
    ).pipe(map(({ appointmentAvailabilityIntervals }) => appointmentAvailabilityIntervals));
  }

  blockedPeriods(organizationId: string, clinicUnitId: string, from: string, to: string): Observable<BlockedPeriod[]> {
    return this.graphql.query<{ appointmentBlockedPeriods: BlockedPeriod[] }>(
      `query BlockedPeriods($organizationId: ID!, $clinicUnitId: ID!, $from: String!, $to: String!) { appointmentBlockedPeriods(organizationId: $organizationId, clinicUnitId: $clinicUnitId, from: $from, to: $to) { globalId clinicUnitId practitionerId startAt endAt version } }`,
      { organizationId, clinicUnitId, from, to }).pipe(map(({ appointmentBlockedPeriods }) => appointmentBlockedPeriods));
  }
  createBlockedPeriod(organizationId: string, input: BlockedPeriodInput): Observable<BlockedPeriod> {
    return this.graphql.query<{ createAppointmentBlockedPeriod: BlockedPeriod }>(
      `mutation CreateBlockedPeriod($organizationId: ID!, $input: AppointmentBlockedPeriodMutationInput!) { createAppointmentBlockedPeriod(organizationId: $organizationId, input: $input) { globalId clinicUnitId practitionerId startAt endAt version } }`, { organizationId, input }).pipe(map(({ createAppointmentBlockedPeriod }) => createAppointmentBlockedPeriod));
  }
  updateBlockedPeriod(organizationId: string, blockedPeriodId: string, version: number, input: BlockedPeriodInput): Observable<BlockedPeriod> {
    return this.graphql.query<{ updateAppointmentBlockedPeriod: BlockedPeriod }>(
      `mutation UpdateBlockedPeriod($organizationId: ID!, $blockedPeriodId: ID!, $version: Int!, $input: AppointmentBlockedPeriodMutationInput!) { updateAppointmentBlockedPeriod(organizationId: $organizationId, blockedPeriodId: $blockedPeriodId, version: $version, input: $input) { globalId clinicUnitId practitionerId startAt endAt version } }`, { organizationId, blockedPeriodId, version, input }).pipe(map(({ updateAppointmentBlockedPeriod }) => updateAppointmentBlockedPeriod));
  }
  deleteBlockedPeriod(organizationId: string, clinicUnitId: string, blockedPeriodId: string, version: number): Observable<boolean> {
    return this.graphql.query<{ deleteAppointmentBlockedPeriod: boolean }>(
      `mutation DeleteBlockedPeriod($organizationId: ID!, $clinicUnitId: ID!, $blockedPeriodId: ID!, $version: Int!) { deleteAppointmentBlockedPeriod(organizationId: $organizationId, clinicUnitId: $clinicUnitId, blockedPeriodId: $blockedPeriodId, version: $version) }`, { organizationId, clinicUnitId, blockedPeriodId, version }).pipe(map(({ deleteAppointmentBlockedPeriod }) => deleteAppointmentBlockedPeriod));
  }
  performedProcedures(organizationId: string, clinicUnitId: string, appointmentId: string): Observable<PerformedProcedure[]> {
    return this.graphql.query<{ performedProcedures: PerformedProcedure[] }>(`query PerformedProcedures($organizationId: ID!, $clinicUnitId: ID!, $appointmentId: ID!) { performedProcedures(organizationId: $organizationId, clinicUnitId: $clinicUnitId, appointmentId: $appointmentId) { globalId dentalProcedureId procedureNameSnapshot performedAt voidedAt } }`, { organizationId, clinicUnitId, appointmentId }).pipe(map(({ performedProcedures }) => performedProcedures));
  }
  eligiblePerformedProcedureOptions(organizationId: string, clinicUnitId: string, appointmentId: string): Observable<PerformedProcedureOption[]> {
    return this.graphql.query<{ eligiblePerformedProcedureOptions: PerformedProcedureOption[] }>(`query EligiblePerformedProcedureOptions($organizationId: ID!, $clinicUnitId: ID!, $appointmentId: ID!) { eligiblePerformedProcedureOptions(organizationId: $organizationId, clinicUnitId: $clinicUnitId, appointmentId: $appointmentId) { id displayName } }`, { organizationId, clinicUnitId, appointmentId }).pipe(map(({ eligiblePerformedProcedureOptions }) => eligiblePerformedProcedureOptions));
  }
  createPerformedProcedure(organizationId: string, clinicUnitId: string, appointmentId: string, dentalProcedureId: string, performedAt: string): Observable<PerformedProcedure> {
    return this.graphql.query<{ createPerformedProcedure: PerformedProcedure }>(`mutation CreatePerformedProcedure($organizationId: ID!, $clinicUnitId: ID!, $appointmentId: ID!, $input: PerformedProcedureMutationInput!) { createPerformedProcedure(organizationId: $organizationId, clinicUnitId: $clinicUnitId, appointmentId: $appointmentId, input: $input) { globalId dentalProcedureId procedureNameSnapshot performedAt voidedAt } }`, { organizationId, clinicUnitId, appointmentId, input: { dentalProcedureId, performedAt } }).pipe(map(({ createPerformedProcedure }) => createPerformedProcedure));
  }
  voidPerformedProcedure(organizationId: string, clinicUnitId: string, performedProcedureId: string, reason: string): Observable<PerformedProcedure> {
    return this.graphql.query<{ voidPerformedProcedure: PerformedProcedure }>(`mutation VoidPerformedProcedure($organizationId: ID!, $clinicUnitId: ID!, $performedProcedureId: ID!, $input: VoidPerformedProcedureMutationInput!) { voidPerformedProcedure(organizationId: $organizationId, clinicUnitId: $clinicUnitId, performedProcedureId: $performedProcedureId, input: $input) { globalId dentalProcedureId procedureNameSnapshot performedAt voidedAt } }`, { organizationId, clinicUnitId, performedProcedureId, input: { reason } }).pipe(map(({ voidPerformedProcedure }) => voidPerformedProcedure));
  }
}
