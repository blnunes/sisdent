import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ProcedureOption } from '../speciality-form.utils';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { CatalogDisplayNameService } from '../../../core/catalog-display-name.service';
import { MatExpansionModule } from '@angular/material/expansion';

const LOCALES = ['en', 'pt-PT', 'nl'] as const;
export type SpecialityFormData = {
  title: string;
  name?: string;
  translations?: Readonly<Record<string, string>>;
  procedures?: readonly ProcedureOption[];
};
export type SpecialityFormResult = {
  name: string;
  translations: Readonly<Record<string, string>>;
  procedures: readonly ProcedureOption[];
};

@Component({
  selector: 'app-speciality-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    TranslatePipe,
  ],
  templateUrl: './speciality-form-dialog.component.html',
  styleUrl: './speciality-form-dialog.component.scss',
})
export class SpecialityFormDialogComponent {
  readonly data = inject<SpecialityFormData>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<SpecialityFormDialogComponent, SpecialityFormResult>);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);
  readonly catalogNames = inject(CatalogDisplayNameService);
  readonly locales = LOCALES;
  readonly form = this.fb.nonNullable.group({
    name: [this.data.name ?? '', Validators.required],
    procedureName: [''],
    translationEn: [this.data.translations?.['en'] ?? ''],
    translationPt: [this.data.translations?.['pt-PT'] ?? ''],
    translationNl: [this.data.translations?.['nl'] ?? ''],
  });
  readonly procedures = signal<ProcedureOption[]>([...(this.data.procedures ?? [])]);
  readonly proceduresTouched = signal(false);
  readonly translationsOpen = signal(false);

  addProcedure(): void {
    const name = this.form.controls.procedureName.value.trim();
    if (!name) return;
    const locale = this.currentLocale();
    this.procedures.update((current) => [...current, { name, translations: { [locale]: name } }]);
    this.form.controls.procedureName.setValue('');
  }

  removeProcedure(index: number): void {
    this.procedures.update((current) => current.filter((_, candidate) => candidate !== index));
  }

  procedureTranslation(index: number, locale: string): string {
    return this.procedures()[index]?.translations?.[locale] ?? '';
  }
  updateProcedureTranslation(index: number, locale: string, value: string): void {
    this.procedures.update((items) =>
      items.map((item, candidate) =>
        candidate === index
          ? { ...item, translations: { ...item.translations, [locale]: value } }
          : item,
      ),
    );
  }

  save(): void {
    this.proceduresTouched.set(true);
    const name = this.form.controls.name.value.trim();
    if (!name) this.form.controls.name.setErrors({ required: true });
    if (this.form.controls.name.invalid || !this.procedures().length) {
      this.form.controls.name.markAsTouched();
      return;
    }
    const result = this.result(name);
    const missing = this.missingCount(result);
    if (!missing) {
      this.ref.close(result);
      return;
    }
    this.dialog
      .open(MissingTranslationsDialog, {
        width: '480px',
        maxWidth: '92vw',
        data: { count: missing },
      })
      .afterClosed()
      .subscribe((action?: 'add' | 'continue') => {
        if (action === 'add') this.translationsOpen.set(true);
        else if (action === 'continue') this.ref.close(result);
      });
  }

  private result(name: string): SpecialityFormResult {
    const translations: Record<string, string> = {
      en: this.form.controls.translationEn.value.trim(),
      'pt-PT': this.form.controls.translationPt.value.trim(),
      nl: this.form.controls.translationNl.value.trim(),
    };
    const current = this.currentLocale();
    if (!translations[current]) translations[current] = name;
    return {
      name,
      translations,
      procedures: this.procedures().map((item) => ({
        ...item,
        translations: {
          ...item.translations,
          [current]: item.translations?.[current]?.trim() || item.name,
        },
      })),
    };
  }
  private missingCount(result: SpecialityFormResult): number {
    return (
      LOCALES.filter((locale) => !result.translations[locale]?.trim()).length +
      result.procedures.reduce(
        (count, item) =>
          count + LOCALES.filter((locale) => !item.translations?.[locale]?.trim()).length,
        0,
      )
    );
  }
  private currentLocale(): (typeof LOCALES)[number] {
    const language = this.translate.getCurrentLang();
    return language?.startsWith('pt') ? 'pt-PT' : language === 'nl' ? 'nl' : 'en';
  }
}

@Component({
  selector: 'app-missing-translations-dialog',
  imports: [MatButtonModule, MatDialogModule, MatIconModule, TranslatePipe],
  template: `<div class="warning-icon"><mat-icon>translate</mat-icon></div>
    <h2 mat-dialog-title>{{ 'SPECIALITIES.TRANSLATION_WARNING.TITLE' | translate }}</h2>
    <mat-dialog-content
      ><p>
        {{ 'SPECIALITIES.TRANSLATION_WARNING.DESCRIPTION' | translate: { count: data.count } }}
      </p>
      <p class="fallback">
        {{ 'SPECIALITIES.TRANSLATION_WARNING.FALLBACK' | translate }}
      </p></mat-dialog-content
    ><mat-dialog-actions align="end"
      ><button mat-button [mat-dialog-close]="undefined">
        {{ 'SPECIALITIES.FORM.CANCEL' | translate }}</button
      ><button mat-stroked-button [mat-dialog-close]="'continue'">
        {{ 'SPECIALITIES.TRANSLATION_WARNING.CONTINUE' | translate }}</button
      ><button mat-flat-button [mat-dialog-close]="'add'">
        {{ 'SPECIALITIES.TRANSLATION_WARNING.ADD_NOW' | translate }}
      </button></mat-dialog-actions
    >`,
  styles: [
    `
      .warning-icon {
        display: grid;
        width: 48px;
        height: 48px;
        margin: 24px 24px 4px;
        place-items: center;
        border-radius: 14px;
        background: #fff3d5;
        color: #8a5700;
      }
      h2 {
        padding-top: 8px;
      }
      .fallback {
        padding: 12px;
        border-radius: 10px;
        background: var(--app-canvas);
        color: var(--app-text-muted);
      }
      mat-dialog-actions {
        padding: 12px 24px 22px;
        gap: 6px;
      }
    `,
  ],
})
export class MissingTranslationsDialog {
  readonly data = inject<{ count: number }>(MAT_DIALOG_DATA);
}
