import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { localeInterceptor } from './locale.interceptor';
import { HttpClient } from '@angular/common/http';

describe('localeInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([localeInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('sends the selected supported language to API endpoints', () => {
    localStorage.setItem('sisdent.language', 'nl');

    http.get('/api/specialities').subscribe();

    const request = controller.expectOne('/api/specialities');
    expect(request.request.headers.get('Accept-Language')).toBe('nl');
    request.flush({});
  });

  it('uses English for invalid saved values and does not alter translation asset calls', () => {
    localStorage.setItem('sisdent.language', 'unsupported');
    http.get('/api/countries/continents').subscribe();
    http.get('/i18n/en.json').subscribe();

    const apiRequest = controller.expectOne('/api/countries/continents');
    expect(apiRequest.request.headers.get('Accept-Language')).toBe('en');
    apiRequest.flush({});
    const assetRequest = controller.expectOne('/i18n/en.json');
    expect(assetRequest.request.headers.has('Accept-Language')).toBe(false);
    assetRequest.flush({});
  });
});
