import { expect, test } from '@playwright/test';

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
    const unit = await request.post(`http://localhost:8080/api/organizations/${organizationId}/clinic-units`, { headers: { Authorization: `Bearer ${token}` }, data: { name: `E2E Scheduling Unit ${Date.now()}` } });
    expect(unit.status()).toBe(201);
    const clinicUnitId = (await unit.json()).globalId;

    await page.goto('/appointments');
    await expect(page.getByRole('heading', { name: 'Appointments' })).toBeVisible();
    await expect(page.getByRole('option', { name: 'Olivia Bennett' })).toBeVisible();
    await expect(page.getByRole('option', { name: 'Dr. Avery Morgan' })).toBeVisible();
    await page.getByLabel('Clinic unit UUID').fill(clinicUnitId);
    await page.getByLabel('Patient').selectOption({ label: 'Olivia Bennett' });
    await page.getByLabel('Practitioner').selectOption({ label: 'Dr. Avery Morgan' });
    const start = new Date(Date.now() + 86_400_000); start.setMinutes(0, 0, 0);
    const end = new Date(start.getTime() + 30 * 60_000);
    await page.getByLabel('Start (local)').fill(localDateTime(start));
    await page.getByLabel('End (local)').fill(localDateTime(end));
    await page.getByRole('button', { name: 'Schedule' }).click();
    await expect(page.getByText('Olivia Bennett with Dr. Avery Morgan')).toBeVisible();
    await page.getByRole('button', { name: 'Schedule' }).click();
    await expect(page.getByRole('alert')).toHaveText('The practitioner is unavailable for this interval.');
  });
});

function localDateTime(value: Date): string {
  const offset = value.getTimezoneOffset() * 60_000;
  return new Date(value.getTime() - offset).toISOString().slice(0, 16);
}
