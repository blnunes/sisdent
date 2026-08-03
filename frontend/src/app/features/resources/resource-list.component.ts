import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PageResponse, Permission } from '../../core/models';
import { AuthService } from '../../core/auth.service';
import { TableQueryService } from '../../core/table-query.service';
import { TranslatePipe } from '@ngx-translate/core';
import { distinctUntilChanged, forkJoin } from 'rxjs';
import { AppHeaderComponent } from '../../shared/app-header.component';
import { ModuleNavigationComponent } from '../../shared/module-navigation.component';
import { PatientDetailsDialog } from './patient-details-dialog.component';

type FilterOption = { value: string; label: string };
type ResourceFilter = {
  key: string;
  label: string;
  type: 'text' | 'select' | 'date' | 'number' | 'autocomplete';
  options?: FilterOption[];
  selectionRequired?: boolean;
  placement?: 'primary' | 'advanced';
};
type ResourceConfig = {
  key: string;
  endpoint: string;
  title: string;
  description: string;
  maintainPermission?: Permission;
  filters?: ResourceFilter[];
};
type SelectOption = { value: string; label: string };
type SpecialityOption = { id: number; name: string };
type ProcedureOption = { id?: number; name: string };
type AddressOption = {
  id: number;
  street: string;
  district?: string;
  city: string;
  additionalInfo?: string;
  block?: string;
  postalCode?: string;
  administrativeDivision?: { name: string; code: string; type: string };
  country: { code: string };
};
type CountryOption = { code: string; name: string };
type AdministrativeDivisionOption = {
  id: number;
  name: string;
  code: string;
  type: string;
  country: { code: string };
};
type Field = {
  key: string;
  label: string;
  required?: boolean;
  options?: readonly SelectOption[];
  type?: 'date';
  section?: string;
  fullWidth?: boolean;
};
type FormValues = Record<string, string>;
type FormSchema = {
  fields: Field[];
  toRequest: (values: FormValues) => unknown;
  fromRecord: (record: Record<string, unknown>) => FormValues;
};
type Column = { key: string; label: string; sortable?: boolean };

const TABLE_COLUMNS: Record<string, Column[]> = {
  patients: [
    { key: 'name', label: 'RESOURCE.TABLE.NAME', sortable: true },
    { key: 'identificationNumber', label: 'RESOURCE.TABLE.NATIONAL_ID', sortable: true },
    { key: 'gender', label: 'RESOURCE.TABLE.GENDER', sortable: true },
    { key: 'active', label: 'RESOURCE.TABLE.STATUS', sortable: true },
    { key: 'actions', label: '' },
  ],
  specialities: [
    { key: 'name', label: 'Name', sortable: true },
    { key: 'procedures', label: 'Procedures' },
    { key: 'actions', label: '' },
  ],
  addresses: [
    { key: 'street', label: 'Street', sortable: true },
    { key: 'district', label: 'District', sortable: true },
    { key: 'postalCode', label: 'Postal code', sortable: true },
    { key: 'administrativeDivision', label: 'Administrative division' },
    { key: 'country', label: 'Country' },
    { key: 'actions', label: '' },
  ],
  countries: [
    { key: 'name', label: 'Name', sortable: true },
    { key: 'code', label: 'ISO code', sortable: true },
    { key: 'continent', label: 'Continent', sortable: true },
    { key: 'actions', label: '' },
  ],
  administrativeDivisions: [
    { key: 'name', label: 'Name', sortable: true },
    { key: 'code', label: 'Code', sortable: true },
    { key: 'type', label: 'Type', sortable: true },
    { key: 'country', label: 'Country' },
    { key: 'actions', label: '' },
  ],
};

