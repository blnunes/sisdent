import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Permission, User, UserWrite } from './models';

@Injectable({ providedIn: 'root' })
export class UserApiService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/users';

  list() {
    return this.http.get<User[]>(this.url);
  }

  create(user: UserWrite) {
    return this.http.post<User>(this.url, user);
  }

  update(id: number, user: UserWrite) {
    return this.http.put<User>(`${this.url}/${id}`, user);
  }

  delete(id: number) {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  updatePermissions(id: number, permissions: Permission[]) {
    return this.http.put<User>(`${this.url}/${id}/permissions`, { permissions });
  }
}
