import { Type } from '@angular/core';
import { Route } from '@angular/router';
import { AddressesComponent } from './features/addresses/addresses.component';
import { AdministrativeDivisionsComponent } from './features/administrative-divisions/administrative-divisions.component';
import { CountriesComponent } from './features/countries/countries.component';
import { PatientsComponent } from './features/patients/patients.component';
import { SpecialitiesComponent } from './features/specialities/specialities.component';
import { routes } from './app.routes';

describe('feature routes', () => {
  const expectedComponents = new Map<string, Type<unknown>>([
    ['patients', PatientsComponent],
    ['specialities', SpecialitiesComponent],
    ['addresses', AddressesComponent],
    ['countries', CountriesComponent],
    ['administrative-divisions', AdministrativeDivisionsComponent],
  ]);

  it.each([...expectedComponents])(
    'loads /%s from its matching feature component',
    async (path, expectedComponent) => {
      const route = routeFor(path);

      const component = await route.loadComponent?.();

      expect(component as Type<unknown>).toBe(expectedComponent);
    },
  );

  it('does not use route data as domain configuration', () => {
    for (const path of expectedComponents.keys()) {
      expect(routeFor(path).data).toBeUndefined();
    }
  });

  it('keeps the fallback route after all named routes', () => {
    expect(routes.at(-1)?.path).toBe('**');
  });
});

function routeFor(path: string): Route {
  const route = routes.find((candidate) => candidate.path === path);
  expect(route, `Missing route /${path}`).toBeDefined();
  expect(route?.loadComponent, `Missing component for /${path}`).toBeTypeOf('function');
  return route as Route;
}
