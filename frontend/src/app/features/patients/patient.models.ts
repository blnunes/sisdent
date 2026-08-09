export type PatientRecord = Record<string, unknown>;
export type PatientFormValues = Record<string, string>;
export type PatientField = { key: string; label: string; required?: boolean; options?: readonly { value: string; label: string }[]; type?: 'date'; section?: string; fullWidth?: boolean };
export type SpecialityOption = { id: number; name: string };
export type CountryOption = { code: string; name: string };
export type AdministrativeDivisionOption = { id: number; name: string; code: string; type: string; country: { code: string } };
export type AddressOption = { id: number; street: string; district?: string; city: string; additionalInfo?: string; block?: string; postalCode?: string; administrativeDivision?: { name: string; code: string; type: string }; country: { code: string } };
export type ProcedureOption = { id?: number; name: string };

export const PATIENT_FIELDS: readonly PatientField[] = [
  { key: 'name', label: 'Full name', required: true, section: 'Personal details', fullWidth: true },
  { key: 'birthDate', label: 'Birth date', required: true, type: 'date', section: 'Personal details' },
  { key: 'active', label: 'Status', required: true, section: 'Personal details', options: [{ value: 'true', label: 'Active' }, { value: 'false', label: 'Inactive' }] },
  { key: 'gender', label: 'Gender', required: true, section: 'Personal details', options: [{ value: 'FEMALE', label: 'Female' }, { value: 'MALE', label: 'Male' }, { value: 'OTHER', label: 'Other' }] },
  { key: 'taxId', label: 'Tax ID', section: 'Personal details' },
  { key: 'identificationType', label: 'Identification type', required: true, section: 'Identification', options: [{ value: 'NATIONAL_ID_CARD', label: 'National ID card' }, { value: 'PASSPORT', label: 'Passport' }] },
  { key: 'identificationNumber', label: 'Identification number', required: true, section: 'Identification' },
  { key: 'documentIssuerCountryCode', label: 'Document issuer country code', required: true, section: 'Identification' },
  { key: 'nationalityCode', label: 'Nationality country code', required: true, section: 'Nationality' },
  { key: 'countryCode', label: 'Address country code', required: true, section: 'Address' },
  { key: 'postalCode', label: 'Postal code', required: true, section: 'Address' },
  { key: 'street', label: 'Street', required: true, section: 'Address', fullWidth: true },
  { key: 'district', label: 'District', section: 'Address' },
  { key: 'city', label: 'City', required: true, section: 'Address' },
  { key: 'additionalInfo', label: 'Additional information', section: 'Address' },
  { key: 'block', label: 'Block', section: 'Address' },
  { key: 'administrativeDivisionName', label: 'Administrative division name', section: 'Address' },
  { key: 'administrativeDivisionCode', label: 'Administrative division code', required: true, section: 'Address' },
  { key: 'administrativeDivisionType', label: 'Administrative division type', section: 'Address' },
  { key: 'specialityIds', label: 'Specialities', section: 'Specialities', fullWidth: true },
];

export function patientToForm(record: PatientRecord): PatientFormValues {
  const address = (record['address'] as PatientRecord | undefined) ?? {};
  const division = (address['administrativeDivision'] as PatientRecord | undefined) ?? {};
  return {
    addressId: String(address['id'] ?? ''), street: String(address['street'] ?? ''), district: String(address['district'] ?? ''), city: String(address['city'] ?? ''), additionalInfo: String(address['additionalInfo'] ?? ''), block: String(address['block'] ?? ''), postalCode: String(address['postalCode'] ?? ''), administrativeDivisionName: String(division['name'] ?? ''), administrativeDivisionCode: String(division['code'] ?? ''), administrativeDivisionType: String(division['type'] ?? ''), countryCode: String((address['country'] as PatientRecord | undefined)?.['code'] ?? ''),
    name: String(record['name'] ?? ''), birthDate: String(record['birthDate'] ?? ''), active: String(record['active'] ?? true), gender: String(record['gender'] ?? ''), taxId: String(record['taxId'] ?? ''), identificationType: String(record['identificationType'] ?? ''), identificationNumber: String(record['identificationNumber'] ?? ''), documentIssuerCountryCode: String((record['documentIssuerCountry'] as PatientRecord | undefined)?.['code'] ?? ''), nationalityCode: String((record['nationality'] as PatientRecord | undefined)?.['code'] ?? ''), specialityIds: ((record['specialities'] as PatientRecord[] | undefined) ?? []).map((speciality) => String(speciality['id'])).join(', '),
  };
}

export function patientRequest(value: PatientFormValues): unknown {
  const address = { street: value['street'], district: value['district'], city: value['city'], additionalInfo: value['additionalInfo'] || null, block: value['block'] || null, postalCode: value['postalCode'], administrativeDivision: value['administrativeDivisionCode'] ? { name: value['administrativeDivisionName'], code: value['administrativeDivisionCode'], type: value['administrativeDivisionType'] } : null, countryCode: value['countryCode'] };
  return { name: value['name'], birthDate: value['birthDate'], active: value['active'] === 'true', gender: value['gender'], taxId: value['taxId'] || null, identificationType: value['identificationType'], identificationNumber: value['identificationNumber'], documentIssuerCountryCode: value['documentIssuerCountryCode'], nationalityCode: value['nationalityCode'], ...(value['addressId'] ? { addressId: Number(value['addressId']) } : { address }), specialityIds: value['specialityIds'].split(',').map((id) => Number(id.trim())).filter((id) => Number.isInteger(id) && id > 0) };
}
