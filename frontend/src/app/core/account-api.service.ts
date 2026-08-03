import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { AccountSummary, ClinicUnit, OrganizationOption, PageResponse, Practitioner } from './models';
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
  createClinicUnit(organizationId: string, request: { name: string }) {
    return this.http.post<ClinicUnit>(`/api/organizations/${organizationId}/clinic-units`, request);
  }
  listPractitioners(organizationId: string) {
    return this.http.get<Practitioner[]>(`/api/organizations/${organizationId}/practitioners`);
  }
  createPractitioner(organizationId: string, request: PractitionerWrite) {
    return this.http.post<Practitioner>(`/api/organizations/${organizationId}/practitioners`, request);
  }
  updatePractitioner(organizationId: string, practitionerId: string, request: PractitionerWrite) {
    return this.http.put<Practitioner>(`/api/organizations/${organizationId}/practitioners/${practitionerId}`, request);
  }
  deactivatePractitioner(organizationId: string, practitionerId: string) {
    return this.http.delete<void>(`/api/organizations/${organizationId}/practitioners/${practitionerId}`);
  }
  grantMembership(organizationId: string, request: { email: string; clinicUnitId?: string | null; role: string }) {
    return this.http.post(`/api/organizations/${organizationId}/account-memberships`, request);
  }
  changeMembershipRole(organizationId: string, membershipId: string, request: { role: string; version: number }) {
    return this.http.patch(`/api/organizations/${organizationId}/memberships/${membershipId}`, request);
  }
}

export interface PractitionerWrite {
  displayName: string;
  registrationNumber: string;
  specialityIds: number[];
  accountId?: string | null;
}
