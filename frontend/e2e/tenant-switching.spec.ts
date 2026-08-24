import { expect, test } from '@playwright/test';

test.describe('Tenant switching', () => {
  test('reloads the patient list for the selected organization', async ({ page }) => {
    await page.addInitScript(() => localStorage.setItem('sisdent.language', 'en'));
    await page.goto('/login');
    await page.getByLabel('Email address', { exact: true }).fill('group.admin@sisdent.demo');
    await page.getByLabel('Password', { exact: true }).fill('odonto2026@O');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page).toHaveURL(/\/home$/);

    await page.goto('/patients');
    await expect(page.getByRole('cell', { name: 'Olivia Bennett' })).toBeVisible();
    await expect(page.getByRole('cell', { name: 'Scarlett Adams' })).toHaveCount(0);

    const patientRequest = page.waitForResponse((response) =>
      response.request().method() === 'POST' &&
      new URL(response.url()).pathname === '/graphql' &&
      String(response.request().postData()).includes('query Patients'),
    );
    await page.locator('.membership-select').click();
    await page.getByRole('option', { name: 'Southstart Dental Group', exact: true }).click();
    const request = await patientRequest;
    expect(JSON.parse(request.request().postData() ?? '{}').variables.organizationId).toBeTruthy();

    await expect(page.getByRole('cell', { name: 'Scarlett Adams' })).toBeVisible();
    await expect(page.getByRole('cell', { name: 'Olivia Bennett' })).toHaveCount(0);
  });
});