const FIELDS = {
  administrativeDivision: [
    { key: 'name', label: 'Name', required: true },
    { key: 'code', label: 'Code', required: true },
    { key: 'type', label: 'Type', required: true },
    { key: 'countryCode', label: 'Country code', required: true },
  ],
  country: [
    { key: 'name', label: 'Name', required: true },
    { key: 'code', label: 'ISO code', required: true },
    { key: 'continent', label: 'Continent', required: true },
  ],
  address: [
    { key: 'street', label: 'Street', required: true },
    { key: 'district', label: 'District' },
    { key: 'city', label: 'City', required: true },
    { key: 'additionalInfo', label: 'Additional information' },
    { key: 'block', label: 'Block' },
    { key: 'postalCode', label: 'Postal code', required: true },
    { key: 'administrativeDivisionName', label: 'Administrative division name' },
    { key: 'administrativeDivisionCode', label: 'Administrative division code' },
    { key: 'administrativeDivisionType', label: 'Administrative division type' },
    { key: 'countryCode', label: 'Country code', required: true },
  ],
  speciality: [
    { key: 'name', label: 'Name', required: true },
    { key: 'procedures', label: 'Procedures', required: true, fullWidth: true },
  ],
  patient: [
    {
      key: 'name',
      label: 'Full name',
      required: true,
      section: 'Personal details',
      fullWidth: true,
    },
    {
      key: 'birthDate',
      label: 'Birth date',
      required: true,
      type: 'date',
      section: 'Personal details',
    },
    {
      key: 'active',
      label: 'Status',
      required: true,
      section: 'Personal details',
      options: [
        { value: 'true', label: 'Active' },
        { value: 'false', label: 'Inactive' },
      ],
    },
    {
      key: 'gender',
      label: 'Gender',
      required: true,
      section: 'Personal details',
      options: [
        { value: 'FEMALE', label: 'Female' },
        { value: 'MALE', label: 'Male' },
        { value: 'OTHER', label: 'Other' },
      ],
    },
    { key: 'taxId', label: 'Tax ID', section: 'Personal details' },
    {
      key: 'identificationType',
      label: 'Identification type',
      required: true,
      section: 'Identification',
      options: [
        { value: 'NATIONAL_ID_CARD', label: 'National ID card' },
        { value: 'PASSPORT', label: 'Passport' },
      ],
    },
    {
      key: 'identificationNumber',
      label: 'Identification number',
      required: true,
      section: 'Identification',
    },
    {
      key: 'documentIssuerCountryCode',
      label: 'Document issuer country code',
      required: true,
      section: 'Identification',
    },
    {
      key: 'nationalityCode',
      label: 'Nationality country code',
      required: true,
      section: 'Nationality',
    },
    { key: 'countryCode', label: 'Address country code', required: true, section: 'Address' },
    { key: 'postalCode', label: 'Postal code', required: true, section: 'Address' },
    { key: 'street', label: 'Street', required: true, section: 'Address', fullWidth: true },
    { key: 'district', label: 'District', section: 'Address' },
    { key: 'city', label: 'City', required: true, section: 'Address' },
    { key: 'additionalInfo', label: 'Additional information', section: 'Address' },
    { key: 'block', label: 'Block', section: 'Address' },
    { key: 'administrativeDivisionName', label: 'Administrative division name', section: 'Address' },
    { key: 'administrativeDivisionCode', label: 'Administrative division code', required: true, section: 'Address' },
    { key: 'administrativeDivisionType', label: 'Administrative division type', section: 'Address' },
    { key: 'specialityIds', label: 'Specialities', section: 'Specialities', fullWidth: true },
  ],
} as const;

const addressValues = (record: Record<string, unknown>): FormValues => {
  const division = record['administrativeDivision'] as Record<string, unknown> | undefined;
  const country = record['country'] as Record<string, unknown> | undefined;
  return {
    street: String(record['street'] ?? ''),
    district: String(record['district'] ?? ''),
    city: String(record['city'] ?? ''),
    additionalInfo: String(record['additionalInfo'] ?? ''),
    block: String(record['block'] ?? ''),
    postalCode: String(record['postalCode'] ?? ''),
    administrativeDivisionName: String(division?.['name'] ?? ''),
    administrativeDivisionCode: String(division?.['code'] ?? ''),
    administrativeDivisionType: String(division?.['type'] ?? ''),
    countryCode: String(country?.['code'] ?? ''),
    addressId: String(record['id'] ?? ''),
  };
};

