import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { JwtPayload, LoginRequest, Permission, TokenResponse } from './models';

const TOKEN_KEY = 'sisdent.access-token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly tokenState = signal<string | null>(localStorage.getItem(TOKEN_KEY));

  readonly token = this.tokenState.asReadonly();
  readonly payload = computed(() => this.decode(this.tokenState()));
  readonly authenticated = computed(() => {
    const payload = this.payload();
    return !!payload && payload.exp * 1000 > Date.now();
  });
  readonly isAdmin = computed(() => this.payload()?.authorities.includes('ROLE_ADMIN') ?? false);

  hasPermission(permission: Permission): boolean {
    return this.isAdmin() || (this.payload()?.authorities.includes(permission) ?? false);
  }

  hasAllPermissions(...permissions: Permission[]): boolean {
    return permissions.every((permission) => this.hasPermission(permission));
  }

  hasAnyPermission(...permissions: Permission[]): boolean {
    return permissions.some((permission) => this.hasPermission(permission));
  }

  login(request: LoginRequest) {
    return this.http.post<TokenResponse>('/api/auth/login', request).pipe(
      tap(({ accessToken }) => {
        localStorage.setItem(TOKEN_KEY, accessToken);
        this.tokenState.set(accessToken);
      }),
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.tokenState.set(null);
    void this.router.navigateByUrl('/login');
  }

  destination(): string {
    return '/home';
  }

  private decode(token: string | null): JwtPayload | null {
    if (!token) return null;
    try {
      const part = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(decodeURIComponent(escape(atob(part)))) as JwtPayload;
    } catch {
      return null;
    }
  }
}
