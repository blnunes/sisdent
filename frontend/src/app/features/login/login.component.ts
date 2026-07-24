import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { AuthService } from '../../core/auth.service';
import { IdentificationType } from '../../core/models';

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
    MatSelectModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly hidePassword = signal(true);
  readonly error = signal('');
  readonly types: IdentificationType[] = ['NATIONAL_ID', 'PASSPORT'];
  readonly form = this.fb.nonNullable.group({
    identificationType: ['NATIONAL_ID' as IdentificationType, Validators.required],
    identificationNumber: ['ADMIN', Validators.required],
    password: ['admin', Validators.required],
  });

  submit(): void {
    if (this.form.invalid || this.loading()) return;
    this.error.set('');
    this.loading.set(true);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => void this.router.navigateByUrl(this.auth.destination()),
      error: () => {
        this.error.set('Identificação ou password inválida. Verifique os dados e tente novamente.');
        this.loading.set(false);
      },
    });
  }
}
