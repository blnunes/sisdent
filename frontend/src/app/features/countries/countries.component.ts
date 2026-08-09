import { Component } from '@angular/core';
import { DataTableColumn } from '../../shared/data-table/data-table.models';
import { FormDialogField } from '../../shared/dialogs/form-dialog-shell/form-dialog-shell.models';
import { CatalogueListController } from '../resource-support/catalogue-list.controller';
import { RESOURCE_PAGE_IMPORTS } from '../resource-support/resource-page.imports';
import { ResourceRecord } from '../resource-support/resource-list.controller';

const COLUMNS: readonly DataTableColumn[] = [{ key: 'name', label: 'Name', sortable: true }, { key: 'code', label: 'ISO code', sortable: true }, { key: 'continent', label: 'Continent', sortable: true }, { key: 'actions', label: '' }];
const BASE_FIELDS: readonly FormDialogField[] = [{ key: 'name', label: 'Name', required: true }, { key: 'code', label: 'ISO code', required: true }];
@Component({ selector: 'app-countries', imports: [...RESOURCE_PAGE_IMPORTS], templateUrl: '../resource-support/resource-page.component.html', styleUrl: '../resource-support/resource-page.component.scss' })
export class CountriesComponent extends CatalogueListController {
  readonly activeKey = 'countries'; readonly title = 'MODULES.COUNTRIES'; readonly description = 'MODULES.COUNTRIES_DESCRIPTION'; readonly translationPrefix = 'RESOURCE'; readonly columns = COLUMNS;
  constructor() { super({ endpoint: () => '/api/countries', maintainPermission: 'MAINTAIN_COUNTRIES', columns: COLUMNS, identifier: (record) => Number(record['id']), primary: (record) => String(record['name'] ?? '—'), cells: (record) => ({ name: String(record['name'] ?? '—'), code: String(record['code'] ?? '—'), continent: String(record['continent'] ?? '—') }) }, { fields: BASE_FIELDS, fromRecord: (record) => ({ name: String(record['name'] ?? ''), code: String(record['code'] ?? ''), continent: String(record['continent'] ?? '') }), toRequest: (values) => values, title: (editing) => editing ? 'Edit country' : 'New country' }); this.load(); }
  override create(): void { this.openWithContinents(); }
  protected override edit(record: ResourceRecord): void { this.openWithContinents(record); }
  private openWithContinents(record?: ResourceRecord): void {
    this.http.get<string[]>('/api/countries/continents').subscribe({ next: (continents) => this.openEditor(record, [...BASE_FIELDS, { key: 'continent', label: 'Continent', required: true, type: 'select', options: continents.map((value) => ({ value, label: value })) }]), error: () => this.error.set(true) });
  }
}
