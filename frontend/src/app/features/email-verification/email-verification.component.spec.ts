import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { EmailVerificationComponent } from './email-verification.component';

describe('EmailVerificationComponent', () => {
  it('shows a successful outcome and clears the stale session', async () => {
    const auth = {
      verifyEmail: vi.fn(() => of({ status: 'VERIFIED' as const })),
      clearSession: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [EmailVerificationComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: { get: () => 'delivered-secret' } },
          },
        },
      ],
    })
      .overrideComponent(EmailVerificationComponent, {
        set: { template: '<span>{{ status() }}</span>' },
      })
      .compileComponents();

    const fixture = TestBed.createComponent(EmailVerificationComponent);
    fixture.detectChanges();

    expect(auth.verifyEmail).toHaveBeenCalledWith('delivered-secret');
    expect(auth.clearSession).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('VERIFIED');
  });

  it('shows the generic failure outcome', async () => {
    const auth = {
      verifyEmail: vi.fn(() => of({ status: 'INVALID_OR_EXPIRED' as const })),
      clearSession: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [EmailVerificationComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: { get: () => 'unknown-secret' } },
          },
        },
      ],
    })
      .overrideComponent(EmailVerificationComponent, {
        set: { template: '<span>{{ status() }}</span>' },
      })
      .compileComponents();

    const fixture = TestBed.createComponent(EmailVerificationComponent);
    fixture.detectChanges();

    expect(auth.clearSession).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('INVALID_OR_EXPIRED');
  });
});
