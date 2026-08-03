import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

const backendUrl = process.env['E2E_BACKEND_URL'] ?? 'http://localhost:8081';
const groupAdmin = { email: 'group.admin@sisdent.demo', password: 'odonto2026@O' };

test.describe('Patient tenant isolation', () => {
  test('does not disclose another tenant, and deactivates only the selected tenant link', async ({ page, request }) => {
    const token = await loginApi(request, groupAdmin.email, groupAdmin.password);
    const session = await apiJson(request, '/api/session', token);
    const [firstScope, secondScope] = distinctOrganizationMemberships(session.memberships);
    expect(firstScope).toBeTruthy();
    expect(secondScope).toBeTruthy();
    if (!firstScope || !secondScope) throw new Error('The E2E administrator needs memberships in two organizations');

    const stamp = Date.now();
    const patientRequest = {
      name: `E2E Tenant Patient ${stamp}`,
      birthDate: '2000-07-15',
      active: true,
      gender: 'FEMALE',
      taxId: String(stamp).slice(-11).padStart(11, '0'),
      identificationType: 'NATIONAL_ID_CARD',
      identificationNumber: `TENANT-${stamp}`,
      documentIssuerCountryCode: 'PT',
      nationalityCode: 'PT',
      address: {
        street: 'E2E Tenant Street', district: 'Lisbon', city: 'Lisbon', postalCode: '1000-001',
        administrativeDivisionName: 'Lisbon', administrativeDivisionCode: '11',
        administrativeDivisionType: 'DISTRICT', countryCode: 'PT',
      },
      specialityIds: [],
    };
    const created = await request.post(`${backendUrl}/api/organizations/${firstScope.organizationId}/patients`, {
      headers: bearer(token), data: patientRequest,
    });
    expect(created.status(), await created.text()).toBe(201);
    const patient = await created.json();

    const exactBeforeLink = await request.post(
      `${backendUrl}/api/organizations/${secondScope.organizationId}/patient-intake/exact-match`,
      {
        headers: bearer(token),
        data: {
          documentType: patientRequest.identificationType,
          issuerCountryCode: patientRequest.documentIssuerCountryCode,
          documentNumber: patientRequest.identificationNumber,
          birthDate: patientRequest.birthDate,
        },
      },
    );
    expect(exactBeforeLink.status()).toBe(200);
    expect((await exactBeforeLink.json()).possibleMatchExists).toBe(false);

    const linked = await request.post(`${backendUrl}/api/organizations/${secondScope.organizationId}/patient-links`, {
      headers: bearer(token),
      data: {
        documentType: patientRequest.identificationType,
        issuerCountryCode: patientRequest.documentIssuerCountryCode,
        documentNumber: patientRequest.identificationNumber,
        birthDate: patientRequest.birthDate,
        operationalBasis: 'ATTENDANCE',
      },
    });
    expect(linked.status(), await linked.text()).toBe(201);

    await loginThroughUi(page, groupAdmin.email, groupAdmin.password);
    await selectScope(page, firstScope.id);
    await page.goto('/patients');
    await expect(page.getByRole('cell', { name: patientRequest.name })).toBeVisible();

    page.once('dialog', (dialog) => dialog.accept());
    const deactivation = page.waitForResponse((response) =>
      response.request().method() === 'DELETE'
      && response.url().includes(`/api/organizations/${firstScope.organizationId}/patients/${patient.globalId}`)
      && response.status() === 204,
    );
    await page.getByRole('row', { name: new RegExp(patientRequest.name) })
      .getByRole('button', { name: 'Deactivate patient' }).click();
    await deactivation;
    await expect(page.getByRole('cell', { name: patientRequest.name })).toHaveCount(0);

    await selectScope(page, secondScope.id);
    await page.goto('/patients');
    await expect(page.getByRole('cell', { name: patientRequest.name })).toBeVisible();
  });
});

async function loginApi(request: APIRequestContext, email: string, password: string): Promise<string> {
  const response = await request.post(`${backendUrl}/api/auth/login`, { data: { email, password } });
  expect(response.status()).toBe(200);
  return (await response.json()).accessToken as string;
}

async function apiJson(request: APIRequestContext, path: string, token: string): Promise<any> {
  const response = await request.get(`${backendUrl}${path}`, { headers: bearer(token) });
  expect(response.ok(), path).toBeTruthy();
  return response.json();
}

function bearer(token: string): { Authorization: string } { return { Authorization: `Bearer ${token}` }; }

function distinctOrganizationMemberships(memberships: any[]): [any | undefined, any | undefined] {
  const scopes = memberships.filter((membership, index) =>
    memberships.findIndex((candidate) => candidate.organizationId === membership.organizationId) === index,
  );
  return [scopes[0], scopes[1]];
}

async function loginThroughUi(page: Page, email: string, password: string): Promise<void> {
  await page.addInitScript(() => localStorage.setItem('sisdent.language', 'en'));
  await page.goto('/login');
  await page.getByLabel('Email address', { exact: true }).fill(email);
  await page.getByLabel('Password', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL(/\/home$/);
}

async function selectScope(page: Page, membershipId: string): Promise<void> {
  await page.evaluate((id) => localStorage.setItem('sisdent.active-membership', id), membershipId);
  await page.reload();
}
