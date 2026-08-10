import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { DataTableComponent } from './data-table.component';

describe('DataTableComponent', () => {
  beforeEach(() => TestBed.configureTestingModule({ imports: [DataTableComponent], providers: [provideTranslateService()] }));

  it('renders loading, error, empty, and row states safely', () => {
    const fixture = TestBed.createComponent(DataTableComponent);
    fixture.componentRef.setInput('loading', true); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('mat-spinner')).toBeTruthy();
    fixture.componentRef.setInput('emptyLabel', 'EMPTY'); fixture.componentRef.setInput('loading', false); fixture.componentRef.setInput('error', true); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.error')).toBeTruthy();
    fixture.componentRef.setInput('error', false); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('EMPTY');
    fixture.componentRef.setInput('columns', [{ key: 'name', label: 'Name' }, { key: 'actions', label: '' }]);
    fixture.componentRef.setInput('rows', [{ id: 7, cells: { name: 'Ana' }, actions: [{ key: 'view', label: 'View', icon: 'visibility' }, { key: 'edit', label: 'Edit', icon: 'edit' }, { key: 'delete', label: 'Delete', icon: 'delete_outline', destructive: true }] }]); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Ana');
    expect(fixture.nativeElement.querySelector('colgroup')).toBeNull();
    expect(fixture.nativeElement.querySelector('th.mat-column-actions')?.style.width).toBe('');
    expect(fixture.nativeElement.querySelector('td.mat-column-actions')?.style.minWidth).toBe('');
    const actionButtons = fixture.nativeElement.querySelectorAll('.actions button');
    expect(actionButtons).toHaveLength(3);
    expect(fixture.nativeElement.querySelector('.actions')?.children).toHaveLength(3);
  });

  it('emits page, sort, row-action, and retry events', () => {
    const component = TestBed.createComponent(DataTableComponent).componentInstance;
    const page = vi.fn(); const sort = vi.fn(); const action = vi.fn(); const retry = vi.fn();
    component.pageChange.subscribe(page); component.sortChange.subscribe(sort); component.rowAction.subscribe(action); component.retry.subscribe(retry);
    component.onPage({ pageIndex: 2, pageSize: 20 } as never); component.onSort({ active: 'name', direction: 'desc' }); component.rowAction.emit({ rowId: 1, action: 'edit' }); component.retry.emit();
    expect(page).toHaveBeenCalledWith({ pageIndex: 2, pageSize: 20 }); expect(sort).toHaveBeenCalledWith({ active: 'name', direction: 'desc' }); expect(action).toHaveBeenCalledWith({ rowId: 1, action: 'edit' }); expect(retry).toHaveBeenCalled();
  });
});
