import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { OrganizationMutationGraphqlService } from './organization-mutation-graphql.service';

describe('OrganizationMutationGraphqlService', () => {
  let http: HttpTestingController;
  beforeEach(() => { TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] }); http = TestBed.inject(HttpTestingController); });
  afterEach(() => http.verify());

  it('sends a scoped practitioner update without REST transport details', () => {
    const result = vi.fn();
    TestBed.inject(OrganizationMutationGraphqlService).savePractitioner('org-1', 'practitioner-1', { displayName: 'Dr Ada', registrationNumber: 'R1', accountId: null, specialityIds: [4, 8] }).subscribe(result);
    const request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain('updatePractitioner(organizationId: $organizationId, practitionerId: $practitionerId, input: $input)');
    expect(request.request.body.variables).toEqual({ organizationId: 'org-1', practitionerId: 'practitioner-1', input: { displayName: 'Dr Ada', registrationNumber: 'R1', accountId: null, specialityIds: ['4', '8'] } });
    request.flush({ data: { updatePractitioner: { globalId: 'practitioner-1', displayName: 'Dr Ada', registrationNumber: 'R1', accountId: null, active: true, specialityIds: ['4', '8'] } } });
    expect(result).toHaveBeenCalledWith(expect.objectContaining({ globalId: 'practitioner-1' }));
  });
});
