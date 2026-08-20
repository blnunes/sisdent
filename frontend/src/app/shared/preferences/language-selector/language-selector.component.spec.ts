import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideTranslateService } from '@ngx-translate/core';
import { LanguageSelectorComponent } from './language-selector.component';
import { LanguageService } from '../../../core/language.service';
import { AuthService } from '../../../core/auth.service';
import { AccountSettingsApiService } from '../../../core/account-settings-api.service';

describe('LanguageSelectorComponent', () => {
  const language = { current: vi.fn(() => 'en'), set: vi.fn() };
  const auth = { authenticated: vi.fn(() => true), updatePreferredLanguage: vi.fn() };
  const settings = { updatePreferredLanguage: vi.fn(() => of({ preferredLanguage: 'nl' })) };
  let component: LanguageSelectorComponent;

  beforeEach(async () => {
    vi.clearAllMocks();
    language.current.mockReturnValue('en');
    auth.authenticated.mockReturnValue(true);
    await TestBed.configureTestingModule({ imports: [LanguageSelectorComponent], providers: [provideTranslateService(), { provide: LanguageService, useValue: language }, { provide: AuthService, useValue: auth }, { provide: AccountSettingsApiService, useValue: settings }] }).compileComponents();
    component = TestBed.createComponent(LanguageSelectorComponent).componentInstance;
  });

  it('persists a changed language for an authenticated user', () => {
    component.setLanguage('nl');
    expect(settings.updatePreferredLanguage).toHaveBeenCalledWith({ preferredLanguage: 'nl' });
    expect(language.set).toHaveBeenCalledWith('nl');
  });

  it('does not make a request when the language did not change or there is no session', () => {
    component.setLanguage('en');
    expect(settings.updatePreferredLanguage).not.toHaveBeenCalled();
    auth.authenticated.mockReturnValue(false);
    component.setLanguage('pt-PT');
    expect(settings.updatePreferredLanguage).not.toHaveBeenCalled();
    expect(language.set).toHaveBeenCalledWith('pt-PT');
  });
});
