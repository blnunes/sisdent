import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { AppointmentsComponent } from './appointments.component';
import { AppointmentGraphqlService } from '../../core/appointment-graphql.service';
import { AuthService } from '../../core/auth.service';
import { OrganizationReadGraphqlService } from '../../core/organization-read-graphql.service';

describe('AppointmentsComponent', () => {
  let component: AppointmentsComponent;
  const membership = signal({
    id: 'membership-1',
    organizationId: 'organization-1',
    organizationName: 'Dental',
    clinicUnitId: 'clinic-1',
    clinicUnitName: 'Central',
    role: 'APPOINTMENT_MANAGER' as const,
    version: 1,
  });
  const appointmentApi = {
    list: vi.fn(),
    availabilityIntervals: vi.fn(),
    patients: vi.fn(),
    create: vi.fn(),
    transition: vi.fn(),
  };
  const organizationReads = { listPractitioners: vi.fn(), listClinicUnits: vi.fn() };
  const queryParams = new BehaviorSubject(convertToParamMap({}));
  const router = { navigate: vi.fn(() => Promise.resolve(true)) };
  const dialog = { open: vi.fn() };

  beforeEach(() => {
    queryParams.next(convertToParamMap({}));
    appointmentApi.list.mockReturnValue(of(emptyPage()));
    appointmentApi.availabilityIntervals.mockReturnValue(of([]));
    appointmentApi.patients.mockReturnValue(
      of([{ globalId: 'patient-1', name: 'Patient', active: true }]),
    );
    organizationReads.listPractitioners.mockReturnValue(of([]));
    organizationReads.listClinicUnits.mockReturnValue(
      of([
        {
          id: 'clinic-1',
          organizationId: 'organization-1',
          name: 'Central',
          active: true,
          timezone: 'Europe/Lisbon',
        },
      ]),
    );
    router.navigate.mockClear();
    dialog.open.mockReset();
    dialog.open.mockReturnValue({ afterClosed: () => new Subject<void>() });
    TestBed.configureTestingModule({
      imports: [AppointmentsComponent],
      providers: [
        {
          provide: AuthService,
          useValue: { activeMembership: membership, canManageAppointments: () => true },
        },
        { provide: AppointmentGraphqlService, useValue: appointmentApi },
        { provide: OrganizationReadGraphqlService, useValue: organizationReads },
        { provide: ActivatedRoute, useValue: { queryParamMap: queryParams } },
        { provide: Router, useValue: router },
        { provide: MatDialog, useValue: dialog },
        {
          provide: TranslateService,
          useValue: { currentLang: signal('en'), instant: (key: string) => key },
        },
      ],
    });
    TestBed.overrideComponent(AppointmentsComponent, { set: { template: '' } });
    const fixture: ComponentFixture<AppointmentsComponent> =
      TestBed.createComponent(AppointmentsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('uses the selected clinic timezone, allows only explicit slot selection, and disables editing', () => {
    const options = component.calendarOptions();
    expect(options.timeZone).toBe('Europe/Lisbon');
    expect(options.editable).toBe(false);
    expect(options.selectable).toBe(true);
    expect(options.editable).toBe(false);
    expect(options.eventStartEditable).toBe(false);
    expect(options.eventDurationEditable).toBe(false);
    expect(options.select).toEqual(expect.any(Function));
    expect(appointmentApi.create).not.toHaveBeenCalled();
  });

  it('reloads the visible range with selected practitioner filters', () => {
    (component as any).asUtcIso = (date: Date) => date.toISOString();
    component.onDatesSet({
      start: new Date('2030-01-01T00:00:00Z'),
      end: new Date('2030-01-08T00:00:00Z'),
    } as never);
    component.practitionerIds = ['practitioner-1'];
    component.onPractitionerChanged();
    expect(appointmentApi.list).toHaveBeenLastCalledWith(
      'organization-1',
      'clinic-1',
      expect.any(String),
      expect.any(String),
      0,
      100,
      ['practitioner-1'],
    );
    expect(appointmentApi.availabilityIntervals).toHaveBeenLastCalledWith(
      'organization-1',
      'clinic-1',
      expect.any(String),
      expect.any(String),
      ['practitioner-1'],
    );
  });

  it('deep-links to a scoped detail and preserves the current calendar configuration', () => {
    const afterClosed = new Subject<void>();
    dialog.open.mockReturnValue({ afterClosed: () => afterClosed });
    const options = component.calendarOptions();
    component.practitionerIds = ['practitioner-1'];
    queryParams.next(convertToParamMap({ appointmentId: '11111111-1111-4111-8111-111111111111' }));

    expect(dialog.open).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        data: expect.objectContaining({
          organizationId: 'organization-1',
          clinicUnitId: 'clinic-1',
          timezone: 'Europe/Lisbon',
        }),
        autoFocus: 'dialog',
        restoreFocus: false,
      }),
    );
    expect(component.selectedView()).toBe('timeGridWeek');
    expect(component.practitionerIds).toEqual(['practitioner-1']);
    expect(component.calendarOptions()).toBe(options);
  });

  it('clears malformed deep links without querying or opening a detail', () => {
    queryParams.next(convertToParamMap({ appointmentId: 'not-an-id' }));
    expect(dialog.open).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(
      [],
      expect.objectContaining({
        queryParams: { appointmentId: null },
        replaceUrl: true,
      }),
    );
  });

  it('updates the selection from a calendar event and restores its focus after close', () => {
    const afterClosed = new Subject<void>();
    dialog.open.mockReturnValue({ afterClosed: () => afterClosed });
    const origin = document.createElement('button');
    document.body.append(origin);
    vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => {
      callback(0);
      return 0;
    });
    component.onEventClick({
      jsEvent: new MouseEvent('click'),
      el: origin,
      event: { id: '11111111-1111-4111-8111-111111111111' },
    } as never);
    expect(router.navigate).toHaveBeenCalledWith(
      [],
      expect.objectContaining({
        queryParams: { appointmentId: '11111111-1111-4111-8111-111111111111' },
      }),
    );
    queryParams.next(convertToParamMap({ appointmentId: '11111111-1111-4111-8111-111111111111' }));
    afterClosed.next();
    expect(document.activeElement).toBe(origin);
    document.body.removeChild(origin);
    vi.unstubAllGlobals();
  });

  it('opens the explicit create form with selected-clinic candidates and refreshes after a mutation', () => {
    const afterClosed = new Subject<void>();
    dialog.open.mockReturnValue({ afterClosed: () => afterClosed });
    organizationReads.listPractitioners.mockReturnValue(
      of([{ globalId: 'practitioner-1', displayName: 'Practitioner', active: true }]),
    );
    component.openCreate();
    expect(appointmentApi.patients).toHaveBeenCalledWith('organization-1', 'clinic-1');
    expect(dialog.open).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        data: expect.objectContaining({
          organizationId: 'organization-1',
          clinicUnitId: 'clinic-1',
          timezone: 'Europe/Lisbon',
        }),
      }),
    );
  });

  it('provides a scoped lifecycle refresh callback to details without changing calendar state', () => {
    const afterClosed = new Subject<void>();
    dialog.open.mockReturnValue({ afterClosed: () => afterClosed });
    queryParams.next(convertToParamMap({ appointmentId: '11111111-1111-4111-8111-111111111111' }));
    const options = component.calendarOptions();
    const data = dialog.open.mock.calls.at(-1)?.[1].data;
    data.onLifecycleFinished();
    expect(component.calendarOptions()).toBe(options);
    expect(appointmentApi.availabilityIntervals).toHaveBeenCalled();
  });

  it('changes the calendar view when a calendar instance is available', () => {
    const changeView = vi.fn();
    (component as any).calendar = { getApi: () => ({ changeView }) };

    component.changeView('timeGridDay');

    expect(component.selectedView()).toBe('timeGridDay');
    expect(changeView).toHaveBeenCalledWith('timeGridDay');
  });

  it('opens blocked-period management only for authorized users with a visible range', () => {
    (component as any).visibleRange = {
      start: new Date('2030-01-01T00:00:00Z'),
      end: new Date('2030-01-08T00:00:00Z'),
    };
    (component as any).asUtcIso = (date: Date) => date.toISOString();

    component.openBlockedPeriods();

    expect(dialog.open).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        data: expect.objectContaining({
          organizationId: 'organization-1',
          clinicUnitId: 'clinic-1',
          from: '2030-01-01T00:00:00.000Z',
          to: '2030-01-08T00:00:00.000Z',
        }),
      }),
    );
  });

  it('shows a localized scheduling error when a range request fails', () => {
    (component as any).asUtcIso = (date: Date) => date.toISOString();
    appointmentApi.list.mockReturnValue(throwError(() => new Error('unavailable')));

    component.onDatesSet({
      start: new Date('2030-01-01T00:00:00Z'),
      end: new Date('2030-01-08T00:00:00Z'),
    } as never);

    expect(component.error()).toBe('APPOINTMENTS.ERROR.LOAD_SCHEDULE');
    expect(component.loading()).toBe(false);
  });

  it('maps availability and appointment records to their calendar event representations', () => {
    const events = (component as any).events(
      [
        {
          globalId: 'appointment-1',
          startAt: '2030-01-01T10:00:00Z',
          endAt: '2030-01-01T10:30:00Z',
          practitionerName: 'Dr. Ada',
          status: 'BOOKED',
        },
      ],
      [{ startAt: '2030-01-01T09:00:00Z', endAt: '2030-01-01T10:00:00Z', category: 'BLOCKED' }],
    );

    expect(events).toEqual([
      expect.objectContaining({
        display: 'background',
        title: 'APPOINTMENTS.UNAVAILABLE',
        classNames: ['availability-blocked'],
      }),
      expect.objectContaining({
        id: 'appointment-1',
        title: 'Dr. Ada · APPOINTMENTS.STATUS.BOOKED',
        classNames: ['appointment-booked'],
      }),
    ]);
  });

  it('resets filters and reloads context when the clinic changes', () => {
    const loadPractitioners = vi.spyOn(component as any, 'loadPractitioners');
    const applyCalendarOptions = vi.spyOn(component as any, 'applyCalendarOptions');
    const refresh = vi.spyOn(component, 'refresh');
    component.practitionerIds = ['practitioner-1'];

    component.onClinicChanged();

    expect(component.practitionerIds).toEqual([]);
    expect(loadPractitioners).toHaveBeenCalled();
    expect(applyCalendarOptions).toHaveBeenCalledWith([]);
    expect(refresh).toHaveBeenCalled();
  });

  it('clears practitioners when there is no selected clinic and reports loading failures', () => {
    component.clinicUnitId = '';
    component.practitioners.set([
      { globalId: 'practitioner-1', displayName: 'Practitioner', active: true, specialityIds: [] },
    ]);
    (component as any).loadPractitioners();
    expect(component.practitioners()).toEqual([]);

    component.clinicUnitId = 'clinic-1';
    organizationReads.listPractitioners.mockReturnValue(throwError(() => new Error('unavailable')));
    (component as any).loadPractitioners();
    expect(component.error()).toBe('APPOINTMENTS.ERROR.LOAD_OPTIONS');
  });

  it('configures keyboard-accessible slots and event labels', () => {
    const onDatesSet = vi.spyOn(component, 'onDatesSet').mockImplementation(() => undefined);
    const onEventClick = vi.spyOn(component, 'onEventClick').mockImplementation(() => undefined);
    const options = (component as any).options('UTC', []);
    const slot = document.createElement('div');
    const event = document.createElement('a');

    options.datesSet!({} as never);
    options.eventClick!({} as never);
    options.slotLaneDidMount!({ el: slot, date: null } as never);
    slot.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    slot.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
    options.eventDidMount!({ el: event, event: { title: 'Check-up' } } as never);

    expect(onDatesSet).toHaveBeenCalled();
    expect(onEventClick).toHaveBeenCalled();
    expect(slot.tabIndex).toBe(0);
    expect(slot.getAttribute('role')).toBe('button');
    expect(slot.getAttribute('aria-label')).toBe('APPOINTMENTS.SELECT_SLOT');
    expect(event.getAttribute('aria-label')).toBe('APPOINTMENTS.DETAILS.OPEN_EVENT');
  });

  it('reports form-option retrieval failures without opening a form dialog', () => {
    appointmentApi.patients.mockReturnValue(throwError(() => new Error('unavailable')));

    component.openCreate();

    expect(component.error()).toBe('APPOINTMENTS.ERROR.LOAD_OPTIONS');
    expect(dialog.open).not.toHaveBeenCalled();
  });
});

function emptyPage() {
  return { content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 };
}
