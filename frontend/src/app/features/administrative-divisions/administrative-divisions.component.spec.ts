import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { AdministrativeDivisionGraphqlService } from '../../core/administrative-division-graphql.service';
import { AuthService } from '../../core/auth.service';
import { CatalogDisplayNameService } from '../../core/catalog-display-name.service';
import { TableQueryService } from '../../core/table-query.service';
import { AdministrativeDivisionsComponent } from './administrative-divisions.component';

describe('AdministrativeDivisionsComponent', () => {
  let component: AdministrativeDivisionsComponent;
  const divisions = {
    list: vi.fn(() => of({ content: [] as Record<string, unknown>[], totalElements: 0 })),
    save: vi.fn(() => of({ id: 'division-1' })),
    delete: vi.fn(() => of(true)),
  };

  beforeEach(() => {
    divisions.list.mockReset();
    divisions.list.mockReturnValue(of({ content: [] as Record<string, unknown>[], totalElements: 0 }));
    divisions.save.mockClear();
    divisions.delete.mockClear();
    TestBed.configureTestingModule({
      imports: [AdministrativeDivisionsComponent],
      providers: [
        { provide: AdministrativeDivisionGraphqlService, useValue: divisions },
        { provide: AuthService, useValue: { hasPermission: () => true } },
        { provide: CatalogDisplayNameService, useValue: {} },
        { provide: TranslateService, useValue: { instant: (key: string) => key } },
        { provide: TableQueryService, useValue: { nextSort: vi.fn() } },
        { provide: MatDialog, useValue: { open: vi.fn() } },
      ],
    });
    TestBed.overrideComponent(AdministrativeDivisionsComponent, { set: { template: '' } });
    component = TestBed.createComponent(AdministrativeDivisionsComponent).componentInstance;
  });

  it('loads divisions and safely renders malformed nested countries', () => {
    divisions.list.mockReturnValue(of({
      content: [{ id: 1, name: 'Lisbon', code: 'LX', type: 'DISTRICT', country: [] }], totalElements: 1,
    }));

    component.load();

    expect(divisions.list).toHaveBeenLastCalledWith(0, 10, 'id', 'asc');
    expect(component.rows()[0].cells).toMatchObject({ name: 'Lisbon', code: 'LX', type: 'DISTRICT', country: '—' });
    expect(component.totalElements()).toBe(1);
  });

  it('marks failures while loading divisions', () => {
    divisions.list.mockReturnValue(throwError(() => new Error('unavailable')));

    component.load();

    expect(component.error()).toBe(true);
    expect(component.loading()).toBe(false);
  });

  it('creates a division from dialog input and reloads the table', () => {
    const dialog = TestBed.inject(MatDialog) as never as { open: ReturnType<typeof vi.fn> };
    dialog.open.mockReturnValue({ afterClosed: () => of({ name: 'Lisbon', code: 'LX', type: 'DISTRICT', countryCode: 'PT' }) });
    const callsBefore = divisions.list.mock.calls.length;

    component.create();

    expect(divisions.save).toHaveBeenCalledWith(undefined, { name: 'Lisbon', code: 'LX', type: 'DISTRICT', countryCode: 'PT' });
    expect(divisions.list.mock.calls).toHaveLength(callsBefore + 1);
  });

  it('does not delete when the confirmation is cancelled', () => {
    vi.stubGlobal('confirm', vi.fn(() => false));

    (component as never as { remove: (record: Record<string, unknown>) => void }).remove({ id: 1, name: 'Lisbon' });

    expect(divisions.delete).not.toHaveBeenCalled();
  });

  it('deletes confirmed divisions and reports deletion failures', () => {
    vi.stubGlobal('confirm', vi.fn(() => true));
    (component as never as { remove: (record: Record<string, unknown>) => void }).remove({ id: 1, name: 'Lisbon' });
    expect(divisions.delete).toHaveBeenCalledWith('1');

    divisions.delete.mockReturnValue(throwError(() => new Error('unavailable')));
    (component as never as { remove: (record: Record<string, unknown>) => void }).remove({ id: 2, name: 'Porto' });
    expect(component.error()).toBe(true);
  });
});
