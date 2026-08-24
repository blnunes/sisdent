import { expect, test } from '@playwright/test';

test.describe('Clinic unit search', () => {
  test('returns only the active tenant clinic units by name', async ({ page }) => {
    await page.addInitScript(() => localStorage.setItem('sisdent.language', 'en'));
    await page.goto('/login');
    await page.getByLabel('Email address', { exact: true }).fill('group.admin@sisdent.demo');
    await page.getByLabel('Password', { exact: true }).fill('odonto2026@O');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page).toHaveURL(/\/home$/);
    const southstartOrganizationId = await page.evaluate(async () => {
      const token = localStorage.getItem('sisdent.access-token');
      const session = await (await fetch('/api/session', { headers: { Authorization: `Bearer ${token}` } })).json();
      return session.memberships.find((membership: { organizationName: string }) =>
        membership.organizationName === 'Southstart Dental Group',
      )?.organizationId;
    });
    expect(southstartOrganizationId).toBeTruthy();
    await page.goto('/appointments');
    await page.getByRole('button', { name: 'Schedule appointment', exact: true }).first().click();

    const clinicInput = page.getByLabel('Clinic unit', { exact: true });
    await clinicInput.fill('North');
    await expect(page.getByRole('option', { name: /Northstar Central Clinic/ })).toBeVisible();
    await expect(page.getByRole('option', { name: /Southstart Downtown Clinic/ })).toHaveCount(0);

    await page.locator('.membership-select').click();
    await page.getByRole('option', { name: 'Southstart Dental Group', exact: true }).click();
    await expect(page.locator('.membership-select')).toHaveText('Southstart Dental Group');
    await clinicInput.fill('South');
    await expect(page.getByRole('option', { name: /Southstart Downtown Clinic/ })).toBeVisible();
    await expect(page.getByRole('option', { name: /Northstar Central Clinic/ })).toHaveCount(0);
    const patientRequest = page.waitForResponse((response) =>
      response.request().method() === 'POST'
      && new URL(response.url()).pathname === '/graphql'
      && String(response.request().postData()).includes('query Patients'),
    );
    await page.getByRole('option', { name: /Southstart Downtown Clinic/ }).click();
    const variables = JSON.parse((await patientRequest).request().postData() ?? '{}').variables;
    expect(variables).toMatchObject({
      organizationId: southstartOrganizationId,
      clinicUnitId: expect.any(String),
    });
  });
});
