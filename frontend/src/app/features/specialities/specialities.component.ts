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
import { SpecialityCatalogGraphqlService } from '../../core/speciality-catalog-graphql.service';
import { GraphQlUserError } from '../../core/graphql-client.service';
import { CatalogueMutationGraphqlService, SpecialityWrite } from '../../core/catalogue-mutation-graphql.service';
import { inject } from '@angular/core';

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
  private readonly specialitiesGraphql = inject(SpecialityCatalogGraphqlService);
  private readonly mutations = inject(CatalogueMutationGraphqlService);
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
  override load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.errorMessage.set('');
    this.specialitiesGraphql.list({
      page: this.page(),
      size: this.pageSize(),
      sort: this.sort(),
      direction: this.sortDirection(),
      filter: this.filterValues(),
    }).subscribe({
      next: (response) => {
        this.records.set(response.content);
        this.totalElements.set(response.totalElements);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(true);
        this.errorMessage.set(error instanceof GraphQlUserError
          ? error.message : 'The speciality catalogue could not be loaded.');
        this.loading.set(false);
      },
    });
  }
  protected override edit(record: ResourceRecord): void {
    this.openSpecialityEditor(record);
  }
  protected override save(record: ResourceRecord | undefined, body: unknown): void {
    this.mutations.saveSpeciality(record as never, body as SpecialityWrite).subscribe({
      next: () => this.load(),
      error: (error: unknown) => {
        this.error.set(true);
        this.errorMessage.set(error instanceof GraphQlUserError ? error.message : 'The speciality could not be saved.');
      },
    });
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
