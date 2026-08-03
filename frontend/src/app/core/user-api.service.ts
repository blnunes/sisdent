import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { PageResponse, Permission, User, UserWrite } from './models';
import { TableQuery, TableQueryService } from './table-query.service';

@Injectable({ providedIn: 'root' })
export class UserApiService {
  private readonly http = inject(HttpClient);
  private readonly tableQuery = inject(TableQueryService);
  private readonly url = '/api/users';

  list(query: TableQuery = this.tableQuery.defaultQuery) {
    return this.http.get<PageResponse<User>>(this.url, { params: this.tableQuery.toHttpParams(query) });
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
