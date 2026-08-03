import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('stores a valid admin token after login', () => {
    service
      .login({
        email: 'admin@sisdent.local',
        password: 'admin',
      })
      .subscribe();

    const request = http.expectOne('/api/auth/login');
    expect(request.request.method).toBe('POST');
    const token = jwt({ accountId: 'account-1', email: 'admin@sisdent.local', platformAdministrator: true, memberships: [], authorities: ['ROLE_ADMIN', 'READ_USERS'], exp: futureExpiration() });
    request.flush({ accessToken: token, tokenType: 'Bearer', expiresIn: 3600 });
    http.expectOne('/api/session').flush({
      accountId: 'account-1', email: 'admin@sisdent.local', displayName: 'Administrator',
      platformAdministrator: true, emailMigrationRequired: false, memberships: [],
    });

    expect(service.authenticated()).toBe(true);
    expect(service.isAdmin()).toBe(true);
    expect(service.destination()).toBe('/home');
    expect(localStorage.getItem('sisdent.access-token')).toBe(token);
  });

  it('sends a non-admin user to the friendly restricted page', () => {
    service
      .login({
        email: 'user@example.com',
        password: 'password',
      })
      .subscribe();

    const request = http.expectOne('/api/auth/login');
    request.flush({
      accessToken: jwt({ accountId: 'account-2', email: 'user@example.com', platformAdministrator: false, memberships: [], authorities: ['ROLE_USER', 'READ_USERS'], exp: futureExpiration() }),
      tokenType: 'Bearer',
      expiresIn: 3600,
    });
    http.expectOne('/api/session').flush({
      accountId: 'account-2', email: 'user@example.com', displayName: 'User',
      platformAdministrator: false, emailMigrationRequired: false, memberships: [],
    });

    expect(service.isAdmin()).toBe(false);
    expect(service.destination()).toBe('/home');
  });

  it('routes a legacy login to required email enrollment', () => {
    service
      .login({
        identificationType: 'PASSPORT',
        identificationNumber: 'LEGACY-USER',
        password: 'password',
      })
      .subscribe();

    http.expectOne('/api/auth/login').flush({
      accessToken: jwt({
        accountId: 'account-legacy',
        email: 'passport.legacy-user@legacy.sisdent.invalid',
        emailMigrationRequired: true,
        platformAdministrator: false,
        memberships: [],
        authorities: ['ROLE_USER'],
        exp: futureExpiration(),
      }),
      tokenType: 'Bearer',
      expiresIn: 3600,
    });
    http.expectOne('/api/session').flush({
      accountId: 'account-legacy',
      email: 'passport.legacy-user@legacy.sisdent.invalid',
      displayName: 'Legacy user',
      platformAdministrator: false,
      emailMigrationRequired: true,
      memberships: [],
    });

    expect(service.destination()).toBe('/email-enrollment');
  });

  it('selects the requested membership when the header supplies its ID', () => {
    service.login({ email: 'group.admin@sisdent.demo', password: 'odonto2026@O' }).subscribe();
    http.expectOne('/api/auth/login').flush({
      accessToken: jwt({ accountId: 'group-admin', email: 'group.admin@sisdent.demo', platformAdministrator: false, memberships: [], authorities: [], exp: futureExpiration() }),
      tokenType: 'Bearer',
      expiresIn: 3600,
    });
    http.expectOne('/api/session').flush({
      accountId: 'group-admin',
      email: 'group.admin@sisdent.demo',
      displayName: 'Demo Group Administrator',
      platformAdministrator: false,
      emailMigrationRequired: false,
      memberships: [
        { id: 'northstar-membership', organizationId: 'northstar', organizationName: 'Northstar Dental Group', role: 'ORGANIZATION_ADMIN' },
        { id: 'southstart-membership', organizationId: 'southstart', organizationName: 'Southstart Dental Group', role: 'ORGANIZATION_ADMIN' },
      ],
    });

    service.selectMembership('southstart-membership');

    expect(service.activeMembership()?.organizationName).toBe('Southstart Dental Group');
    expect(localStorage.getItem('sisdent.active-membership')).toBe('southstart-membership');
  });

  it('matches appointment visibility to the server role matrix', () => {
    service.login({ email: 'clinical@example.com', password: 'password' }).subscribe();
    http.expectOne('/api/auth/login').flush({
      accessToken: jwt({ accountId: 'clinical', email: 'clinical@example.com', platformAdministrator: false, memberships: [], authorities: [], exp: futureExpiration() }),
      tokenType: 'Bearer', expiresIn: 3600,
    });
    http.expectOne('/api/session').flush({
      accountId: 'clinical', email: 'clinical@example.com', displayName: 'Clinical',
      platformAdministrator: false, emailMigrationRequired: false,
      memberships: [{ id: 'clinical-membership', organizationId: 'northstar', organizationName: 'Northstar', role: 'CLINICAL_READER' }],
    });

    expect(service.canReadAppointments()).toBe(false);
    service.loadSession().subscribe();
    http.expectOne('/api/session').flush({
      accountId: 'clinical', email: 'clinical@example.com', displayName: 'Clinical',
      platformAdministrator: false, emailMigrationRequired: false,
      memberships: [{ id: 'appointment-reader', organizationId: 'northstar', organizationName: 'Northstar', role: 'APPOINTMENT_READER' }],
    });
    expect(service.canReadAppointments()).toBe(true);
  });

  it('returns the controlled verification outcome without authentication', () => {
    let status = '';
    service.verifyEmail('invalid-token').subscribe((response) => (status = response.status));

    const request = http.expectOne('/api/auth/email-verification');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ token: 'invalid-token' });
    request.flush({ status: 'INVALID_OR_EXPIRED' });

    expect(status).toBe('INVALID_OR_EXPIRED');
  });
});

function futureExpiration(): number {
  return Math.floor(Date.now() / 1000) + 3600;
}

function jwt(payload: object): string {
  return `header.${btoa(JSON.stringify({ sub: 'test', ...payload }))}.signature`;
}