const addressRequest = (value: FormValues) => ({
  street: value['street'],
  district: value['district'],
  city: value['city'],
  additionalInfo: value['additionalInfo'] || null,
  block: value['block'] || null,
  postalCode: value['postalCode'],
  administrativeDivision: value['administrativeDivisionCode']
    ? {
        name: value['administrativeDivisionName'],
        code: value['administrativeDivisionCode'],
        type: value['administrativeDivisionType'],
      }
    : null,
  countryCode: value['countryCode'],
});
const parseProcedures = (value: string): ProcedureOption[] => {
  try {
    const procedures = JSON.parse(value) as unknown;
    if (!Array.isArray(procedures)) return [];
    return procedures.flatMap((procedure) => {
      if (!procedure || typeof procedure !== 'object') return [];
      const { id, name } = procedure as Record<string, unknown>;
      const normalizedName = String(name ?? '').trim();
      if (!normalizedName) return [];
      return [{ ...(typeof id === 'number' ? { id } : {}), name: normalizedName }];
    });
  } catch {
    return [];
  }
};
const SCHEMAS: Record<string, FormSchema> = {
  administrativeDivisions: {
    fields: [...FIELDS.administrativeDivision],
    toRequest: (value) => value,
    fromRecord: (record) => ({
      name: String(record['name'] ?? ''),
      code: String(record['code'] ?? ''),
      type: String(record['type'] ?? ''),
      countryCode: String(
        (record['country'] as Record<string, unknown> | undefined)?.['code'] ?? '',
      ),
    }),
  },
  countries: {
    fields: [...FIELDS.country],
    toRequest: (value) => value,
    fromRecord: (record) => ({
      name: String(record['name'] ?? ''),
      code: String(record['code'] ?? ''),
      continent: String(record['continent'] ?? ''),
    }),
  },
  addresses: { fields: [...FIELDS.address], toRequest: addressRequest, fromRecord: addressValues },
  specialities: {
    fields: [...FIELDS.speciality],
    toRequest: (value) => ({
      name: value['name'],
      procedures: parseProcedures(value['procedures']),
    }),
    fromRecord: (record) => ({
      name: String(record['name'] ?? ''),
      procedures: JSON.stringify(record['procedures'] ?? []),
    }),
  },
  patients: {
    fields: [...FIELDS.patient],
    toRequest: (value) => ({
      name: value['name'],
      birthDate: value['birthDate'],
      active: value['active'] === 'true',
      gender: value['gender'],
      taxId: value['taxId'] || null,
      identificationType: value['identificationType'],
      identificationNumber: value['identificationNumber'],
      documentIssuerCountryCode: value['documentIssuerCountryCode'],
      nationalityCode: value['nationalityCode'],
      ...(value['addressId'] ? { addressId: Number(value['addressId']) } : { address: addressRequest(value) }),
      specialityIds: value['specialityIds']
        .split(',')
        .map((id) => Number(id.trim()))
        .filter((id) => Number.isInteger(id) && id > 0),
    }),
    fromRecord: (record) => ({
      ...addressValues(record['address'] as Record<string, unknown>),
      name: String(record['name'] ?? ''),
      birthDate: String(record['birthDate'] ?? ''),
      active: String(record['active'] ?? true),
      gender: String(record['gender'] ?? ''),
      taxId: String(record['taxId'] ?? ''),
      identificationType: String(record['identificationType'] ?? ''),
      identificationNumber: String(record['identificationNumber'] ?? ''),
      documentIssuerCountryCode: String(
        (record['documentIssuerCountry'] as Record<string, unknown> | undefined)?.['code'] ?? '',
      ),
      nationalityCode: String(
        (record['nationality'] as Record<string, unknown> | undefined)?.['code'] ?? '',
      ),
      specialityIds: ((record['specialities'] as Record<string, unknown>[] | undefined) ?? [])
        .map((speciality) => String(speciality['id']))
        .join(', '),
    }),
  },
};

