import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TestRequest } from '@angular/common/http/testing';
import { AccountApiService } from './account-api.service';

describe('AccountApiService', () => {
  let service: AccountApiService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AccountApiService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('uses typed GraphQL operations for scoped organization administration', () => {
    service.listClinicUnits('northstar').subscribe();
    expectGraphQl('query ClinicUnits', { organizationId: 'northstar', clinicUnitId: null })
      .flush({ data: { clinicUnits: [] } });
    service.createClinicUnit('northstar', { name: 'Riverside' }).subscribe();
    expectGraphQl('mutation CreateClinicUnit', {
      organizationId: 'northstar',
      input: { name: 'Riverside' },
    }).flush({ data: { createClinicUnit: { id: 'clinic-1' } } });
  });

  it('delegates practitioner lifecycle operations with the organization scope', () => {
    const practitioner = {
      displayName: 'Ada',
      registrationNumber: 'REG-1',
      specialityIds: [3],
    };
    service.listPractitioners('northstar').subscribe();
    expectGraphQl('query Practitioners', { organizationId: 'northstar', clinicUnitId: null })
      .flush({ data: { practitioners: [] } });
    service.createPractitioner('northstar', practitioner).subscribe();
    expectGraphQl('mutation SavePractitioner', {
      organizationId: 'northstar',
      practitionerId: undefined,
      input: { ...practitioner, specialityIds: ['3'] },
    }).flush({ data: { createPractitioner: { globalId: 'practitioner-1' } } });
    service.updatePractitioner('northstar', 'practitioner-1', practitioner).subscribe();
    expectGraphQl('mutation SavePractitioner', {
      organizationId: 'northstar',
      practitionerId: 'practitioner-1',
      input: { ...practitioner, specialityIds: ['3'] },
    }).flush({ data: { updatePractitioner: { globalId: 'practitioner-1' } } });
    service.deactivatePractitioner('northstar', 'practitioner-1').subscribe();
    expectGraphQl('mutation DeactivatePractitioner', {
      organizationId: 'northstar',
      practitionerId: 'practitioner-1',
    }).flush({ data: { deactivatePractitioner: true } });
  });
  it('preserves optimistic-lock variables for membership changes', () => {
    service
      .changeMembershipRole('northstar', 'membership-1', { role: 'MANAGER', version: 3 })
      .subscribe();
    expectGraphQl('mutation ChangeMembershipRole', {
      organizationId: 'northstar',
      membershipId: 'membership-1',
      input: { role: 'MANAGER', version: 3 },
    }).flush({ data: { changeMembershipRole: { id: 'membership-1' } } });
    service.revokeMembership('northstar', 'membership-1', 3).subscribe();
    expectGraphQl('mutation RevokeMembership', {
      organizationId: 'northstar',
      membershipId: 'membership-1',
      input: { version: 3 },
    }).flush({ data: { revokeMembership: true } });
  });

  it('maps account lists, mutations, and membership grants to GraphQL', () => {
    service.listPlatform({ page: 1, size: 25, sort: 'email', direction: 'desc' }, 'ada').subscribe();
    expectGraphQl('query platformAccounts', {
      page: { page: 1, size: 25, sort: 'email', direction: 'DESC' },
      filter: 'ada',
    }).flush({ data: { platformAccounts: { content: [] } } });
    service.listOrganization('northstar').subscribe();
    expectGraphQl('query organizationAccounts', {
      organizationId: 'northstar',
      page: { page: 0, size: 10, sort: 'person.displayName', direction: 'ASC' },
      filter: null,
    }).flush({ data: { organizationAccounts: { content: [] } } });
    service.changeLifecycle({ id: 'account-1', version: 4 } as never, false).subscribe();
    expectGraphQl('mutation changeAccountLifecycle', {
      accountId: 'account-1',
      input: { active: false, version: 4 },
    }).flush({ data: { changeAccountLifecycle: { id: 'account-1' } } });
    service.changePlatformAdministrator({ id: 'account-1', version: 4 } as never, true).subscribe();
    expectGraphQl('mutation changeAccountPlatformAdministrator', {
      accountId: 'account-1',
      input: { platformAdministrator: true, version: 4 },
    }).flush({ data: { changeAccountPlatformAdministrator: { id: 'account-1' } } });
    service.create({ displayName: 'Ada', email: 'ada@example.test', password: 'secret' }).subscribe();
    expectGraphQl('mutation createPlatformAccount', {
      input: { displayName: 'Ada', email: 'ada@example.test', password: 'secret' },
    }).flush({ data: { createPlatformAccount: { id: 'account-1' } } });
    service.listPlatformOrganizations().subscribe();
    expectGraphQl('query PlatformOrganizations', {})
      .flush({ data: { platformOrganizations: [{ id: 'northstar', name: 'Northstar', active: true }] } });
    service.grantMembership('northstar', { email: 'ada@example.test', role: 'MANAGER' }).subscribe();
    expectGraphQl('mutation GrantMembership', {
      organizationId: 'northstar',
      input: { email: 'ada@example.test', role: 'MANAGER' },
    }).flush({ data: { grantMembership: { id: 'membership-1' } } });
  });

  function expectGraphQl(operation: string, variables: unknown): TestRequest {
    const request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain(operation);
    expect(request.request.body.variables).toEqual(variables);
    return request;
  }
});
