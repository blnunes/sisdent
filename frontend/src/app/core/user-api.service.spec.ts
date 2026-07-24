import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { UserApiService } from './user-api.service';

describe('UserApiService', () => {
  let service: UserApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UserApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses the user CRUD and permission endpoints', () => {
    const user = {
      identificationType: 'NATIONAL_ID' as const,
      identificationNumber: 'TEST',
      password: 'password',
      role: 'USER' as const,
    };

    service.list().subscribe();
    expect(http.expectOne('/api/users').request.method).toBe('GET');

    service.create(user).subscribe();
    expect(http.expectOne('/api/users').request.method).toBe('POST');

    service.update(7, user).subscribe();
    expect(http.expectOne('/api/users/7').request.method).toBe('PUT');

    service.updatePermissions(7, ['READ_PATIENTS']).subscribe();
    const permissions = http.expectOne('/api/users/7/permissions');
    expect(permissions.request.method).toBe('PUT');
    expect(permissions.request.body).toEqual({ permissions: ['READ_PATIENTS'] });

    service.delete(7).subscribe();
    expect(http.expectOne('/api/users/7').request.method).toBe('DELETE');
  });
});
