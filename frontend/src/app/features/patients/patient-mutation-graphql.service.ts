import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { Membership } from '../../core/models';
import { GraphQlClientService } from '../../core/graphql-client.service';

type PatientMutationResult = { globalId: string; name: string; active: boolean };

/** Typed, scoped patient-update transport. Patient creation and deactivation intentionally remain REST. */
@Injectable({ providedIn: 'root' })
export class PatientMutationGraphqlService {
  private readonly graphql = inject(GraphQlClientService);

  update(membership: Membership, patientId: string, input: Record<string, unknown>): Observable<PatientMutationResult> {
    return this.graphql.query<{ updatePatient: PatientMutationResult }>(
      `mutation UpdatePatient($organizationId: ID!, $clinicUnitId: ID, $patientId: ID!, $input: PatientUpdateMutationInput!) {
        updatePatient(organizationId: $organizationId, clinicUnitId: $clinicUnitId, patientId: $patientId, input: $input) { globalId name active }
      }`, { organizationId: membership.organizationId, clinicUnitId: membership.clinicUnitId, patientId, input },
    ).pipe(map(({ updatePatient }) => updatePatient));
  }
}
