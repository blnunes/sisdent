import { of, throwError } from 'rxjs';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../core/auth.service';
import { AddressGraphqlService } from '../../core/address-graphql.service';
import { CatalogDisplayNameService } from '../../core/catalog-display-name.service';
import { TableQueryService } from '../../core/table-query.service';
import { AddressesComponent } from './addresses.component';

describe('AddressesComponent', () => {
  let fixture: ComponentFixture<AddressesComponent>;
  let component: AddressesComponent;
  const addresses = {
    list: vi.fn(() => of({ content: [] as Record<string, unknown>[], totalElements: 0 })),
    save: vi.fn(() => of({ id: 'address-1' })),
    delete: vi.fn(() => of(true)),
  };

  beforeEach(() => {
    addresses.list.mockReset();
    addresses.list.mockReturnValue(of({ content: [] as Record<string, unknown>[], totalElements: 0 }));
    addresses.save.mockClear();
    addresses.delete.mockClear();
    TestBed.configureTestingModule({
      imports: [AddressesComponent],
      providers: [
        { provide: AddressGraphqlService, useValue: addresses },
        { provide: AuthService, useValue: { hasPermission: () => true } },
        { provide: CatalogDisplayNameService, useValue: {} },
        { provide: TranslateService, useValue: { instant: (key: string) => key } },
        { provide: TableQueryService, useValue: { nextSort: vi.fn() } },
        { provide: MatDialog, useValue: { open: vi.fn() } },
      ],
    });
    TestBed.overrideComponent(AddressesComponent, { set: { template: '' } });
    fixture = TestBed.createComponent(AddressesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads paged addresses and safely renders absent nested catalogues', () => {
    addresses.list.mockReturnValue(of({
      content: [{ id: 4, street: 'Main', district: null, postalCode: null, country: 'invalid' }],
      totalElements: 1,
    }));

    component.load();

    expect(addresses.list).toHaveBeenLastCalledWith(0, 10, 'id', 'asc');
    expect(component.records()).toHaveLength(1);
    expect(component.rows()[0].cells).toMatchObject({
      street: 'Main', district: '—', postalCode: '—', administrativeDivision: '—', country: '—',
    });
    expect(component.loading()).toBe(false);
  });

  it('shows an error when address loading fails', () => {
    addresses.list.mockReturnValue(throwError(() => new Error('unavailable')));

    component.load();

    expect(component.error()).toBe(true);
    expect(component.loading()).toBe(false);
  });

  it('saves a transformed address and refreshes the list', () => {
    const loadCount = addresses.list.mock.calls.length;
    const dialog = TestBed.inject(MatDialog) as never as { open: ReturnType<typeof vi.fn> };
    dialog.open.mockReturnValue({ afterClosed: () => of({
      street: 'Main', district: 'Centre', city: 'Lisbon', additionalInfo: '', block: '', postalCode: '1000',
      administrativeDivisionName: 'Lisbon', administrativeDivisionCode: 'LX', administrativeDivisionType: 'DISTRICT', countryCode: 'PT',
    }) });

    component.create();

    expect(addresses.save).toHaveBeenCalledWith(undefined, expect.objectContaining({
      additionalInfo: null, block: null, countryCode: 'PT',
      administrativeDivision: { name: 'Lisbon', code: 'LX', type: 'DISTRICT' },
    }));
    expect(addresses.list.mock.calls).toHaveLength(loadCount + 1);
  });

  it('cancels deletion when it is not confirmed', () => {
    vi.stubGlobal('confirm', vi.fn(() => false));

    (component as never as { remove: (record: Record<string, unknown>) => void }).remove({ id: 4, street: 'Main' });

    expect(addresses.delete).not.toHaveBeenCalled();
  });

  it('deletes confirmed records and exposes delete failures', () => {
    vi.stubGlobal('confirm', vi.fn(() => true));
    (component as never as { remove: (record: Record<string, unknown>) => void }).remove({ id: 4, street: 'Main' });
    expect(addresses.delete).toHaveBeenCalledWith('4');

    addresses.delete.mockReturnValue(throwError(() => new Error('unavailable')));
    (component as never as { remove: (record: Record<string, unknown>) => void }).remove({ id: 5, street: 'Second' });
    expect(component.error()).toBe(true);
  });
});
