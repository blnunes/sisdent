import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, ViewChild, ElementRef, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AccountSettingsApiService } from '../../core/account-settings-api.service';
import { AuthService } from '../../core/auth.service';
import { CurrentAccountSettings } from '../../core/models';
import { Language, LanguageService, LANGUAGE_OPTIONS } from '../../core/language.service';
import { AppHeaderComponent } from '../../core/layout/app-header/app-header.component';
import { ModuleNavigationComponent } from '../../core/layout/module-navigation/module-navigation.component';

function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  const { newPassword, confirmation } = control.value as { newPassword?: string; confirmation?: string };
  return newPassword && confirmation && newPassword !== confirmation ? { passwordMismatch: true } : null;
}

@Component({
  selector: 'app-account-settings',
  imports: [ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule, MatProgressSpinnerModule, MatSidenavModule, TranslatePipe, AppHeaderComponent, ModuleNavigationComponent],
  templateUrl: './account-settings.component.html',
  styleUrl: './account-settings.component.scss',
})
export class AccountSettingsComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AccountSettingsApiService);
  private readonly auth = inject(AuthService);
  private readonly translate = inject(TranslateService);
  private readonly language = inject(LanguageService);
  @ViewChild('heading') private readonly heading?: ElementRef<HTMLElement>;
  @ViewChild(AppHeaderComponent) private readonly header?: AppHeaderComponent;

  readonly settings = signal<CurrentAccountSettings | null>(null);
  readonly loading = signal(true);
  readonly profileSubmitting = signal(false);
  readonly passwordSubmitting = signal(false);
  readonly profileMessage = signal('');
  readonly passwordMessage = signal('');
  readonly profileError = signal(false);
  readonly passwordError = signal(false);
  readonly languageSubmitting = signal(false);
  readonly languageMessage = signal('');
  readonly languageError = signal(false);
  readonly languageOptions = LANGUAGE_OPTIONS;
  readonly hideCurrent = signal(true);
  readonly hideNew = signal(true);
  readonly hideConfirmation = signal(true);
  readonly avatarPreview = signal<string | null>(null);
  readonly avatarCurrent = signal<string | null>(null);
  readonly selectedAvatar = signal<File | null>(null);
  readonly avatarSubmitting = signal(false);
  readonly avatarMessage = signal('');
  readonly avatarError = signal(false);
  readonly profileForm = this.fb.nonNullable.group({ displayName: ['', [Validators.required, Validators.pattern(/.*\S.*/)]], version: [0] });
  readonly passwordForm = this.fb.nonNullable.group({
    currentPassword: ['', Validators.required], newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]], confirmation: ['', Validators.required],
  }, { validators: passwordsMatch });
  readonly languageForm = this.fb.nonNullable.group({ preferredLanguage: ['en' as Language] });

  ngOnInit(): void { this.load(); }
  ngOnDestroy(): void { this.revokePreview(); this.revokeCurrent(); }

  load(): void {
    this.loading.set(true);
    this.api.current().subscribe({
      next: (settings) => { this.settings.set(settings); this.profileForm.setValue({ displayName: settings.displayName, version: settings.version }); this.languageForm.setValue({ preferredLanguage: settings.preferredLanguage }); this.loadAvatar(settings.avatarUrl); this.loading.set(false); queueMicrotask(() => this.heading?.nativeElement.focus()); },
      error: () => { this.profileError.set(true); this.profileMessage.set(this.translate.instant('ACCOUNT_SETTINGS.LOAD_ERROR')); this.loading.set(false); },
    });
  }

  savePreferredLanguage(): void {
    if (this.languageSubmitting()) return;
    const preferredLanguage = this.languageForm.getRawValue().preferredLanguage;
    if (preferredLanguage === this.settings()?.preferredLanguage) return;
    this.languageSubmitting.set(true); this.languageMessage.set(''); this.languageError.set(false);
    this.api.updatePreferredLanguage({ preferredLanguage }).subscribe({
      next: (settings) => {
        this.settings.set(settings); this.languageForm.setValue({ preferredLanguage: settings.preferredLanguage });
        this.language.set(settings.preferredLanguage); this.auth.updatePreferredLanguage(settings.preferredLanguage);
        this.languageMessage.set(this.translate.instant('ACCOUNT_SETTINGS.LANGUAGE_SUCCESS')); this.languageSubmitting.set(false);
      },
      error: () => { this.languageError.set(true); this.languageMessage.set(this.translate.instant('ACCOUNT_SETTINGS.LANGUAGE_ERROR')); this.languageSubmitting.set(false); },
    });
  }

  saveProfile(): void {
    if (this.profileForm.invalid || this.profileSubmitting()) return;
    this.profileSubmitting.set(true); this.profileMessage.set(''); this.profileError.set(false);
    const value = this.profileForm.getRawValue();
    this.api.updateProfile({ displayName: value.displayName.trim(), version: value.version }).subscribe({
      next: (settings) => { this.settings.set(settings); this.profileForm.setValue({ displayName: settings.displayName, version: settings.version }); this.auth.updateDisplayName(settings.displayName); this.profileMessage.set(this.translate.instant('ACCOUNT_SETTINGS.PROFILE_SUCCESS')); this.profileSubmitting.set(false); },
      error: () => { this.profileError.set(true); this.profileMessage.set(this.translate.instant('ACCOUNT_SETTINGS.PROFILE_ERROR')); this.profileSubmitting.set(false); },
    });
  }

  savePassword(): void {
    if (this.passwordForm.invalid || this.passwordSubmitting()) return;
    this.passwordSubmitting.set(true); this.passwordMessage.set(''); this.passwordError.set(false);
    const { currentPassword, newPassword } = this.passwordForm.getRawValue();
    this.api.changePassword({ currentPassword, newPassword }).subscribe({
      next: () => { this.passwordForm.reset(); this.passwordMessage.set(this.translate.instant('ACCOUNT_SETTINGS.PASSWORD_SUCCESS')); this.passwordSubmitting.set(false); },
      error: () => { this.passwordError.set(true); this.passwordMessage.set(this.translate.instant('ACCOUNT_SETTINGS.PASSWORD_ERROR')); this.passwordSubmitting.set(false); },
    });
  }

  selectAvatar(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.avatarMessage.set(''); this.avatarError.set(false);
    if (!['image/jpeg', 'image/png'].includes(file.type) || file.size === 0 || file.size > 5 * 1024 * 1024) {
      this.avatarError.set(true); this.avatarMessage.set(this.translate.instant('ACCOUNT_SETTINGS.AVATAR_INVALID_FILE')); return;
    }
    this.revokePreview();
    this.selectedAvatar.set(file); this.avatarPreview.set(URL.createObjectURL(file));
  }

  uploadAvatar(): void {
    const file = this.selectedAvatar();
    if (!file || this.avatarSubmitting()) return;
    this.avatarSubmitting.set(true); this.avatarError.set(false); this.avatarMessage.set('');
    this.api.uploadAvatar(file).subscribe({
      next: (settings) => { this.settings.set(settings); this.auth.updateAvatar(settings.avatarUrl); this.selectedAvatar.set(null); this.revokePreview(); this.loadAvatar(settings.avatarUrl); this.avatarMessage.set(this.translate.instant('ACCOUNT_SETTINGS.AVATAR_SUCCESS')); this.avatarSubmitting.set(false); },
      error: (error: unknown) => { this.avatarError.set(true); this.avatarMessage.set(this.avatarUploadError(error)); this.avatarSubmitting.set(false); },
    });
  }

  removeAvatar(): void {
    if (this.avatarSubmitting()) return;
    this.avatarSubmitting.set(true); this.avatarError.set(false); this.avatarMessage.set('');
    this.api.removeAvatar().subscribe({
      next: () => { const settings = this.settings(); if (settings) this.settings.set({ ...settings, avatarUrl: undefined }); this.auth.updateAvatar(); this.selectedAvatar.set(null); this.revokePreview(); this.revokeCurrent(); this.avatarMessage.set(this.translate.instant('ACCOUNT_SETTINGS.AVATAR_REMOVED')); this.avatarSubmitting.set(false); },
      error: () => { this.avatarError.set(true); this.avatarMessage.set(this.translate.instant('ACCOUNT_SETTINGS.AVATAR_ERROR')); this.avatarSubmitting.set(false); },
    });
  }

  initials(name: string): string { return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase(); }
  onDrawerChange(opened: boolean, drawerScroll: HTMLElement): void { if (opened) drawerScroll.scrollTop = 0; else queueMicrotask(() => this.header?.focusMenuButton()); }
  closeMenu(drawer: MatSidenav): void { void drawer.close(); }
  private revokePreview(): void { const preview = this.avatarPreview(); if (preview) URL.revokeObjectURL(preview); this.avatarPreview.set(null); }
  private avatarUploadError(error: unknown): string {
    const code = error instanceof HttpErrorResponse && typeof error.error === 'object' && error.error !== null
      ? (error.error as { code?: unknown }).code : undefined;
    const key = typeof code === 'string' ? ({
      'ACCOUNT.AVATAR_EMPTY': 'AVATAR_EMPTY', 'ACCOUNT.AVATAR_TOO_LARGE': 'AVATAR_TOO_LARGE',
      'ACCOUNT.AVATAR_INVALID_TYPE': 'AVATAR_UNSUPPORTED_TYPE', 'ACCOUNT.AVATAR_INVALID_IMAGE': 'AVATAR_INVALID_IMAGE',
      'AUTHENTICATION.FAILED': 'AVATAR_SESSION_EXPIRED', 'INFRASTRUCTURE.FAILURE': 'AVATAR_UNAVAILABLE',
    } as Record<string, string>)[code] : undefined;
    return this.translate.instant(`ACCOUNT_SETTINGS.${key ?? 'AVATAR_ERROR'}`);
  }
  private loadAvatar(url?: string): void { this.revokeCurrent(); if (url) this.api.avatar().subscribe({ next: (blob) => this.avatarCurrent.set(URL.createObjectURL(blob)) }); }
  private revokeCurrent(): void { const current = this.avatarCurrent(); if (current) URL.revokeObjectURL(current); this.avatarCurrent.set(null); }
}
