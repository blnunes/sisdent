import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { switchMap, tap } from 'rxjs';
import {
  JwtPayload,
  LoginRequest,
  Membership,
  Permission,
  Session,
  TokenResponse,
} from './models';

const TOKEN_KEY = 'sisdent.access-token';
const MEMBERSHIP_KEY = 'sisdent.active-membership';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly tokenState = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  private readonly sessionState = signal<Session | null>(null);
  private readonly activeMembershipId = signal<string | null>(localStorage.getItem(MEMBERSHIP_KEY));

  readonly token = this.tokenState.asReadonly();
  readonly session = this.sessionState.asReadonly();
  readonly payload = computed(() => this.decode(this.tokenState()));
  readonly authenticated = computed(() => {
    const payload = this.payload();
    return !!payload && payload.exp * 1000 > Date.now();
  });
  readonly isAdmin = computed(() => this.payload()?.platformAdministrator ?? false);
  readonly isPlatformAdministrator = computed(
    () => this.sessionState()?.platformAdministrator ?? this.payload()?.platformAdministrator ?? false,
  );
  readonly activeMembership = computed(() => {
    const memberships = this.sessionState()?.memberships ?? this.payload()?.memberships ?? [];
    return memberships.find(({ id }) => id === this.activeMembershipId()) ?? memberships[0] ?? null;
  });

  constructor() {
    if (this.authenticated()) this.loadSession().subscribe();
  }

  hasPermission(permission: Permission): boolean {
    if (permission === 'READ_PATIENTS') return !!this.activeMembership();
    if (permission === 'MAINTAIN_PATIENTS') {
      const role = this.activeMembership()?.role;
      return role === 'ORGANIZATION_ADMIN' || role === 'MANAGER';
    }
    return this.isAdmin() || (this.payload()?.authorities.includes(permission) ?? false);
  }

  hasAllPermissions(...permissions: Permission[]): boolean {
    return permissions.every((permission) => this.hasPermission(permission));
  }

  hasAnyPermission(...permissions: Permission[]): boolean {
    return permissions.some((permission) => this.hasPermission(permission));
  }

  canReadAppointments(): boolean {
    const role = this.activeMembership()?.role;
    return role === 'ORGANIZATION_ADMIN'
      || role === 'MANAGER'
      || role === 'APPOINTMENT_MANAGER'
      || role === 'APPOINTMENT_READER'
      || role === 'READ_ONLY';
  }

  canManageAppointments(): boolean {
    const role = this.activeMembership()?.role;
    return role === 'ORGANIZATION_ADMIN' || role === 'MANAGER' || role === 'APPOINTMENT_MANAGER';
  }

  canManagePractitioners(): boolean {
    const membership = this.activeMembership();
    if (!membership || membership.clinicUnitId) return false;
    return membership.role === 'ORGANIZATION_ADMIN'
      || membership.role === 'MANAGER'
      || membership.role === 'PRACTITIONER_MANAGER';
  }

  canReadClinical(): boolean {
    const role = this.activeMembership()?.role;
    return role === 'ORGANIZATION_ADMIN' || role === 'CLINICAL_READER' || role === 'CLINICAL_AUTHOR' || role === 'CLINICAL_MANAGER';
  }

  canAuthorClinical(): boolean {
    const role = this.activeMembership()?.role;
    return role === 'ORGANIZATION_ADMIN' || role === 'CLINICAL_AUTHOR' || role === 'CLINICAL_MANAGER';
  }

  canManageClinical(): boolean {
    const role = this.activeMembership()?.role;
    return role === 'ORGANIZATION_ADMIN' || role === 'CLINICAL_MANAGER';
  }

  canManageOrganizationAccess(): boolean {
    return this.canAdministerOrganization();
  }

  /** Organization administration is deliberately restricted to organization-wide administrators. */
  canAdministerOrganization(): boolean {
    const membership = this.activeMembership();
    return membership?.role === 'ORGANIZATION_ADMIN' && !membership.clinicUnitId;
  }

  login(request: LoginRequest) {
    return this.http.post<TokenResponse>('/api/auth/login', request).pipe(
      tap(({ accessToken }) => {
        localStorage.setItem(TOKEN_KEY, accessToken);
        this.tokenState.set(accessToken);
      }),
      switchMap(() => this.loadSession()),
    );
  }

  loadSession() {
    return this.http.get<Session>('/api/session').pipe(
      tap((session) => {
        this.sessionState.set(session);
        if (!session.memberships.some(({ id }) => id === this.activeMembershipId())) {
          this.selectMembership(session.memberships[0] ?? null);
        }
      }),
    );
  }

  selectMembership(membership: Membership | string | null): void {
    const selectedMembership = typeof membership === 'string'
      ? (this.sessionState()?.memberships ?? this.payload()?.memberships ?? [])
          .find(({ id }) => id === membership) ?? null
      : membership;
    if (selectedMembership) localStorage.setItem(MEMBERSHIP_KEY, selectedMembership.id);
    else localStorage.removeItem(MEMBERSHIP_KEY);
    this.activeMembershipId.set(selectedMembership?.id ?? null);
  }

  logout(): void {
    this.clearSession();
    void this.router.navigateByUrl('/login');
  }

  clearSession(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(MEMBERSHIP_KEY);
    this.tokenState.set(null);
    this.sessionState.set(null);
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
