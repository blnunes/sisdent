import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AccountSettingsApiService } from './account-settings-api.service';

describe('AccountSettingsApiService', () => {
  let service: AccountSettingsApiService;
  let http: HttpTestingController;
  beforeEach(() => { TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] }); service = TestBed.inject(AccountSettingsApiService); http = TestBed.inject(HttpTestingController); });
  afterEach(() => http.verify());

  it('uses self-service endpoints without an account identifier', () => {
    service.current().subscribe();
    expect(http.expectOne('/api/account/settings').request.method).toBe('GET');
    service.updateProfile({ displayName: 'Ana', version: 2 }).subscribe();
    const profile = http.expectOne('/api/account/settings/profile').request;
    expect(profile.method).toBe('PATCH'); expect(profile.body).toEqual({ displayName: 'Ana', version: 2 });
    service.changePassword({ currentPassword: 'old-password', newPassword: 'new-password' }).subscribe();
    const password = http.expectOne('/api/account/settings/password').request;
    expect(password.method).toBe('PATCH'); expect(password.body).toEqual({ currentPassword: 'old-password', newPassword: 'new-password' });
  });
});
