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

  it('uploads the selected file as a PUT multipart field named file without a manual content type', () => {
    const file = new File(['png'], 'avatar.png', { type: 'image/png' });
    service.uploadAvatar(file).subscribe();

    const request = http.expectOne('/api/account/settings/avatar').request;
    expect(request.method).toBe('PUT');
    expect(request.body).toBeInstanceOf(FormData);
    expect((request.body as FormData).get('file')).toBe(file);
    expect(request.headers.has('Content-Type')).toBe(false);
  });
});
