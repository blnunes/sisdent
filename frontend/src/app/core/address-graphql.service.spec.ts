import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AddressGraphqlService } from './address-graphql.service';

describe('AddressGraphqlService', () => {
  let service: AddressGraphqlService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AddressGraphqlService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('maps pages and postal-code suggestions to GraphQL variables', () => {
    service.list(1, 20, 'street', 'desc').subscribe();
    expectRequest('query Addresses', { page: { page: 1, size: 20, sort: 'street', direction: 'DESC' } })
      .flush({ data: { addresses: { content: [] } } });
    service.postalCodeSuggestions('PT', '1000').subscribe();
    expectRequest('query AddressPostalCodeSuggestions', { countryCode: 'PT', query: '1000' })
      .flush({ data: { addressPostalCodeSuggestions: [] } });
  });

  it('creates, updates, and deletes addresses with their identifiers', () => {
    const input = { street: 'Main', city: 'Lisbon', postalCode: '1000', countryCode: 'PT' };
    service.save(undefined, input).subscribe();
    expectRequest('createAddress', { id: undefined, input })
      .flush({ data: { createAddress: { id: 'address-1' } } });
    service.save({ id: 'address-1' } as never, input).subscribe();
    expectRequest('updateAddress', { id: 'address-1', input })
      .flush({ data: { updateAddress: { id: 'address-1' } } });
    service.delete('address-1').subscribe();
    expectRequest('deleteAddress', { id: 'address-1' })
      .flush({ data: { deleteAddress: true } });
  });

  function expectRequest(operation: string, variables: unknown) {
    const request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain(operation);
    expect(request.request.body.variables).toEqual(variables);
    return request;
  }
});