@Component({
  selector: 'app-resource-list',
  imports: [
    MatAutocompleteModule,
    MatButtonModule,
    MatCardModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatNativeDateModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSidenavModule,
    MatSortModule,
    MatTableModule,
    MatTooltipModule,
    TranslatePipe,
    AppHeaderComponent,
    ModuleNavigationComponent,
  ],
  templateUrl: './resource-list.component.html',
  styleUrl: './resource-list.component.scss',
})
export class ResourceListComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  private readonly dialog = inject(MatDialog);
  private readonly tableQuery = inject(TableQueryService);
  private readonly destroyRef = inject(DestroyRef);
  readonly auth = inject(AuthService);
  readonly config = this.route.snapshot.data as ResourceConfig;
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly records = signal<Record<string, unknown>[]>([]);
  readonly page = signal(0);
  readonly pageSize = signal(10);
  readonly totalElements = signal(0);
  readonly sort = signal('id');
  readonly sortDirection = signal<'asc' | 'desc'>('asc');
  readonly filterValues = signal<Record<string, string>>({});
  readonly filterDisplayValues = signal<Record<string, string>>({});
  readonly filterOptions = signal<Record<string, FilterOption[]>>({});
  readonly nationalityOptions = signal<FilterOption[]>([]);
  readonly showAdvancedFilters = signal(false);
  readonly title = computed(() => this.config.title);
  readonly schema = computed(() => SCHEMAS[this.config.key]);
  readonly fields = computed(() => this.schema()?.fields ?? []);
  readonly canManage = computed(
    () =>
      !!this.config.maintainPermission && this.auth.hasPermission(this.config.maintainPermission),
  );
  readonly canView = computed(
    () =>
      this.config.key === 'patients' &&
      this.auth.hasAnyPermission('READ_PATIENTS', 'MAINTAIN_PATIENTS'),
  );
  readonly columns = computed(() => TABLE_COLUMNS[this.config.key] ?? []);
  readonly displayedColumns = computed(() => this.columns().map((column) => column.key));
  readonly filters = computed(() =>
    (this.config.filters ?? []).map((filter) =>
      filter.key === 'nationalityCode'
        ? { ...filter, options: this.nationalityOptions() }
        : filter,
    ),
  );
  readonly primaryFilters = computed(() =>
    this.filters().filter((filter) => filter.placement !== 'advanced'),
  );
  readonly advancedFilters = computed(() =>
    this.filters().filter((filter) => filter.placement === 'advanced'),
  );

  constructor() {
    if (this.config.key !== 'patients') {
      this.load();
      return;
    }
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

  load(): void {
    this.loading.set(true);
    this.error.set(false);
    const query = {
      page: this.page(),
      size: this.pageSize(),
      sort: this.sort(),
      direction: this.sortDirection(),
      filters: this.filterValues(),
    };
    this.http
      .get<PageResponse<Record<string, unknown>>>(this.config.endpoint, {
        params: this.tableQuery.toHttpParams(query),
      })
      .subscribe({
        next: (response) => {
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

  changePage(event: PageEvent): void {
    this.page.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  changeSort(sort: Sort): void {
    const next = this.tableQuery.nextSort(
      {
        page: this.page(),
        size: this.pageSize(),
        sort: this.sort(),
        direction: this.sortDirection(),
      },
      sort,
    );
    this.page.set(next.page);
    this.sort.set(next.sort);
    this.sortDirection.set(next.direction);
    this.load();
  }

  updateFilter(key: string, value: string): void {
    this.filterValues.update((filters) => ({ ...filters, [key]: value }));
  }

  hasFilterValue(key: string): boolean {
    return !!(this.filterValues()[key] || this.filterDisplayValues()[key]);
  }

  clearFilter(key: string): void {
    this.filterValues.update((filters) => ({ ...filters, [key]: '' }));
    this.filterDisplayValues.update((values) => ({ ...values, [key]: '' }));
    this.filterOptions.update((options) => ({ ...options, [key]: [] }));
    this.refreshFilteredResults();
  }

  updateDateFilter(key: string, value: Date | null): void {
    this.updateFilter(
      key,
      value
        ? [
            value.getFullYear(),
            String(value.getMonth() + 1).padStart(2, '0'),
            String(value.getDate()).padStart(2, '0'),
          ].join('-')
        : '',
    );
    this.refreshFilteredResults();
  }

  updateSelectFilter(key: string, value: string): void {
    this.updateFilter(key, value);
    this.refreshFilteredResults();
  }

  dateValue(value: string | undefined): Date | null {
    return value ? new Date(`${value}T00:00:00`) : null;
  }

  updateAutocomplete(filter: ResourceFilter, value: string): void {
    this.filterDisplayValues.update((values) => ({ ...values, [filter.key]: value }));
    this.updateFilter(filter.key, filter.selectionRequired ? '' : value);
    this.http
      .get<FilterOption[]>(`${this.config.endpoint}/filter-options`, {
        params: { field: filter.key, query: value },
      })
      .subscribe({
        next: (options) =>
          this.filterOptions.update((current) => ({ ...current, [filter.key]: options })),
        error: () => this.filterOptions.update((current) => ({ ...current, [filter.key]: [] })),
      });
  }

  selectFilterOption(key: string, option: FilterOption): void {
    this.filterDisplayValues.update((values) => ({ ...values, [key]: option.label }));
    this.updateFilter(key, option.value);
    this.refreshFilteredResults();
  }

  applyFilters(): void {
    this.refreshFilteredResults();
  }

  private refreshFilteredResults(): void {
    this.page.set(0);
    this.load();
  }

  toggleAdvancedFilters(): void {
    this.showAdvancedFilters.update((value) => !value);
  }

  clearFilters(): void {
    this.filterValues.set({});
    this.filterDisplayValues.set({});
    this.filterOptions.set({});
    this.page.set(0);
    this.load();
  }

  private loadNationalityOptions(): void {
    this.http
      .get<FilterOption[]>(`${this.config.endpoint}/filter-options`, {
        params: { field: 'nationalityCode' },
      })
      .subscribe({
        next: (options) => this.nationalityOptions.set(options),
        error: () => this.nationalityOptions.set([]),
      });
  }

  create(): void {
    this.openEditor();
  }
  edit(record: Record<string, unknown>): void {
    this.openEditor(record);
  }
  view(record: Record<string, unknown>): void {
    this.dialog.open(PatientDetailsDialog, {
      width: '680px',
      maxWidth: '94vw',
      autoFocus: 'dialog',
      data: record,
    });
  }
  canDelete(record: Record<string, unknown>): boolean {
    return this.config.key !== 'patients' || record['active'] === true;
  }
  remove(record: Record<string, unknown>): void {
    if (!confirm(`Delete ${this.primary(record)}?`)) return;
    this.http
      .delete(`${this.config.endpoint}/${this.resourceIdentifier(record)}`)
      .subscribe({ next: () => this.load(), error: () => this.error.set(true) });
  }

  private openEditor(record?: Record<string, unknown>): void {
    if (!this.fields().length) return;
    const schema = this.schema();
    if (!schema) return;
    if (this.config.key === 'countries') {
      this.http.get<string[]>('/api/countries/continents').subscribe({
        next: (options) => this.openDialog(schema, record, options),
        error: () => this.error.set(true),
      });
      return;
    }
    if (this.config.key === 'patients') {
      forkJoin({
        specialities: this.http.get<PageResponse<SpecialityOption>>('/api/specialities', {
          params: { page: '0', size: '100', sort: 'name', direction: 'asc' },
        }),
        countries: this.http.get<PageResponse<CountryOption>>('/api/countries', {
          params: { page: '0', size: '100', sort: 'name', direction: 'asc' },
        }),
        administrativeDivisions: this.http.get<PageResponse<AdministrativeDivisionOption>>('/api/administrative-divisions', {
          params: { page: '0', size: '100', sort: 'name', direction: 'asc' },
        }),
      })
        .subscribe({
          next: (response) => this.openDialog(
            schema,
            record,
            undefined,
            response.specialities.content,
            response.countries.content,
            response.administrativeDivisions.content,
          ),
          error: () => this.error.set(true),
        });
      return;
    }
    this.openDialog(schema, record);
  }

  private openDialog(
    schema: FormSchema,
    record?: Record<string, unknown>,
    continents?: string[],
    specialities?: SpecialityOption[],
    countries?: CountryOption[],
    administrativeDivisions?: AdministrativeDivisionOption[],
  ): void {
    const fields = schema.fields.map((field) =>
      field.key === 'continent'
        ? { ...field, options: continents?.map((value) => ({ value, label: value })) }
        : field,
    );
    this.dialog
      .open(ResourceFormDialog, {
        width: '760px',
        maxWidth: '94vw',
        autoFocus: 'first-tabbable',
        data: { fields, values: record ? schema.fromRecord(record) : undefined, specialities, countries, administrativeDivisions, resourceKey: this.config.key },
      })
      .afterClosed()
      .subscribe((value?: FormValues) => {
        if (!value) return;
        const request = record
          ? this.http.put(`${this.config.endpoint}/${this.resourceIdentifier(record)}`, schema.toRequest(value))
          : this.http.post(this.config.endpoint, schema.toRequest(value));
        request.subscribe({ next: () => this.load(), error: () => this.error.set(true) });
      });
  }

  private resourceIdentifier(record: Record<string, unknown>): unknown {
    return this.config.key === 'patients' ? record['globalId'] : record['id'];
  }

  closeMenu(drawer: MatSidenav): void {
    void drawer.close();
  }
  primary(record: Record<string, unknown>): string {
    return String(record['name'] ?? record['street'] ?? record['code'] ?? '—');
  }
  secondary(record: Record<string, unknown>): string {
    return Object.entries(record)
      .filter(
        ([key, value]) =>
          !['id', 'name', 'street'].includes(key) && value != null && typeof value !== 'object',
      )
      .map(([, value]) => String(value))
      .join(' · ');
  }
  value(record: Record<string, unknown>, key: string): string {
    if (key === 'primary') return this.primary(record);
    if (key === 'secondary') return this.secondary(record);
    if (key === 'nationality' || key === 'country' || key === 'administrativeDivision')
      return String((record[key] as Record<string, unknown> | undefined)?.['name'] ?? '—');
    if (key === 'address') {
      const address = record[key] as Record<string, unknown> | undefined;
      return address ? `${address['street']} · ${address['postalCode']}` : '—';
    }
    if (key === 'specialities')
      return ((record[key] as Record<string, unknown>[] | undefined) ?? [])
        .map((speciality) => String(speciality['name']))
        .join(', ');
    if (key === 'procedures')
      return ((record[key] as Record<string, unknown>[] | undefined) ?? [])
        .map((procedure) => String(procedure['name'] ?? ''))
        .filter(Boolean)
        .join(', ') || '—';
    if (key === 'active') return record[key] ? 'Active' : 'Inactive';
    return String(record[key] ?? '—');
  }
}

@Component({
  selector: 'app-resource-form-dialog',
  imports: [
    MatAutocompleteModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatNativeDateModule,
    MatSelectModule,
    MatTooltipModule,
    TranslatePipe,
  ],
  templateUrl: './resource-form-dialog.component.html',
  styleUrl: './resource-form-dialog.component.scss',
})
export class ResourceFormDialog {
  private readonly patientAddressFieldKeys = [
    'street',
    'district',
    'city',
    'additionalInfo',
    'block',
    'administrativeDivisionName',
    'administrativeDivisionCode',
    'administrativeDivisionType',
  ];
  readonly data = inject<{
    fields: Field[];
    values?: FormValues;
    specialities?: SpecialityOption[];
    countries?: CountryOption[];
    administrativeDivisions?: AdministrativeDivisionOption[];
    resourceKey: string;
  }>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<ResourceFormDialog, Record<string, string>>);
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  readonly form = this.fb.group(
    Object.fromEntries(
      this.data.fields.map((field) => [
        field.key,
        [this.initialValue(field), field.required ? Validators.required : []],
      ]),
    ),
  );
  readonly selectedSpecialities = signal<SpecialityOption[]>(this.initialSpecialities());
  readonly specialityToAdd = signal<number | null>(null);
  readonly selectedProcedures = signal<ProcedureOption[]>(this.initialProcedures());
  readonly procedureNameToAdd = signal('');
  readonly postalCodeSuggestions = signal<AddressOption[]>([]);
  readonly selectedAddressId = signal(this.data.values?.['addressId'] ?? '');
  readonly isAddressLocked = computed(() => Boolean(this.selectedAddressId()));
  readonly selectedCountryCode = signal(this.data.values?.['countryCode'] ?? '');
  readonly availableAdministrativeDivisions = computed(() =>
    (this.data.administrativeDivisions ?? []).filter(
      (division) => division.country.code === this.selectedCountryCode(),
    ),
  );
  readonly countriesWithAdministrativeDivisions = computed(() => {
    const countryCodes = new Set((this.data.administrativeDivisions ?? []).map((division) => division.country.code));
    return (this.data.countries ?? []).filter((country) => countryCodes.has(country.code));
  });
  readonly availableSpecialities = computed(() => {
    const selectedIds = new Set(this.selectedSpecialities().map((speciality) => speciality.id));
    return (this.data.specialities ?? []).filter((speciality) => !selectedIds.has(speciality.id));
  });
  constructor() {
    if (this.isPatientForm() && this.isAddressLocked()) this.lockSelectedAddressFields();
  }
  sections(): string[] {
    return [...new Set(this.data.fields.map((field) => field.section ?? 'Record details'))];
  }
  fieldsIn(section: string): Field[] {
    return this.data.fields.filter((field) => (field.section ?? 'Record details') === section);
  }
  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const values = Object.fromEntries(
      this.data.fields.map((field) => {
        const value = this.form.get(field.key)?.value;
        return [
          field.key,
          field.key === 'specialityIds'
            ? this.selectedSpecialities()
                .map((speciality) => speciality.id)
                .join(',')
            : field.key === 'procedures'
              ? JSON.stringify(this.selectedProcedures())
              : value instanceof Date
              ? this.toIsoDate(value)
              : String(value ?? ''),
        ];
      }),
    );
    if (this.isPatientForm()) values['addressId'] = this.selectedAddressId();
    this.ref.close(values);
  }
  isPatientForm(): boolean {
    return this.data.resourceKey === 'patients';
  }
  isHiddenPatientAddressField(field: Field): boolean {
    return this.isPatientForm() &&
      (field.key === 'administrativeDivisionName' || field.key === 'administrativeDivisionType');
  }
  isPatientAddressField(field: Field): boolean {
    return this.isPatientForm() && field.section === 'Address';
  }
  onPostalCodeInput(postalCode: string): void {
    this.clearSelectedAddress();
    const countryCode = String(this.form.get('countryCode')?.value ?? '').trim().toUpperCase();
    const query = postalCode.trim();
    if (!/^[A-Z]{2}$/.test(countryCode) || query.length < 2) {
      this.postalCodeSuggestions.set([]);
      return;
    }
    this.http
      .get<AddressOption[]>('/api/addresses/postal-code-suggestions', { params: { countryCode, query } })
      .subscribe({ next: (addresses) => this.postalCodeSuggestions.set(addresses), error: () => this.postalCodeSuggestions.set([]) });
  }
  onCountryCodeInput(): void {
    this.selectedAddressId.set('');
    this.postalCodeSuggestions.set([]);
  }
  onCountrySelected(countryCode: string): void {
    this.selectedCountryCode.set(countryCode);
    this.clearSelectedAddress();
    this.postalCodeSuggestions.set([]);
    this.form.patchValue({
      postalCode: '',
      street: '',
      district: '',
      city: '',
      additionalInfo: '',
      block: '',
      administrativeDivisionCode: '',
      administrativeDivisionName: '',
      administrativeDivisionType: '',
    });
  }
  onAdministrativeDivisionSelected(code: string): void {
    const division = this.availableAdministrativeDivisions().find((option) => option.code === code);
    this.selectedAddressId.set('');
    this.form.patchValue({
      administrativeDivisionName: division?.name ?? '',
      administrativeDivisionType: division?.type ?? '',
    });
  }
  onAddressFieldInput(fieldKey: string): void {
    this.clearSelectedAddress();
    if (fieldKey === 'countryCode') this.postalCodeSuggestions.set([]);
  }
  selectPostalCodeAddress(address: AddressOption): void {
    this.selectedAddressId.set(String(address.id));
    this.selectedCountryCode.set(address.country.code);
    this.postalCodeSuggestions.set([]);
    this.form.patchValue({
      countryCode: address.country.code,
      postalCode: address.postalCode ?? '',
      street: address.street,
      district: address.district ?? '',
      city: address.city,
      additionalInfo: address.additionalInfo ?? '',
      block: address.block ?? '',
      administrativeDivisionName: address.administrativeDivision?.name ?? '',
      administrativeDivisionCode: address.administrativeDivision?.code ?? '',
      administrativeDivisionType: address.administrativeDivision?.type ?? '',
    });
    this.lockSelectedAddressFields();
  }
  postalCodeAddressLabel(address: AddressOption): string {
    return `${address.postalCode ?? '—'} — ${address.street}, ${address.city}`;
  }
  postalCodeDisplayValue(value: AddressOption | string | null): string {
    return typeof value === 'string' ? value : value?.postalCode ?? '';
  }
  private lockSelectedAddressFields(): void {
    this.patientAddressFieldKeys.forEach((key) => this.form.get(key)?.disable({ emitEvent: false }));
  }
  private clearSelectedAddress(): void {
    if (!this.selectedAddressId()) return;
    this.selectedAddressId.set('');
    this.patientAddressFieldKeys.forEach((key) => this.form.get(key)?.enable({ emitEvent: false }));
    this.form.patchValue({
      street: '',
      district: '',
      city: '',
      additionalInfo: '',
      block: '',
      administrativeDivisionCode: '',
      administrativeDivisionName: '',
      administrativeDivisionType: '',
    });
  }
  private initialValue(field: Field): string | Date | null {
    const value = this.data.values?.[field.key] ?? '';
    if (field.type !== 'date') return value;
    if (!value) return null;
    const [year, month, day] = value.split('-').map(Number);
    const date = new Date(year, month - 1, day);
    return Number.isInteger(year) &&
      Number.isInteger(month) &&
      Number.isInteger(day) &&
      date.getFullYear() === year &&
      date.getMonth() === month - 1 &&
      date.getDate() === day
      ? date
      : null;
  }
  addSpeciality(id: number): void {
    const speciality = (this.data.specialities ?? []).find((option) => option.id === id);
    if (speciality && !this.selectedSpecialities().some((option) => option.id === id)) {
      this.selectedSpecialities.update((current) => [...current, speciality]);
    }
    this.specialityToAdd.set(null);
    this.syncSpecialitiesControl();
  }
  removeSpeciality(id: number): void {
    this.selectedSpecialities.update((current) =>
      current.filter((speciality) => speciality.id !== id),
    );
    this.syncSpecialitiesControl();
  }
  setProcedureName(value: string): void {
    this.procedureNameToAdd.set(value);
  }
  addProcedure(): void {
    const name = this.procedureNameToAdd().trim();
    if (!name || this.selectedProcedures().some((procedure) => procedure.name.toLowerCase() === name.toLowerCase())) return;
    this.selectedProcedures.update((current) => [...current, { name }]);
    this.procedureNameToAdd.set('');
    this.syncProceduresControl();
  }
  removeProcedure(procedure: ProcedureOption): void {
    this.selectedProcedures.update((current) => current.filter((item) => item !== procedure));
    this.syncProceduresControl();
  }
  private initialSpecialities(): SpecialityOption[] {
    const ids = new Set(
      (this.data.values?.['specialityIds'] ?? '')
        .split(',')
        .map((value) => Number(value.trim()))
        .filter(Number.isInteger),
    );
    return (this.data.specialities ?? []).filter((speciality) => ids.has(speciality.id));
  }
  private syncSpecialitiesControl(): void {
    this.form.get('specialityIds')?.setValue(
      this.selectedSpecialities()
        .map((speciality) => speciality.id)
        .join(','),
    );
  }
  private initialProcedures(): ProcedureOption[] {
    return parseProcedures(this.data.values?.['procedures'] ?? '');
  }
  private syncProceduresControl(): void {
    this.form.get('procedures')?.setValue(JSON.stringify(this.selectedProcedures()));
  }
  private toIsoDate(value: Date): string {
    return [
      value.getFullYear(),
      String(value.getMonth() + 1).padStart(2, '0'),
      String(value.getDate()).padStart(2, '0'),
    ].join('-');
  }
}
