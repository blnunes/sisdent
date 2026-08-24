import { computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { Sort } from '@angular/material/sort';
import { MatSidenav } from '@angular/material/sidenav';
import { AuthService } from '../../core/auth.service';
import { Permission } from '../../core/models';
import { TableQueryService } from '../../core/table-query.service';
import {
  DataTableActionEvent,
  DataTableColumn,
  DataTablePageEvent,
  DataTableRow,
  DataTableRowAction,
  DataTableSortEvent,
} from '../../shared/data-table/data-table.models';
import {
  FilterAutocompleteEvent,
  FilterDefinition,
  FilterOption,
  FilterValueEvent,
} from '../../shared/filters/filter.models';
import { fromEvent } from 'rxjs';
import { LANGUAGE_CHANGED_EVENT } from '../../core/language.service';
import { CatalogDisplayNameService } from '../../core/catalog-display-name.service';
import { TranslateService } from '@ngx-translate/core';

export type ResourceRecord = Record<string, unknown>;
export type ResourceListDefinition = {
  maintainPermission: Permission;
  columns: readonly DataTableColumn[];
  filters?: readonly FilterDefinition[];
  identifier: (record: ResourceRecord) => string | number;
  cells: (
    record: ResourceRecord,
    catalogNames: CatalogDisplayNameService,
    translate: TranslateService,
  ) => Readonly<Record<string, string>>;
  primary: (record: ResourceRecord) => string;
  canView?: () => boolean;
  canDelete?: (record: ResourceRecord) => boolean;
  actionLabels?: { view?: string; edit?: string; delete?: string };
};

export abstract class ResourceListController {
  readonly filterAriaLabel: string = 'RESOURCE.FILTER.ARIA';
  protected readonly dialog = inject(MatDialog);
  protected readonly tableQuery = inject(TableQueryService);
  protected readonly destroyRef = inject(DestroyRef);
  readonly auth = inject(AuthService);
  protected readonly catalogNames = inject(CatalogDisplayNameService);
  protected readonly translate = inject(TranslateService);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly errorMessage = signal('');
  readonly records = signal<ResourceRecord[]>([]);
  readonly page = signal(0);
  readonly pageSize = signal(10);
  readonly totalElements = signal(0);
  readonly sort = signal('id');
  readonly sortDirection = signal<'asc' | 'desc'>('asc');
  readonly filterValues = signal<Record<string, string>>({});
  readonly filterDisplayValues = signal<Record<string, string>>({});
  readonly filterOptions = signal<Record<string, readonly FilterOption[]>>({});
  readonly filters = signal<readonly FilterDefinition[]>([]);
  readonly canManage: ReturnType<typeof computed<boolean>>;
  readonly rows: ReturnType<typeof computed<DataTableRow[]>>;

  protected constructor(readonly definition: ResourceListDefinition) {
    this.filters.set(definition.filters ?? []);
    this.canManage = computed(() => this.auth.hasPermission(definition.maintainPermission));
    this.rows = computed(() =>
      this.records().map((record) => ({
        id: definition.identifier(record),
        cells: definition.cells(record, this.catalogNames, this.translate),
        actions: this.actions(record),
      })),
    );
    fromEvent(window, LANGUAGE_CHANGED_EVENT)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load());
  }

  abstract load(): void;

  changePage(event: DataTablePageEvent): void {
    this.page.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }
  changeSort(event: DataTableSortEvent): void {
    const next = this.tableQuery.nextSort(
      {
        page: this.page(),
        size: this.pageSize(),
        sort: this.sort(),
        direction: this.sortDirection(),
      },
      event as Sort,
    );
    this.page.set(next.page);
    this.sort.set(next.sort);
    this.sortDirection.set(next.direction);
    this.load();
  }
  updateFilter(event: FilterValueEvent): void {
    this.filterValues.update((values) => ({ ...values, [event.key]: event.value }));
  }
  applyFilters(): void {
    this.page.set(0);
    this.load();
  }
  clearFilter(key: string): void {
    this.filterValues.update((values) => ({ ...values, [key]: '' }));
    this.filterDisplayValues.update((values) => ({ ...values, [key]: '' }));
    this.filterOptions.update((values) => ({ ...values, [key]: [] }));
    this.applyFilters();
  }
  clearFilters(): void {
    this.filterValues.set({});
    this.filterDisplayValues.set({});
    this.filterOptions.set({});
    this.applyFilters();
  }
  updateAutocomplete(_event: FilterAutocompleteEvent): void {
    throw new Error('Autocomplete is not supported by this resource.');
  }
  selectFilterOption(event: FilterValueEvent & { label: string }): void {
    this.filterDisplayValues.update((values) => ({ ...values, [event.key]: event.label }));
    this.updateFilter(event);
    this.applyFilters();
  }
  handleAction(event: DataTableActionEvent): void {
    const record = this.records().find(
      (candidate) => this.definition.identifier(candidate) === event.rowId,
    );
    if (!record) return;
    if (event.action === 'view') this.view(record);
    else if (event.action === 'edit') this.edit(record);
    else if (event.action === 'delete') this.remove(record);
  }
  abstract create(): void;
  closeMenu(drawer: MatSidenav): void {
    void drawer.close();
  }
  protected view(_record: ResourceRecord): void {
    throw new Error('View action is not supported by this resource.');
  }
  protected abstract edit(record: ResourceRecord): void;
  protected abstract remove(record: ResourceRecord): void;
  private actions(record: ResourceRecord): DataTableRowAction[] {
    const actions: DataTableRowAction[] = [];
    if (this.definition.canView?.())
      actions.push({
        key: 'view',
        label: this.definition.actionLabels?.view ?? 'RESOURCE.VIEW',
        icon: 'visibility',
      });
    if (this.canManage()) {
      actions.push({
        key: 'edit',
        label: this.definition.actionLabels?.edit ?? 'RESOURCE.EDIT',
        icon: 'edit',
      });
      if (this.definition.canDelete?.(record) ?? true)
        actions.push({
          key: 'delete',
          label: this.definition.actionLabels?.delete ?? 'RESOURCE.DELETE',
          icon: 'delete_outline',
          destructive: true,
        });
    }
    return actions;
  }
}
