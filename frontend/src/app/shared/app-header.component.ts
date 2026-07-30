import { Component, ElementRef, EventEmitter, Output, ViewChild, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../core/auth.service';
import { LanguageSelectorComponent } from './language-selector.component';
import { ThemeToggleComponent } from './theme-toggle.component';

@Component({
  selector: 'app-header',
  imports: [RouterLink, MatButtonModule, MatIconModule, MatToolbarModule, MatSelectModule, TranslatePipe, LanguageSelectorComponent, ThemeToggleComponent],
  templateUrl: './app-header.component.html',
  styleUrl: './app-header.component.scss',
})
export class AppHeaderComponent {
  readonly auth = inject(AuthService);
  @Output() readonly menuClick = new EventEmitter<void>();
  @ViewChild('menuButton') private menuButton?: ElementRef<HTMLButtonElement>;

  focusMenuButton(): void {
    this.menuButton?.nativeElement.focus();
  }
}
