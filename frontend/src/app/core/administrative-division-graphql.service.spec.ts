import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AdministrativeDivisionGraphqlService } from './administrative-division-graphql.service';

describe('AdministrativeDivisionGraphqlService', () => {
  let service: AdministrativeDivisionGraphqlService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AdministrativeDivisionGraphqlService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('maps page direction and create/update operations to GraphQL', () => {
    service.list(2, 25, 'name', 'desc').subscribe();
    expectRequest('query AdministrativeDivisions', { page: { page: 2, size: 25, sort: 'name', direction: 'DESC' } })
      .flush({ data: { administrativeDivisions: { content: [] } } });

    service.save(undefined, { name: 'Lisbon', code: 'LX', type: 'DISTRICT', countryCode: 'PT' }).subscribe();
    expectRequest('createAdministrativeDivision', {
      id: undefined, input: { name: 'Lisbon', code: 'LX', type: 'DISTRICT', countryCode: 'PT' },
    }).flush({ data: { createAdministrativeDivision: { id: 'division-1' } } });

    service.save({ id: 'division-1' } as never, { name: 'Porto', code: 'PT', type: 'DISTRICT', countryCode: 'PT' }).subscribe();
    expectRequest('updateAdministrativeDivision', {
      id: 'division-1', input: { name: 'Porto', code: 'PT', type: 'DISTRICT', countryCode: 'PT' },
    }).flush({ data: { updateAdministrativeDivision: { id: 'division-1' } } });
  });

  it('maps deletion to the selected identifier', () => {
    service.delete('division-1').subscribe();

    expectRequest('deleteAdministrativeDivision', { id: 'division-1' })
      .flush({ data: { deleteAdministrativeDivision: true } });
  });

  function expectRequest(operation: string, variables: unknown) {
    const request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain(operation);
    expect(request.request.body.variables).toEqual(variables);
    return request;
  }
});
