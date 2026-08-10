import { Component } from '@angular/core';
import { DataTableColumn } from '../../shared/data-table/data-table.models';
import { FilterDefinition } from '../../shared/filters/filter.models';
import { RESOURCE_PAGE_IMPORTS } from '../resource-support/resource-page.imports';
import {
  ResourceListController,
  ResourceRecord,
} from '../resource-support/resource-list.controller';
import { parseProcedures } from './speciality-form.utils';
import {
  SpecialityFormDialogComponent,
  SpecialityFormResult,
} from './speciality-form-dialog/speciality-form-dialog.component';
import { CatalogDisplayNameService } from '../../core/catalog-display-name.service';

const COLUMNS: readonly DataTableColumn[] = [
  { key: 'name', label: 'SPECIALITIES.TABLE.NAME', sortable: true },
  { key: 'procedures', label: 'SPECIALITIES.TABLE.PROCEDURES' },
  { key: 'actions', label: '' },
];
const FILTERS: readonly FilterDefinition[] = [
  { key: 'name', label: 'RESOURCE.FILTER.NAME', type: 'text' },
];

@Component({
  selector: 'app-specialities',
  imports: [...RESOURCE_PAGE_IMPORTS],
  templateUrl: '../resource-support/resource-page.component.html',
  styleUrl: '../resource-support/resource-page.component.scss',
})
export class SpecialitiesComponent extends ResourceListController {
  readonly activeKey = 'specialities';
  readonly title = 'MODULES.SPECIALITIES';
  readonly description = 'MODULES.SPECIALITIES_DESCRIPTION';
  readonly translationPrefix = 'RESOURCE';
  readonly columns = COLUMNS;
  constructor() {
    super({
      endpoint: () => '/api/specialities',
      maintainPermission: 'MAINTAIN_SPECIALITIES',
      columns: COLUMNS,
      filters: FILTERS,
      identifier: (record) => Number(record['id']),
      primary: (record) => String(record['displayName'] ?? record['name'] ?? '—'),
      cells: specialityCells,
    });
    this.load();
  }
  override create(): void {
    this.openSpecialityEditor();
  }
  protected override edit(record: ResourceRecord): void {
    this.openSpecialityEditor(record);
  }
  private openSpecialityEditor(record?: ResourceRecord): void {
    this.dialog
      .open(SpecialityFormDialogComponent, {
        width: '820px',
        maxWidth: '94vw',
        maxHeight: '92vh',
        autoFocus: 'first-tabbable',
        data: {
          title: record ? 'SPECIALITIES.FORM.EDIT_TITLE' : 'SPECIALITIES.FORM.NEW_TITLE',
          name: record ? String(record['name'] ?? '') : undefined,
          translations: record?.['translations'] as Record<string, string> | undefined,
          procedures: record ? parseProcedures(JSON.stringify(record['procedures'] ?? [])) : [],
        },
      })
      .afterClosed()
      .subscribe((result?: SpecialityFormResult) => {
        if (result) this.save(record, result);
      });
  }
}
function specialityCells(
  record: ResourceRecord,
  names: CatalogDisplayNameService,
): Readonly<Record<string, string>> {
  return {
    name: names.speciality(record),
    procedures:
      ((record['procedures'] as ResourceRecord[] | undefined) ?? [])
        .map((item) => names.procedure(item))
        .filter(Boolean)
        .join(', ') || '—',
  };
}
