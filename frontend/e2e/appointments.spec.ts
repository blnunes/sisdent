import { expect, test, type APIRequestContext } from '@playwright/test';

const backendUrl = process.env['E2E_BACKEND_URL'] ?? 'http://localhost:8081';

test.describe('Operational scheduling', () => {
  test('schedules a patient-linked appointment and shows a private conflict', async ({ page, request }) => {
    await page.addInitScript(() => localStorage.setItem('sisdent.language', 'en'));
    await page.goto('/login');
    await page.getByLabel('Email address', { exact: true }).fill('admin@sisdent.local');
    await page.getByLabel('Password', { exact: true }).fill('admin');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page).toHaveURL(/\/home$/);
    const token = await page.evaluate(() => localStorage.getItem('sisdent.access-token'));
    const session = await page.evaluate(async () => (await fetch('/api/session', { headers: { Authorization: `Bearer ${localStorage.getItem('sisdent.access-token')}` } })).json());
    const organizationId = session.memberships[0].organizationId;
    const clinics = await request.get(`${backendUrl}/api/organizations/${organizationId}/clinic-units`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(clinics.status()).toBe(200);
    const clinicUnit = await clinicWithPatients(request, organizationId, await clinics.json(), token);

    await page.goto('/appointments');
    await expect(page.getByRole('heading', { name: 'Appointments', exact: true })).toBeVisible();
    await expect(page.getByRole('combobox', { name: 'Patient', exact: true })).toBeVisible();
    await expect(page.getByRole('combobox', { name: 'Practitioner', exact: true })).toBeVisible();
    await page.getByLabel('Clinic unit').fill(clinicUnit.name);
    await page.getByRole('option', { name: clinicUnit.name, exact: true }).click();
    await page.getByRole('combobox', { name: 'Patient', exact: true }).click();
    await page.getByRole('option', { name: 'Olivia Bennett', exact: true }).click();
    await page.getByRole('combobox', { name: 'Practitioner', exact: true }).click();
    await page.getByRole('option', { name: 'Dr. Avery Morgan', exact: true }).click();
    const start = new Date(Date.now() + 86_400_000); start.setHours(10, 0, 0, 0);
    const dateLabel = start.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' });
    await page.getByRole('button', { name: 'Open calendar' }).click();
    await page.locator('mat-calendar').getByRole('button', { name: dateLabel }).click();
    await page.getByRole('combobox', { name: 'Start time', exact: true }).click();
    await page.getByRole('option', { name: '10:00', exact: true }).click();
    await page.getByRole('combobox', { name: 'End time', exact: true }).click();
    await page.getByRole('option', { name: '10:30', exact: true }).click();
    await page.getByRole('button', { name: 'Schedule' }).click();
    await expect(page.locator('.appointment-row').filter({ hasText: 'Olivia Bennett' })).toBeVisible();
    await page.getByRole('button', { name: 'Schedule' }).click();
    await expect(page.getByRole('alert')).toHaveText('The practitioner is unavailable for this interval.');
  });
});

async function clinicWithPatients(
  request: APIRequestContext, organizationId: string, clinics: { id: string; name: string }[], token: string,
): Promise<{ id: string; name: string }> {
  for (const clinic of clinics) {
    const patients = await request.get(
      `${backendUrl}/api/organizations/${organizationId}/patients?clinicUnitId=${clinic.id}&size=1`,
      { headers: { Authorization: `Bearer ${token}` } },
    );
    if (patients.ok() && (await patients.json()).content.length > 0) return clinic;
  }
  throw new Error(`No seeded patient is available in organization ${organizationId}`);
}
