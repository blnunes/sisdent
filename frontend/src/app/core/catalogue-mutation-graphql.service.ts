import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { LanguageService } from './language.service';
import { CountryCatalogueItem } from './country-catalog-graphql.service';
import { GraphQlClientService } from './graphql-client.service';
import { SpecialityCatalogueItem } from './speciality-catalog-graphql.service';

type Translation = { locale: string; value: string };
export type CountryWrite = { name: string; code: string; continent: string };
export type SpecialityWrite = {
  name: string;
  translations: Readonly<Record<string, string>>;
  procedures: readonly { id?: number; name: string; translations?: Readonly<Record<string, string>> }[];
};

const COUNTRY_FIELDS = 'id name displayName code continent';
const SPECIALITY_FIELDS = 'id name displayName status procedures { id name displayName }';

/** Dedicated write transport for platform catalogues. Components never own GraphQL documents. */
@Injectable({ providedIn: 'root' })
export class CatalogueMutationGraphqlService {
  private readonly graphql = inject(GraphQlClientService);
  private readonly language = inject(LanguageService);

  saveCountry(record: CountryCatalogueItem | undefined, input: CountryWrite): Observable<CountryCatalogueItem> {
    const operation = record ? 'updateCountry' : 'createCountry';
    const query = `mutation SaveCountry($id: ID, $input: CountryMutationInput!, $locale: String) {
      ${operation}${record ? '(id: $id, input: $input, locale: $locale)' : '(input: $input, locale: $locale)'} { ${COUNTRY_FIELDS} }
    }`;
    return this.graphql.query<Record<string, CountryCatalogueItem>>(query, {
      id: record?.id,
      input,
      locale: this.language.current(),
    }).pipe(map((response) => response[operation]));
  }

  saveSpeciality(record: SpecialityCatalogueItem | undefined, input: SpecialityWrite): Observable<SpecialityCatalogueItem> {
    const operation = record ? 'updateSpeciality' : 'createSpeciality';
    const query = `mutation SaveSpeciality($id: ID, $input: SpecialityMutationInput!, $locale: String) {
      ${operation}${record ? '(id: $id, input: $input, locale: $locale)' : '(input: $input, locale: $locale)'} { ${SPECIALITY_FIELDS} }
    }`;
    return this.graphql.query<Record<string, SpecialityCatalogueItem>>(query, {
      id: record?.id,
      input: {
        name: input.name,
        translations: translationEntries(input.translations),
        procedures: input.procedures.map((procedure) => ({
          ...(procedure.id === undefined ? {} : { id: String(procedure.id) }),
          name: procedure.name,
          translations: translationEntries(procedure.translations ?? {}),
        })),
      },
      locale: this.language.current(),
    }).pipe(map((response) => response[operation]));
  }
}

function translationEntries(translations: Readonly<Record<string, string>>): Translation[] {
  return Object.entries(translations)
    .filter(([, value]) => value.trim().length > 0)
    .map(([locale, value]) => ({ locale, value: value.trim() }));
}
