import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { PageResponse, Permission } from '../../core/models';
import { AuthService } from '../../core/auth.service';
import { TableQueryService } from '../../core/table-query.service';
import { TranslatePipe } from '@ngx-translate/core';
import { AppHeaderComponent } from '../../shared/app-header.component';
import { ModuleNavigationComponent } from '../../shared/module-navigation.component';

type ResourceConfig = { key: string; endpoint: string; title: string; description: string; maintainPermission?: Permission };
type Field = { key: string; label: string; required?: boolean; options?: string[] };
type FormValues = Record<string, string>;
type FormSchema = { fields: Field[]; toRequest: (values: FormValues) => unknown; fromRecord: (record: Record<string, unknown>) => FormValues };
type Column = { key: string; label: string; sortable?: boolean };

const TABLE_COLUMNS: Record<string, Column[]> = {
  patients: [{ key: 'name', label: 'Name', sortable: true }, { key: 'identificationNumber', label: 'National ID', sortable: true }, { key: 'gender', label: 'Gender', sortable: true }, { key: 'active', label: 'Status', sortable: true }, { key: 'nationality', label: 'Nationality' }, { key: 'address', label: 'Address' }, { key: 'specialities', label: 'Specialities' }, { key: 'actions', label: '' }],
  specialities: [{ key: 'name', label: 'Name', sortable: true }, { key: 'procedures', label: 'Procedures' }, { key: 'actions', label: '' }],
  addresses: [{ key: 'street', label: 'Street', sortable: true }, { key: 'district', label: 'District', sortable: true }, { key: 'postalCode', label: 'Postal code', sortable: true }, { key: 'state', label: 'State' }, { key: 'country', label: 'Country' }, { key: 'actions', label: '' }],
  countries: [{ key: 'name', label: 'Name', sortable: true }, { key: 'code', label: 'ISO code', sortable: true }, { key: 'continent', label: 'Continent', sortable: true }, { key: 'actions', label: '' }],
  states: [{ key: 'name', label: 'Name', sortable: true }, { key: 'abbreviation', label: 'Abbreviation', sortable: true }, { key: 'actions', label: '' }],
};

const FIELDS = {
  state: [{ key: 'name', label: 'Name', required: true }, { key: 'abbreviation', label: 'Abbreviation', required: true }],
  country: [{ key: 'name', label: 'Name', required: true }, { key: 'code', label: 'ISO code', required: true }, { key: 'continent', label: 'Continent', required: true }],
  address: [{ key: 'street', label: 'Street', required: true }, { key: 'district', label: 'District', required: true }, { key: 'additionalInfo', label: 'Additional information' }, { key: 'block', label: 'Block' }, { key: 'postalCode', label: 'Postal code', required: true }, { key: 'stateName', label: 'State name', required: true }, { key: 'stateAbbreviation', label: 'State abbreviation', required: true }, { key: 'countryCode', label: 'Country code', required: true }],
  speciality: [{ key: 'name', label: 'Name', required: true }, { key: 'procedures', label: 'Procedures (comma-separated)', required: true }],
  patient: [{ key: 'name', label: 'Name', required: true }, { key: 'birthDate', label: 'Birth date (YYYY-MM-DD)', required: true }, { key: 'active', label: 'Active (true or false)', required: true }, { key: 'gender', label: 'Gender (FEMALE, MALE or OTHER)', required: true }, { key: 'taxId', label: 'Tax ID', required: true }, { key: 'identificationType', label: 'Identification type (NATIONAL_ID or PASSPORT)', required: true }, { key: 'identificationNumber', label: 'Identification number', required: true }, { key: 'nationalityCode', label: 'Nationality country code', required: true }, { key: 'street', label: 'Street', required: true }, { key: 'district', label: 'District', required: true }, { key: 'additionalInfo', label: 'Additional information' }, { key: 'block', label: 'Block' }, { key: 'postalCode', label: 'Postal code', required: true }, { key: 'stateName', label: 'State name', required: true }, { key: 'stateAbbreviation', label: 'State abbreviation', required: true }, { key: 'countryCode', label: 'Address country code', required: true }, { key: 'specialityIds', label: 'Speciality IDs (comma-separated)', required: true }],
} as const;

