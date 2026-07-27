import { Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';
import { Language, LanguageService } from '../core/language.service';

@Component({
  selector: 'app-language-selector',
  imports: [MatIconModule, MatSelectModule, TranslatePipe],
  template: `
    <mat-icon aria-hidden="true">language</mat-icon>
    <mat-select
      class="language-selector"
      [value]="language.current()"
      (selectionChange)="language.set($event.value)"
      [attr.aria-label]="'LANGUAGE.SELECT' | translate"
    >
      <mat-option value="pt-PT">Português</mat-option>
      <mat-option value="en">English</mat-option>
      <mat-option value="nl">Nederlands</mat-option>
    </mat-select>
  `,
  styles: [`:host { display:inline-flex; align-items:center; gap:5px; min-width:126px; color:inherit; } .language-selector { width:104px; font-size:14px; font-weight:600; }`],
})
export class LanguageSelectorComponent {
  readonly language = inject(LanguageService);
  setLanguage(language: Language): void { this.language.set(language); }
}
