import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { LanguageService } from '../../core/language.service';
import { PatientsComponent } from './patients.component';

describe('PatientsComponent country data', () => {
  const dialog = { open: vi.fn(() => ({ afterClosed: () => of(undefined) })) };

  beforeEach(() => {
    dialog.open.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService(),
        { provide: MatDialog, useValue: dialog },
        { provide: LanguageService, useValue: { current: signal('en') } },
        {
          provide: AuthService,
          useValue: {
            activeMembership: signal({
              id: 'membership-1',
              organizationId: 'organization-1',
              organizationName: 'Clinic',
              role: 'MANAGER',
              version: 1,
            }),
            hasPermission: () => true,
          },
        },
      ],
    });
  });

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('loads nationality options through the country GraphQL query', () => {
    const component = TestBed.runInInjectionContext(() => new PatientsComponent());
    (component as unknown as { loadNationalityOptions(): void }).loadNationalityOptions();

    const request = TestBed.inject(HttpTestingController).expectOne('/graphql');
    expect(request.request.body.variables).toEqual({
      page: { page: 0, size: 100, sort: 'name', direction: 'ASC' },
      locale: 'en',
    });
    request.flush({
      data: {
        countries: {
          content: [{ id: '1', code: 'PT', name: 'Portugal', displayName: 'Portugal', continent: 'EUROPE' }],
          page: 0,
          size: 100,
          totalElements: 1,
          totalPages: 1,
        },
      },
    });
    expect(component.filters().find((filter) => filter.key === 'nationalityCode')?.options).toEqual([
      { value: 'PT', label: 'Portugal (PT)' },
    ]);
  });

  it('sends the active patient filter as a GraphQL Boolean', () => {
    const component = TestBed.runInInjectionContext(() => new PatientsComponent());
    component.updateFilter({ key: 'active', value: 'false' });
    component.load();

    const request = TestBed.inject(HttpTestingController).expectOne((candidate) =>
      candidate.url === '/graphql' && candidate.body.query.includes('query Patients'),
    );
    expect(request.request.body.variables.filter).toEqual({ active: false });
    request.flush({ data: { patients: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 } } });
  });

  it('loads patient editor country data through the country GraphQL query', () => {
    const component = TestBed.runInInjectionContext(() => new PatientsComponent());

    component.create();

    const http = TestBed.inject(HttpTestingController);
    const specialitiesRequest = http.expectOne((request) =>
      request.url === '/graphql' && request.body.query.includes('query Specialities'),
    );
    expect(specialitiesRequest.request.body.variables).toEqual({
      page: { page: 0, size: 100, sort: 'name', direction: 'ASC' },
      filter: {},
      locale: 'en',
    });
    specialitiesRequest.flush({ data: { specialities: {
      content: [], page: 0, size: 100, totalElements: 0, totalPages: 0,
    } } });
    http.expectOne((request) =>
      request.url === '/graphql' && request.body.query.includes('query AdministrativeDivisions'),
    ).flush({ data: { administrativeDivisions: {
      content: [], page: 0, size: 100, totalElements: 0, totalPages: 0,
    } } });
    http.expectOne((request) =>
      request.url === '/graphql' && request.body.query.includes('query Countries'),
    ).flush({
      data: {
        countries: {
          content: [{ id: '1', code: 'PT', name: 'Portugal', displayName: 'Portugal', continent: 'EUROPE' }],
          page: 0,
          size: 100,
          totalElements: 1,
          totalPages: 1,
        },
      },
    });

    expect(dialog.open).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ data: expect.objectContaining({ countries: [expect.objectContaining({ code: 'PT' })] }) }),
    );
  });
});
