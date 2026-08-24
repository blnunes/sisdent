import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

const backendUrl = process.env['E2E_BACKEND_URL'] ?? 'http://localhost:8081';
const password = 'e2e-role-password';
type MembershipRole =
  | 'ORGANIZATION_ADMIN'
  | 'MANAGER'
  | 'READ_ONLY'
  | 'PRACTITIONER_MANAGER'
  | 'APPOINTMENT_MANAGER'
  | 'APPOINTMENT_READER'
  | 'CLINICAL_READER'
  | 'CLINICAL_AUTHOR'
  | 'CLINICAL_MANAGER';

const appointmentReaders: MembershipRole[] = [
  'ORGANIZATION_ADMIN',
  'MANAGER',
  'APPOINTMENT_MANAGER',
  'APPOINTMENT_READER',
  'READ_ONLY',
];

test.describe('Authorization role matrix', () => {
  test('allows and denies appointment reads according to every membership role', async ({ request }) => {
    const platformToken = await loginApi(request, 'platform.operations@sisdent.demo', 'odonto2026@O');
    const organizations = await apiJson(request, '/api/platform/organizations', platformToken);
    const organization = organizations.find((item: { name: string }) => item.name === 'Northstar Dental Group');
    expect(organization).toBeTruthy();
    if (!organization) throw new Error('Northstar organization was not seeded');
    const clinics = await apiJson(request, `/api/organizations/${organization.id}/clinic-units`, platformToken);
    const clinicUnitId = clinics[0].id;

    for (const role of appointmentReaders.concat(['PRACTITIONER_MANAGER', 'CLINICAL_READER', 'CLINICAL_AUTHOR', 'CLINICAL_MANAGER'])) {
      const email = `e2e-${role.toLowerCase()}-${Date.now()}@example.test`;
      const account = await request.post(`${backendUrl}/api/platform/accounts`, {
        headers: bearer(platformToken),
        data: { displayName: `E2E ${role}`, email, password },
      });
      expect(account.status(), `${role} account creation`).toBe(201);

      const membership = await request.post(`${backendUrl}/api/organizations/${organization.id}/account-memberships`, {
        headers: bearer(platformToken),
        data: { email, ...(['ORGANIZATION_ADMIN', 'PRACTITIONER_MANAGER'].includes(role) ? {} : { clinicUnitId }), role },
      });
      expect(membership.status(), `${role} membership creation`).toBe(201);

      const token = await loginApi(request, email, password);
      const response = await request.post(`${backendUrl}/graphql`, {
        headers: bearer(token),
        data: {
          query: `query { appointments(organizationId: "${organization.id}", clinicUnitId: "${clinicUnitId}", from: "2030-01-01T00:00:00Z", to: "2030-01-02T00:00:00Z") { totalElements } }`,
        },
      });
      expect(response.status(), `${role} appointment read transport`).toBe(200);
      const payload = await response.json();
      if (appointmentReaders.includes(role)) {
        expect(payload.errors, `${role} appointment read`).toBeUndefined();
      } else {
        expect(payload.errors?.[0]?.extensions?.code, `${role} appointment read`).toBe('AUTHORIZATION.DENIED');
      }
    }
  });

  test('hides the appointment module from a clinical reader and shows it to an appointment reader', async ({ page }) => {
    await loginThroughUi(page, 'northstar.viewer@sisdent.demo', 'odonto2026@O');
    await page.getByRole('button', { name: 'Open navigation menu' }).click();
    await expect(page.getByRole('link', { name: 'Appointments' })).toBeVisible();
    await page.locator('.mat-drawer-backdrop').click({ position: { x: 5, y: 5 } });
    await page.getByRole('button', { name: /logout|sign out/i }).click();

    // The clinical role is created by the API so this test does not depend on
    // optional demo seed profiles.
    const clinicalEmail = `e2e-clinical-reader-${Date.now()}@example.test`;
    const request = await page.context().request;
    const platformToken = await loginApi(request, 'platform.operations@sisdent.demo', 'odonto2026@O');
    const organizations = await apiJson(request, '/api/platform/organizations', platformToken);
    const organization = organizations.find((item: { name: string }) => item.name === 'Northstar Dental Group');
    if (!organization) throw new Error('Northstar organization was not seeded');
    const clinics = await apiJson(request, `/api/organizations/${organization.id}/clinic-units`, platformToken);
    await request.post(`${backendUrl}/api/platform/accounts`, { headers: bearer(platformToken), data: { displayName: 'E2E Clinical Reader', email: clinicalEmail, password } });
    await request.post(`${backendUrl}/api/organizations/${organization.id}/account-memberships`, { headers: bearer(platformToken), data: { email: clinicalEmail, clinicUnitId: clinics[0].id, role: 'CLINICAL_READER' } });

    await loginThroughUi(page, clinicalEmail, password);
    await page.getByRole('button', { name: 'Open navigation menu' }).click();
    await expect(page.getByRole('link', { name: 'Appointments' })).toHaveCount(0);
  });

  test('organization administration is limited to its persisted organization and rejects stale or revoked memberships', async ({ request }) => {
    const platformToken = await loginApi(request, 'platform.operations@sisdent.demo', 'odonto2026@O');
    const organizations = await apiJson(request, '/api/platform/organizations', platformToken);
    const managedOrganization = organizations.find((item: { name: string }) => item.name === 'Northstar Dental Group');
    const otherOrganization = organizations.find((item: { name: string }) => item.name === 'Southstart Dental Group');
    expect(managedOrganization).toBeTruthy();
    expect(otherOrganization).toBeTruthy();
    if (!managedOrganization || !otherOrganization) throw new Error('The E2E organizations were not seeded');

    const stamp = Date.now();
    const administratorEmail = `e2e-organization-admin-${stamp}@example.test`;
    const memberEmail = `e2e-membership-target-${stamp}@example.test`;
    await createAccount(request, platformToken, administratorEmail, 'E2E Organization Administrator');
    await createAccount(request, platformToken, memberEmail, 'E2E Membership Target');
    const administratorMembership = await request.post(
      `${backendUrl}/api/organizations/${managedOrganization.id}/account-memberships`,
      { headers: bearer(platformToken), data: { email: administratorEmail, role: 'ORGANIZATION_ADMIN' } },
    );
    expect(administratorMembership.status(), await administratorMembership.text()).toBe(201);

    const administratorToken = await loginApi(request, administratorEmail, password);
    const clinic = await request.post(`${backendUrl}/api/organizations/${managedOrganization.id}/clinic-units`, {
      headers: bearer(administratorToken), data: { name: `E2E Release Unit ${stamp}` },
    });
    expect(clinic.status(), await clinic.text()).toBe(201);

    const crossOrganizationClinic = await request.post(`${backendUrl}/api/organizations/${otherOrganization.id}/clinic-units`, {
      headers: bearer(administratorToken), data: { name: `E2E Forbidden Unit ${stamp}` },
    });
    expect(crossOrganizationClinic.status()).toBe(403);
    const crossOrganizationAccounts = await request.get(`${backendUrl}/api/organizations/${otherOrganization.id}/accounts`, {
      headers: bearer(administratorToken),
    });
    expect(crossOrganizationAccounts.status()).toBe(403);

    const granted = await request.post(`${backendUrl}/api/organizations/${managedOrganization.id}/account-memberships`, {
      headers: bearer(administratorToken), data: { email: memberEmail, role: 'APPOINTMENT_READER' },
    });
    expect(granted.status(), await granted.text()).toBe(201);
    const membership = await granted.json();
    const changed = await request.patch(`${backendUrl}/api/organizations/${managedOrganization.id}/memberships/${membership.id}`, {
      headers: bearer(administratorToken), data: { role: 'READ_ONLY', version: membership.version },
    });
    expect(changed.status(), await changed.text()).toBe(200);
    const changedMembership = await changed.json();
    const staleChange = await request.patch(`${backendUrl}/api/organizations/${managedOrganization.id}/memberships/${membership.id}`, {
      headers: bearer(administratorToken), data: { role: 'APPOINTMENT_READER', version: membership.version },
    });
    expect(staleChange.status()).toBe(409);

    const revoke = await request.post(`${backendUrl}/api/organizations/${managedOrganization.id}/memberships/${membership.id}/revoke`, {
      headers: bearer(administratorToken), data: { version: changedMembership.version },
    });
    expect(revoke.status(), await revoke.text()).toBe(204);
    const revokedMemberToken = await loginApi(request, memberEmail, password);
    const deniedAfterRevocation = await request.get(`${backendUrl}/api/organizations/${managedOrganization.id}/patients`, {
      headers: bearer(revokedMemberToken),
    });
    expect(deniedAfterRevocation.status()).toBe(403);
  });
});

