import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AccountSettingsApiService } from './account-settings-api.service';

describe('AccountSettingsApiService', () => {
  let service: AccountSettingsApiService;
  let http: HttpTestingController;
  beforeEach(() => { TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] }); service = TestBed.inject(AccountSettingsApiService); http = TestBed.inject(HttpTestingController); });
  afterEach(() => http.verify());

  it('uses typed self-service GraphQL operations without an account identifier', () => {
    service.current().subscribe();
    const current = http.expectOne('/graphql');
    expect(current.request.body.query).toContain('query CurrentAccountSettings');
    current.flush({ data: { currentAccountSettings: {} } });
    service.updateProfile({ displayName: 'Ana', version: 2 }).subscribe();
    const profile = http.expectOne('/graphql');
    expect(profile.request.body.query).toContain('mutation UpdateOwnProfile'); expect(profile.request.body.variables).toEqual({ input: { displayName: 'Ana', version: 2 } });
    profile.flush({ data: { updateOwnProfile: {} } });
    service.changePassword({ currentPassword: 'old-password', newPassword: 'new-password' }).subscribe();
    const password = http.expectOne('/graphql');
    expect(password.request.body.query).toContain('mutation ChangeOwnPassword'); expect(password.request.body.variables).toEqual({ input: { currentPassword: 'old-password', newPassword: 'new-password' } });
    password.flush({ data: { changeOwnPassword: true } });
  });

  it('uploads the selected file through the typed GraphQL avatar mutation', async () => {
    const file = new File(['png'], 'avatar.png', { type: 'image/png' });
    service.uploadAvatar(file).subscribe();
    await Promise.resolve();

    const request = http.expectOne('/graphql').request;
    expect(request.method).toBe('POST');
    expect(request.body.query).toContain('mutation UploadOwnAvatar');
    expect(request.body.variables.input).toMatchObject({ fileName: 'avatar.png', contentType: 'image/png' });
  });
});
