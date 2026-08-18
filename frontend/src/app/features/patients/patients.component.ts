import { Component, inject } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { distinctUntilChanged, forkJoin } from 'rxjs';
import { PageResponse } from '../../core/models';
import { DataTableColumn } from '../../shared/data-table/data-table.models';
import {
  FilterAutocompleteEvent,
  FilterDefinition,
  FilterOption,
} from '../../shared/filters/filter.models';
import { RESOURCE_PAGE_IMPORTS } from '../resource-support/resource-page.imports';
import {
  ResourceListController,
  ResourceRecord,
} from '../resource-support/resource-list.controller';
import { PatientApiService } from './patient-api.service';
import { PatientMutationGraphqlService } from './patient-mutation-graphql.service';
import {
  PATIENT_FIELDS,
  PatientFormValues,
  PatientRecord,
  patientRequest,
  patientToForm,
} from './patient.models';
import { PatientDetailsDialog } from './patient-details-dialog/patient-details-dialog.component';
import { PatientFormDialog } from './patient-form-dialog/patient-form-dialog.component';
import { TranslateService } from '@ngx-translate/core';

const COLUMNS: readonly DataTableColumn[] = [
  { key: 'name', label: 'PATIENTS.TABLE.NAME', sortable: true },
  { key: 'identificationNumber', label: 'PATIENTS.TABLE.NATIONAL_ID', sortable: true },
  { key: 'gender', label: 'PATIENTS.TABLE.GENDER', sortable: true },
  { key: 'active', label: 'PATIENTS.TABLE.STATUS', sortable: true },
  { key: 'actions', label: '' },
];
export const PATIENT_FILTERS: readonly FilterDefinition[] = [
  { key: 'name', label: 'PATIENTS.FILTER.NAME', type: 'autocomplete' },
  {
    key: 'active',
    label: 'PATIENTS.FILTER.STATUS',
    type: 'select',
    options: [
      { value: 'true', label: 'PATIENTS.FILTER.ACTIVE' },
      { value: 'false', label: 'PATIENTS.FILTER.INACTIVE' },
    ],
  },
  {
    key: 'specialityId',
    label: 'PATIENTS.FILTER.SPECIALITY',
    type: 'autocomplete',
    selectionRequired: true,
  },
  {
    key: 'birthDate',
    label: 'PATIENTS.FILTER.BIRTH_DATE',
    type: 'date',
    placement: 'advanced',
    dateStart: '1992-01-01',
  },
  {
    key: 'gender',
    label: 'PATIENTS.FILTER.GENDER',
    type: 'select',
    placement: 'advanced',
    options: [
      { value: 'FEMALE', label: 'PATIENTS.FILTER.FEMALE' },
      { value: 'MALE', label: 'PATIENTS.FILTER.MALE' },
      { value: 'OTHER', label: 'PATIENTS.FILTER.OTHER' },
    ],
  },
  { key: 'taxId', label: 'PATIENTS.FILTER.TAX_ID', type: 'autocomplete', placement: 'advanced' },
  {
    key: 'identificationType',
    label: 'PATIENTS.FILTER.IDENTIFICATION_TYPE',
    type: 'select',
    placement: 'advanced',
    options: [
      { value: 'NATIONAL_ID_CARD', label: 'PATIENTS.FILTER.NATIONAL_ID' },
      { value: 'PASSPORT', label: 'PATIENTS.FILTER.PASSPORT' },
    ],
  },
  {
    key: 'nationalityCode',
    label: 'PATIENTS.FILTER.NATIONALITY',
    type: 'select',
    placement: 'advanced',
  },
  {
    key: 'addressId',
    label: 'PATIENTS.FILTER.ADDRESS',
    type: 'autocomplete',
    selectionRequired: true,
    placement: 'advanced',
  },
];

@Component({
  selector: 'app-patients',
  imports: [...RESOURCE_PAGE_IMPORTS],
  templateUrl: '../resource-support/resource-page.component.html',
  styleUrl: '../resource-support/resource-page.component.scss',
})
export class PatientsComponent extends ResourceListController {
  private readonly api = inject(PatientApiService);
  private readonly mutations = inject(PatientMutationGraphqlService);
  readonly activeKey = 'patients';
  readonly title = 'MODULES.PATIENTS';
  readonly description = 'MODULES.PATIENTS_DESCRIPTION';
  readonly translationPrefix = 'PATIENTS';
  override readonly filterAriaLabel = 'PATIENTS.FILTER.ARIA';
  readonly columns = COLUMNS;

