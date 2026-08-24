import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { defer, from, map, mergeMap } from 'rxjs';
import { GraphQlClientService } from './graphql-client.service';
import { CurrentAccountSettings } from './models';

@Injectable({ providedIn: 'root' })
export class AccountSettingsApiService {
  private readonly http = inject(HttpClient);
  private readonly graphql = inject(GraphQlClientService);

  current() { return this.graphql.query<{ currentAccountSettings: CurrentAccountSettings }>('query CurrentAccountSettings { currentAccountSettings { id displayName email preferredLanguage avatarUrl version } }', {}).pipe(map(({ currentAccountSettings }) => currentAccountSettings)); }
  updateProfile(request: { displayName: string; version: number }) {
    return this.graphql.query<{ updateOwnProfile: CurrentAccountSettings }>('mutation UpdateOwnProfile($input: UpdateOwnProfileInput!) { updateOwnProfile(input: $input) { id displayName email preferredLanguage avatarUrl version } }', { input: request }).pipe(map(({ updateOwnProfile }) => updateOwnProfile));
  }
  updatePreferredLanguage(request: { preferredLanguage: string }) {
    return this.graphql.query<{ updateOwnPreferredLanguage: CurrentAccountSettings }>('mutation UpdateOwnPreferredLanguage($input: UpdateOwnPreferredLanguageInput!) { updateOwnPreferredLanguage(input: $input) { id displayName email preferredLanguage avatarUrl version } }', { input: request }).pipe(map(({ updateOwnPreferredLanguage }) => updateOwnPreferredLanguage));
  }
  changePassword(request: { currentPassword: string; newPassword: string }) {
    return this.graphql.query<{ changeOwnPassword: boolean }>('mutation ChangeOwnPassword($input: ChangeOwnPasswordInput!) { changeOwnPassword(input: $input) }', { input: request }).pipe(map(() => undefined));
  }
  uploadAvatar(file: File) {
    return defer(() => from(file.arrayBuffer())).pipe(mergeMap((buffer) => this.graphql.query<{ uploadOwnAvatar: CurrentAccountSettings }>('mutation UploadOwnAvatar($input: AvatarUploadInput!) { uploadOwnAvatar(input: $input) { id displayName email preferredLanguage avatarUrl version } }', { input: { fileName: file.name, contentType: file.type, contentBase64: this.base64(buffer) } }).pipe(map(({ uploadOwnAvatar }) => uploadOwnAvatar))));
  }
  removeAvatar() { return this.graphql.query<{ removeOwnAvatar: boolean }>('mutation RemoveOwnAvatar { removeOwnAvatar }', {}).pipe(map(() => undefined)); }
  avatar() { return this.graphql.query<{ ownAvatar: { contentType: string; contentBase64: string } }>('query OwnAvatar { ownAvatar { contentType contentBase64 } }', {}).pipe(map(({ ownAvatar }) => this.base64Blob(ownAvatar.contentBase64, ownAvatar.contentType))); }

  private base64Blob(content: string, contentType: string): Blob {
    const bytes = Uint8Array.from(atob(content), (character) => character.charCodeAt(0));
    return new Blob([bytes], { type: contentType });
  }

  private base64(buffer: ArrayBuffer): string {
    return btoa(String.fromCharCode(...new Uint8Array(buffer)));
  }
}
