import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { CountryCatalogGraphqlService } from './country-catalog-graphql.service';
import { GraphQlUserError } from './graphql-client.service';
import { LanguageService } from './language.service';

describe('CountryCatalogGraphqlService', () => {
  let http: HttpTestingController;
  const language = signal('pt-PT');

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LanguageService, useValue: { current: language.asReadonly() } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    language.set('pt-PT');
  });

  afterEach(() => http.verify());

  it('posts the countries operation with exact pagination and active catalogue locale variables', () => {
    const result = vi.fn();
    TestBed.inject(CountryCatalogGraphqlService)
      .list({ page: 2, size: 20, sort: 'name', direction: 'desc' })
      .subscribe(result);

    const request = http.expectOne('/graphql');
    expect(request.request.method).toBe('POST');
    expect(request.request.body.variables).toEqual({
      page: { page: 2, size: 20, sort: 'name', direction: 'DESC' },
      locale: 'pt-PT',
    });
    expect(request.request.body.query).toContain('query Countries($page: CataloguePageInput');
    expect(request.request.body.query).toContain('content { id code name displayName continent }');
    request.flush({
      data: {
        countries: {
          content: [
            { id: '1', code: 'PT', name: 'Portugal', displayName: 'Portugal', continent: 'EUROPE' },
          ],
          page: 2,
          size: 20,
          totalElements: 31,
          totalPages: 2,
        },
      },
    });

    expect(result).toHaveBeenCalledWith({
      content: [
        { id: '1', code: 'PT', name: 'Portugal', displayName: 'Portugal', continent: 'EUROPE' },
      ],
      page: 2,
      size: 20,
      totalElements: 31,
      totalPages: 2,
    });
  });

  it.each([
    ['PAGINATION.INVALID_VALUES', 'The requested page is invalid.'],
    ['PAGINATION.UNSUPPORTED_SORT', 'That sort field is not supported.'],
    ['PAGINATION.UNSUPPORTED_DIRECTION', 'That sort direction is not supported.'],
    ['CATALOG.UNSUPPORTED_LOCALE', 'This catalogue language is not available.'],
  ])('maps GraphQL %s to its exact safe code and message', (code, message) => {
    const failure = vi.fn();
    TestBed.inject(CountryCatalogGraphqlService)
      .list({ page: 0, size: 10, sort: 'name', direction: 'asc' })
      .subscribe({ error: failure });
    http.expectOne('/graphql').flush({
      errors: [
        {
          message,
          extensions: { code, correlationId: 'correlation-123', internal: 'never expose' },
        },
      ],
    });

    expect(failure).toHaveBeenCalledWith(
      expect.objectContaining({ code, message, correlationId: 'correlation-123' }),
    );
  });

  it('maps authorization transport failures without exposing their response body', () => {
    const failure = vi.fn();
    TestBed.inject(CountryCatalogGraphqlService)
      .list({ page: 0, size: 10, sort: 'name', direction: 'asc' })
      .subscribe({ error: failure });
    http
      .expectOne('/graphql')
      .flush('sensitive server details', { status: 403, statusText: 'Forbidden' });

    expect(failure).toHaveBeenCalledWith(
      expect.objectContaining({
        code: 'AUTHORIZATION.FORBIDDEN',
        message: 'You are not authorized to view the country catalogue.',
      }),
    );
  });

  it('maps authentication transport failures to the safe session-expired error', () => {
    const failure = vi.fn();
    TestBed.inject(CountryCatalogGraphqlService)
      .list({ page: 0, size: 10, sort: 'name', direction: 'asc' })
      .subscribe({ error: failure });
    http.expectOne('/graphql').flush('token details must not reach the screen', {
      status: 401,
      statusText: 'Unauthorized',
    });

    expect(failure).toHaveBeenCalledWith(
      expect.objectContaining({
        code: 'AUTHENTICATION.UNAUTHORIZED',
        message: 'Your session has expired. Please sign in again.',
      }),
    );
  });

  it('rejects an unsupported locale before issuing a GraphQL request', () => {
    const failure = vi.fn();
    TestBed.inject(CountryCatalogGraphqlService)
      .list({ page: 0, size: 10, sort: 'name', direction: 'asc' }, 'fr')
      .subscribe({ error: failure });

    expect(failure).toHaveBeenCalledWith(
      expect.objectContaining({
        code: 'CATALOG.UNSUPPORTED_LOCALE',
        message: 'The selected language is not supported for the country catalogue.',
      }),
    );
  });

  it('accepts supported regional locale variants', () => {
    TestBed.inject(CountryCatalogGraphqlService)
      .list({ page: 0, size: 10, sort: 'name', direction: 'asc' }, 'nl-BE')
      .subscribe();
    const request = http.expectOne('/graphql');
    expect(request.request.body.variables.locale).toBe('nl-BE');
    request.flush({ data: { countries: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 } } });
  });
});
