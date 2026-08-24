import { expect, test, type APIRequestContext } from '@playwright/test';

const backendUrl = process.env['E2E_BACKEND_URL'] ?? 'http://localhost:8081';

test.describe('Operational scheduling', () => {
  test('schedules a patient-linked appointment and shows a private conflict', async ({ page, request }) => {
    await page.addInitScript(() => localStorage.setItem('sisdent.language', 'en'));
    await page.goto('/login');
    await page.getByLabel('Email address', { exact: true }).fill('northstar.scheduler@sisdent.demo');
    await page.getByLabel('Password', { exact: true }).fill('odonto2026@O');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page).toHaveURL(/\/home$/);
    const token = await page.evaluate(() => localStorage.getItem('sisdent.access-token'));
    const session = await page.evaluate(async () => (await fetch('/api/session', { headers: { Authorization: `Bearer ${localStorage.getItem('sisdent.access-token')}` } })).json());
    const membership = session.memberships[0];
    const organizationId = membership.organizationId;
    const clinicUnitId = membership.clinicUnitId;
    if (!clinicUnitId) throw new Error('The scheduler must have a clinic-unit membership');
    const patients = await graphQl(request, token, `query { patients(organizationId: "${organizationId}", clinicUnitId: "${clinicUnitId}", page: { page: 0, size: 1 }) { content { globalId } } }`);
    expect(patients.patients.content.length).toBeGreaterThan(0);

    const practitionersResponse = page.waitForResponse((response) =>
      new URL(response.url()).pathname === '/graphql'
      && response.request().method() === 'POST'
      && String(response.request().postData()).includes('query Practitioners'),
    );
    await page.goto('/appointments');
    const practitionersRequest = await practitionersResponse;
    await expect(page.getByRole('heading', { name: 'Appointments', exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Schedule appointment', exact: true }).first().click();
    expect(JSON.parse(practitionersRequest.request().postData() ?? '{}').variables).toMatchObject({
      organizationId,
      clinicUnitId,
    });
    await expect(page.getByRole('combobox', { name: 'Patient', exact: true })).toBeVisible();
    await expect(page.getByRole('combobox', { name: 'Practitioner', exact: true })).toBeVisible();
    await page.getByRole('combobox', { name: 'Patient', exact: true }).click();
    await page.getByRole('option', { name: 'Olivia Bennett', exact: true }).click();
    await page.getByRole('combobox', { name: 'Practitioner', exact: true }).click();
    // The option proves the scoped GraphQL read is available to this appointment manager.
    await page.getByRole('option', { name: 'Dr. Rowan Blake', exact: true }).click();
    const start = new Date(Date.now() + 86_400_000); start.setHours(10, 0, 0, 0);
    const dateLabel = start.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' });
    await page.getByRole('button', { name: 'Open calendar' }).click();
    await page.locator('mat-calendar').getByRole('button', { name: dateLabel }).click();
    await page.getByRole('combobox', { name: 'Start time', exact: true }).click();
    await page.getByRole('option', { name: '10:00', exact: true }).click();
    await page.getByRole('combobox', { name: 'End time', exact: true }).click();
    await page.getByRole('option', { name: '10:30', exact: true }).click();
    await page.locator('button.schedule-button[type="submit"]').click();
    await expect(page.locator('.appointment-row').filter({ hasText: 'Olivia Bennett' })).toBeVisible();
    await page.locator('button.schedule-button[type="submit"]').click();
    await expect(page.getByRole('alert')).toContainText('This appointment was changed or the selected time is no longer available. Refresh and try again.');
  });
});

async function graphQl(request: APIRequestContext, token: string, query: string): Promise<any> {
  const response = await request.post(`${backendUrl}/graphql`, { headers: { Authorization: `Bearer ${token}` }, data: { query } });
  expect(response.ok(), query).toBeTruthy();
  const payload = await response.json();
  expect(payload.errors, query).toBeUndefined();
  return payload.data;
}
