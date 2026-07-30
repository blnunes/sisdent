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
        identificationType: 'NATIONAL_ID',
        identificationNumber: 'admin',
        password: 'admin',
      })
      .subscribe();

    const request = http.expectOne('/api/auth/login');
    expect(request.request.method).toBe('POST');
    const token = jwt({ userId: 1, authorities: ['ROLE_ADMIN', 'READ_USERS'], exp: futureExpiration() });
    request.flush({ accessToken: token, tokenType: 'Bearer', expiresIn: 3600 });

    expect(service.authenticated()).toBe(true);
    expect(service.isAdmin()).toBe(true);
    expect(service.destination()).toBe('/home');
    expect(localStorage.getItem('sisdent.access-token')).toBe(token);
  });

  it('sends a non-admin user to the friendly restricted page', () => {
    service
      .login({
        identificationType: 'PASSPORT',
        identificationNumber: 'USER',
        password: 'password',
      })
      .subscribe();

    const request = http.expectOne('/api/auth/login');
    request.flush({
      accessToken: jwt({ userId: 2, authorities: ['ROLE_USER', 'READ_USERS'], exp: futureExpiration() }),
      tokenType: 'Bearer',
      expiresIn: 3600,
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
