import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';
import { clearSystemUnavailable, wasSystemUnavailable } from './system-availability';

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  const token = signal<string | null>('valid-token');
  const clearSession = vi.fn(() => token.set(null));
  const router = { navigateByUrl: vi.fn(() => Promise.resolve(true)) };

  beforeEach(() => {
    clearSystemUnavailable();
    token.set('valid-token');
    clearSession.mockClear();
    router.navigateByUrl.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { token, clearSession } },
        { provide: Router, useValue: router },
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('clears the local session and redirects to login when an authenticated API request returns 401', () => {
    http.get('/api/patients').subscribe({ error: () => undefined });
    const request = controller.expectOne('/api/patients');
    expect(request.request.headers.get('Authorization')).toBe('Bearer valid-token');
    request.flush('expired', { status: 401, statusText: 'Unauthorized' });

    expect(clearSession).toHaveBeenCalledOnce();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });

  it('keeps an authenticated session for authorization errors', () => {
    http.get('/api/patients').subscribe({ error: () => undefined });
    controller
      .expectOne('/api/patients')
      .flush('forbidden', { status: 403, statusText: 'Forbidden' });

    expect(clearSession).not.toHaveBeenCalled();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('preserves JWT authentication for GraphQL requests', () => {
    http.post('/graphql', { query: 'query Countries { countries { page } }' }).subscribe();
    const request = controller.expectOne('/graphql');
    expect(request.request.headers.get('Authorization')).toBe('Bearer valid-token');
    request.flush({ data: { countries: { page: 0 } } });
  });

  it('moves to the unavailable screen when an authenticated API request cannot reach the server', () => {
    http.get('/api/patients').subscribe({ error: () => undefined });
    controller
      .expectOne('/api/patients')
      .flush('down', { status: 503, statusText: 'Service Unavailable' });

    expect(clearSession).toHaveBeenCalledOnce();
    expect(wasSystemUnavailable()).toBe(true);
  });

  it('does not redirect when a login attempt is rejected', () => {
    http.post('/api/auth/login', {}).subscribe({ error: () => undefined });
    const request = controller.expectOne('/api/auth/login');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush('invalid credentials', { status: 401, statusText: 'Unauthorized' });

    expect(clearSession).not.toHaveBeenCalled();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
