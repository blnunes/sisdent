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
    const patientInput = {
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
        administrativeDivision: { name: 'Lisbon', code: '11', type: 'DISTRICT' }, countryCode: 'PT',
      },
      specialityIds: [],
    };
    const created = await graphQl(request, token, `mutation CreatePatient($organizationId: ID!, $input: PatientMutationInput!) {
      createPatient(organizationId: $organizationId, input: $input) { globalId }
    }`, { organizationId: firstScope.organizationId, input: patientInput });
    const patient = created.createPatient;

    const identity = {
      documentType: patientInput.identificationType,
      issuerCountryCode: patientInput.documentIssuerCountryCode,
      documentNumber: patientInput.identificationNumber,
      birthDate: patientInput.birthDate,
    };
    const exactBeforeLink = await graphQl(request, token, `mutation ExactPatientMatch($organizationId: ID!, $input: ExactPatientMatchInput!) {
      exactPatientMatch(organizationId: $organizationId, input: $input) { possibleMatchExists }
    }`, { organizationId: secondScope.organizationId, input: identity });
    expect(exactBeforeLink.exactPatientMatch.possibleMatchExists).toBe(false);

    await graphQl(request, token, `mutation LinkPatient($organizationId: ID!, $input: PatientLinkMutationInput!) {
      linkPatient(organizationId: $organizationId, input: $input) { id }
    }`, { organizationId: secondScope.organizationId, input: { ...identity, operationalBasis: 'ATTENDANCE' } });

    await loginThroughUi(page, groupAdmin.email, groupAdmin.password);
    await selectScope(page, firstScope.id);
    await page.goto('/patients');
    await expect(page.getByRole('cell', { name: patientInput.name })).toBeVisible();

    page.once('dialog', (dialog) => dialog.accept());
    const deactivation = page.waitForResponse((response) => response.url().endsWith('/graphql') && String(response.request().postData()).includes('deactivatePatient'));
    await page.getByRole('row', { name: new RegExp(patientInput.name) })
      .getByRole('button', { name: 'Deactivate patient' }).click();
    const response = await deactivation;
    expect(JSON.parse(response.request().postData() ?? '{}').variables).toMatchObject({
      organizationId: firstScope.organizationId,
      patientId: patient.globalId,
    });
    await expect(page.getByRole('cell', { name: patientInput.name })).toHaveCount(0);

    await selectScope(page, secondScope.id);
    await page.goto('/patients');
    await expect(page.getByRole('cell', { name: patientInput.name })).toBeVisible();
  });
});

async function loginApi(request: APIRequestContext, email: string, password: string): Promise<string> {
  const csrf = await request.get(`${backendUrl}/api/csrf`);
  expect(csrf.ok()).toBeTruthy();
  const { headerName, token } = await csrf.json();
  const response = await request.post(`${backendUrl}/api/auth/login`, {
    headers: { [headerName]: token },
    data: { email, password },
  });
  expect(response.status()).toBe(200);
  return (await response.json()).accessToken as string;
}

async function graphQl(request: APIRequestContext, token: string, query: string, variables: Record<string, unknown> = {}): Promise<any> {
  const response = await request.post(`${backendUrl}/graphql`, { headers: bearer(token), data: { query, variables } });
  expect(response.ok(), query).toBeTruthy();
  const payload = await response.json();
  expect(payload.errors, query).toBeUndefined();
  return payload.data;
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