async function createAccount(request: APIRequestContext, token: string, email: string, displayName: string): Promise<void> {
  const created = await request.post(`${backendUrl}/api/platform/accounts`, {
    headers: bearer(token), data: { displayName, email, password },
  });
  expect(created.status(), await created.text()).toBe(201);
}

async function loginApi(request: APIRequestContext, email: string, userPassword: string): Promise<string> {
  const response = await request.post(`${backendUrl}/api/auth/login`, { data: { email, password: userPassword } });
  expect(response.status(), `login for ${email}`).toBe(200);
  return (await response.json()).accessToken as string;
}

async function apiJson(request: APIRequestContext, path: string, token: string): Promise<any> {
  const response = await request.get(`${backendUrl}${path}`, { headers: bearer(token) });
  expect(response.ok(), path).toBeTruthy();
  return response.json();
}

function bearer(token: string): { Authorization: string } {
  return { Authorization: `Bearer ${token}` };
}

async function loginThroughUi(page: Page, email: string, userPassword: string): Promise<void> {
  await page.addInitScript(() => localStorage.setItem('sisdent.language', 'en'));
  await page.goto('/login');
  await page.getByLabel('Email address', { exact: true }).fill(email);
  await page.getByLabel('Password', { exact: true }).fill(userPassword);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL(/\/home$/);
}
