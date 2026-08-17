import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslatePipe } from '@ngx-translate/core';
import {
  DataTableActionEvent,
  DataTableColumn,
  DataTablePageEvent,
  DataTableRow,
  DataTableSortEvent,
} from './data-table.models';

@Component({
  selector: 'app-data-table',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSortModule,
    MatTableModule,
    MatTooltipModule,
    TranslatePipe,
  ],
  templateUrl: './data-table.component.html',
  styleUrl: './data-table.component.scss',
})
export class DataTableComponent {
  readonly columns = input<readonly DataTableColumn[]>([]);
  readonly rows = input<readonly DataTableRow[]>([]);
  readonly loading = input(false);
  readonly error = input(false);
  readonly errorMessage = input('');
  readonly pageIndex = input(0);
  readonly pageSize = input(10);
  readonly totalElements = input(0);
  readonly sortActive = input('');
  readonly sortDirection = input<'asc' | 'desc' | ''>('');
  readonly emptyLabel = input('');
  readonly errorLabel = input('');
  readonly retryLabel = input('');
  readonly paginationLabel = input('');
  readonly pageChange = output<DataTablePageEvent>();
  readonly sortChange = output<DataTableSortEvent>();
  readonly rowAction = output<DataTableActionEvent>();
  readonly retry = output<void>();

  displayedColumns(): string[] {
    return this.columns().map(({ key }) => key);
  }
  onPage(event: PageEvent): void {
    this.pageChange.emit({ pageIndex: event.pageIndex, pageSize: event.pageSize });
  }
  onSort(event: Sort): void {
    this.sortChange.emit({ active: event.active, direction: event.direction });
  }
}
