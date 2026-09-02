import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { AccountSummary, OrganizationOption, PageResponse } from './models';
import { GraphQlClientService } from './graphql-client.service';
import {
  OrganizationMutationGraphqlService,
  PractitionerWrite,
} from './organization-mutation-graphql.service';
import { OrganizationReadGraphqlService } from './organization-read-graphql.service';
import { TableQuery, TableQueryService } from './table-query.service';

@Injectable({ providedIn: 'root' })
export class AccountApiService {
  private readonly graphql = inject(GraphQlClientService);
  private readonly mutations = inject(OrganizationMutationGraphqlService);
  private readonly reads = inject(OrganizationReadGraphqlService);
  private readonly tables = inject(TableQueryService);
  listPlatform(
    query: TableQuery = { ...this.tables.defaultQuery, sort: 'person.displayName' },
    filter = '',
  ) {
    return this.accounts('platformAccounts', { page: this.graphqlPage(query), filter });
  }
  changeLifecycle(account: AccountSummary, active: boolean) {
    return this.accountMutation('changeAccountLifecycle', {
      accountId: account.id,
      input: { active, version: account.version },
    });
  }
  changePlatformAdministrator(account: AccountSummary, platformAdministrator: boolean) {
    return this.accountMutation('changeAccountPlatformAdministrator', {
      accountId: account.id,
      input: { platformAdministrator, version: account.version },
    });
  }
  create(request: { displayName: string; email: string; password: string }) {
    return this.accountMutation('createPlatformAccount', { input: request });
  }
  listOrganization(
    organizationId: string,
    query: TableQuery = { ...this.tables.defaultQuery, sort: 'person.displayName' },
  ) {
    return this.accounts('organizationAccounts', {
      organizationId,
      page: this.graphqlPage(query),
      filter: null,
    });
  }
  listPlatformOrganizations() {
    return this.graphql
      .query<{ platformOrganizations: OrganizationOption[] }>(
        'query PlatformOrganizations { platformOrganizations { id name active } }',
        {},
      )
      .pipe(map(({ platformOrganizations }) => platformOrganizations));
  }
  listClinicUnits(organizationId: string) {
    return this.reads.listClinicUnits(organizationId);
  }
  createClinicUnit(organizationId: string, request: { name: string }) {
    return this.mutations.createClinicUnit(organizationId, request);
  }
  listPractitioners(organizationId: string) {
    return this.reads.listPractitioners(organizationId);
  }
  createPractitioner(organizationId: string, request: PractitionerWrite) {
    return this.mutations.savePractitioner(organizationId, undefined, request);
  }
  updatePractitioner(organizationId: string, practitionerId: string, request: PractitionerWrite) {
    return this.mutations.savePractitioner(organizationId, practitionerId, request);
  }
  deactivatePractitioner(organizationId: string, practitionerId: string) {
    return this.mutations.deactivatePractitioner(organizationId, practitionerId);
  }
  grantMembership(
    organizationId: string,
    request: { email: string; clinicUnitId?: string | null; role: string },
  ) {
    return this.graphql
      .query<{ grantMembership: unknown }>(
        'mutation GrantMembership($organizationId: ID!, $input: AccountMembershipInput!) { grantMembership(organizationId: $organizationId, input: $input) { id } }',
        { organizationId, input: request },
      )
      .pipe(map(() => undefined));
  }
  changeMembershipRole(
    organizationId: string,
    membershipId: string,
    request: { role: string; version: number },
  ) {
    return this.graphql
      .query<{ changeMembershipRole: unknown }>(
        'mutation ChangeMembershipRole($organizationId: ID!, $membershipId: ID!, $input: MembershipRoleUpdateInput!) { changeMembershipRole(organizationId: $organizationId, membershipId: $membershipId, input: $input) { id } }',
        { organizationId, membershipId, input: request },
      )
      .pipe(map(() => undefined));
  }
  revokeMembership(organizationId: string, membershipId: string, version: number) {
    return this.graphql
      .query<{ revokeMembership: boolean }>(
        'mutation RevokeMembership($organizationId: ID!, $membershipId: ID!, $input: MembershipRevokeInput!) { revokeMembership(organizationId: $organizationId, membershipId: $membershipId, input: $input) }',
        { organizationId, membershipId, input: { version } },
      )
      .pipe(map(() => undefined));
  }

  private accounts(
    operation: 'platformAccounts' | 'organizationAccounts',
    variables: Record<string, unknown>,
  ): Observable<PageResponse<AccountSummary>> {
    const organizationArgument =
      operation === 'organizationAccounts' ? 'organizationId: $organizationId, ' : '';
    const declaration = operation === 'organizationAccounts' ? '$organizationId: ID!, ' : '';
    const query = `query ${operation}(${declaration}$page: AccountPageInput, $filter: String) { ${operation}(${organizationArgument}page: $page, filter: $filter) { content { id displayName email active platformAdministrator version memberships { id organizationId organizationName clinicUnitId clinicUnitName role version } } page size totalElements totalPages } }`;
    return this.graphql
      .query<Record<typeof operation, PageResponse<AccountSummary>>>(query, variables)
      .pipe(map((response) => response[operation]));
  }

  private accountMutation(
    operation:
      'changeAccountLifecycle' | 'changeAccountPlatformAdministrator' | 'createPlatformAccount',
    variables: Record<string, unknown>,
  ): Observable<AccountSummary> {
    const isCreate = operation === 'createPlatformAccount';
    const declaration = this.accountMutationDeclaration(operation, isCreate);
    const argumentsList = isCreate ? 'input: $input' : 'accountId: $accountId, input: $input';
    const query = `mutation ${operation}(${declaration}) { ${operation}(${argumentsList}) { id displayName email active platformAdministrator version memberships { id organizationId organizationName clinicUnitId clinicUnitName role version } } }`;
    return this.graphql
      .query<Record<typeof operation, AccountSummary>>(query, variables)
      .pipe(map((response) => response[operation]));
  }

  private accountMutationDeclaration(operation: string, isCreate: boolean): string {
    if (isCreate) {
      return '$input: AccountCreateInput!';
    }
    const inputType =
      operation === 'changeAccountLifecycle'
        ? 'AccountLifecycleInput!'
        : 'AccountPlatformAdministratorInput!';
    return `$accountId: ID!, $input: ${inputType}`;
  }

  private graphqlPage(query: TableQuery): Record<string, string | number> {
    return {
      page: query.page,
      size: query.size,
      sort: query.sort,
      direction: query.direction.toUpperCase(),
    };
  }
}

export type { PractitionerWrite } from './organization-mutation-graphql.service';
