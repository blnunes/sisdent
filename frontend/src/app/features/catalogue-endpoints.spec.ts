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
import { provideTranslateService } from '@ngx-translate/core';

describe('catalogue feature endpoints', () => {
  const cases: [Type<unknown>, string][] = [
    [SpecialitiesComponent, '/api/specialities'],
    [AddressesComponent, '/api/addresses'],
    [CountriesComponent, '/api/countries'],
    [AdministrativeDivisionsComponent, '/api/administrative-divisions'],
  ];

  it.each(cases)('%s loads only its own endpoint', (componentType, endpoint) => {
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
    TestBed.runInInjectionContext(() => new (componentType as Type<{ load(): void }>)());
    const http = TestBed.inject(HttpTestingController);
    const request = http.expectOne((candidate) => candidate.url === endpoint);
    expect(request.request.method).toBe('GET');
    request.flush({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
    http.verify();
    TestBed.resetTestingModule();
  });

  it('surfaces the continent lookup error without opening the country dialog', () => {
    const dialog = { open: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService(),
        { provide: MatDialog, useValue: dialog },
        {
          provide: AuthService,
          useValue: { activeMembership: signal(null), hasPermission: () => true },
        },
      ],
    });
    const component = TestBed.runInInjectionContext(() => new CountriesComponent());
    const http = TestBed.inject(HttpTestingController);
    http
      .expectOne((candidate) => candidate.url === '/api/countries')
      .flush({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
    component.create();
    http
      .expectOne('/api/countries/continents')
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
      .expectOne((candidate) => candidate.url === '/api/specialities')
      .flush({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });

    window.dispatchEvent(new Event(LANGUAGE_CHANGED_EVENT));

    http
      .expectOne((candidate) => candidate.url === '/api/specialities')
      .flush({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
    http.verify();
  });

  it('renders the localized display name while preserving the canonical speciality name', () => {
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
    const component = TestBed.runInInjectionContext(() => new SpecialitiesComponent());
    const http = TestBed.inject(HttpTestingController);
    http
      .expectOne((candidate) => candidate.url === '/api/specialities')
      .flush({
        content: [
          { id: 1, name: 'Pediatric Dentistry', displayName: 'Odontopediatria', procedures: [] },
        ],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
      });

    expect(component.rows()[0].cells['name']).toBe('Odontopediatria');
    expect(component.records()[0]['name']).toBe('Pediatric Dentistry');
    http.verify();
  });
});
