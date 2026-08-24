import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ClinicUnit, Practitioner } from './models';
import { GraphQlClientService } from './graphql-client.service';

const CLINIC_UNITS_QUERY = `query ClinicUnits($organizationId: ID!, $clinicUnitId: ID) {
  clinicUnits(organizationId: $organizationId, clinicUnitId: $clinicUnitId) {
    id organizationId name active
  }
}`;

const PRACTITIONERS_QUERY = `query Practitioners($organizationId: ID!, $clinicUnitId: ID) {
  practitioners(organizationId: $organizationId, clinicUnitId: $clinicUnitId) {
    globalId displayName registrationNumber accountId active specialityIds
  }
}`;

/** Typed read operations for organization-scoped GraphQL workflows. */
@Injectable({ providedIn: 'root' })
export class OrganizationReadGraphqlService {
  private readonly graphql = inject(GraphQlClientService);

  listClinicUnits(organizationId: string, clinicUnitId?: string): Observable<ClinicUnit[]> {
    return this.graphql
      .query<{ clinicUnits: ClinicUnit[] }>(CLINIC_UNITS_QUERY, {
        organizationId,
        clinicUnitId: clinicUnitId ?? null,
      })
      .pipe(map(({ clinicUnits }) => clinicUnits));
  }

  listPractitioners(organizationId: string, clinicUnitId?: string): Observable<Practitioner[]> {
    return this.graphql
      .query<{ practitioners: Practitioner[] }>(PRACTITIONERS_QUERY, {
        organizationId,
        clinicUnitId: clinicUnitId ?? null,
      })
      .pipe(map(({ practitioners }) => practitioners));
  }
}
