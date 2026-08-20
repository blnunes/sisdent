import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { tap } from 'rxjs';

interface CsrfTokenResponse { token: string; headerName: string; }

@Injectable({ providedIn: 'root' })
export class CsrfService {
  private readonly http = inject(HttpClient);
  private readonly state = signal<CsrfTokenResponse | null>(null);

  initialize() {
    return this.http.get<CsrfTokenResponse>('/api/csrf').pipe(tap((token) => this.state.set(token)));
  }

  header(): Record<string, string> {
    const token = this.state();
    return token ? { [token.headerName]: token.token } : {};
  }
}
