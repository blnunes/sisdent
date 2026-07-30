import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../core/auth.service';
import { EmailVerificationStatus } from '../../core/models';

@Component({
  selector: 'app-email-verification',
  imports: [
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    TranslatePipe,
  ],
  templateUrl: './email-verification.component.html',
  styleUrl: './email-verification.component.scss',
})
export class EmailVerificationComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);

  readonly loading = signal(true);
  readonly status = signal<EmailVerificationStatus>('INVALID_OR_EXPIRED');

  constructor() {
    const token = this.route.snapshot.queryParamMap.get('token') ?? '';
    this.auth.verifyEmail(token).subscribe({
      next: (response) => {
        this.status.set(response.status);
        if (response.status === 'VERIFIED') this.auth.clearSession();
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
