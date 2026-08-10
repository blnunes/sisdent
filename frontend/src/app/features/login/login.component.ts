import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../core/auth.service';
import { LanguageSelectorComponent } from '../../shared/preferences/language-selector/language-selector.component';
import { ThemeToggleComponent } from '../../shared/preferences/theme-toggle/theme-toggle.component';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    TranslatePipe,
    LanguageSelectorComponent,
    ThemeToggleComponent,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly hidePassword = signal(true);
  readonly error = signal('');
  readonly emailForm = this.fb.nonNullable.group({
    email: ['admin@sisdent.local', [Validators.required, Validators.email]],
    password: ['admin', Validators.required],
  });

  submit(): void {
    const form = this.emailForm;
    if (form.invalid || this.loading()) return;
    this.error.set('');
    this.loading.set(true);
    this.auth.login(form.getRawValue()).subscribe({
      next: () => void this.router.navigateByUrl(this.auth.destination()),
      error: () => {
        this.error.set(this.translate.instant('LOGIN.INVALID'));
        this.loading.set(false);
      },
    });
  }
}
