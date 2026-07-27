import { Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';
import { Language, LanguageService } from '../core/language.service';

@Component({
  selector: 'app-language-selector',
  imports: [MatIconModule, MatSelectModule, TranslatePipe],
  templateUrl: './language-selector.component.html',
  styleUrl: './language-selector.component.scss',
})
export class LanguageSelectorComponent {
  readonly language = inject(LanguageService);
  setLanguage(language: Language): void { this.language.set(language); }
}
