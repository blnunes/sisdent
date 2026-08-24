import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

const backendUrl = process.env['E2E_BACKEND_URL'] ?? 'http://localhost:8081';
const password = 'e2e-clinical-password';

test.describe('Clinical workspace', () => {
  test('a clinical manager drafts, finalizes, amends, and corrects an odontogram finding', async ({ page, request }) => {
    const adminToken = await loginApi(request, 'admin@sisdent.local', 'admin');
    const session = await apiJson(request, '/api/session', adminToken);
    const organizationId = session.memberships[0].organizationId;
    const clinics = await graphQl(request, adminToken, `query { clinicUnits(organizationId: "${organizationId}") { id } }`);
    const clinicUnitId = await clinicWithPatients(request, organizationId, clinics.clinicUnits, adminToken);
    const email = `e2e-clinical-manager-${Date.now()}@example.test`;
    await graphQl(request, adminToken, `mutation { createPlatformAccount(input: { displayName: "E2E Clinical Manager", email: "${email}", password: "${password}" }) { id } }`);
    await graphQl(request, adminToken, `mutation { grantMembership(organizationId: "${organizationId}", input: { email: "${email}", clinicUnitId: "${clinicUnitId}", role: CLINICAL_MANAGER }) { id } }`);

    const managerToken = await loginApi(request, email, password);
    const patients = await graphQl(request, managerToken, `query { patients(organizationId: "${organizationId}", clinicUnitId: "${clinicUnitId}", page: { page: 0, size: 1 }) { content { globalId name } } }`);
    expect(patients.patients.content.length).toBeGreaterThan(0);
    const patient = patients.patients.content[0];

    await loginThroughUi(page, email, password);
    await page.goto('/clinical');
    await expect(page.getByRole('heading', { name: 'Clinical workspace' })).toBeVisible();
    await page.getByRole('combobox', { name: 'Patient', exact: true }).click();
    await page.getByRole('option', { name: patient.name, exact: true }).click();

    await page.getByRole('textbox', { name: 'Clinical narrative', exact: true }).fill('Initial E2E clinical note');
    await page.getByRole('button', { name: 'Save draft', exact: true }).click();
    // The component reloads its clinical state after each mutation. The focused
    // backend integration test verifies the mutation payload contract; this E2E
    // test verifies the resulting workflow state instead of a discarded response body.
    await expect(page.getByText('Initial E2E clinical note', { exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Edit draft', exact: true }).click();
    await page.getByRole('textbox', { name: 'Clinical narrative', exact: true }).fill('Updated E2E clinical note');
    await page.getByRole('button', { name: 'Save draft', exact: true }).click();
    await page.getByRole('button', { name: 'Finalize', exact: true }).click();
    await expect(page.getByText('FINAL', { exact: true })).toBeVisible();

    await page.getByRole('button', { name: 'Create amendment', exact: true }).click();
    await page.getByRole('textbox', { name: 'Clinical narrative', exact: true }).fill('E2E traceable amendment');
    await page.getByRole('textbox', { name: 'Reason for amendment', exact: true }).fill('Correcting the finalized note');
    await page.getByRole('button', { name: 'Create final amendment', exact: true }).click();
    await expect(page.getByText('E2E traceable amendment', { exact: true })).toBeVisible();

    await page.getByRole('combobox', { name: 'FDI tooth', exact: true }).click();
    await page.getByRole('option', { name: '11', exact: true }).click();
    await page.getByRole('combobox', { name: 'Condition', exact: true }).click();
    await page.getByRole('option', { name: 'Caries', exact: true }).click();
    await page.getByRole('button', { name: 'Record finding', exact: true }).click();
    await expect(page.getByText('CARIES', { exact: true })).toBeVisible();
    await page.getByRole('textbox', { name: 'Reason for voiding', exact: true }).fill('E2E correction');
    await page.getByRole('button', { name: 'Void 11', exact: true }).click();
    await expect(page.getByText('11 · CARIES — Voided: E2E correction', { exact: true })).toBeVisible();
    await page.getByRole('combobox', { name: 'Condition', exact: true }).click();
    await page.getByRole('option', { name: 'Restoration', exact: true }).click();
    await page.getByRole('button', { name: 'Record finding', exact: true }).click();
    await expect(page.getByText('RESTORATION', { exact: true })).toBeVisible();

    const history = await graphQl(request, managerToken, `query { odontogramHistory(organizationId: "${organizationId}", clinicUnitId: "${clinicUnitId}", patientId: "${patient.globalId}") { content { voidReason } } }`);
    expect(history.odontogramHistory.content.some((finding: { voidReason?: string }) => finding.voidReason === 'E2E correction')).toBeTruthy();
  });
});

async function loginApi(request: APIRequestContext, email: string, userPassword: string): Promise<string> {
  const csrf = await request.get(`${backendUrl}/api/csrf`);
  expect(csrf.ok()).toBeTruthy();
  const { headerName, token } = await csrf.json();
  const response = await request.post(`${backendUrl}/api/auth/login`, {
    headers: { [headerName]: token },
    data: { email, password: userPassword },
  });
  expect(response.status(), `login for ${email}`).toBe(200);
  return (await response.json()).accessToken as string;
}

async function apiJson(request: APIRequestContext, path: string, token: string): Promise<any> {
  const response = await request.get(`${backendUrl}${path}`, { headers: bearer(token) });
  expect(response.ok(), path).toBeTruthy();
  return response.json();
}

async function graphQl(request: APIRequestContext, token: string, query: string): Promise<any> {
  const response = await request.post(`${backendUrl}/graphql`, { headers: bearer(token), data: { query } });
  expect(response.ok(), query).toBeTruthy();
  const payload = await response.json();
  expect(payload.errors, query).toBeUndefined();
  return payload.data;
}

async function clinicWithPatients(request: APIRequestContext, organizationId: string, clinics: any[], token: string): Promise<string> {
  for (const clinic of clinics) {
    const patients = await graphQl(request, token, `query { patients(organizationId: "${organizationId}", clinicUnitId: "${clinic.id}", page: { page: 0, size: 1 }) { content { globalId } } }`);
    if (patients.patients.content.length > 0) return clinic.id;
  }
  throw new Error(`No seeded patient is available in organization ${organizationId}`);
}

function bearer(token: string): { Authorization: string } { return { Authorization: `Bearer ${token}` }; }

async function loginThroughUi(page: Page, email: string, userPassword: string): Promise<void> {
  await page.addInitScript(() => localStorage.setItem('sisdent.language', 'en'));
  await page.goto('/login');
  await page.getByLabel('Email address', { exact: true }).fill(email);
  await page.getByLabel('Password', { exact: true }).fill(userPassword);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL(/\/home$/);
}
