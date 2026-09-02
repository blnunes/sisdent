import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { OrganizationReadGraphqlService } from '../../core/organization-read-graphql.service';
import { PatientApiService } from '../patients/patient-api.service';
import { ClinicalWorkspaceComponent } from './clinical-workspace.component';

describe('ClinicalWorkspaceComponent', () => {
  let fixture: ComponentFixture<ClinicalWorkspaceComponent>;
  let component: ClinicalWorkspaceComponent;
  let http: HttpTestingController;
  const patientApi = { list: vi.fn(() => of({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })) };
  const membership = signal({ id: 'membership-1', organizationId: 'organization-1', organizationName: 'Dental', clinicUnitId: 'clinic-1', role: 'CLINICAL_MANAGER' as const, version: 1 });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ClinicalWorkspaceComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: AuthService, useValue: { activeMembership: membership, canReadClinical: () => true, canAuthorClinical: () => true, canManageClinical: () => true } },
        { provide: TranslateService, useValue: { instant: (key: string) => key } },
        { provide: PatientApiService, useValue: patientApi },
        { provide: OrganizationReadGraphqlService, useValue: { listClinicUnits: () => of([]) } },
      ],
    });
    TestBed.overrideComponent(ClinicalWorkspaceComponent, { set: { template: '' } });
    fixture = TestBed.createComponent(ClinicalWorkspaceComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('sends the immutable replacement reference when recording a correction', () => {
    component.patientId = 'patient-1'; component.toothCode = '11'; component.condition = 'RESTORATION'; component.replacementForId = 'voided-finding-1';
    component.recordFinding();
    const request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain('createOdontogramFinding');
    expect(request.request.body.variables).toMatchObject({ organizationId: 'organization-1', input: { clinicUnitId: 'clinic-1', patientId: 'patient-1', toothCode: '11', condition: 'RESTORATION', replacementForId: 'voided-finding-1' } });
    request.flush({ data: { createOdontogramFinding: { globalId: 'finding-1', toothCode: '11', replacementForId: 'voided-finding-1', version: 0 } } });
    const reload = http.expectOne('/graphql');
    expect(reload.request.body.query).toContain('ClinicalWorkspace');
    reload.flush({ data: { clinicalEncounters: { content: [] }, currentOdontogram: [], odontogramHistory: { content: [] } } });
  });

  it('searches patients by name only after two characters', () => {
    component.onPatientInput('a');
    http.expectNone('/graphql');

    component.onPatientInput('Ana');
    expect(patientApi.list).toHaveBeenLastCalledWith(expect.anything(), expect.objectContaining({ filter: { name: 'Ana' } }));
  });

  it('selects an autocomplete patient and loads their clinical data', () => {
    component.choosePatient({ globalId: 'patient-1', name: 'Ana Silva' });
    expect(component.patientId).toBe('patient-1');
    expect(component.displayPatient(component.patientInput)).toBe('Ana Silva');
    const request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain('ClinicalWorkspace');
    request.flush({ data: { clinicalEncounters: { content: [] }, currentOdontogram: [], odontogramHistory: { content: [] } } });
  });

  it('clears stale data while a short patient query is entered', () => {
    component.patientId = 'patient-1';
    component.encounters.set([{ globalId: 'encounter-1' } as never]);

    component.onPatientInput('a');

    expect(component.patientId).toBe('');
    expect(component.patients()).toEqual([]);
    expect(component.encounters()).toEqual([]);
  });

  it('prepares and cancels an odontogram correction without retaining draft state', () => {
    const finding = { globalId: 'finding-1', toothCode: '11', surface: 'MESIAL' } as never;
    component.narrative = 'Draft';
    component.administrativeNote = 'Note';

    component.replacementFor(finding);
    component.cancelEdit();

    expect(component.replacementForId).toBe('finding-1');
    expect(component.toothCode).toBe('11');
    expect(component.surface).toBe('MESIAL');
    expect(component.narrative).toBe('');
    expect(component.administrativeNote).toBe('');
  });

  it('creates a draft and refreshes the selected patient data', () => {
    component.patientId = 'patient-1';
    component.narrative = '  Clinical note  ';
    component.administrativeNote = '  Reviewed  ';

    component.saveDraft();

    const create = http.expectOne('/graphql');
    expect(create.request.body.variables.input).toMatchObject({ narrative: 'Clinical note', administrativeNote: 'Reviewed' });
    create.flush({ data: { createClinicalEncounter: { globalId: 'encounter-1' } } });
    const reload = http.expectOne('/graphql');
    reload.flush({ data: { clinicalEncounters: { content: [] }, currentOdontogram: [], odontogramHistory: { content: [] } } });
    expect(component.narrative).toBe('');
  });

  it('does not submit incomplete clinical drafts', () => {
    component.patientId = 'patient-1';
    component.narrative = '   ';

    component.saveDraft();

    http.expectNone('/graphql');
    expect(component.narrative).toBe('   ');
  });

  it('finalizes a draft and reloads the clinical workspace', () => {
    component.patientId = 'patient-1';

    component.finalize({ globalId: 'encounter-1' } as never);

    const finalize = http.expectOne('/graphql');
    expect(finalize.request.body.query).toContain('FinalizeClinicalEncounter');
    finalize.flush({ data: { finalizeClinicalEncounter: { globalId: 'encounter-1' } } });
    const reload = http.expectOne('/graphql');
    reload.flush({ data: { clinicalEncounters: { content: [] }, currentOdontogram: [], odontogramHistory: { content: [] } } });
  });

  it('loads amendments and reports a scoped GraphQL failure', () => {
    component.patientId = 'patient-1';
    const encounter = { globalId: 'encounter-1' } as never;
    component.loadAmendments(encounter);
    const amendments = http.expectOne('/graphql');
    amendments.flush({ data: { encounterAmendments: [{ globalId: 'amendment-1' }] } });
    expect(component.amendments()).toEqual([{ globalId: 'amendment-1' }]);

    component.selectPatient();
    const load = http.expectOne('/graphql');
    load.flush({ errors: [{ message: 'No access', extensions: { code: 'AUTHORIZATION.DENIED' } }] });
    expect(component.error()).toBe('CLINICAL.ERROR.FORBIDDEN');
  });

  it('updates an edited draft with its optimistic-lock version', () => {
    component.patientId = 'patient-1';
    const draft = {
      globalId: 'encounter-1',
      narrative: 'Previous note',
      administrativeNote: 'Previous administration',
      careAt: '2026-01-01T10:00:00Z',
      careTimezone: 'UTC',
      version: 7,
    } as never;
    component.editDraft(draft);
    component.narrative = 'Updated note';

    component.saveDraft();

    const update = http.expectOne('/graphql');
    expect(update.request.body.query).toContain('updateClinicalEncounter');
    expect(update.request.body.variables.input).toMatchObject({ narrative: 'Updated note', version: 7 });
    update.flush({ data: { updateClinicalEncounter: { globalId: 'encounter-1' } } });
    http.expectOne('/graphql').flush({
      data: { clinicalEncounters: { content: [] }, currentOdontogram: [], odontogramHistory: { content: [] } },
    });
    expect(component.selectedEncounter()).toBeNull();
  });

  it('creates a validated amendment and reloads the workspace', () => {
    component.patientId = 'patient-1';
    component.editDraft({ globalId: 'final-1', status: 'FINAL' } as never);
    component.narrative = 'Corrected note';
    component.amendmentReason = 'Clarification';

    component.createAmendment();

    const amendment = http.expectOne('/graphql');
    expect(amendment.request.body.query).toContain('amendClinicalEncounter');
    expect(amendment.request.body.variables.input).toMatchObject({
      clinicUnitId: 'clinic-1',
      narrative: 'Corrected note',
      reason: 'Clarification',
    });
    amendment.flush({ data: { amendClinicalEncounter: { globalId: 'amendment-1' } } });
    http.expectOne('/graphql').flush({
      data: { clinicalEncounters: { content: [] }, currentOdontogram: [], odontogramHistory: { content: [] } },
    });
  });

  it('does not amend a draft without a final encounter and a reason', () => {
    const draft = { globalId: 'draft-1', status: 'DRAFT' } as never;
    component.editDraft(draft);
    component.narrative = 'Note';

    component.createAmendment();

    http.expectNone('/graphql');
    expect(component.selectedEncounter()).toBe(draft);
  });

  it('voids a finding, prepares its replacement, and reloads the workspace', () => {
    component.patientId = 'patient-1';
    component.voidReason = 'Incorrect surface';
    const finding = { globalId: 'finding-1', toothCode: '11', surface: 'MESIAL', version: 3 } as never;

    component.voidFinding(finding);

    const voidRequest = http.expectOne('/graphql');
    expect(voidRequest.request.body.query).toContain('voidOdontogramFinding');
    expect(voidRequest.request.body.variables).toMatchObject({
      findingId: 'finding-1',
      input: { reason: 'Incorrect surface', version: 3 },
    });
    voidRequest.flush({ data: { voidOdontogramFinding: { globalId: 'finding-1' } } });
    http.expectOne('/graphql').flush({
      data: { clinicalEncounters: { content: [] }, currentOdontogram: [], odontogramHistory: { content: [] } },
    });
    expect(component.voidReason).toBe('');
    expect(component.replacementForId).toBe('finding-1');
  });

  it('does not void a finding without a reason', () => {
    component.voidReason = '   ';

    component.voidFinding({ globalId: 'finding-1' } as never);

    http.expectNone('/graphql');
    expect(component.voidReason).toBe('   ');
  });

  it('resets clinical state and reloads patients for the active clinic', () => {
    component.patientId = 'patient-1';
    component.patientInput = { globalId: 'patient-1', name: 'Ana' };
    component.encounters.set([{ globalId: 'encounter-1' } as never]);

    component.reset();

    expect(component.patientId).toBe('');
    expect(component.clinicUnitId).toBe('clinic-1');
    expect(component.encounters()).toEqual([]);
    expect(patientApi.list).toHaveBeenLastCalledWith(expect.objectContaining({ clinicUnitId: 'clinic-1' }), expect.anything());
  });

  it('clears selected data and exposes the no-clinic state when changing clinic', () => {
    component.patientId = 'patient-1';
    component.encounters.set([{ globalId: 'encounter-1' } as never]);
    component.clinicUnitId = '';

    component.changeClinic();

    expect(component.patientId).toBe('');
    expect(component.encounters()).toEqual([]);
    expect(component.error()).toBe('CLINICAL.NO_CLINIC');
  });
});