const addressValues = (record: Record<string, unknown>): FormValues => {
  const state = record['state'] as Record<string, unknown> | undefined;
  const country = record['country'] as Record<string, unknown> | undefined;
  return { street: String(record['street'] ?? ''), district: String(record['district'] ?? ''), additionalInfo: String(record['additionalInfo'] ?? ''), block: String(record['block'] ?? ''), postalCode: String(record['postalCode'] ?? ''), stateName: String(state?.['name'] ?? ''), stateAbbreviation: String(state?.['abbreviation'] ?? ''), countryCode: String(country?.['code'] ?? '') };
};

const addressRequest = (value: FormValues) => ({ street: value['street'], district: value['district'], additionalInfo: value['additionalInfo'] || null, block: value['block'] || null, postalCode: value['postalCode'], state: { name: value['stateName'], abbreviation: value['stateAbbreviation'] }, countryCode: value['countryCode'] });
const SCHEMAS: Record<string, FormSchema> = {
  states: { fields: [...FIELDS.state], toRequest: value => value, fromRecord: record => ({ name: String(record['name'] ?? ''), abbreviation: String(record['abbreviation'] ?? '') }) },
  countries: { fields: [...FIELDS.country], toRequest: value => value, fromRecord: record => ({ name: String(record['name'] ?? ''), code: String(record['code'] ?? ''), continent: String(record['continent'] ?? '') }) },
  addresses: { fields: [...FIELDS.address], toRequest: addressRequest, fromRecord: addressValues },
  specialities: { fields: [...FIELDS.speciality], toRequest: value => ({ name: value['name'], procedures: value['procedures'].split(',').map(name => name.trim()).filter(Boolean).map(name => ({ name })) }), fromRecord: record => ({ name: String(record['name'] ?? ''), procedures: ((record['procedures'] as Record<string, unknown>[] | undefined) ?? []).map(procedure => String(procedure['name'])).join(', ') }) },
  patients: { fields: [...FIELDS.patient], toRequest: value => ({ name: value['name'], birthDate: value['birthDate'], active: value['active'] === 'true', gender: value['gender'], taxId: value['taxId'], identificationType: value['identificationType'], identificationNumber: value['identificationNumber'], nationalityCode: value['nationalityCode'], address: addressRequest(value), specialityIds: value['specialityIds'].split(',').map(id => Number(id.trim())).filter(Number.isInteger) }), fromRecord: record => ({ ...addressValues(record['address'] as Record<string, unknown>), name: String(record['name'] ?? ''), birthDate: String(record['birthDate'] ?? ''), active: String(record['active'] ?? true), gender: String(record['gender'] ?? ''), taxId: String(record['taxId'] ?? ''), identificationType: String(record['identificationType'] ?? ''), identificationNumber: String(record['identificationNumber'] ?? ''), nationalityCode: String((record['nationality'] as Record<string, unknown> | undefined)?.['code'] ?? ''), specialityIds: ((record['specialities'] as Record<string, unknown>[] | undefined) ?? []).map(speciality => String(speciality['id'])).join(', ') }) },
};

