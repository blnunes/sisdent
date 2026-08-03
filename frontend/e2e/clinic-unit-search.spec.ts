import { expect, test } from '@playwright/test';

test.describe('Clinic unit search', () => {
  test('returns only the active tenant clinic units by name', async ({ page }) => {
    await page.addInitScript(() => localStorage.setItem('sisdent.language', 'en'));
    await page.goto('/login');
    await page.getByLabel('Email address', { exact: true }).fill('group.admin@sisdent.demo');
    await page.getByLabel('Password', { exact: true }).fill('odonto2026@O');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await page.goto('/appointments');

    const clinicInput = page.getByLabel('Clinic unit', { exact: true });
    await clinicInput.fill('North');
    await expect(page.getByRole('option', { name: /Northstar Central Clinic/ })).toBeVisible();
    await expect(page.getByRole('option', { name: /Southstart Downtown Clinic/ })).toHaveCount(0);

    await page.getByRole('combobox', { name: 'Active organization and clinic' }).click();
    await page.getByRole('option', { name: 'Southstart Dental Group', exact: true }).click();
    await clinicInput.fill('South');
    await expect(page.getByRole('option', { name: /Southstart Downtown Clinic/ })).toBeVisible();
    await expect(page.getByRole('option', { name: /Northstar Central Clinic/ })).toHaveCount(0);
  });
});
