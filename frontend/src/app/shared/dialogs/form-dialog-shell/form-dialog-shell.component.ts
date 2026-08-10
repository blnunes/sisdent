import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';
import { FormDialogData, FormDialogField, FormDialogValues } from './form-dialog-shell.models';

@Component({ selector: 'app-form-dialog-shell', imports: [ReactiveFormsModule, MatButtonModule, MatNativeDateModule, MatDatepickerModule, MatDialogModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule, TranslatePipe], templateUrl: './form-dialog-shell.component.html', styleUrl: './form-dialog-shell.component.scss' })
export class FormDialogShellComponent {
  readonly data = inject<FormDialogData>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<FormDialogShellComponent, FormDialogValues>);
  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.group(Object.fromEntries(this.data.fields.map((field) => [field.key, [this.initialValue(field), field.required ? Validators.required : []]])));

  sections(): string[] { return [...new Set(this.data.fields.map(({ section }) => section ?? ''))]; }
  fieldsIn(section: string): readonly FormDialogField[] { return this.data.fields.filter((field) => (field.section ?? '') === section); }
  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const values = Object.fromEntries(this.data.fields.map(({ key }) => {
      const value = this.form.get(key)?.value;
      return [key, value instanceof Date ? this.toIsoDate(value) : String(value ?? '')];
    }));
    this.ref.close(values);
  }
  private initialValue(field: FormDialogField): string | Date {
    const value = this.data.values?.[field.key] ?? '';
    return field.type === 'date' && value ? new Date(`${value}T00:00:00`) : value;
  }
  private toIsoDate(value: Date): string { return [value.getFullYear(), String(value.getMonth() + 1).padStart(2, '0'), String(value.getDate()).padStart(2, '0')].join('-'); }
}
