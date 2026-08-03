import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-translation-error',
  imports: [MatButtonModule, MatIconModule, RouterLink],
  templateUrl: './translation-error.component.html',
  styleUrl: './translation-error.component.scss',
})
export class TranslationErrorComponent {
  private readonly route = inject(ActivatedRoute);
  language = '';
  resource = '';
  message = '';

  constructor() { this.readDetails(); }

  retry(): void { window.location.reload(); }

  private readDetails(): void {
    const params = this.route.snapshot.queryParamMap;
    this.language = params.get('language') ?? 'unknown';
    this.resource = params.get('resource') ?? 'unknown';
    this.message = params.get('message') ?? 'No response details';
  }
}
