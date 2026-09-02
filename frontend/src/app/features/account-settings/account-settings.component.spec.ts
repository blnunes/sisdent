import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { provideTranslateService } from '@ngx-translate/core';
import { AccountSettingsComponent } from './account-settings.component';
import { AccountSettingsApiService } from '../../core/account-settings-api.service';
import { AuthService } from '../../core/auth.service';
import { LanguageService } from '../../core/language.service';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpErrorResponse } from '@angular/common/http';

describe('AccountSettingsComponent', () => {
  let fixture: ComponentFixture<AccountSettingsComponent>;
  let component: AccountSettingsComponent;
  const api = {
    current: vi.fn(),
    updateProfile: vi.fn(),
    updatePreferredLanguage: vi.fn(),
    changePassword: vi.fn(),
    uploadAvatar: vi.fn(),
    removeAvatar: vi.fn(),
    avatar: vi.fn(),
  };
  const auth = {
    updateDisplayName: vi.fn(),
    updatePreferredLanguage: vi.fn(),
    updateAvatar: vi.fn(),
    session: vi.fn(() => null),
    activeMembership: vi.fn(() => null),
    isAdmin: vi.fn(() => false),
    isPlatformAdministrator: vi.fn(() => false),
    hasAnyPermission: vi.fn(() => false),
    canReadAppointments: vi.fn(() => false),
    canReadClinical: vi.fn(() => false),
    canAdministerOrganization: vi.fn(() => false),
    canManagePractitioners: vi.fn(() => false),
    canManageOrganizationAccess: vi.fn(() => false),
    logout: vi.fn(),
  };
  const language = { current: vi.fn(() => 'en'), set: vi.fn(), isSupported: vi.fn(() => true) };
  beforeEach(async () => {
    api.current.mockReturnValue(
      of({
        id: 'me',
        email: 'me@example.com',
        displayName: 'Me',
        preferredLanguage: 'en',
        version: 1,
      }),
    );
    await TestBed.configureTestingModule({
      imports: [AccountSettingsComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        provideTranslateService(),
        { provide: AccountSettingsApiService, useValue: api },
        { provide: AuthService, useValue: auth },
        { provide: LanguageService, useValue: language },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(AccountSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('validates profile and mismatched passwords', () => {
    component.profileForm.controls.displayName.setValue(' ');
    component.passwordForm.setValue({
      currentPassword: 'current',
      newPassword: 'new-password',
      confirmation: 'different',
    });
    expect(component.profileForm.invalid).toBe(true);
    expect(component.passwordForm.hasError('passwordMismatch')).toBe(true);
  });

  it('associates labels with every account settings input and uses output for messages', () => {
    const element = fixture.nativeElement as HTMLElement;
    for (const inputId of [
      'account-avatar-input',
      'account-email',
      'account-display-name',
      'account-current-password',
      'account-new-password',
      'account-password-confirmation',
    ]) {
      expect(element.querySelector(`input#${inputId}`)).not.toBeNull();
      expect(element.querySelector(`label[for="${inputId}"]`)).not.toBeNull();
    }
    expect(element.querySelectorAll('p[role="status"]')).toHaveLength(0);
  });

  it('updates the session after a successful profile update', () => {
    api.updateProfile.mockReturnValue(
      of({ id: 'me', email: 'me@example.com', displayName: 'Updated', version: 2 }),
    );
    component.profileForm.setValue({ displayName: 'Updated', version: 1 });
    component.saveProfile();
    expect(auth.updateDisplayName).toHaveBeenCalledWith('Updated');
  });

  it('shows an error when a password update fails', () => {
    api.changePassword.mockReturnValue(throwError(() => new Error('failed')));
    component.passwordForm.setValue({
      currentPassword: 'current',
      newPassword: 'new-password',
      confirmation: 'new-password',
    });
    component.savePassword();
    expect(component.passwordError()).toBe(true);
  });

  it('persists a selected language and updates the local session and language cache on success', () => {
    api.updatePreferredLanguage.mockReturnValue(
      of({
        id: 'me',
        email: 'me@example.com',
        displayName: 'Me',
        preferredLanguage: 'nl',
        version: 1,
      }),
    );
    component.languageForm.setValue({ preferredLanguage: 'nl' });

    component.savePreferredLanguage();

    expect(api.updatePreferredLanguage).toHaveBeenCalledWith({ preferredLanguage: 'nl' });
    expect(language.set).toHaveBeenCalledWith('nl');
    expect(auth.updatePreferredLanguage).toHaveBeenCalledWith('nl');
  });

  it('keeps the selected language and exposes an error when saving fails', () => {
    api.updatePreferredLanguage.mockReturnValue(throwError(() => new Error('failed')));
    component.languageForm.setValue({ preferredLanguage: 'nl' });

    component.savePreferredLanguage();

    expect(component.languageForm.getRawValue().preferredLanguage).toBe('nl');
    expect(component.languageError()).toBe(true);
  });

  it('keeps the selected preview and shows the specific RFC 9457 avatar error after a failed upload', () => {
    const file = new File(['image'], 'avatar.png', { type: 'image/png' });
    component.selectedAvatar.set(file);
    component.avatarPreview.set('blob:preview');
    api.uploadAvatar.mockReturnValue(
      throwError(
        () => new HttpErrorResponse({ status: 400, error: { code: 'ACCOUNT.AVATAR_TOO_LARGE' } }),
      ),
    );

    component.uploadAvatar();

    expect(component.avatarError()).toBe(true);
    expect(component.avatarMessage()).toBe('ACCOUNT_SETTINGS.AVATAR_TOO_LARGE');
    expect(component.selectedAvatar()).toBe(file);
    expect(component.avatarSubmitting()).toBe(false);
  });

  it('uses the generic message only when the avatar error has no known problem code', () => {
    component.selectedAvatar.set(new File(['image'], 'avatar.png', { type: 'image/png' }));
    api.uploadAvatar.mockReturnValue(throwError(() => new Error('network')));

    component.uploadAvatar();

    expect(component.avatarMessage()).toBe('ACCOUNT_SETTINGS.AVATAR_ERROR');
    expect(component.avatarSubmitting()).toBe(false);
  });

  it('removes an avatar, resets its local state, and returns focus after the drawer closes', () => {
    component.selectedAvatar.set(new File(['image'], 'avatar.png', { type: 'image/png' }));
    api.removeAvatar.mockReturnValue(
      of({ id: 'me', email: 'me@example.com', displayName: 'Me', version: 2 }),
    );
    const drawerScroll = { scrollTop: 42 } as HTMLElement;

    component.removeAvatar();
    component.onDrawerOpened(drawerScroll);

    expect(auth.updateAvatar).toHaveBeenCalledWith();
    expect(component.selectedAvatar()).toBeNull();
    expect(drawerScroll.scrollTop).toBe(0);
  });

  it('maps known GraphQL avatar failures without rendering unsafe error values', () => {
    expect(
      (component as unknown as { avatarErrorCode: (error: unknown) => unknown }).avatarErrorCode({
        code: 'raw',
      }),
    ).toBeUndefined();
    expect(
      (component as unknown as { avatarUploadError: (error: unknown) => string }).avatarUploadError(
        { code: 'raw' },
      ),
    ).toBe('ACCOUNT_SETTINGS.AVATAR_ERROR');
  });

  it('extracts only the typed code from HTTP avatar failures', () => {
    const helper = component as unknown as { avatarErrorCode: (error: unknown) => unknown };
    expect(
      helper.avatarErrorCode(new HttpErrorResponse({ error: { code: 'ACCOUNT.AVATAR_EMPTY' } })),
    ).toBe('ACCOUNT.AVATAR_EMPTY');
    expect(helper.avatarErrorCode(new HttpErrorResponse({ error: 'invalid' }))).toBeUndefined();
  });
});
