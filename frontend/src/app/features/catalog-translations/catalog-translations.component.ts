import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import {
  MatDialog,
  MatDialogModule,
  MAT_DIALOG_DATA,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSidenavModule } from '@angular/material/sidenav';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AppHeaderComponent } from '../../core/layout/app-header/app-header.component';
import { ModuleNavigationComponent } from '../../core/layout/module-navigation/module-navigation.component';
import {
  CatalogResourceType,
  CatalogTranslationApiService,
  CatalogTranslationEntry,
} from '../../core/catalog-translation-api.service';

const LOCALES = ['en', 'pt-PT', 'nl'] as const;

@Component({
  selector: 'app-catalog-translations',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSidenavModule,
    TranslatePipe,
    AppHeaderComponent,
    ModuleNavigationComponent,
  ],
  templateUrl: './catalog-translations.component.html',
  styleUrl: './catalog-translations.component.scss',
})
export class CatalogTranslationsComponent {
  private readonly api = inject(CatalogTranslationApiService);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);
  readonly entries = signal<CatalogTranslationEntry[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly query = signal('');
  readonly type = signal<CatalogResourceType | ''>('');
  readonly missingOnly = signal(false);
  readonly visibleEntries = computed(() =>
    this.missingOnly()
      ? this.entries().filter((entry) => entry.missingLocales.length > 0)
      : this.entries(),
  );
  readonly missingCount = computed(
    () => this.entries().filter((entry) => entry.missingLocales.length > 0).length,
  );

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.list(this.type(), this.query()).subscribe({
      next: (entries) => {
        this.entries.set(entries);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.translate.instant('CATALOG_TRANSLATIONS.LOAD_ERROR'));
        this.loading.set(false);
      },
    });
  }

  clear(): void {
    this.query.set('');
    this.type.set('');
    this.missingOnly.set(false);
    this.load();
  }
  localeLabel(locale: string): string {
    return this.translate.instant(`CATALOG_TRANSLATIONS.LOCALES.${locale}`);
  }
  typeLabel(type: CatalogResourceType): string {
    return this.translate.instant(`CATALOG_TRANSLATIONS.TYPES.${type}`);
  }

  edit(entry: CatalogTranslationEntry): void {
    this.dialog
      .open(CatalogTranslationDialog, { width: '680px', maxWidth: '94vw', data: entry })
      .afterClosed()
      .subscribe((updated?: CatalogTranslationEntry) => {
        if (updated)
          this.entries.update((entries) =>
            entries.map((item) =>
              item.resourceType === updated.resourceType && item.resourceId === updated.resourceId
                ? updated
                : item,
            ),
          );
      });
  }
}

@Component({
  selector: 'app-catalog-translation-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    TranslatePipe,
  ],
  template: `<header>
      <div>
        <p>{{ 'CATALOG_TRANSLATIONS.TYPES.' + data.resourceType | translate }}</p>
        <h2 mat-dialog-title>{{ data.canonicalName }}</h2>
      </div>
      <button mat-icon-button mat-dialog-close [attr.aria-label]="'RESOURCE.CLOSE' | translate">
        <mat-icon>close</mat-icon>
      </button>
    </header>
    <mat-dialog-content
      ><p class="help">{{ 'CATALOG_TRANSLATIONS.DIALOG_HELP' | translate }}</p>
      <form [formGroup]="form">
        @for (locale of locales; track locale) {
          <mat-form-field appearance="outline"
            ><mat-label>{{ 'CATALOG_TRANSLATIONS.LOCALES.' + locale | translate }}</mat-label
            ><input
              matInput
              [formControlName]="locale"
              [placeholder]="data.canonicalName"
            /><mat-hint>{{
              (data.customizedLocales.includes(locale)
                ? 'CATALOG_TRANSLATIONS.CUSTOMIZED'
                : 'CATALOG_TRANSLATIONS.FALLBACK'
              ) | translate
            }}</mat-hint></mat-form-field
          >
        }
      </form>
      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end"
      ><button mat-button mat-dialog-close>{{ 'SPECIALITIES.FORM.CANCEL' | translate }}</button
      ><button mat-flat-button (click)="save()" [disabled]="saving()">
        <mat-icon>save</mat-icon>{{ 'SPECIALITIES.FORM.SAVE' | translate }}
      </button></mat-dialog-actions
    >`,
  styles: [
    `
      header {
        display: flex;
        justify-content: space-between;
        align-items: start;
        padding: 24px 28px 10px;
      }
      header p,
      header h2 {
        margin: 0;
      }
      header p,
      .help {
        color: var(--app-text-muted);
      }
      form {
        display: grid;
        gap: 8px;
      }
      mat-form-field {
        width: 100%;
      }
      .error {
        color: var(--mat-sys-error);
      }
      mat-dialog-actions {
        padding: 12px 28px 22px;
      }
    `,
  ],
})
export class CatalogTranslationDialog {
  readonly data = inject<CatalogTranslationEntry>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<CatalogTranslationDialog, CatalogTranslationEntry>);
  private readonly api = inject(CatalogTranslationApiService);
  private readonly translate = inject(TranslateService);
  private readonly forms = inject(FormBuilder);
  readonly locales = LOCALES;
  readonly saving = signal(false);
  readonly error = signal('');
  readonly form = this.forms.nonNullable.group(
    Object.fromEntries(
      LOCALES.map((locale) => [locale, this.data.translations[locale] ?? '']),
    ) as Record<(typeof LOCALES)[number], string>,
  );

  save(): void {
    this.saving.set(true);
    this.error.set('');
    this.api.replace(this.data, this.form.getRawValue()).subscribe({
      next: (entry) => {
        this.saving.set(false);
        this.ref.close(entry);
      },
      error: () => {
        this.saving.set(false);
        this.error.set(this.translate.instant('CATALOG_TRANSLATIONS.SAVE_ERROR'));
      },
    });
  }
}
