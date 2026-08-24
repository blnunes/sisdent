import { expect, test, type APIRequestContext } from '@playwright/test';

const backendUrl = process.env['E2E_BACKEND_URL'] ?? 'http://localhost:8081';
const password = 'e2e-role-password';

test('enforces appointment read roles and administration lifecycle through GraphQL', async ({ request }) => {
  const platform = await login(request, 'platform.operations@sisdent.demo', 'odonto2026@O');
  const organizations = await gql(request, platform, 'query { platformOrganizations { id name } }');
  const organization = organizations.platformOrganizations.find((item: { name: string }) => item.name === 'Northstar Dental Group');
  if (!organization) throw new Error('Northstar organization was not seeded');
  const clinics = await gql(request, platform, `query { clinicUnits(organizationId: "${organization.id}") { id } }`);
  const clinicId = clinics.clinicUnits[0].id;
  for (const [role, allowed] of [['APPOINTMENT_READER', true], ['CLINICAL_READER', false]] as const) {
    const email = `e2e-${role.toLowerCase()}-${Date.now()}@example.test`;
    await gql(request, platform, `mutation { createPlatformAccount(input: { displayName: "${role}", email: "${email}", password: "${password}" }) { id } }`);
    await gql(request, platform, `mutation { grantMembership(organizationId: "${organization.id}", input: { email: "${email}", clinicUnitId: "${clinicId}", role: ${role} }) { id } }`);
    const payload = await gqlResponse(request, await login(request, email, password), `query { appointments(organizationId: "${organization.id}", clinicUnitId: "${clinicId}", from: "2030-01-01T00:00:00Z") { totalElements } }`);
    expect(payload.errors?.[0]?.extensions?.code).toBe(allowed ? undefined : 'AUTHORIZATION.DENIED');
  }
  const memberEmail = `e2e-member-${Date.now()}@example.test`;
  await gql(request, platform, `mutation { createPlatformAccount(input: { displayName: "Member", email: "${memberEmail}", password: "${password}" }) { id } }`);
  const membership = await gql(request, platform, `mutation { grantMembership(organizationId: "${organization.id}", input: { email: "${memberEmail}", role: APPOINTMENT_READER }) { id version } }`);
  await gql(request, platform, `mutation { changeMembershipRole(organizationId: "${organization.id}", membershipId: "${membership.grantMembership.id}", input: { role: READ_ONLY, version: ${membership.grantMembership.version} }) { id } }`);
  const stale = await gqlResponse(request, platform, `mutation { changeMembershipRole(organizationId: "${organization.id}", membershipId: "${membership.grantMembership.id}", input: { role: APPOINTMENT_READER, version: ${membership.grantMembership.version} }) { id } }`);
  expect(stale.errors?.[0]?.extensions?.code).toBe('CONFLICT');
});

async function login(request: APIRequestContext, email: string, userPassword: string): Promise<string> {
  const csrf = await request.get(`${backendUrl}/api/csrf`);
  expect(csrf.ok()).toBeTruthy();
  const { headerName, token } = await csrf.json();
  const response = await request.post(`${backendUrl}/api/auth/login`, {
    headers: { [headerName]: token },
    data: { email, password: userPassword },
  });
  expect(response.ok()).toBeTruthy();
  return (await response.json()).accessToken as string;
}
async function gql(request: APIRequestContext, token: string, query: string): Promise<any> { const payload = await gqlResponse(request, token, query); expect(payload.errors, query).toBeUndefined(); return payload.data; }
async function gqlResponse(request: APIRequestContext, token: string, query: string): Promise<any> { const response = await request.post(`${backendUrl}/graphql`, { headers: { Authorization: `Bearer ${token}` }, data: { query } }); expect(response.ok(), query).toBeTruthy(); return response.json(); }
