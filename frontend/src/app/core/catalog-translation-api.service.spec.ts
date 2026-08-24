import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  CatalogTranslationApiService,
  CatalogTranslationEntry,
} from './catalog-translation-api.service';

describe('CatalogTranslationApiService', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }),
  );

  it('filters the platform translation catalogue', () => {
    const service = TestBed.inject(CatalogTranslationApiService);
    service.list('SPECIALITY', ' implant ').subscribe();
    const request = TestBed.inject(HttpTestingController).expectOne('/graphql');
    expect(request.request.body.query).toContain('query CatalogTranslations');
    expect(request.request.body.variables).toEqual({ type: 'SPECIALITY', query: 'implant' });
    request.flush({ data: { catalogTranslations: [] } });
  });

  it('replaces all supported locale values for one entry', () => {
    const service = TestBed.inject(CatalogTranslationApiService);
    const entry: CatalogTranslationEntry = {
      resourceType: 'PROCEDURE',
      resourceId: 9,
      canonicalName: 'Exam',
      translations: {},
      customizedLocales: [],
      missingLocales: ['en', 'pt-PT', 'nl'],
    };
    service.replace(entry, { en: 'Exam', 'pt-PT': 'Exame', nl: 'Onderzoek' }).subscribe();
    const request = TestBed.inject(HttpTestingController).expectOne('/graphql');
    expect(request.request.body.query).toContain('mutation ReplaceCatalogTranslations');
    expect(request.request.body.variables.translations).toContainEqual({ locale: 'pt-PT', value: 'Exame' });
    request.flush({ data: { replaceCatalogTranslations: { ...entry, translations: [{ locale: 'en', value: 'Exam' }, { locale: 'pt-PT', value: 'Exame' }, { locale: 'nl', value: 'Onderzoek' }] } } });
  });
});
