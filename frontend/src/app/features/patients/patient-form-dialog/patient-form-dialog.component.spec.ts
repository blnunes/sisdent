import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNativeDateAdapter } from '@angular/material/core';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { CatalogDisplayNameService } from '../../../core/catalog-display-name.service';
import { PatientApiService } from '../patient-api.service';
import { PatientFormDialog, PatientFormDialogData } from './patient-form-dialog.component';

describe('PatientFormDialog', () => {
  const api = { postalCodeSuggestions: vi.fn() };
  const ref = { close: vi.fn() };
  const data: PatientFormDialogData = {
    recordType: 'patients',
    translationKey: 'PATIENTS',
    fields: [
      { key: 'name', label: 'Name', required: true },
      { key: 'birthDate', label: 'Birth date', type: 'date' },
      { key: 'specialityIds', label: 'Specialities' },
      { key: 'procedures', label: 'Procedures' },
      { key: 'countryCode', label: 'Country' },
      { key: 'postalCode', label: 'Postal code' },
      { key: 'street', label: 'Street', section: 'Address' },
      { key: 'city', label: 'City', section: 'Address' },
      { key: 'district', label: 'District', section: 'Address' },
      { key: 'additionalInfo', label: 'Additional', section: 'Address' },
      { key: 'block', label: 'Block', section: 'Address' },
      { key: 'administrativeDivisionCode', label: 'Division', section: 'Address' },
      { key: 'administrativeDivisionName', label: 'Division name', section: 'Address' },
      { key: 'administrativeDivisionType', label: 'Division type', section: 'Address' },
    ],
    values: { name: 'Ada', countryCode: 'PT' },
    specialities: [{ id: 1, name: 'Orthodontics' }],
  };

  beforeEach(() => {
    api.postalCodeSuggestions.mockReset();
    api.postalCodeSuggestions.mockReturnValue(of([]));
    ref.close.mockReset();
    TestBed.configureTestingModule({
      imports: [PatientFormDialog],
      providers: [
        { provide: PatientApiService, useValue: api },
        { provide: MatDialogRef, useValue: ref },
        { provide: MAT_DIALOG_DATA, useValue: data },
        {
          provide: CatalogDisplayNameService,
          useValue: {
            country: (country: { code: string }) => country.code,
            speciality: (speciality: { name: string }) => speciality.name,
          },
        },
        provideNativeDateAdapter(),
        provideTranslateService(),
      ],
    });
  });

  it('serializes valid patient values without coercing malformed values', () => {
    const component = TestBed.createComponent(PatientFormDialog).componentInstance;
    component.form.patchValue({
      birthDate: new Date(2026, 0, 2),
      specialityIds: '',
      procedures: '',
    });
    component.addSpeciality(1);
    component.setProcedureName('Cleaning');
    component.addProcedure();
    component.save();

    expect(ref.close).toHaveBeenCalledWith(
      expect.objectContaining({
        birthDate: '2026-01-02',
        specialityIds: '1',
        procedures: '[{"name":"Cleaning"}]',
        addressId: '',
      }),
    );
    expect(
      (
        component as unknown as { serializedValue: (key: string, value: unknown) => string }
      ).serializedValue('name', { unsafe: true }),
    ).toBe('');
  });

  it('validates postal-code searches and clears suggestions for invalid or failed requests', () => {
    const component = TestBed.createComponent(PatientFormDialog).componentInstance;
    component.onPostalCodeInput('1');
    expect(api.postalCodeSuggestions).not.toHaveBeenCalled();

    component.onPostalCodeInput('1000');
    expect(api.postalCodeSuggestions).toHaveBeenCalledWith('PT', '1000');
    api.postalCodeSuggestions.mockReturnValue(throwError(() => new Error('offline')));
    component.onPostalCodeInput('1001');
    expect(component.postalCodeSuggestions()).toEqual([]);
  });

  it('renders the patient form controls with one accessible name and keeps invalid fields visible', () => {
    const fixture = TestBed.createComponent(PatientFormDialog);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('label[for="patient-field-name"]')?.textContent).toContain('Name');
    expect(element.querySelectorAll('mat-form-field').length).toBeGreaterThan(4);
  });

  it('updates, locks, unlocks, and validates address and catalogue selections', () => {
    const component = TestBed.createComponent(PatientFormDialog).componentInstance;
    const address = {
      id: 7,
      country: { code: 'PT' },
      postalCode: '1000-001',
      street: 'Rua Um',
      district: 'Lisboa',
      city: 'Lisboa',
      additionalInfo: null,
      block: null,
      administrativeDivision: { code: 'LX', name: 'Lisbon', type: 'District' },
    } as never;

    component.selectPostalCodeAddress(address);
    expect(component.selectedAddressId()).toBe('7');
    expect(component.form.controls['street'].disabled).toBe(true);
    component.onAddressFieldInput('street');
    expect(component.selectedAddressId()).toBe('');
    expect(component.form.controls['street'].enabled).toBe(true);
    component.onCountrySelected('NL');
    expect(component.selectedCountryCode()).toBe('NL');
    component.removeSpeciality(1);
    component.setProcedureName('Cleaning');
    component.addProcedure();
    component.addProcedure();
    component.removeProcedure(component.selectedProcedures()[0]);
    expect(component.postalCodeAddressLabel(address)).toContain('Rua Um');
    expect(component.postalCodeDisplayValue(address)).toBe('1000-001');
  });

  it('marks required fields when saving invalid data', () => {
    const component = TestBed.createComponent(PatientFormDialog).componentInstance;
    component.form.controls['name'].setValue('');
    component.save();
    expect(component.form.controls['name'].touched).toBe(true);
    expect(ref.close).not.toHaveBeenCalled();
  });
});
