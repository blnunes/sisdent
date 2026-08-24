import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { HomeComponent } from './home.component';
import { AuthService } from '../../core/auth.service';
import { AppointmentGraphqlService } from '../../core/appointment-graphql.service';

describe('HomeComponent', () => {
  let fixture: ComponentFixture<HomeComponent>;
  let component: HomeComponent;
  const membership = signal({ id: 'membership-1', organizationId: 'organization-1', organizationName: 'Dental', clinicUnitId: 'clinic-1', clinicUnitName: 'Central', role: 'APPOINTMENT_MANAGER' as const, version: 1 });
  const auth = {
    activeMembership: membership,
    session: signal({ displayName: 'Taylor', memberships: [] }),
    canReadAppointments: () => true,
    canManageAppointments: () => true,
    canReadClinical: () => false,
    hasAnyPermission: () => false,
  };
  const appointments = { list: vi.fn() };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: AppointmentGraphqlService, useValue: appointments },
      ],
    });
    TestBed.overrideComponent(HomeComponent, { set: { template: '' } });
    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    appointments.list.mockReset();
    appointments.list.mockReturnValue(of(emptyPage()));
    fixture.detectChanges();
  });

  it('loads seven-day appointments for the active clinic and derives the next scheduled visit', () => {
    appointments.list.mockReturnValueOnce(of(page([appointment('scheduled', 'SCHEDULED'), appointment('completed', 'COMPLETED')], 12)));
    appointments.list.mockReturnValueOnce(of(page([appointment('recent', 'COMPLETED')], 4)));
    component.loadDashboard();

    expect(appointments.list).toHaveBeenCalledWith('organization-1', 'clinic-1', expect.any(String), expect.any(String), 0, 12);
    expect(component.appointments()).toHaveLength(2);
    expect(component.nextAppointment()?.globalId).toBe('scheduled');
    expect(component.scheduledToday()).toBe(0);
    expect(component.recentAppointments()[0]?.globalId).toBe('recent');
  });

  it('shows a load error when the agenda request fails', () => {
    appointments.list.mockReturnValueOnce(throwError(() => new Error('unavailable')));
    appointments.list.mockReturnValueOnce(of(emptyPage()));
    component.loadDashboard();

    expect(component.loadError()).toBe(true);
    expect(component.loading()).toBe(false);
  });

  it('does not request appointments for a role without appointment visibility', () => {
    const callsBeforeDenial = appointments.list.mock.calls.length;
    auth.canReadAppointments = () => false;

    component.loadDashboard();

    expect(appointments.list).toHaveBeenCalledTimes(callsBeforeDenial);
    expect(component.appointments()).toEqual([]);
    auth.canReadAppointments = () => true;
  });
});

function appointment(globalId: string, status: 'SCHEDULED' | 'COMPLETED') {
  const now = new Date();
  now.setDate(now.getDate() + 1);
  now.setHours(12, 0, 0, 0);
  return { globalId, clinicUnitId: 'clinic-1', patientId: 'patient-1', patientName: 'Jordan Silva', practitionerId: 'practitioner-1', practitionerName: 'Dr. Rowe', startAt: now.toISOString(), endAt: new Date(now.getTime() + 1_800_000).toISOString(), schedulingTimezone: 'Europe/Lisbon', status };
}

function emptyPage() {
  return page([], 12);
}

function page(content: ReturnType<typeof appointment>[], size: number) {
  return { content, page: 0, size, totalElements: content.length, totalPages: content.length ? 1 : 0 };
}
