import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { firstValueFrom, of, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { authGuard } from './auth.guard';
import { clearSystemUnavailable } from './system-availability';

describe('authGuard', () => {
  const loginTree = { login: true };
  const unavailableTree = { unavailable: true };
  const router = { createUrlTree: vi.fn((commands: string[]) => commands[0] === '/unavailable' ? unavailableTree : loginTree) };
  const auth = { authenticated: vi.fn(), loadSession: vi.fn(), clearSession: vi.fn() };

  beforeEach(() => {
    sessionStorage.clear(); clearSystemUnavailable();
    router.createUrlTree.mockClear(); auth.authenticated.mockReset(); auth.loadSession.mockReset(); auth.clearSession.mockClear();
    TestBed.configureTestingModule({ providers: [{ provide: Router, useValue: router }, { provide: AuthService, useValue: auth }] });
  });

  it('allows navigation only after the backend confirms the active session', async () => {
    auth.authenticated.mockReturnValue(true); auth.loadSession.mockReturnValue(of({}));

    const result = await resolveGuard();

    expect(result).toBe(true);
    expect(auth.loadSession).toHaveBeenCalledOnce();
  });

  it('clears the cached session and returns to login when the backend cannot validate it', async () => {
    auth.authenticated.mockReturnValue(true); auth.loadSession.mockReturnValue(throwError(() => new Error('backend unavailable')));

    const result = await resolveGuard();

    expect(auth.clearSession).toHaveBeenCalledOnce();
    expect(result).toBe(loginTree);
  });

  it('shows the unavailable screen when the backend cannot be reached', async () => {
    auth.authenticated.mockReturnValue(true); auth.loadSession.mockReturnValue(throwError(() => ({ status: 0 })));

    expect(await resolveGuard()).toBe(unavailableTree);
    expect(auth.clearSession).toHaveBeenCalledOnce();
  });

  it('does not call the backend when the locally held JWT is already expired', () => {
    auth.authenticated.mockReturnValue(false);

    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));

    expect(result).toBe(loginTree);
    expect(auth.loadSession).not.toHaveBeenCalled();
  });
});

async function resolveGuard(): Promise<unknown> {
  const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
  return firstValueFrom(result as any);
}
