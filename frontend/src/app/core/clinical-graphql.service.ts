import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ClinicalEncounter, OdontogramFinding, PageResponse } from './models';
import { GraphQlClientService } from './graphql-client.service';

export type ClinicalEncounterInput = {
  clinicUnitId: string;
  patientId: string;
  appointmentId?: string | null;
  practitionerId?: string | null;
  careAt: string;
  careTimezone: string;
  narrative: string;
  administrativeNote?: string | null;
  version?: number;
};

export type AmendmentInput = Omit<ClinicalEncounterInput, 'patientId' | 'version'> & { reason: string };
export type OdontogramFindingInput = {
  clinicUnitId: string; patientId: string; practitionerId?: string | null; replacementForId?: string | null;
  toothCode: string; surface: string; condition: string; observedAt: string; observationTimezone: string; clinicalNote?: string | null;
};

@Injectable({ providedIn: 'root' })
export class ClinicalGraphqlService {
  private readonly graphql = inject(GraphQlClientService);

  load(organizationId: string, clinicUnitId: string, patientId: string): Observable<ClinicalWorkspaceData> {
    return this.graphql.query<{ clinicalEncounters: PageResponse<ClinicalEncounter>; currentOdontogram: OdontogramFinding[]; odontogramHistory: PageResponse<OdontogramFinding> }>(
      `query ClinicalWorkspace($organizationId: ID!, $clinicUnitId: ID!, $patientId: ID!) {
        clinicalEncounters(organizationId: $organizationId, clinicUnitId: $clinicUnitId, patientId: $patientId) { content { globalId clinicUnitId patientId appointmentId practitionerId careAt careTimezone narrative administrativeNote status finalizedAt originalEncounterId amendmentReason version } }
        currentOdontogram(organizationId: $organizationId, clinicUnitId: $clinicUnitId, patientId: $patientId) { globalId clinicUnitId patientId replacementForId toothCode surface condition observedAt observationTimezone clinicalNote voidedAt voidReason version }
        odontogramHistory(organizationId: $organizationId, clinicUnitId: $clinicUnitId, patientId: $patientId) { content { globalId clinicUnitId patientId replacementForId toothCode surface condition observedAt observationTimezone clinicalNote voidedAt voidReason version } }
      }`, { organizationId, clinicUnitId, patientId },
    ).pipe(map(({ clinicalEncounters, currentOdontogram, odontogramHistory }) => ({ encounters: clinicalEncounters.content, chart: currentOdontogram, history: odontogramHistory.content })));
  }

  createEncounter(organizationId: string, input: ClinicalEncounterInput): Observable<ClinicalEncounter> { return this.encounter('createClinicalEncounter', organizationId, undefined, input); }
  updateEncounter(organizationId: string, encounterId: string, input: ClinicalEncounterInput): Observable<ClinicalEncounter> { return this.encounter('updateClinicalEncounter', organizationId, encounterId, input); }
  finalizeEncounter(organizationId: string, clinicUnitId: string, encounterId: string): Observable<ClinicalEncounter> { return this.graphql.query<{ finalizeClinicalEncounter: ClinicalEncounter }>(`mutation FinalizeClinicalEncounter($organizationId: ID!, $clinicUnitId: ID!, $encounterId: ID!) { finalizeClinicalEncounter(organizationId: $organizationId, clinicUnitId: $clinicUnitId, encounterId: $encounterId) { globalId status version } }`, { organizationId, clinicUnitId, encounterId }).pipe(map(({ finalizeClinicalEncounter }) => finalizeClinicalEncounter)); }
  amendments(organizationId: string, clinicUnitId: string, encounterId: string): Observable<ClinicalEncounter[]> { return this.graphql.query<{ encounterAmendments: ClinicalEncounter[] }>(`query EncounterAmendments($organizationId: ID!, $clinicUnitId: ID!, $encounterId: ID!) { encounterAmendments(organizationId: $organizationId, clinicUnitId: $clinicUnitId, encounterId: $encounterId) { globalId status narrative amendmentReason } }`, { organizationId, clinicUnitId, encounterId }).pipe(map(({ encounterAmendments }) => encounterAmendments)); }
  amendEncounter(organizationId: string, encounterId: string, input: AmendmentInput): Observable<ClinicalEncounter> { return this.graphql.query<{ amendClinicalEncounter: ClinicalEncounter }>(`mutation AmendClinicalEncounter($organizationId: ID!, $encounterId: ID!, $input: AmendEncounterMutationInput!) { amendClinicalEncounter(organizationId: $organizationId, encounterId: $encounterId, input: $input) { globalId status originalEncounterId amendmentReason } }`, { organizationId, encounterId, input }).pipe(map(({ amendClinicalEncounter }) => amendClinicalEncounter)); }
  createFinding(organizationId: string, input: OdontogramFindingInput): Observable<OdontogramFinding> { return this.graphql.query<{ createOdontogramFinding: OdontogramFinding }>(`mutation CreateOdontogramFinding($organizationId: ID!, $input: OdontogramFindingMutationInput!) { createOdontogramFinding(organizationId: $organizationId, input: $input) { globalId toothCode replacementForId version } }`, { organizationId, input }).pipe(map(({ createOdontogramFinding }) => createOdontogramFinding)); }
  voidFinding(organizationId: string, clinicUnitId: string, findingId: string, reason: string, version: number): Observable<OdontogramFinding> { return this.graphql.query<{ voidOdontogramFinding: OdontogramFinding }>(`mutation VoidOdontogramFinding($organizationId: ID!, $clinicUnitId: ID!, $findingId: ID!, $input: VoidOdontogramFindingMutationInput!) { voidOdontogramFinding(organizationId: $organizationId, clinicUnitId: $clinicUnitId, findingId: $findingId, input: $input) { globalId voidReason version } }`, { organizationId, clinicUnitId, findingId, input: { reason, version } }).pipe(map(({ voidOdontogramFinding }) => voidOdontogramFinding)); }

  private encounter(operation: 'createClinicalEncounter' | 'updateClinicalEncounter', organizationId: string, encounterId: string | undefined, input: ClinicalEncounterInput): Observable<ClinicalEncounter> {
    const declaration = encounterId
      ? '$organizationId: ID!, $encounterId: ID!, $input: ClinicalEncounterMutationInput!'
      : '$organizationId: ID!, $input: ClinicalEncounterMutationInput!';
    const variables = encounterId ? { organizationId, encounterId, input } : { organizationId, input };
    return this.graphql.query<Record<typeof operation, ClinicalEncounter>>(`mutation ClinicalEncounter(${declaration}) { ${operation}(organizationId: $organizationId${encounterId ? ', encounterId: $encounterId' : ''}, input: $input) { globalId clinicUnitId patientId careAt careTimezone narrative administrativeNote status version } }`, variables).pipe(map((response) => response[operation]));
  }
}

type ClinicalWorkspaceData = { encounters: ClinicalEncounter[]; chart: OdontogramFinding[]; history: OdontogramFinding[] };
