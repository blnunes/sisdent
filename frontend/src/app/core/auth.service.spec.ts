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
});

function futureExpiration(): number {
  return Math.floor(Date.now() / 1000) + 3600;
}

function jwt(payload: object): string {
  return `header.${btoa(JSON.stringify({ sub: 'test', ...payload }))}.signature`;
}
