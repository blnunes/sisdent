import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { LanguageService } from './language.service';
import { SpecialityCatalogGraphqlService } from './speciality-catalog-graphql.service';

describe('SpecialityCatalogGraphqlService', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(), provideHttpClientTesting(),
      { provide: LanguageService, useValue: { current: signal('nl-BE').asReadonly() } },
    ] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('uses the typed specialities operation with filters, pagination and locale', () => {
    TestBed.inject(SpecialityCatalogGraphqlService).list({
      page: 1, size: 10, sort: 'name', direction: 'asc', filter: { name: 'ortho' },
    }).subscribe();
    const request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain('query Specialities($page: CataloguePageInput, $filter: SpecialityFilterInput');
    expect(request.request.body.variables).toEqual({
      page: { page: 1, size: 10, sort: 'name', direction: 'ASC' },
      filter: { name: 'ortho' }, locale: 'nl-BE',
    });
    request.flush({ data: { specialities: { content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 } } });
  });

  it('does not call GraphQL for unsupported locales', () => {
    const failure = vi.fn();
    TestBed.inject(SpecialityCatalogGraphqlService).list(
      { page: 0, size: 10, sort: 'name', direction: 'asc' }, 'fr-FR',
    ).subscribe({ error: failure });
    expect(failure).toHaveBeenCalledWith(expect.objectContaining({ code: 'CATALOG.UNSUPPORTED_LOCALE' }));
  });
});
