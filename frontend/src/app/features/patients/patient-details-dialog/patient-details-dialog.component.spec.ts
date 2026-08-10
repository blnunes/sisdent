import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { PatientDetailsDialog } from './patient-details-dialog.component';

describe('PatientDetailsDialog', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      imports: [PatientDetailsDialog],
      providers: [
        provideTranslateService(),
        { provide: MatDialogRef, useValue: { close: vi.fn() } },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            name: 'Ava Mitchell',
            active: false,
            birthDate: '1990-05-12',
            gender: 'FEMALE',
            nationality: { code: 'MX', name: 'Mexico' },
            address: { street: 'Rua Central' },
            specialities: [{ id: 1, name: 'Oral and Maxillofacial Radiology' }],
          },
        },
      ],
    }),
  );

  it('renders Portuguese labels and localized catalogue values without joining labels to values', () => {
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation(
      'pt-PT',
      {
        PATIENTS: {
          CLOSE: 'Fechar',
          FILTER: { FEMALE: 'Feminino' },
          DETAILS: {
            PROFILE: 'Perfil do paciente',
            ACTIVE: 'Ativo',
            INACTIVE: 'Inativo',
            PERSONAL: 'Dados pessoais',
            GLOBAL_ID: 'ID global',
            BIRTH_DATE: 'Data de nascimento',
            GENDER: 'Género',
            TAX_ID: 'NIF',
            IDENTIFICATION: 'Identificação',
            NATIONALITY: 'Nacionalidade',
            COUNTRY: 'País',
            ADDRESS: 'Endereço',
            STREET: 'Rua',
            CITY: 'Cidade',
            POSTAL_CODE: 'Código postal',
            DISTRICT: 'Distrito',
            ADMINISTRATIVE_DIVISION: 'Divisão administrativa',
            BLOCK: 'Bloco',
            ADDITIONAL_INFO: 'Informação adicional',
            SPECIALITIES: 'Especialidades',
            NO_SPECIALITIES: 'Nenhuma especialidade atribuída.',
            DOCUMENT_TYPES: { NATIONAL_ID_CARD: 'Documento nacional' },
          },
        },
        CATALOG: {
          SPECIALITIES: { 'oral-and-maxillofacial-radiology': 'Radiologia Oral e Maxilofacial' },
        },
      },
      true,
    );
    translate.use('pt-PT');

    const fixture = TestBed.createComponent(PatientDetailsDialog);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent?.replace(/\s+/g, ' ') ?? '';

    expect(text).toContain('Perfil do paciente');
    expect(text).toContain('Inativo');
    const labels = [
      ...(fixture.nativeElement as HTMLElement).querySelectorAll('.details-grid span'),
    ].map((element) => element.textContent?.trim());
    const values = [
      ...(fixture.nativeElement as HTMLElement).querySelectorAll('.details-grid strong'),
    ].map((element) => element.textContent?.trim());
    expect(labels).toContain('País');
    expect(values).toContain('México (MX)');
    expect(text).toContain('EspecialidadesRadiologia Oral e Maxilofacial');
    expect(text).not.toContain('PATIENTS.DETAILS.DOCUMENT_TYPES');
    expect(text).not.toContain('Patient profile');
  });
});
