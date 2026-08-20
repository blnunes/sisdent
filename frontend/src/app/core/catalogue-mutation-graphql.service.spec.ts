import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { CatalogueMutationGraphqlService } from './catalogue-mutation-graphql.service';
import { LanguageService } from './language.service';

describe('CatalogueMutationGraphqlService', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), { provide: LanguageService, useValue: { current: signal('pt-PT').asReadonly() } }] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('sends exact typed country mutation variables and maps the response', () => {
    const result = vi.fn();
    TestBed.inject(CatalogueMutationGraphqlService).saveCountry(undefined, { name: 'Portugal', code: 'PT', continent: 'EUROPE' }).subscribe(result);
    const request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain('mutation SaveCountry');
    expect(request.request.body.query).toContain('createCountry(input: $input, locale: $locale)');
    expect(request.request.body.variables).toEqual({ id: undefined, input: { name: 'Portugal', code: 'PT', continent: 'EUROPE' }, locale: 'pt-PT' });
    request.flush({ data: { createCountry: { id: '1', name: 'Portugal', displayName: 'Portugal', code: 'PT', continent: 'EUROPE' } } });
    expect(result).toHaveBeenCalledWith(expect.objectContaining({ code: 'PT' }));
  });

  it('uses typed translation entries and preserves safe GraphQL errors', () => {
    const failure = vi.fn();
    TestBed.inject(CatalogueMutationGraphqlService).saveSpeciality(undefined, {
      name: 'Endodontics', translations: { en: 'Endodontics', nl: '' }, procedures: [{ name: 'Root canal', translations: { en: 'Root canal' } }],
    }).subscribe({ error: failure });
    const request = http.expectOne('/graphql');
    expect(request.request.body.variables.input).toEqual({ name: 'Endodontics', translations: [{ locale: 'en', value: 'Endodontics' }], procedures: [{ name: 'Root canal', translations: [{ locale: 'en', value: 'Root canal' }] }] });
    request.flush({ errors: [{ message: 'One or more fields are invalid.', extensions: { code: 'VALIDATION.FAILED', correlationId: 'mutation-42' } }] });
    expect(failure).toHaveBeenCalledWith(expect.objectContaining({ code: 'VALIDATION.FAILED', correlationId: 'mutation-42' }));
  });
});
