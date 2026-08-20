import { Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';
import { Language, LanguageService, LANGUAGE_OPTIONS } from '../../../core/language.service';
import { AuthService } from '../../../core/auth.service';
import { AccountSettingsApiService } from '../../../core/account-settings-api.service';

@Component({
  selector: 'app-language-selector',
  imports: [MatIconModule, MatSelectModule, TranslatePipe],
  templateUrl: './language-selector.component.html',
  styleUrl: './language-selector.component.scss',
})
export class LanguageSelectorComponent {
  readonly language = inject(LanguageService);
  readonly languageOptions = LANGUAGE_OPTIONS;
  private readonly auth = inject(AuthService);
  private readonly settings = inject(AccountSettingsApiService);

  setLanguage(language: Language): void {
    if (language === this.language.current()) return;
    if (!this.auth.authenticated()) {
      this.language.set(language);
      return;
    }
    this.settings.updatePreferredLanguage({ preferredLanguage: language }).subscribe({
      next: (response) => {
        this.language.set(response.preferredLanguage);
        this.auth.updatePreferredLanguage(response.preferredLanguage);
      },
    });
  }
}