@Component({
  selector: 'app-resource-list',
  imports: [MatButtonModule, MatCardModule, MatIconModule, MatPaginatorModule, MatProgressSpinnerModule, MatSidenavModule, MatSortModule, MatTableModule, TranslatePipe, AppHeaderComponent, ModuleNavigationComponent],
  templateUrl: './resource-list.component.html',
  styleUrl: './resource-list.component.scss',
})
export class ResourceListComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  private readonly dialog = inject(MatDialog);
  private readonly tableQuery = inject(TableQueryService);
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
  readonly title = computed(() => this.config.title);
  readonly schema = computed(() => SCHEMAS[this.config.key]);
  readonly fields = computed(() => this.schema()?.fields ?? []);
  readonly canManage = computed(() => !!this.config.maintainPermission && this.auth.hasPermission(this.config.maintainPermission));
  readonly columns = computed(() => TABLE_COLUMNS[this.config.key] ?? []);
  readonly displayedColumns = computed(() => this.columns().map(column => column.key));

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(false);
    const query = { page: this.page(), size: this.pageSize(), sort: this.sort(), direction: this.sortDirection() };
    this.http.get<PageResponse<Record<string, unknown>>>(this.config.endpoint, {
      params: this.tableQuery.toHttpParams(query),
    }).subscribe({
      next: (response) => {
        this.records.set(response.content);
        this.totalElements.set(response.totalElements);
        this.loading.set(false);
      },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }

  changePage(event: PageEvent): void {
    this.page.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  changeSort(sort: Sort): void {
    const next = this.tableQuery.nextSort(
      { page: this.page(), size: this.pageSize(), sort: this.sort(), direction: this.sortDirection() }, sort);
    this.page.set(next.page); this.sort.set(next.sort); this.sortDirection.set(next.direction);
    this.load();
  }

  create(): void { this.openEditor(); }
  edit(record: Record<string, unknown>): void { this.openEditor(record); }
  remove(record: Record<string, unknown>): void {
    if (!confirm(`Delete ${this.primary(record)}?`)) return;
    this.http.delete(`${this.config.endpoint}/${record['id']}`).subscribe({ next: () => this.load(), error: () => this.error.set(true) });
  }

  private openEditor(record?: Record<string, unknown>): void {
    if (!this.fields().length) return;
    const schema = this.schema();
    if (!schema) return;
    if (this.config.key === 'countries') {
      this.http.get<string[]>('/api/countries/continents').subscribe({ next: options => this.openDialog(schema, record, options), error: () => this.error.set(true) });
      return;
    }
    this.openDialog(schema, record);
  }

  private openDialog(schema: FormSchema, record?: Record<string, unknown>, continents?: string[]): void {
    const fields = schema.fields.map(field => field.key === 'continent' ? { ...field, options: continents } : field);
    this.dialog.open(ResourceFormDialog, { width: '640px', maxWidth: '94vw', data: { fields, values: record ? schema.fromRecord(record) : undefined } }).afterClosed().subscribe((value?: FormValues) => {
      if (!value) return;
      const request = record ? this.http.put(`${this.config.endpoint}/${record['id']}`, schema.toRequest(value)) : this.http.post(this.config.endpoint, schema.toRequest(value));
      request.subscribe({ next: () => this.load(), error: () => this.error.set(true) });
    });
  }

  closeMenu(drawer: MatSidenav): void { void drawer.close(); }
  primary(record: Record<string, unknown>): string { return String(record['name'] ?? record['street'] ?? record['code'] ?? '—'); }
  secondary(record: Record<string, unknown>): string { return Object.entries(record).filter(([key, value]) => !['id', 'name', 'street'].includes(key) && value != null && typeof value !== 'object').map(([, value]) => String(value)).join(' · '); }
  value(record: Record<string, unknown>, key: string): string {
    if (key === 'primary') return this.primary(record);
    if (key === 'secondary') return this.secondary(record);
    if (key === 'nationality' || key === 'country' || key === 'state') return String((record[key] as Record<string, unknown> | undefined)?.['name'] ?? '—');
    if (key === 'address') { const address = record[key] as Record<string, unknown> | undefined; return address ? `${address['street']} · ${address['postalCode']}` : '—'; }
    if (key === 'specialities') return ((record[key] as Record<string, unknown>[] | undefined) ?? []).map(speciality => String(speciality['name'])).join(', ');
    if (key === 'active') return record[key] ? 'Active' : 'Inactive';
    return String(record[key] ?? '—');
  }
}

@Component({ selector: 'app-resource-form-dialog', imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule], templateUrl: './resource-form-dialog.component.html', styleUrl: './resource-form-dialog.component.scss' })
export class ResourceFormDialog {
  readonly data = inject<{ fields: Field[]; values?: FormValues }>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<ResourceFormDialog, Record<string, string>>);
  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.group(Object.fromEntries(this.data.fields.map(field => [field.key, [this.data.values?.[field.key] ?? '', field.required ? Validators.required : []]])));
  save(): void { if (this.form.invalid) return; this.ref.close(this.form.getRawValue() as Record<string, string>); }
}
