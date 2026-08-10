import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageSelectorComponent } from '../../shared/preferences/language-selector/language-selector.component';
import { ThemeToggleComponent } from '../../shared/preferences/theme-toggle/theme-toggle.component';
import { clearSystemUnavailable } from '../../core/system-availability';

@Component({
  selector: 'app-unavailable',
  imports: [MatButtonModule, MatCardModule, MatIconModule, TranslatePipe, LanguageSelectorComponent, ThemeToggleComponent],
  templateUrl: './unavailable.component.html',
  styleUrl: './unavailable.component.scss',
})
export class UnavailableComponent {
  private readonly router = inject(Router);
  retry(): void { clearSystemUnavailable(); void this.router.navigateByUrl('/login'); }
}
