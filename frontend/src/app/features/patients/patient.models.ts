export type PatientRecord = Record<string, unknown>;
export type PatientFormValues = Record<string, string>;
export type PatientField = {
  key: string;
  label: string;
  required?: boolean;
  options?: readonly { value: string; label: string }[];
  type?: 'date';
  section?: string;
  fullWidth?: boolean;
};
export type SpecialityOption = { id: number; name: string; displayName?: string };
export type AdministrativeDivisionOption = {
  id: number;
  name: string;
  code: string;
  type: string;
  country: { code: string };
};
export type AddressOption = {
  id: number;
  street: string;
  district?: string;
  city: string;
  additionalInfo?: string;
  block?: string;
  postalCode?: string;
  administrativeDivision?: { name: string; code: string; type: string };
  country: { code: string };
};
export type ProcedureOption = { id?: number; name: string };

export const PATIENT_FIELDS: readonly PatientField[] = [
  { key: 'name', label: 'Full name', required: true, section: 'Personal details', fullWidth: true },
  {
    key: 'birthDate',
    label: 'Birth date',
    required: true,
    type: 'date',
    section: 'Personal details',
  },
  {
    key: 'active',
    label: 'Status',
    required: true,
    section: 'Personal details',
    options: [
      { value: 'true', label: 'Active' },
      { value: 'false', label: 'Inactive' },
    ],
  },
  {
    key: 'gender',
    label: 'Gender',
    required: true,
    section: 'Personal details',
    options: [
      { value: 'FEMALE', label: 'Female' },
      { value: 'MALE', label: 'Male' },
      { value: 'OTHER', label: 'Other' },
    ],
  },
  { key: 'taxId', label: 'Tax ID', section: 'Personal details' },
  {
    key: 'identificationType',
    label: 'Identification type',
    required: true,
    section: 'Identification',
    options: [
      { value: 'NATIONAL_ID_CARD', label: 'National ID card' },
      { value: 'PASSPORT', label: 'Passport' },
    ],
  },
  {
    key: 'identificationNumber',
    label: 'Identification number',
    required: true,
    section: 'Identification',
  },
  {
    key: 'documentIssuerCountryCode',
    label: 'Document issuer country code',
    required: true,
    section: 'Identification',
  },
  {
    key: 'nationalityCode',
    label: 'Nationality country code',
    required: true,
    section: 'Nationality',
  },
  { key: 'countryCode', label: 'Address country code', required: true, section: 'Address' },
  { key: 'postalCode', label: 'Postal code', required: true, section: 'Address' },
  { key: 'street', label: 'Street', required: true, section: 'Address', fullWidth: true },
  { key: 'district', label: 'District', section: 'Address' },
  { key: 'city', label: 'City', required: true, section: 'Address' },
  { key: 'additionalInfo', label: 'Additional information', section: 'Address' },
  { key: 'block', label: 'Block', section: 'Address' },
  { key: 'administrativeDivisionName', label: 'Administrative division name', section: 'Address' },
  {
    key: 'administrativeDivisionCode',
    label: 'Administrative division code',
    required: true,
    section: 'Address',
  },
  { key: 'administrativeDivisionType', label: 'Administrative division type', section: 'Address' },
  { key: 'specialityIds', label: 'Specialities', section: 'Specialities', fullWidth: true },
];

export function patientToForm(record: PatientRecord): PatientFormValues {
  const address = (record['address'] as PatientRecord | undefined) ?? {};
  const division = (address['administrativeDivision'] as PatientRecord | undefined) ?? {};
  return {
    addressId: text(address['id']),
    street: text(address['street']),
    district: text(address['district']),
    city: text(address['city']),
    additionalInfo: text(address['additionalInfo']),
    block: text(address['block']),
    postalCode: text(address['postalCode']),
    administrativeDivisionName: text(division['name']),
    administrativeDivisionCode: text(division['code']),
    administrativeDivisionType: text(division['type']),
    countryCode: text((address['country'] as PatientRecord | undefined)?.['code']),
    name: text(record['name']),
    birthDate: text(record['birthDate']),
    active: text(record['active'] ?? true),
    gender: text(record['gender']),
    taxId: text(record['taxId']),
    identificationType: text(record['identificationType']),
    identificationNumber: text(record['identificationNumber']),
    documentIssuerCountryCode: text(
      (record['documentIssuerCountry'] as PatientRecord | undefined)?.['code'] ?? '',
    ),
    nationalityCode: text((record['nationality'] as PatientRecord | undefined)?.['code']),
    specialityIds: ((record['specialities'] as PatientRecord[] | undefined) ?? [])
      .map((speciality) => text(speciality['id']))
      .join(', '),
  };
}

function text(value: unknown, fallback = ''): string {
  return typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean'
    ? String(value)
    : fallback;
}

export function patientRequest(value: PatientFormValues): Record<string, unknown> {
  const address = {
    street: value['street'],
    district: value['district'],
    city: value['city'],
    additionalInfo: value['additionalInfo'] || null,
    block: value['block'] || null,
    postalCode: value['postalCode'],
    administrativeDivision: value['administrativeDivisionCode']
      ? {
          name: value['administrativeDivisionName'],
          code: value['administrativeDivisionCode'],
          type: value['administrativeDivisionType'],
        }
      : null,
    countryCode: value['countryCode'],
  };
  return {
    name: value['name'],
    birthDate: value['birthDate'],
    active: value['active'] === 'true',
    gender: value['gender'],
    taxId: value['taxId'] || null,
    identificationType: value['identificationType'],
    identificationNumber: value['identificationNumber'],
    documentIssuerCountryCode: value['documentIssuerCountryCode'],
    nationalityCode: value['nationalityCode'],
    ...(value['addressId'] ? { addressId: Number(value['addressId']) } : { address }),
    specialityIds: value['specialityIds']
      .split(',')
      .map((id) => Number(id.trim()))
      .filter((id) => Number.isInteger(id) && id > 0),
  };
}
