import { Component, ElementRef, EventEmitter, OnDestroy, Output, ViewChild, effect, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSelectModule } from '@angular/material/select';
import { MatMenuModule } from '@angular/material/menu';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../auth.service';
import { LanguageSelectorComponent } from '../../../shared/preferences/language-selector/language-selector.component';
import { ThemeToggleComponent } from '../../../shared/preferences/theme-toggle/theme-toggle.component';

@Component({
  selector: 'app-header',
  imports: [RouterLink, MatButtonModule, MatIconModule, MatToolbarModule, MatSelectModule, MatMenuModule, TranslatePipe, LanguageSelectorComponent, ThemeToggleComponent],
  templateUrl: './app-header.component.html',
  styleUrl: './app-header.component.scss',
})
export class AppHeaderComponent implements OnDestroy {
  readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  readonly avatarSource = signal<string | null>(null);
  @Output() readonly menuClick = new EventEmitter<void>();
  @ViewChild('menuButton') private menuButton?: ElementRef<HTMLButtonElement>;
  private avatarObjectUrl: string | null = null;
  private readonly avatarLoader = effect(() => {
    const avatarUrl = this.auth.session()?.avatarUrl;
    this.clearAvatar();
    if (avatarUrl) this.http.get(avatarUrl, { responseType: 'blob' }).subscribe({
      next: (blob) => { this.avatarObjectUrl = URL.createObjectURL(blob); this.avatarSource.set(this.avatarObjectUrl); },
    });
  });

  focusMenuButton(): void {
    this.menuButton?.nativeElement.focus();
  }
  initials(name: string | undefined): string { return (name ?? '').trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase(); }
  ngOnDestroy(): void { this.avatarLoader.destroy(); this.clearAvatar(); }
  private clearAvatar(): void { if (this.avatarObjectUrl) URL.revokeObjectURL(this.avatarObjectUrl); this.avatarObjectUrl = null; this.avatarSource.set(null); }
}
