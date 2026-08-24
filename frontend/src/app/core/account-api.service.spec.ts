import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
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
    expectGraphQl('query ClinicUnits', { organizationId: 'northstar', clinicUnitId: null });
    service.createClinicUnit('northstar', { name: 'Riverside' }).subscribe();
    expectGraphQl('mutation CreateClinicUnit', {
      organizationId: 'northstar',
      input: { name: 'Riverside' },
    });
  });
  it('preserves optimistic-lock variables for membership changes', () => {
    service
      .changeMembershipRole('northstar', 'membership-1', { role: 'MANAGER', version: 3 })
      .subscribe();
    expectGraphQl('mutation ChangeMembershipRole', {
      organizationId: 'northstar',
      membershipId: 'membership-1',
      input: { role: 'MANAGER', version: 3 },
    });
    service.revokeMembership('northstar', 'membership-1', 3).subscribe();
    expectGraphQl('mutation RevokeMembership', {
      organizationId: 'northstar',
      membershipId: 'membership-1',
      input: { version: 3 },
    });
  });

  function expectGraphQl(operation: string, variables: unknown): void {
    const request = http.expectOne('/graphql').request;
    expect(request.body.query).toContain(operation);
    expect(request.body.variables).toEqual(variables);
  }
});
