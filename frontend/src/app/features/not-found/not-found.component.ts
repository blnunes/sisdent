import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageSelectorComponent } from '../../shared/preferences/language-selector/language-selector.component';
import { ThemeToggleComponent } from '../../shared/preferences/theme-toggle/theme-toggle.component';

@Component({
  selector: 'app-not-found',
  imports: [MatButtonModule, MatIconModule, RouterLink, TranslatePipe, LanguageSelectorComponent, ThemeToggleComponent],
  templateUrl: './not-found.component.html',
  styleUrl: './not-found.component.scss',
})
export class NotFoundComponent {
  readonly auth = inject(AuthService);
}
