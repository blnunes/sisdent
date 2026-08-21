import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslatePipe } from '@ngx-translate/core';
import {
  AddressOption,
  AdministrativeDivisionOption,
  PatientField as Field,
  PatientFormValues as FormValues,
  ProcedureOption,
  SpecialityOption,
} from '../patient.models';
import { PatientApiService } from '../patient-api.service';
import { CatalogDisplayNameService } from '../../../core/catalog-display-name.service';
import { CountryCatalogueItem } from '../../../core/country-catalog-graphql.service';

export type PatientFormDialogData = {
  fields: Field[];
  values?: FormValues;
  specialities?: SpecialityOption[];
  countries?: CountryCatalogueItem[];
  administrativeDivisions?: AdministrativeDivisionOption[];
  recordType: string;
  translationKey: string;
};

@Component({
  selector: 'app-patient-form-dialog',
  imports: [
    MatAutocompleteModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatNativeDateModule,
    MatSelectModule,
    MatTooltipModule,
    TranslatePipe,
  ],
  templateUrl: './patient-form-dialog.component.html',
  styleUrl: './patient-form-dialog.component.scss',
})
export class PatientFormDialog {
  private readonly patientAddressFieldKeys = [
    'street',
    'district',
    'city',
    'additionalInfo',
    'block',
    'administrativeDivisionName',
    'administrativeDivisionCode',
    'administrativeDivisionType',
  ];
  readonly data = inject<PatientFormDialogData>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<PatientFormDialog, Record<string, string>>);
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(PatientApiService);
  readonly catalogNames = inject(CatalogDisplayNameService);
  readonly form = this.fb.group(
    Object.fromEntries(
      this.data.fields.map((field) => [
        field.key,
        [this.initialValue(field), field.required ? Validators.required : []],
      ]),
    ),
  );
  readonly selectedSpecialities = signal<SpecialityOption[]>(this.initialSpecialities());
  readonly specialityToAdd = signal<number | null>(null);
  readonly selectedProcedures = signal<ProcedureOption[]>(this.initialProcedures());
  readonly procedureNameToAdd = signal('');
  readonly postalCodeSuggestions = signal<AddressOption[]>([]);
  readonly selectedAddressId = signal(this.data.values?.['addressId'] ?? '');
  readonly isAddressLocked = computed(() => Boolean(this.selectedAddressId()));
  readonly selectedCountryCode = signal(this.data.values?.['countryCode'] ?? '');
  readonly availableAdministrativeDivisions = computed(() =>
    (this.data.administrativeDivisions ?? []).filter(
      (division) => division.country.code === this.selectedCountryCode(),
    ),
  );
  readonly countriesWithAdministrativeDivisions = computed(() => {
    const countryCodes = new Set(
      (this.data.administrativeDivisions ?? []).map((division) => division.country.code),
    );
    return (this.data.countries ?? []).filter((country) => countryCodes.has(country.code));
  });
  readonly availableSpecialities = computed(() => {
    const selectedIds = new Set(this.selectedSpecialities().map((speciality) => speciality.id));
    return (this.data.specialities ?? []).filter((speciality) => !selectedIds.has(speciality.id));
  });

  constructor() {
    if (this.isPatientForm() && this.isAddressLocked()) this.lockSelectedAddressFields();
  }

  sections(): string[] {
    return [...new Set(this.data.fields.map((field) => field.section ?? 'Record details'))];
  }

  fieldsIn(section: string): Field[] {
    return this.data.fields.filter((field) => (field.section ?? 'Record details') === section);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const values = Object.fromEntries(
      this.data.fields.map((field) => {
        const value = this.form.get(field.key)?.value;
        return [
          field.key,
          field.key === 'specialityIds'
            ? this.selectedSpecialities()
                .map((speciality) => speciality.id)
                .join(',')
            : field.key === 'procedures'
              ? JSON.stringify(this.selectedProcedures())
              : value instanceof Date
                ? this.toIsoDate(value)
                : String(value ?? ''),
        ];
      }),
    );
    if (this.isPatientForm()) values['addressId'] = this.selectedAddressId();
    this.ref.close(values);
  }

  isPatientForm(): boolean {
    return this.data.recordType === 'patients';
  }

  translationKey(key: string): string {
    return `${this.data.translationKey}.${key}`;
  }

  isHiddenPatientAddressField(field: Field): boolean {
    return (
      this.isPatientForm() &&
      (field.key === 'administrativeDivisionName' || field.key === 'administrativeDivisionType')
    );
  }

  isPatientAddressField(field: Field): boolean {
    return this.isPatientForm() && field.section === 'Address';
  }

  onPostalCodeInput(postalCode: string): void {
    this.clearSelectedAddress();
    const countryCode = String(this.form.get('countryCode')?.value ?? '')
      .trim()
      .toUpperCase();
    const query = postalCode.trim();
    if (!/^[A-Z]{2}$/.test(countryCode) || query.length < 2) {
      this.postalCodeSuggestions.set([]);
      return;
    }
    this.api.postalCodeSuggestions(countryCode, query).subscribe({
      next: (addresses) => this.postalCodeSuggestions.set(addresses),
      error: () => this.postalCodeSuggestions.set([]),
    });
  }

  onCountryCodeInput(): void {
    this.selectedAddressId.set('');
    this.postalCodeSuggestions.set([]);
  }

  onCountrySelected(countryCode: string): void {
    this.selectedCountryCode.set(countryCode);
    this.clearSelectedAddress();
    this.postalCodeSuggestions.set([]);
    this.form.patchValue({
      postalCode: '',
      street: '',
      district: '',
      city: '',
      additionalInfo: '',
      block: '',
      administrativeDivisionCode: '',
      administrativeDivisionName: '',
      administrativeDivisionType: '',
    });
  }

  onAdministrativeDivisionSelected(code: string): void {
    const division = this.availableAdministrativeDivisions().find((option) => option.code === code);
    this.selectedAddressId.set('');
    this.form.patchValue({
      administrativeDivisionName: division?.name ?? '',
      administrativeDivisionType: division?.type ?? '',
    });
  }

  onAddressFieldInput(fieldKey: string): void {
    this.clearSelectedAddress();
    if (fieldKey === 'countryCode') this.postalCodeSuggestions.set([]);
  }

  selectPostalCodeAddress(address: AddressOption): void {
    this.selectedAddressId.set(String(address.id));
    this.selectedCountryCode.set(address.country.code);
    this.postalCodeSuggestions.set([]);
    this.form.patchValue({
      countryCode: address.country.code,
      postalCode: address.postalCode ?? '',
      street: address.street,
      district: address.district ?? '',
      city: address.city,
      additionalInfo: address.additionalInfo ?? '',
      block: address.block ?? '',
      administrativeDivisionName: address.administrativeDivision?.name ?? '',
      administrativeDivisionCode: address.administrativeDivision?.code ?? '',
      administrativeDivisionType: address.administrativeDivision?.type ?? '',
    });
    this.lockSelectedAddressFields();
  }

  postalCodeAddressLabel(address: AddressOption): string {
    return `${address.postalCode ?? '—'} — ${address.street}, ${address.city}`;
  }

  postalCodeDisplayValue(value: AddressOption | string | null): string {
    return typeof value === 'string' ? value : (value?.postalCode ?? '');
  }

  addSpeciality(id: number): void {
    const speciality = (this.data.specialities ?? []).find((option) => option.id === id);
    if (speciality && !this.selectedSpecialities().some((option) => option.id === id)) {
      this.selectedSpecialities.update((current) => [...current, speciality]);
    }
    this.specialityToAdd.set(null);
    this.syncSpecialitiesControl();
  }

  removeSpeciality(id: number): void {
    this.selectedSpecialities.update((current) =>
      current.filter((speciality) => speciality.id !== id),
    );
    this.syncSpecialitiesControl();
  }

  setProcedureName(value: string): void {
    this.procedureNameToAdd.set(value);
  }

  addProcedure(): void {
    const name = this.procedureNameToAdd().trim();
    if (
      !name ||
      this.selectedProcedures().some(
        (procedure) => procedure.name.toLowerCase() === name.toLowerCase(),
      )
    )
      return;
    this.selectedProcedures.update((current) => [...current, { name }]);
    this.procedureNameToAdd.set('');
    this.syncProceduresControl();
  }

  removeProcedure(procedure: ProcedureOption): void {
    this.selectedProcedures.update((current) => current.filter((item) => item !== procedure));
    this.syncProceduresControl();
  }

  private lockSelectedAddressFields(): void {
    this.patientAddressFieldKeys.forEach((key) =>
      this.form.get(key)?.disable({ emitEvent: false }),
    );
  }

  private clearSelectedAddress(): void {
    if (!this.selectedAddressId()) return;
    this.selectedAddressId.set('');
    this.patientAddressFieldKeys.forEach((key) => this.form.get(key)?.enable({ emitEvent: false }));
    this.form.patchValue({
      street: '',
      district: '',
      city: '',
      additionalInfo: '',
      block: '',
      administrativeDivisionCode: '',
      administrativeDivisionName: '',
      administrativeDivisionType: '',
    });
  }

  private initialValue(field: Field): string | Date | null {
    const value = this.data.values?.[field.key] ?? '';
    if (field.type !== 'date') return value;
    if (!value) return null;
    const [year, month, day] = value.split('-').map(Number);
    const date = new Date(year, month - 1, day);
    return Number.isInteger(year) &&
      Number.isInteger(month) &&
      Number.isInteger(day) &&
      date.getFullYear() === year &&
      date.getMonth() === month - 1 &&
      date.getDate() === day
      ? date
      : null;
  }

  private initialSpecialities(): SpecialityOption[] {
    const ids = new Set(
      (this.data.values?.['specialityIds'] ?? '')
        .split(',')
        .map((value) => Number(value.trim()))
        .filter(Number.isInteger),
    );
    return (this.data.specialities ?? []).filter((speciality) => ids.has(speciality.id));
  }

  private syncSpecialitiesControl(): void {
    this.form.get('specialityIds')?.setValue(
      this.selectedSpecialities()
        .map((speciality) => speciality.id)
        .join(','),
    );
  }

  private initialProcedures(): ProcedureOption[] {
    try {
      const value = JSON.parse(this.data.values?.['procedures'] ?? '[]') as unknown;
      return Array.isArray(value)
        ? value.filter(
            (item): item is ProcedureOption =>
              !!item &&
              typeof item === 'object' &&
              typeof (item as ProcedureOption).name === 'string',
          )
        : [];
    } catch {
      return [];
    }
  }

  private syncProceduresControl(): void {
    this.form.get('procedures')?.setValue(JSON.stringify(this.selectedProcedures()));
  }

  private toIsoDate(value: Date): string {
    return [
      value.getFullYear(),
      String(value.getMonth() + 1).padStart(2, '0'),
      String(value.getDate()).padStart(2, '0'),
    ].join('-');
  }
}
