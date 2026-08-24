import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal, Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { AuthService } from '../core/auth.service';
import { AddressesComponent } from './addresses/addresses.component';
import { AdministrativeDivisionsComponent } from './administrative-divisions/administrative-divisions.component';
import { CountriesComponent } from './countries/countries.component';
import { SpecialitiesComponent } from './specialities/specialities.component';
import { LANGUAGE_CHANGED_EVENT } from '../core/language.service';
import { LanguageService } from '../core/language.service';
import { provideTranslateService } from '@ngx-translate/core';

describe('catalogue feature endpoints', () => {
  const featureSources = import.meta.glob('./**/*.ts', {
    query: '?raw',
    import: 'default',
    eager: true,
  }) as Record<string, string>;

  it('contains no retired REST consumer or direct HTTP orchestration', () => {
    const productionSources = Object.entries(featureSources).filter(
      ([path]) => !path.endsWith('.spec.ts'),
    );
    const violations = productionSources.filter(([, source]) => /\/api\/|HttpClient/.test(source));

    expect(violations.map(([path]) => path)).toEqual([]);
  });

  it('loads administrative divisions through GraphQL', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService(),
        { provide: MatDialog, useValue: { open: vi.fn() } },
        {
          provide: AuthService,
          useValue: { activeMembership: signal(null), hasPermission: () => true },
        },
      ],
    });
    TestBed.runInInjectionContext(() => new AdministrativeDivisionsComponent());
    const request = TestBed.inject(HttpTestingController).expectOne('/graphql');
    expect(request.request.body.query).toContain('query AdministrativeDivisions');
    request.flush({
      data: {
        administrativeDivisions: {
          content: [],
          page: 0,
          size: 10,
          totalElements: 0,
          totalPages: 0,
        },
      },
    });
    TestBed.inject(HttpTestingController).verify();
  });

  it('loads addresses through GraphQL', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService(),
        { provide: MatDialog, useValue: { open: vi.fn() } },
        {
          provide: AuthService,
          useValue: { activeMembership: signal(null), hasPermission: () => true },
        },
      ],
    });
    TestBed.runInInjectionContext(() => new AddressesComponent());
    const http = TestBed.inject(HttpTestingController);
    const request = http.expectOne('/graphql');
    expect(request.request.body.query).toContain('query Addresses');
    request.flush({
      data: { addresses: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 } },
    });
    http.verify();
    TestBed.resetTestingModule();
  });

  it('loads the country catalogue through GraphQL', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService(),
        { provide: MatDialog, useValue: { open: vi.fn() } },
        { provide: LanguageService, useValue: { current: signal('en') } },
        {
          provide: AuthService,
          useValue: { activeMembership: signal(null), hasPermission: () => true },
        },
      ],
    });
    TestBed.runInInjectionContext(() => new CountriesComponent());
    const request = TestBed.inject(HttpTestingController).expectOne('/graphql');
    expect(request.request.body.variables).toEqual({
      page: { page: 0, size: 10, sort: 'id', direction: 'ASC' },
      locale: 'en',
    });
    request.flush({
      data: { countries: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 } },
    });
    TestBed.inject(HttpTestingController).verify();
  });

  it('shows the safe GraphQL error message for the country catalogue', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService(),
        { provide: MatDialog, useValue: { open: vi.fn() } },
        { provide: LanguageService, useValue: { current: signal('en') } },
        {
          provide: AuthService,
          useValue: { activeMembership: signal(null), hasPermission: () => true },
        },
      ],
    });
    const component = TestBed.runInInjectionContext(() => new CountriesComponent());
    TestBed.inject(HttpTestingController)
      .expectOne('/graphql')
      .flush({
        errors: [
          {
            message: 'That sort field is not supported.',
            extensions: { code: 'PAGINATION.UNSUPPORTED_SORT', correlationId: 'support-42' },
          },
        ],
      });

    expect(component.error()).toBe(true);
    expect(component.errorMessage()).toBe('That sort field is not supported.');
    TestBed.inject(HttpTestingController).verify();
  });

  it('surfaces the continent lookup error without opening the country dialog', () => {
    const dialog = { open: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService(),
        { provide: MatDialog, useValue: dialog },
        { provide: LanguageService, useValue: { current: signal('en') } },
        {
          provide: AuthService,
          useValue: { activeMembership: signal(null), hasPermission: () => true },
        },
      ],
    });
    const component = TestBed.runInInjectionContext(() => new CountriesComponent());
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/graphql').flush({
      data: { countries: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 } },
    });
    component.create();
    http
      .expectOne(
        (request) => request.url === '/graphql' && request.body.query.includes('query Continents'),
      )
      .flush('failure', { status: 500, statusText: 'Server error' });
    expect(component.error()).toBe(true);
    expect(dialog.open).not.toHaveBeenCalled();
    http.verify();
  });

  it('reloads localized catalogue data when the language changes', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService(),
        { provide: LanguageService, useValue: { current: signal('en') } },
        { provide: MatDialog, useValue: { open: vi.fn() } },
        {
          provide: AuthService,
          useValue: { activeMembership: signal(null), hasPermission: () => true },
        },
      ],
    });
    TestBed.runInInjectionContext(() => new SpecialitiesComponent());
    const http = TestBed.inject(HttpTestingController);
    http
      .expectOne('/graphql')
      .flush({
        data: { specialities: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 } },
      });

    window.dispatchEvent(new Event(LANGUAGE_CHANGED_EVENT));

    http
      .expectOne('/graphql')
      .flush({
        data: { specialities: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 } },
      });
    http.verify();
  });

  it('renders the localized display name while preserving the canonical speciality name', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService(),
        { provide: LanguageService, useValue: { current: signal('en') } },
        { provide: MatDialog, useValue: { open: vi.fn() } },
        {
          provide: AuthService,
          useValue: { activeMembership: signal(null), hasPermission: () => true },
        },
      ],
    });
    const component = TestBed.runInInjectionContext(() => new SpecialitiesComponent());
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/graphql').flush({
      data: {
        specialities: {
          content: [
            { id: 1, name: 'Pediatric Dentistry', displayName: 'Odontopediatria', procedures: [] },
          ],
          page: 0,
          size: 10,
          totalElements: 1,
          totalPages: 1,
        },
      },
    });

    expect(component.rows()[0].cells['name']).toBe('Odontopediatria');
    expect(component.records()[0]['name']).toBe('Pediatric Dentistry');
    http.verify();
  });
});
