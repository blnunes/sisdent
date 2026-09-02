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
});
