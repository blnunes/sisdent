import { patientRequest, patientToForm } from './patient.models';

describe('patient mappings', () => {
  it('maps a persisted patient to editable values', () => {
    const values = patientToForm({ name: 'Ana', active: false, address: { id: 7, street: 'Main', country: { code: 'PT' }, administrativeDivision: { code: 'LX' } }, documentIssuerCountry: { code: 'PT' }, nationality: { code: 'BR' }, specialities: [{ id: 2 }, { id: 5 }] });
    expect(values).toMatchObject({ name: 'Ana', active: 'false', addressId: '7', countryCode: 'PT', administrativeDivisionCode: 'LX', documentIssuerCountryCode: 'PT', nationalityCode: 'BR', specialityIds: '2, 5' });
  });

  it('reuses a selected address and keeps only positive integer speciality ids', () => {
    const request = patientRequest({ name: 'Ana', birthDate: '2000-01-02', active: 'true', gender: 'FEMALE', taxId: '', identificationType: 'PASSPORT', identificationNumber: 'P1', documentIssuerCountryCode: 'PT', nationalityCode: 'PT', addressId: '7', street: '', district: '', city: '', additionalInfo: '', block: '', postalCode: '', administrativeDivisionName: '', administrativeDivisionCode: '', administrativeDivisionType: '', countryCode: 'PT', specialityIds: '2, -1, invalid, 3.5, 5' }) as Record<string, unknown>;
    expect(request['addressId']).toBe(7);
    expect(request['address']).toBeUndefined();
    expect(request['specialityIds']).toEqual([2, 5]);
    expect(request['taxId']).toBeNull();
  });

  it('embeds a new address when no reusable address was selected', () => {
    const request = patientRequest({ name: 'Ana', birthDate: '2000-01-02', active: 'false', gender: 'FEMALE', taxId: '123', identificationType: 'PASSPORT', identificationNumber: 'P1', documentIssuerCountryCode: 'PT', nationalityCode: 'PT', addressId: '', street: 'Main', district: '', city: 'Lisbon', additionalInfo: '', block: '', postalCode: '1000', administrativeDivisionName: 'Lisbon', administrativeDivisionCode: 'LX', administrativeDivisionType: 'DISTRICT', countryCode: 'PT', specialityIds: '' }) as Record<string, unknown>;
    expect(request['addressId']).toBeUndefined();
    expect(request['address']).toMatchObject({ street: 'Main', city: 'Lisbon', administrativeDivision: { code: 'LX' }, countryCode: 'PT' });
    expect(request['active']).toBe(false);
  });
});
