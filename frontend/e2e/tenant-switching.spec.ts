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
      response.request().method() === 'GET' &&
      response.url().includes('/patients') &&
      response.url().includes('page=0'),
    );
    await page.getByRole('combobox', { name: 'Active organization and clinic' }).click();
    await page.getByRole('option', { name: 'Southstart Dental Group', exact: true }).click();
    await patientRequest;

    await expect(page.getByRole('cell', { name: 'Scarlett Adams' })).toBeVisible();
    await expect(page.getByRole('cell', { name: 'Olivia Bennett' })).toHaveCount(0);
  });
});
