import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { AppointmentsComponent } from './appointments.component';
import { AppointmentGraphqlService } from '../../core/appointment-graphql.service';
import { AuthService } from '../../core/auth.service';
import { OrganizationReadGraphqlService } from '../../core/organization-read-graphql.service';
import { PatientApiService } from '../patients/patient-api.service';
import { GraphQlUserError } from '../../core/graphql-client.service';

describe('AppointmentsComponent', () => {
  let fixture: ComponentFixture<AppointmentsComponent>;
  let component: AppointmentsComponent;
  const membership = signal({
    id: 'membership-1', organizationId: 'organization-1', organizationName: 'Dental',
    clinicUnitId: 'clinic-1', clinicUnitName: 'Central', role: 'APPOINTMENT_MANAGER' as const, version: 1,
  });
  const appointmentApi = { list: vi.fn(), create: vi.fn(), reschedule: vi.fn(), transition: vi.fn() };
  const organizationReads = { listPractitioners: vi.fn(), listClinicUnits: vi.fn() };
  const patientApi = { list: vi.fn() };

  beforeEach(() => {
    appointmentApi.list.mockReturnValue(of(emptyPage()));
    organizationReads.listPractitioners.mockReturnValue(of([]));
    organizationReads.listClinicUnits.mockReturnValue(of([]));
    patientApi.list.mockReturnValue(of(emptyPage()));
    TestBed.configureTestingModule({
      imports: [AppointmentsComponent],
      providers: [
        { provide: AuthService, useValue: { activeMembership: membership } },
        { provide: AppointmentGraphqlService, useValue: appointmentApi },
        { provide: OrganizationReadGraphqlService, useValue: organizationReads },
        { provide: PatientApiService, useValue: patientApi },
        { provide: TranslateService, useValue: { instant: (key: string) => key } },
      ],
    });
    TestBed.overrideComponent(AppointmentsComponent, { set: { template: '' } });
    fixture = TestBed.createComponent(AppointmentsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('shows the conflict message when the create mutation returns its stable GraphQL conflict code', () => {
    appointmentApi.create.mockReturnValue(throwError(() => new GraphQlUserError('SCHEDULING.PRACTITIONER_UNAVAILABLE', 'Unavailable')));
    component.patientId = 'patient-1';
    component.practitionerId = 'practitioner-1';
    component.appointmentDate = new Date('2030-01-01T00:00:00Z');
    component.startTime = '10:00';
    component.endTime = '10:30';

    component.create();

    expect(component.error()).toBe('APPOINTMENTS.ERROR.CONFLICT');
  });
});

function emptyPage() {
  return { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 };
}
