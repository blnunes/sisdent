import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { EMPTY, of, throwError } from 'rxjs';
import { AppointmentGraphqlService } from '../../core/appointment-graphql.service';
import { TranslateService } from '@ngx-translate/core';
import { BlockedPeriodsDialogComponent } from './blocked-periods-dialog.component';

describe('BlockedPeriodsDialogComponent', () => {
  const api = {
    blockedPeriods: vi.fn(),
    createBlockedPeriod: vi.fn(),
    updateBlockedPeriod: vi.fn(),
    deleteBlockedPeriod: vi.fn(),
  };
  const ref = { close: vi.fn(), keydownEvents: () => EMPTY };
  const dialog = { open: vi.fn(() => ({ afterClosed: () => of(true) })) };
  const data = {
    organizationId: 'organization-1',
    clinicUnitId: 'clinic-1',
    timezone: 'Europe/Lisbon',
    practitioners: [],
    from: '2026-03-29T00:00:00Z',
    to: '2026-03-30T00:00:00Z',
    onFinished: vi.fn(),
  };

  beforeEach(() => {
    Object.values(api).forEach((mock) => mock.mockReset());
    dialog.open.mockReset();
    dialog.open.mockReturnValue({ afterClosed: () => of(true) });
    api.blockedPeriods.mockReturnValue(of([]));
    api.createBlockedPeriod.mockReturnValue(of({}));
    TestBed.configureTestingModule({
      imports: [BlockedPeriodsDialogComponent],
      providers: [
        { provide: AppointmentGraphqlService, useValue: api },
        { provide: MatDialogRef, useValue: ref },
        { provide: MatDialog, useValue: dialog },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: TranslateService, useValue: { instant: (key: string) => key } },
      ],
    }).overrideProvider(MatDialog, { useValue: dialog });
  });

  it('uses the finite selected-clinic range without rendering opaque management fields', () => {
    const component = TestBed.createComponent(BlockedPeriodsDialogComponent).componentInstance;
    expect(api.blockedPeriods).toHaveBeenCalledWith(
      data.organizationId,
      data.clinicUnitId,
      data.from,
      data.to,
    );
    expect(component.periods()).toEqual([]);
  });

  it('requires explicit discard confirmation for pending changes', () => {
    const component = TestBed.createComponent(BlockedPeriodsDialogComponent).componentInstance;
    component.start = '2026-03-29T09:00';
    component.close();
    expect(dialog.open).toHaveBeenCalled();
    expect(ref.close).toHaveBeenCalled();
  });

  it('submits clinic-local input as UTC and refreshes safely', () => {
    const component = TestBed.createComponent(BlockedPeriodsDialogComponent).componentInstance;
    component.start = '2026-03-29T09:00';
    component.end = '2026-03-29T10:00';
    component.save();
    expect(api.createBlockedPeriod).toHaveBeenCalledWith(
      data.organizationId,
      expect.objectContaining({
        clinicUnitId: data.clinicUnitId,
        startAt: expect.stringMatching(/Z$/),
        endAt: expect.stringMatching(/Z$/),
      }),
    );
    expect(data.onFinished).toHaveBeenCalled();
  });

  it('keeps stale-version and authorization failures generic while refreshing the preserved range', () => {
    api.createBlockedPeriod.mockReturnValue(
      throwError(() => ({ code: 'CONFLICT', message: 'raw stale version' })),
    );
    const component = TestBed.createComponent(BlockedPeriodsDialogComponent).componentInstance;
    component.start = '2026-10-25T01:30';
    component.end = '2026-10-25T02:30';
    component.save();
    expect(component.error()).toBe(true);
    expect(data.onFinished).toHaveBeenCalled();
    expect(JSON.stringify(component.periods())).not.toContain('raw stale version');
  });

  it('uses internal opaque identifiers and versions for update/delete only', () => {
    const period = {
      globalId: '11111111-1111-4111-8111-111111111111',
      clinicUnitId: data.clinicUnitId,
      practitionerId: null,
      startAt: '2026-03-29T09:00:00Z',
      endAt: '2026-03-29T10:00:00Z',
      version: 4,
    };
    api.updateBlockedPeriod.mockReturnValue(of(period));
    api.deleteBlockedPeriod.mockReturnValue(of(true));
    const component = TestBed.createComponent(BlockedPeriodsDialogComponent).componentInstance;
    component.edit(period);
    component.end = '2026-03-29T11:00';
    component.save();
    expect(api.updateBlockedPeriod).toHaveBeenCalledWith(
      data.organizationId,
      period.globalId,
      period.version,
      expect.any(Object),
    );
    component.remove(period);
    expect(api.deleteBlockedPeriod).toHaveBeenCalledWith(
      data.organizationId,
      data.clinicUnitId,
      period.globalId,
      period.version,
    );
  });

  it('does not delete when the confirmation branch is declined', () => {
    dialog.open.mockReturnValue({ afterClosed: () => of(false) });
    const component = TestBed.createComponent(BlockedPeriodsDialogComponent).componentInstance;
    component.remove({
      globalId: '11111111-1111-4111-8111-111111111111',
      clinicUnitId: data.clinicUnitId,
      practitionerId: null,
      startAt: '2026-03-29T09:00:00Z',
      endAt: '2026-03-29T10:00:00Z',
      version: 4,
    });
    expect(api.deleteBlockedPeriod).not.toHaveBeenCalled();
  });

  it('handles invalid, failed deletion, and discarded-close branches without leaking state', () => {
    const period = {
      globalId: '11111111-1111-4111-8111-111111111111',
      clinicUnitId: data.clinicUnitId,
      practitionerId: null,
      startAt: '2026-03-29T09:00:00Z',
      endAt: '2026-03-29T10:00:00Z',
      version: 4,
    };
    const component = TestBed.createComponent(BlockedPeriodsDialogComponent).componentInstance;
    component.start = 'invalid';
    component.end = '2026-03-29T10:00';
    component.save();
    expect(component.error()).toBe(true);
    api.deleteBlockedPeriod.mockReturnValue(throwError(() => new Error('offline')));
    dialog.open.mockReturnValue({ afterClosed: () => of(true) });
    component.remove(period);
    expect(component.error()).toBe(true);
    dialog.open.mockReturnValue({ afterClosed: () => of(false) });
    component.start = '2026-03-29T09:00';
    const closeCalls = ref.close.mock.calls.length;
    component.close();
    expect(ref.close).toHaveBeenCalledTimes(closeCalls);
  });
});
