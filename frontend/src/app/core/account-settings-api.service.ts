import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { CurrentAccountSettings } from './models';

@Injectable({ providedIn: 'root' })
export class AccountSettingsApiService {
  private readonly http = inject(HttpClient);

  current() { return this.http.get<CurrentAccountSettings>('/api/account/settings'); }
  updateProfile(request: { displayName: string; version: number }) {
    return this.http.patch<CurrentAccountSettings>('/api/account/settings/profile', request);
  }
  updatePreferredLanguage(request: { preferredLanguage: string }) {
    return this.http.patch<CurrentAccountSettings>('/api/account/settings/preferred-language', request);
  }
  changePassword(request: { currentPassword: string; newPassword: string }) {
    return this.http.patch<void>('/api/account/settings/password', request);
  }
}
