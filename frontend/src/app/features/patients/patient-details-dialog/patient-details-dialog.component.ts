import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { CatalogDisplayNameService } from '../../../core/catalog-display-name.service';
import { textValue } from '../../../shared/text-value';

type PatientRecord = Record<string, unknown>;

@Component({
  selector: 'app-patient-details-dialog',
  imports: [MatButtonModule, MatDialogModule, MatIconModule, MatTooltipModule, TranslatePipe],
  templateUrl: './patient-details-dialog.component.html',
  styleUrl: './patient-details-dialog.component.scss',
})
export class PatientDetailsDialog {
  readonly patient = inject<PatientRecord>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<PatientDetailsDialog>);
  private readonly translate = inject(TranslateService);
  readonly catalogNames = inject(CatalogDisplayNameService);

  value(key: string): string {
    return textValue(this.patient[key], '—');
  }
  object(key: string): PatientRecord {
    const value = this.patient[key];
    return value && typeof value === 'object' && !Array.isArray(value)
      ? (value as PatientRecord)
      : {};
  }
  administrativeDivisionName(): string {
    const division = this.object('address')['administrativeDivision'];
    return division && typeof division === 'object'
      ? textValue((division as PatientRecord)['name'], '—')
      : '—';
  }
  specialities(): PatientRecord[] {
    const value = this.patient['specialities'];
    return Array.isArray(value) ? (value as PatientRecord[]) : [];
  }
  initials(): string {
    return this.value('name')
      .split(/\s+/)
      .map((part) => part[0])
      .slice(0, 2)
      .join('')
      .toUpperCase();
  }
  formattedDate(): string {
    const value = this.value('birthDate');
    return value === '—'
      ? value
      : new Intl.DateTimeFormat(this.translate.getCurrentLang() ?? 'en', {
          dateStyle: 'long',
        }).format(new Date(`${value}T00:00:00`));
  }
  documentType(): string {
    const type = this.value('identificationType');
    return type === '—' ? type : this.translate.instant(`PATIENTS.DETAILS.DOCUMENT_TYPES.${type}`);
  }
  close(): void {
    this.ref.close();
  }
}
