import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../core/auth.service';
import { ClinicalWorkspaceComponent } from './clinical-workspace.component';

describe('ClinicalWorkspaceComponent', () => {
  let fixture: ComponentFixture<ClinicalWorkspaceComponent>;
  let component: ClinicalWorkspaceComponent;
  let http: HttpTestingController;
  const membership = signal({ id: 'membership-1', organizationId: 'organization-1', organizationName: 'Dental', clinicUnitId: 'clinic-1', role: 'CLINICAL_MANAGER' as const, version: 1 });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ClinicalWorkspaceComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: AuthService, useValue: { activeMembership: membership, canReadClinical: () => true, canAuthorClinical: () => true, canManageClinical: () => true } },
        { provide: TranslateService, useValue: { instant: (key: string) => key } },
      ],
    });
    TestBed.overrideComponent(ClinicalWorkspaceComponent, { set: { template: '' } });
    fixture = TestBed.createComponent(ClinicalWorkspaceComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    const patients = http.expectOne(request => request.url === '/api/organizations/organization-1/patients');
    patients.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });

  afterEach(() => http.verify());

  it('sends the immutable replacement reference when recording a correction', () => {
    component.patientId = 'patient-1'; component.toothCode = '11'; component.condition = 'RESTORATION'; component.replacementForId = 'voided-finding-1';
    component.recordFinding();
    const request = http.expectOne('/api/organizations/organization-1/clinical/odontogram/findings');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toMatchObject({ clinicUnitId: 'clinic-1', patientId: 'patient-1', toothCode: '11', condition: 'RESTORATION', replacementForId: 'voided-finding-1' });
    request.flush({});
    const encounters = http.expectOne(request => request.url.endsWith('/clinical/encounters'));
    const current = http.expectOne(request => request.url.endsWith('/clinical/odontogram/current'));
    const history = http.expectOne(request => request.url.endsWith('/clinical/odontogram/history'));
    encounters.flush({ content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 }); current.flush([]); history.flush({ content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 });
  });

  it('searches patients by name only after two characters', () => {
    component.onPatientInput('a');
    http.expectNone('/api/organizations/organization-1/patients');

    component.onPatientInput('Ana');
    const request = http.expectOne(request => request.url === '/api/organizations/organization-1/patients');
    expect(request.request.params.get('clinicUnitId')).toBe('clinic-1');
    expect(request.request.params.get('name')).toBe('Ana');
    expect(request.request.params.get('size')).toBe('20');
    request.flush({ content: [{ globalId: 'patient-1', name: 'Ana Silva' }], page: 0, size: 20, totalElements: 1, totalPages: 1 });
    expect(component.patients()).toEqual([{ globalId: 'patient-1', name: 'Ana Silva' }]);
  });

  it('selects an autocomplete patient and loads their clinical data', () => {
    component.choosePatient({ globalId: 'patient-1', name: 'Ana Silva' });
    expect(component.patientId).toBe('patient-1');
    expect(component.displayPatient(component.patientInput)).toBe('Ana Silva');
    const encounters = http.expectOne(request => request.url.endsWith('/clinical/encounters'));
    const current = http.expectOne(request => request.url.endsWith('/clinical/odontogram/current'));
    const history = http.expectOne(request => request.url.endsWith('/clinical/odontogram/history'));
    encounters.flush({ content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 }); current.flush([]); history.flush({ content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 });
  });
});
