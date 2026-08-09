import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CatalogTranslationApiService, CatalogTranslationEntry } from './catalog-translation-api.service';

describe('CatalogTranslationApiService', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] }));

  it('filters the platform translation catalogue', () => {
    const service = TestBed.inject(CatalogTranslationApiService);
    service.list('SPECIALITY', ' implant ').subscribe();
    const request = TestBed.inject(HttpTestingController).expectOne(candidate => candidate.url === '/api/platform/catalog-translations');
    expect(request.request.params.get('type')).toBe('SPECIALITY');
    expect(request.request.params.get('query')).toBe('implant');
    request.flush([]);
  });

  it('replaces all supported locale values for one entry', () => {
    const service = TestBed.inject(CatalogTranslationApiService);
    const entry: CatalogTranslationEntry = { resourceType: 'PROCEDURE', resourceId: 9, canonicalName: 'Exam', translations: {}, customizedLocales: [], missingLocales: ['en', 'pt-PT', 'nl'] };
    service.replace(entry, { en: 'Exam', 'pt-PT': 'Exame', nl: 'Onderzoek' }).subscribe();
    const request = TestBed.inject(HttpTestingController).expectOne('/api/platform/catalog-translations/PROCEDURE/9');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body.translations['pt-PT']).toBe('Exame');
    request.flush(entry);
  });
});
