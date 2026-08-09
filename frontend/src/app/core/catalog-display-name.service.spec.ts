import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { CatalogDisplayNameService } from './catalog-display-name.service';

describe('CatalogDisplayNameService', () => {
  const translations: Record<string, string> = {
    'CATALOG.SPECIALITIES.pediatric-dentistry': 'Odontopediatria',
    'CATALOG.PROCEDURES.local-anesthesia': 'Anestesia local',
  };

  beforeEach(() => TestBed.configureTestingModule({
    providers: [{
      provide: TranslateService,
      useValue: {
        getCurrentLang: () => 'pt-PT',
        instant: (key: string) => translations[key] ?? key,
      },
    }],
  }));

  it('translates fixed specialities and procedures from their canonical names', () => {
    const service = TestBed.inject(CatalogDisplayNameService);

    expect(service.speciality({ name: 'Pediatric Dentistry', displayName: 'Pediatric Dentistry' })).toBe('Odontopediatria');
    expect(service.procedure({ name: 'Local anesthesia' })).toBe('Anestesia local');
  });

  it('uses the API display name for catalogue values without a known translation', () => {
    const service = TestBed.inject(CatalogDisplayNameService);

    expect(service.speciality({ name: 'Custom speciality', displayName: 'Especialidade personalizada' })).toBe('Especialidade personalizada');
  });

  it('localizes valid country codes and safely falls back for invalid ones', () => {
    const service = TestBed.inject(CatalogDisplayNameService);

    expect(service.country({ code: 'MX', name: 'Mexico' })).toBe('México');
    expect(service.country({ code: 'MEX', displayName: 'México' })).toBe('México');
  });
});