  constructor() {
    super({
      endpoint: () => '',
      maintainPermission: 'MAINTAIN_PATIENTS',
      columns: COLUMNS,
      filters: PATIENT_FILTERS,
      identifier: (record) => String(record['globalId']),
      primary: (record) => String(record['name'] ?? '—'),
      canView: () => true,
      canDelete: (record) => record['active'] === true,
      actionLabels: { view: 'PATIENTS.VIEW', edit: 'PATIENTS.EDIT', delete: 'PATIENTS.DELETE' },
      cells: patientCells,
    });
    toObservable(this.auth.activeMembership)
      .pipe(
        distinctUntilChanged((previous, current) => previous?.id === current?.id),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((membership) => {
        if (!membership) return;
        this.loadNationalityOptions();
        this.load();
      });
  }

  override load(): void {
    const membership = this.auth.activeMembership();
    if (!membership) {
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.error.set(false);
    const params = this.tableQuery.toHttpParams({
      page: this.page(),
      size: this.pageSize(),
      sort: this.sort(),
      direction: this.sortDirection(),
      filters: this.filterValues(),
    });
    this.api.list(membership, params).subscribe({
      next: (response: PageResponse<PatientRecord>) => {
        this.records.set(response.content);
        this.totalElements.set(response.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }
  override updateAutocomplete(event: FilterAutocompleteEvent): void {
    const membership = this.auth.activeMembership();
    if (!membership) return;
    const { filter, query } = event;
    this.filterDisplayValues.update((values) => ({ ...values, [filter.key]: query }));
    this.updateFilter({ key: filter.key, value: filter.selectionRequired ? '' : query });
    this.api
      .filterOptions(membership, filter.key, query)
      .subscribe({
        next: (options) =>
          this.filterOptions.update((values) => ({ ...values, [filter.key]: options })),
        error: () => this.filterOptions.update((values) => ({ ...values, [filter.key]: [] })),
      });
  }
  override create(): void {
    this.openEditor();
  }
  protected override edit(record: ResourceRecord): void {
    this.openEditor(record);
  }
  protected override view(record: ResourceRecord): void {
    this.api.specialities().subscribe({
      next: (response) => {
        const localized = new Map(response.content.map((item) => [item.id, item]));
        const assigned = Array.isArray(record['specialities'])
          ? (record['specialities'] as ResourceRecord[])
          : [];
        this.openDetails({
          ...record,
          specialities: assigned.map((item) => localized.get(Number(item['id'])) ?? item),
        });
      },
      error: () => this.openDetails(record),
    });
  }
  protected override remove(record: ResourceRecord): void {
    const membership = this.auth.activeMembership();
    if (
      !membership ||
      !confirm(
        this.translate.instant('RESOURCE.DELETE_CONFIRM', { name: String(record['name'] ?? '—') }),
      )
    )
      return;
    this.api
      .deactivate(membership, String(record['globalId']))
      .subscribe({ next: () => this.load(), error: () => this.error.set(true) });
  }
  private loadNationalityOptions(): void {
    const membership = this.auth.activeMembership();
    if (!membership) return;
    this.api.countries().subscribe({
      next: (response) => {
        const options = response.content.map((country) => ({
          value: country.code,
          label: `${this.catalogNames.country(country)} (${country.code})`,
        }));
        this.filters.update((filters) =>
          filters.map((filter) =>
            filter.key === 'nationalityCode' ? { ...filter, options } : filter,
          ),
        );
      },
      error: () =>
        this.filters.update((filters) =>
          filters.map((filter) =>
            filter.key === 'nationalityCode'
              ? { ...filter, options: [] as FilterOption[] }
              : filter,
          ),
        ),
    });
  }
  private openEditor(record?: ResourceRecord): void {
    forkJoin({
      specialities: this.api.specialities(),
      countries: this.api.countries(),
      administrativeDivisions: this.api.administrativeDivisions(),
    }).subscribe({
      next: (response) =>
        this.dialog
          .open(PatientFormDialog, {
            width: '760px',
            maxWidth: '94vw',
            autoFocus: 'first-tabbable',
            data: {
              fields: [...PATIENT_FIELDS],
              values: record ? patientToForm(record) : undefined,
              specialities: response.specialities.content,
              countries: response.countries.content,
              administrativeDivisions: response.administrativeDivisions.content,
              recordType: 'patients',
              translationKey: 'PATIENTS',
            },
          })
          .afterClosed()
          .subscribe((values?: PatientFormValues) => {
            if (!values) return;
            const membership = this.auth.activeMembership();
            if (!membership) return;
            const request = record
              ? this.mutations.update(membership, String(record['globalId']), patientRequest(values) as Record<string, unknown>)
              : this.api.create(membership, patientRequest(values));
            request.subscribe({ next: () => this.load(), error: () => this.error.set(true) });
          }),
      error: () => this.error.set(true),
    });
  }
  private openDetails(record: ResourceRecord): void {
    this.dialog.open(PatientDetailsDialog, {
      width: '680px',
      maxWidth: '94vw',
      autoFocus: 'dialog',
      data: record,
    });
  }
}

function patientCells(
  record: ResourceRecord,
  _catalogNames: unknown,
  translate: TranslateService,
): Readonly<Record<string, string>> {
  const gender = String(record['gender'] ?? 'OTHER');
  return {
    name: String(record['name'] ?? '—'),
    identificationNumber: String(record['identificationNumber'] ?? '—'),
    gender: translate.instant(`PATIENTS.FILTER.${gender}`),
    active: translate.instant(
      record['active'] ? 'PATIENTS.FILTER.ACTIVE' : 'PATIENTS.FILTER.INACTIVE',
    ),
  };
}
