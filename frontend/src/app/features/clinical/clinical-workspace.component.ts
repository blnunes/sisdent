import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSidenavModule } from '@angular/material/sidenav';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { distinctUntilChanged } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { ClinicalEncounter, ClinicUnit, OdontogramFinding } from '../../core/models';
import { ClinicalGraphqlService } from '../../core/clinical-graphql.service';
import { GraphQlUserError } from '../../core/graphql-client.service';
import { AppHeaderComponent } from '../../core/layout/app-header/app-header.component';
import { ModuleNavigationComponent } from '../../core/layout/module-navigation/module-navigation.component';
import { OrganizationReadGraphqlService } from '../../core/organization-read-graphql.service';
import { PatientApiService } from '../patients/patient-api.service';

type PatientOption = { globalId: string; name: string };
type Choice = { value: string; labelKey: string };

@Component({
  selector: 'app-clinical-workspace',
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSidenavModule,
    TranslatePipe,
    AppHeaderComponent,
    ModuleNavigationComponent,
  ],
  templateUrl: './clinical-workspace.component.html',
  styleUrl: './clinical-workspace.component.scss',
})
export class ClinicalWorkspaceComponent {
  readonly auth = inject(AuthService);
  private readonly clinical = inject(ClinicalGraphqlService);
  private readonly organizationReads = inject(OrganizationReadGraphqlService);
  private readonly patientApi = inject(PatientApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly translate = inject(TranslateService);
  readonly membership = this.auth.activeMembership;
  readonly clinics = signal<ClinicUnit[]>([]);
  readonly patients = signal<PatientOption[]>([]);
  readonly encounters = signal<ClinicalEncounter[]>([]);
  readonly amendments = signal<ClinicalEncounter[]>([]);
  readonly chart = signal<OdontogramFinding[]>([]);
  readonly history = signal<OdontogramFinding[]>([]);
  readonly error = signal('');
  readonly selectedEncounter = signal<ClinicalEncounter | null>(null);
  readonly teeth: string[] = [
    ...[1, 2, 3, 4].flatMap((quadrant) =>
      Array.from({ length: 8 }, (_, index) => `${quadrant}${index + 1}`),
    ),
    ...[5, 6, 7, 8].flatMap((quadrant) =>
      Array.from({ length: 5 }, (_, index) => `${quadrant}${index + 1}`),
    ),
  ];
  readonly conditions: Choice[] = [
    'SOUND',
    'CARIES',
    'RESTORATION',
    'CROWN',
    'MISSING',
    'IMPLANT',
    'EXTRACTED',
  ].map((value) => ({ value, labelKey: `CONDITIONS.${value}` }));
  readonly surfaces: Choice[] = [
    'WHOLE_TOOTH',
    'MESIAL',
    'DISTAL',
    'BUCCAL',
    'LINGUAL_PALATAL',
    'OCCLUSAL_INCISAL',
  ].map((value) => ({ value, labelKey: `SURFACES.${value}` }));
  patientId = '';
  clinicUnitId = '';
  patientInput: PatientOption | string = '';
  narrative = '';
  administrativeNote = '';
  amendmentReason = '';
  toothCode = '';
  condition = 'SOUND';
  surface = 'WHOLE_TOOTH';
  clinicalNote = '';
  voidReason = '';
  replacementForId = '';

  constructor() {
    toObservable(this.auth.activeMembership)
      .pipe(
        distinctUntilChanged((a, b) => a?.id === b?.id),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.reset());
  }

  reset(): void {
    this.patientId = '';
    this.patientInput = '';
    this.clinicUnitId = '';
    this.patients.set([]);
    this.clinics.set([]);
    this.clearClinicalData();
    this.error.set('');
    const membership = this.membership();
    if (!membership || !this.auth.canReadClinical()) return;
    if (membership.clinicUnitId) {
      this.clinicUnitId = membership.clinicUnitId;
      this.loadPatients();
      return;
    }
    this.organizationReads.listClinicUnits(membership.organizationId).subscribe({
      next: (clinics) => {
        this.clinics.set(clinics);
        this.clinicUnitId = clinics[0]?.id ?? '';
        this.loadPatients();
      },
      error: (error) => this.fail(error, 'LOAD'),
    });
  }

  changeClinic(): void {
    this.patientId = '';
    this.patientInput = '';
    this.clearClinicalData();
    this.loadPatients();
  }

  loadPatients(name?: string): void {
    const membership = this.membership();
    if (!membership || !this.clinicUnitId) {
      this.error.set(this.t('NO_CLINIC'));
      return;
    }
    this.patientApi
      .list(
        { ...membership, clinicUnitId: this.clinicUnitId },
        {
          page: { page: 0, size: 20, sort: 'name', direction: 'ASC' },
          filter: name ? { name } : {},
        },
      )
      .subscribe({
        next: (response) => {
          this.patients.set(
            response.content.map((patient) => ({
              globalId: String(patient['globalId']),
              name: String(patient['name']),
            })),
          );
          if (!response.content.length && !name) this.error.set(this.t('NO_PATIENT'));
        },
        error: (error) => this.fail(error, 'LOAD'),
      });
  }

  displayPatient(patient: PatientOption | string | null): string {
    return typeof patient === 'string' ? patient : (patient?.name ?? '');
  }
  onPatientInput(value: PatientOption | string): void {
    if (typeof value !== 'string') return;
    this.patientId = '';
    this.clearClinicalData();
    const query = value.trim();
    if (query.length >= 2) this.loadPatients(query);
    else this.patients.set([]);
  }
  choosePatient(patient: PatientOption): void {
    this.patientInput = patient;
    this.patientId = patient.globalId;
    this.selectPatient();
  }

  selectPatient(): void {
    this.clearClinicalData();
    this.error.set('');
    const membership = this.membership();
    if (!membership || !this.patientId || !this.clinicUnitId) return;
    this.clinical.load(membership.organizationId, this.clinicUnitId, this.patientId).subscribe({
      next: (data) => {
        this.encounters.set(data.encounters);
        this.chart.set(data.chart);
        this.history.set(data.history);
      },
      error: (error) => this.fail(error, 'LOAD'),
    });
  }

  editDraft(encounter: ClinicalEncounter): void {
    this.selectedEncounter.set(encounter);
    this.narrative = encounter.narrative;
    this.administrativeNote = encounter.administrativeNote ?? '';
    this.amendmentReason = '';
  }
  cancelEdit(): void {
    this.selectedEncounter.set(null);
    this.narrative = '';
    this.administrativeNote = '';
    this.amendmentReason = '';
  }

  saveDraft(): void {
    const membership = this.membership();
    if (!membership || !this.patientId || !this.narrative.trim()) return;
    const selected = this.selectedEncounter();
    const payload = {
      clinicUnitId: this.clinicUnitId,
      patientId: this.patientId,
      careAt: selected?.careAt ?? new Date().toISOString(),
      careTimezone: selected?.careTimezone ?? Intl.DateTimeFormat().resolvedOptions().timeZone,
      narrative: this.narrative.trim(),
      administrativeNote: this.administrativeNote.trim() || null,
    };
    const request = selected
      ? this.clinical.updateEncounter(membership.organizationId, selected.globalId, {
          ...payload,
          version: selected.version,
        })
      : this.clinical.createEncounter(membership.organizationId, payload);
    request.subscribe({
      next: () => {
        this.cancelEdit();
        this.selectPatient();
      },
      error: (error) => this.fail(error, 'SAVE'),
    });
  }

  finalize(encounter: ClinicalEncounter): void {
    const membership = this.membership();
    if (!membership) return;
    this.clinical
      .finalizeEncounter(membership.organizationId, this.clinicUnitId, encounter.globalId)
      .subscribe({ next: () => this.selectPatient(), error: (error) => this.fail(error, 'SAVE') });
  }
  loadAmendments(encounter: ClinicalEncounter): void {
    const membership = this.membership();
    if (!membership) return;
    this.selectedEncounter.set(encounter);
    this.clinical
      .amendments(membership.organizationId, this.clinicUnitId, encounter.globalId)
      .subscribe({
        next: (records) => this.amendments.set(records),
        error: (error) => this.fail(error, 'LOAD'),
      });
  }
  createAmendment(): void {
    const membership = this.membership();
    const original = this.selectedEncounter();
    if (
      !membership ||
      original?.status !== 'FINAL' ||
      !this.narrative.trim() ||
      !this.amendmentReason.trim()
    )
      return;
    this.clinical
      .amendEncounter(membership.organizationId, original.globalId, {
        clinicUnitId: this.clinicUnitId,
        careAt: new Date().toISOString(),
        careTimezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        narrative: this.narrative.trim(),
        administrativeNote: this.administrativeNote.trim() || null,
        reason: this.amendmentReason.trim(),
      })
      .subscribe({
        next: () => {
          this.cancelEdit();
          this.selectPatient();
        },
        error: (error) => this.fail(error, 'SAVE'),
      });
  }

  recordFinding(): void {
    const membership = this.membership();
    if (!membership || !this.patientId || !this.toothCode) return;
    this.clinical
      .createFinding(membership.organizationId, {
        clinicUnitId: this.clinicUnitId,
        patientId: this.patientId,
        toothCode: this.toothCode,
        surface: this.surface,
        condition: this.condition,
        observedAt: new Date().toISOString(),
        observationTimezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        clinicalNote: this.clinicalNote.trim() || null,
        replacementForId: this.replacementForId || null,
      })
      .subscribe({
        next: () => {
          this.toothCode = '';
          this.clinicalNote = '';
          this.replacementForId = '';
          this.selectPatient();
        },
        error: (error) => this.fail(error, 'SAVE'),
      });
  }
  voidFinding(finding: OdontogramFinding): void {
    const membership = this.membership();
    const reason = this.voidReason.trim();
    if (!membership || !reason) return;
    this.clinical
      .voidFinding(
        membership.organizationId,
        this.clinicUnitId,
        finding.globalId,
        reason,
        finding.version,
      )
      .subscribe({
        next: () => {
          this.voidReason = '';
          this.replacementFor(finding);
          this.selectPatient();
        },
        error: (error) => this.fail(error, 'SAVE'),
      });
  }
  replacementFor(finding: OdontogramFinding): void {
    this.replacementForId = finding.globalId;
    this.toothCode = finding.toothCode;
    this.surface = finding.surface;
  }

  private clearClinicalData(): void {
    this.encounters.set([]);
    this.chart.set([]);
    this.history.set([]);
    this.amendments.set([]);
    this.selectedEncounter.set(null);
  }
  private fail(error: unknown, fallback: 'LOAD' | 'SAVE'): void {
    const code = error instanceof GraphQlUserError ? error.code : '';
    const key = this.errorKey(code, fallback);
    this.error.set(this.t(`ERROR.${key}`));
  }
  private errorKey(code: string, fallback: 'LOAD' | 'SAVE'): string {
    if (code.startsWith('AUTHORIZATION.')) {
      return 'FORBIDDEN';
    }
    if (code === 'CONFLICT') {
      return 'STALE_OR_FINAL';
    }
    if (code === 'RESOURCE.NOT_FOUND') {
      return 'SCOPE';
    }
    return fallback;
  }
  private t(key: string): string {
    return this.translate.instant(`CLINICAL.${key}`);
  }
}
