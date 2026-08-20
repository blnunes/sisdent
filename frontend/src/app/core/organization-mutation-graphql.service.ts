import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ClinicUnit, Practitioner } from './models';
import { GraphQlClientService } from './graphql-client.service';

export type PractitionerWrite = {
  displayName: string;
  registrationNumber: string;
  specialityIds: number[];
  accountId?: string | null;
};

const CLINIC_UNIT_FIELDS = 'id organizationId name active';
const PRACTITIONER_FIELDS = 'globalId displayName registrationNumber accountId active specialityIds';

/** Dedicated organization-scoped write transport for explicitly migrated admin workflows. */
@Injectable({ providedIn: 'root' })
export class OrganizationMutationGraphqlService {
  private readonly graphql = inject(GraphQlClientService);

  createClinicUnit(organizationId: string, input: { name: string }): Observable<ClinicUnit> {
    return this.graphql.query<{ createClinicUnit: ClinicUnit }>(
      `mutation CreateClinicUnit($organizationId: ID!, $input: ClinicUnitMutationInput!) {
        createClinicUnit(organizationId: $organizationId, input: $input) { ${CLINIC_UNIT_FIELDS} }
      }`, { organizationId, input },
    ).pipe(map(({ createClinicUnit }) => createClinicUnit));
  }

  savePractitioner(organizationId: string, practitionerId: string | undefined, input: PractitionerWrite): Observable<Practitioner> {
    const operation = practitionerId ? 'updatePractitioner' : 'createPractitioner';
    const query = `mutation SavePractitioner($organizationId: ID!, $practitionerId: ID, $input: PractitionerMutationInput!) {
      ${operation}${practitionerId
        ? '(organizationId: $organizationId, practitionerId: $practitionerId, input: $input)'
        : '(organizationId: $organizationId, input: $input)'} { ${PRACTITIONER_FIELDS} }
    }`;
    return this.graphql.query<Record<string, Practitioner>>(query, {
      organizationId,
      practitionerId,
      input: { ...input, specialityIds: input.specialityIds.map(String) },
    }).pipe(map((response) => response[operation]));
  }
}
