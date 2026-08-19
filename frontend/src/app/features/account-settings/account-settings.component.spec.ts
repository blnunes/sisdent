import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { provideTranslateService } from '@ngx-translate/core';
import { AccountSettingsComponent } from './account-settings.component';
import { AccountSettingsApiService } from '../../core/account-settings-api.service';
import { AuthService } from '../../core/auth.service';

describe('AccountSettingsComponent', () => {
  let fixture: ComponentFixture<AccountSettingsComponent>;
  let component: AccountSettingsComponent;
  const api = { current: vi.fn(), updateProfile: vi.fn(), changePassword: vi.fn() };
  const auth = { updateDisplayName: vi.fn() };
  beforeEach(async () => {
    api.current.mockReturnValue(of({ id: 'me', email: 'me@example.com', displayName: 'Me', version: 1 }));
    await TestBed.configureTestingModule({ imports: [AccountSettingsComponent], providers: [provideTranslateService(), { provide: AccountSettingsApiService, useValue: api }, { provide: AuthService, useValue: auth }] }).compileComponents();
    fixture = TestBed.createComponent(AccountSettingsComponent); component = fixture.componentInstance; fixture.detectChanges();
  });

  it('validates profile and mismatched passwords', () => {
    component.profileForm.controls.displayName.setValue(' ');
    component.passwordForm.setValue({ currentPassword: 'current', newPassword: 'new-password', confirmation: 'different' });
    expect(component.profileForm.invalid).toBe(true);
    expect(component.passwordForm.hasError('passwordMismatch')).toBe(true);
  });

  it('updates the session after a successful profile update', () => {
    api.updateProfile.mockReturnValue(of({ id: 'me', email: 'me@example.com', displayName: 'Updated', version: 2 }));
    component.profileForm.setValue({ displayName: 'Updated', version: 1 });
    component.saveProfile();
    expect(auth.updateDisplayName).toHaveBeenCalledWith('Updated');
  });

  it('shows an error when a password update fails', () => {
    api.changePassword.mockReturnValue(throwError(() => new Error('failed')));
    component.passwordForm.setValue({ currentPassword: 'current', newPassword: 'new-password', confirmation: 'new-password' });
    component.savePassword();
    expect(component.passwordError()).toBe(true);
  });
});
