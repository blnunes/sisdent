import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ProcedureOption } from '../speciality-form.utils';

export type SpecialityFormData = { title: string; name?: string; procedures?: readonly ProcedureOption[] };
export type SpecialityFormResult = { name: string; procedures: readonly ProcedureOption[] };

@Component({
  selector: 'app-speciality-form-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatIconModule, MatInputModule],
  templateUrl: './speciality-form-dialog.component.html',
  styleUrl: './speciality-form-dialog.component.scss',
})
export class SpecialityFormDialogComponent {
  readonly data = inject<SpecialityFormData>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<SpecialityFormDialogComponent, SpecialityFormResult>);
  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.nonNullable.group({ name: [this.data.name ?? '', Validators.required], procedureName: [''] });
  readonly procedures = signal<ProcedureOption[]>([...(this.data.procedures ?? [])]);
  readonly proceduresTouched = signal(false);

  addProcedure(): void {
    const name = this.form.controls.procedureName.value.trim();
    if (!name) return;
    this.procedures.update((current) => [...current, { name }]);
    this.form.controls.procedureName.setValue('');
  }

  removeProcedure(index: number): void { this.procedures.update((current) => current.filter((_, candidate) => candidate !== index)); }

  save(): void {
    this.proceduresTouched.set(true);
    const name = this.form.controls.name.value.trim();
    if (!name) this.form.controls.name.setErrors({ required: true });
    if (this.form.controls.name.invalid || !this.procedures().length) { this.form.controls.name.markAsTouched(); return; }
    this.ref.close({ name, procedures: this.procedures() });
  }
}
