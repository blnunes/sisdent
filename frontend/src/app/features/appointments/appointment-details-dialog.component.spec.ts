import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatDialog } from '@angular/material/dialog';
import { EMPTY, of, throwError } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { AppointmentGraphqlService } from '../../core/appointment-graphql.service';
import { AppointmentDetailsDialogComponent } from './appointment-details-dialog.component';

describe('AppointmentDetailsDialogComponent', () => {
  const api = { detail: vi.fn(), transition: vi.fn(), performedProcedures: vi.fn(), eligiblePerformedProcedureOptions: vi.fn(), createPerformedProcedure: vi.fn(), voidPerformedProcedure: vi.fn() };
  const ref = { close: vi.fn(), keydownEvents: () => EMPTY };
  const dialog = { open: vi.fn(() => ({ afterClosed: () => of<unknown>(true) })) };
  const data = { organizationId: 'organization-1', clinicUnitId: 'clinic-1', appointmentId: '11111111-1111-4111-8111-111111111111', clinicName: 'Central', timezone: 'Europe/Lisbon', canManageAppointments: true, onUnavailable: vi.fn(), onLifecycleFinished: vi.fn() };

  beforeEach(() => {
    Object.values(api).forEach((mock) => mock.mockReset());
    dialog.open.mockReset();
    dialog.open.mockReturnValue({ afterClosed: () => of(true) });
    api.detail.mockReturnValue(of({ patientName: 'Patient', practitionerName: 'Practitioner', startAt: '2026-03-29T00:30:00Z', endAt: '2026-03-29T01:30:00Z', status: 'SCHEDULED' }));
    api.transition.mockReturnValue(of({ status: 'COMPLETED' }));
    api.performedProcedures.mockReturnValue(of([]));
    api.eligiblePerformedProcedureOptions.mockReturnValue(of([{ id: '7', displayName: 'Cleaning' }]));
    api.createPerformedProcedure.mockReturnValue(of({}));
    api.voidPerformedProcedure.mockReturnValue(of({}));
    TestBed.configureTestingModule({
      imports: [AppointmentDetailsDialogComponent],
      providers: [
        { provide: AppointmentGraphqlService, useValue: api },
        { provide: MatDialogRef, useValue: ref },
        { provide: MatDialog, useValue: dialog },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: TranslateService, useValue: { getCurrentLang: () => 'en' } },
      ],
    }).overrideProvider(MatDialog, { useValue: dialog });
  });

  it('loads the scoped detail and renders instants in the selected clinic timezone across DST', () => {
    const component = TestBed.createComponent(AppointmentDetailsDialogComponent).componentInstance;
    expect(api.detail).toHaveBeenCalledWith('organization-1', 'clinic-1', data.appointmentId);
    expect(component.appointmentTime()).toContain('2:30');
    component.detail.set({ patientId: 'patient-1', practitionerId: 'practitioner-1', patientName: 'Patient', practitionerName: 'Practitioner', startAt: '2026-10-25T00:30:00Z', endAt: '2026-10-25T01:30:00Z', status: 'SCHEDULED' });
    expect(component.appointmentTime()).toContain('1:30');
  });

  it('keeps GraphQL failures generic and can close without lifecycle actions', () => {
    api.detail.mockReturnValue(throwError(() => ({ code: 'AUTHORIZATION.DENIED', message: 'secret' })));
    const component = TestBed.createComponent(AppointmentDetailsDialogComponent).componentInstance;
    expect(component.error()).toBe(true);
    expect(component.detail()).toBeNull();
    expect(data.onUnavailable).toHaveBeenCalled();
    component.close();
    expect(ref.close).toHaveBeenCalled();
  });

  it('confirms a scoped lifecycle transition then refreshes without exposing errors', () => {
    const component = TestBed.createComponent(AppointmentDetailsDialogComponent).componentInstance;
    component.requestTransition('COMPLETED');
    expect(dialog.open).toHaveBeenCalled();
    expect(api.transition).toHaveBeenCalledWith('organization-1', 'clinic-1', data.appointmentId, 'COMPLETED');
    expect(component.detail()?.status).toBe('COMPLETED');
    expect(data.onLifecycleFinished).toHaveBeenCalled();
  });

  it('loads performed procedures only with the selected clinic scope', () => {
    TestBed.createComponent(AppointmentDetailsDialogComponent);
    expect(api.performedProcedures).toHaveBeenCalledWith('organization-1', 'clinic-1', data.appointmentId);
  });

  it('creates and voids a completed appointment procedure through scoped, confirmed mutations', () => {
    api.detail.mockReturnValue(of({ patientName: 'Patient', practitionerName: 'Practitioner', startAt: '2026-10-25T00:30:00Z', endAt: '2026-10-25T01:30:00Z', status: 'COMPLETED' }));
    const component = TestBed.createComponent(AppointmentDetailsDialogComponent).componentInstance;
    component.loadProcedureOptions();
    component.selectedProcedureId = '7';
    component.addProcedure();
    expect(api.eligiblePerformedProcedureOptions).toHaveBeenCalledWith(data.organizationId, data.clinicUnitId, data.appointmentId);
    expect(api.createPerformedProcedure).toHaveBeenCalledWith(data.organizationId, data.clinicUnitId, data.appointmentId, '7', expect.stringMatching(/Z$/));
    dialog.open.mockReturnValue({ afterClosed: () => of('Duplicate') });
    component.requestVoid({ globalId: 'procedure-1', dentalProcedureId: '7', procedureNameSnapshot: 'Cleaning', performedAt: '2026-10-25T00:30:00Z', voidedAt: null });
    expect(api.voidPerformedProcedure).toHaveBeenCalledWith(data.organizationId, data.clinicUnitId, 'procedure-1', expect.any(String));
  });

  it('does not create without a selected procedure and keeps failures generic', () => {
    const component = TestBed.createComponent(AppointmentDetailsDialogComponent).componentInstance;
    component.addProcedure();
    expect(component.procedureError()).toBe(true);
    expect(api.createPerformedProcedure).not.toHaveBeenCalled();
  });
});
