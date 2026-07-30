import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-email-enrollment',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    TranslatePipe,
  ],
  templateUrl: './email-enrollment.component.html',
  styleUrl: './email-enrollment.component.scss',
})
export class EmailEnrollmentComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly sent = signal(false);
  readonly message = signal('');
  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
  });

  submit(): void {
    if (this.form.invalid || this.loading()) return;
    this.loading.set(true);
    this.message.set('');
    this.auth.startEmailEnrollment(this.form.getRawValue().email).subscribe({
      next: () => {
        this.sent.set(true);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => this.handleError(error),
    });
  }

  resend(): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.message.set('');
    this.auth.resendEmailEnrollment().subscribe({
      next: () => {
        this.message.set(this.translate.instant('EMAIL_ENROLLMENT.RESENT'));
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => this.handleError(error),
    });
  }

  logout(): void {
    this.auth.logout();
  }

  private handleError(error: HttpErrorResponse): void {
    const key =
      error.status === 429
        ? 'EMAIL_ENROLLMENT.THROTTLED'
        : 'EMAIL_ENROLLMENT.ERROR';
    this.message.set(this.translate.instant(key));
    this.loading.set(false);
  }
}
