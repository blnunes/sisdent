import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AccountApiService } from './account-api.service';

describe('AccountApiService organization administration requests', () => {
  let service: AccountApiService;
  let http: HttpTestingController;
  beforeEach(() => { TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] }); service = TestBed.inject(AccountApiService); http = TestBed.inject(HttpTestingController); });
  afterEach(() => http.verify());
  it('uses only the active organization in clinic-unit requests', () => {
    service.listClinicUnits('northstar').subscribe();
    expect(http.expectOne('/api/organizations/northstar/clinic-units').request.method).toBe('GET');
    service.createClinicUnit('northstar', { name: 'Riverside' }).subscribe();
    const request = http.expectOne('/api/organizations/northstar/clinic-units');
    expect(request.request.method).toBe('POST'); expect(request.request.body).toEqual({ name: 'Riverside' });
  });
  it('constructs scoped practitioner create, update, and deactivate requests', () => {
    const practitioner = { displayName: 'Dr. Ada', registrationNumber: 'REG-1', specialityIds: [4], accountId: null };
    service.listPractitioners('northstar').subscribe(); expect(http.expectOne('/api/organizations/northstar/practitioners').request.method).toBe('GET');
    service.createPractitioner('northstar', practitioner).subscribe(); expect(http.expectOne('/api/organizations/northstar/practitioners').request.body).toEqual(practitioner);
    service.updatePractitioner('northstar', 'practitioner-1', practitioner).subscribe(); expect(http.expectOne('/api/organizations/northstar/practitioners/practitioner-1').request.method).toBe('PUT');
    service.deactivatePractitioner('northstar', 'practitioner-1').subscribe(); expect(http.expectOne('/api/organizations/northstar/practitioners/practitioner-1').request.method).toBe('DELETE');
  });
  it('revokes a membership through its organization-scoped, versioned endpoint', () => {
    service.revokeMembership('northstar', 'membership-1', 3).subscribe();
    const request = http.expectOne('/api/organizations/northstar/memberships/membership-1/revoke').request;
    expect(request.method).toBe('POST'); expect(request.body).toEqual({ version: 3 });
  });
});
