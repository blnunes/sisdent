import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HomeComponent } from './home.component';
import { AuthService } from '../../core/auth.service';

describe('HomeComponent', () => {
  let fixture: ComponentFixture<HomeComponent>;
  let component: HomeComponent;
  let http: HttpTestingController;
  const membership = signal({ id: 'membership-1', organizationId: 'organization-1', organizationName: 'Dental', clinicUnitId: 'clinic-1', clinicUnitName: 'Central', role: 'APPOINTMENT_MANAGER' as const, version: 1 });
  const auth = {
    activeMembership: membership,
    session: signal({ displayName: 'Taylor', memberships: [] }),
    canReadAppointments: () => true,
    canManageAppointments: () => true,
    canReadClinical: () => false,
    hasAnyPermission: () => false,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: AuthService, useValue: auth }],
    });
    TestBed.overrideComponent(HomeComponent, { set: { template: '' } });
    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('loads seven-day appointments for the active clinic and derives the next scheduled visit', () => {
    const request = http.expectOne((candidate) => candidate.url === '/api/organizations/organization-1/appointments' && candidate.params.get('size') === '12');
    expect(request.request.params.get('clinicUnitId')).toBe('clinic-1');
    expect(request.request.params.get('size')).toBe('12');
    request.flush({ content: [appointment('scheduled', 'SCHEDULED'), appointment('completed', 'COMPLETED')], page: 0, size: 12, totalElements: 2, totalPages: 1 });
    http.expectOne((candidate) => candidate.url === '/api/organizations/organization-1/appointments' && candidate.params.get('size') === '4').flush({ content: [appointment('recent', 'COMPLETED')], page: 0, size: 4, totalElements: 1, totalPages: 1 });

    expect(component.appointments()).toHaveLength(2);
    expect(component.nextAppointment()?.globalId).toBe('scheduled');
    expect(component.scheduledToday()).toBe(0);
    expect(component.recentAppointments()[0]?.globalId).toBe('recent');
  });

  it('shows a load error when the agenda request fails', () => {
    const request = http.expectOne((candidate) => candidate.url === '/api/organizations/organization-1/appointments' && candidate.params.get('size') === '12');
    request.flush('unavailable', { status: 503, statusText: 'Unavailable' });
    http.expectOne((candidate) => candidate.url === '/api/organizations/organization-1/appointments' && candidate.params.get('size') === '4').flush({ content: [], page: 0, size: 4, totalElements: 0, totalPages: 0 });

    expect(component.loadError()).toBe(true);
    expect(component.loading()).toBe(false);
  });

  it('does not request appointments for a role without appointment visibility', () => {
    const initialRequests = http.match((candidate) => candidate.url === '/api/organizations/organization-1/appointments');
    initialRequests.forEach((request) => request.flush({ content: [], page: 0, size: 12, totalElements: 0, totalPages: 0 }));
    auth.canReadAppointments = () => false;

    component.loadDashboard();

    http.expectNone((candidate) => candidate.url === '/api/organizations/organization-1/appointments');
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
