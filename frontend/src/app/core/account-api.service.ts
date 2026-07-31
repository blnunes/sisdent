import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { AccountSummary, ClinicUnit, OrganizationOption, PageResponse } from './models';
import { TableQuery, TableQueryService } from './table-query.service';

@Injectable({ providedIn: 'root' })
export class AccountApiService {
  private readonly http = inject(HttpClient);
  private readonly tables = inject(TableQueryService);
  listPlatform(query: TableQuery = { ...this.tables.defaultQuery, sort: 'person.displayName' }, filter = '') {
    return this.http.get<PageResponse<AccountSummary>>('/api/platform/accounts', { params: this.tables.toHttpParams(query).set('filter', filter) });
  }
  changeLifecycle(account: AccountSummary, active: boolean) {
    return this.http.patch<AccountSummary>(`/api/platform/accounts/${account.id}/lifecycle`, { active, version: account.version });
  }
  changePlatformAdministrator(account: AccountSummary, platformAdministrator: boolean) {
    return this.http.patch<AccountSummary>(`/api/platform/accounts/${account.id}/platform-administrator`, { platformAdministrator, version: account.version });
  }
  create(request: { displayName: string; email: string; password: string }) {
    return this.http.post<AccountSummary>('/api/platform/accounts', request);
  }
  listOrganization(organizationId: string, query: TableQuery = { ...this.tables.defaultQuery, sort: 'person.displayName' }) {
    return this.http.get<PageResponse<AccountSummary>>(`/api/organizations/${organizationId}/accounts`, { params: this.tables.toHttpParams(query) });
  }
  listPlatformOrganizations() {
    return this.http.get<OrganizationOption[]>('/api/platform/organizations');
  }
  listClinicUnits(organizationId: string) {
    return this.http.get<ClinicUnit[]>(`/api/organizations/${organizationId}/clinic-units`);
  }
  grantMembership(organizationId: string, request: { email: string; clinicUnitId?: string | null; role: string }) {
    return this.http.post(`/api/organizations/${organizationId}/account-memberships`, request);
  }
  changeMembershipRole(organizationId: string, membershipId: string, request: { role: string; version: number }) {
    return this.http.patch(`/api/organizations/${organizationId}/memberships/${membershipId}`, request);
  }
}
