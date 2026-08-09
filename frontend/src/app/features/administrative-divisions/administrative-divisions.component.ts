import { Component } from '@angular/core';
import { DataTableColumn } from '../../shared/data-table/data-table.models';
import { CatalogueListController } from '../resource-support/catalogue-list.controller';
import { RESOURCE_PAGE_IMPORTS } from '../resource-support/resource-page.imports';
import { ResourceRecord } from '../resource-support/resource-list.controller';

const COLUMNS: readonly DataTableColumn[] = [{ key: 'name', label: 'Name', sortable: true }, { key: 'code', label: 'Code', sortable: true }, { key: 'type', label: 'Type', sortable: true }, { key: 'country', label: 'Country' }, { key: 'actions', label: '' }];
@Component({ selector: 'app-administrative-divisions', imports: [...RESOURCE_PAGE_IMPORTS], templateUrl: '../resource-support/resource-page.component.html', styleUrl: '../resource-support/resource-page.component.scss' })
export class AdministrativeDivisionsComponent extends CatalogueListController {
  readonly activeKey = 'administrativeDivisions'; readonly title = 'MODULES.ADMINISTRATIVE_DIVISIONS'; readonly description = 'MODULES.ADMINISTRATIVE_DIVISIONS_DESCRIPTION'; readonly translationPrefix = 'RESOURCE'; readonly columns = COLUMNS;
  constructor() { super({ endpoint: () => '/api/administrative-divisions', maintainPermission: 'MAINTAIN_ADMINISTRATIVE_DIVISIONS', columns: COLUMNS, identifier: (record) => Number(record['id']), primary: (record) => String(record['name'] ?? '—'), cells: divisionCells }, { fields: [{ key: 'name', label: 'Name', required: true }, { key: 'code', label: 'Code', required: true }, { key: 'type', label: 'Type', required: true }, { key: 'countryCode', label: 'Country code', required: true }], fromRecord: (record) => ({ name: String(record['name'] ?? ''), code: String(record['code'] ?? ''), type: String(record['type'] ?? ''), countryCode: String(nested(record, 'country')['code'] ?? '') }), toRequest: (values) => values, title: (editing) => editing ? 'Edit administrative division' : 'New administrative division' }); this.load(); }
}
function nested(record: ResourceRecord, key: string): ResourceRecord { const value = record[key]; return value && typeof value === 'object' && !Array.isArray(value) ? value as ResourceRecord : {}; }
function divisionCells(record: ResourceRecord): Readonly<Record<string, string>> { return { name: String(record['name'] ?? '—'), code: String(record['code'] ?? '—'), type: String(record['type'] ?? '—'), country: String(nested(record, 'country')['name'] ?? '—') }; }
