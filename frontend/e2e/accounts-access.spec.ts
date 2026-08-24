import { expect, test } from '@playwright/test';

test.describe('Accounts and Access', () => {
  test('lists global accounts for a platform administrator without memberships', async ({ page }) => {
    await page.addInitScript(() => localStorage.setItem('sisdent.language', 'en'));
    await page.goto('/login');
    await page.getByLabel('Email address', { exact: true }).fill('platform.operations@sisdent.demo');
    await page.getByLabel('Password', { exact: true }).fill('odonto2026@O');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page).toHaveURL(/\/home$/);

    const session = await page.evaluate(async () => {
      const token = localStorage.getItem('sisdent.access-token');
      return (await fetch('/api/session', { headers: { Authorization: `Bearer ${token}` } })).json();
    });
    expect(session.platformAdministrator).toBe(true);
    expect(session.memberships).toEqual([]);

    const accountsResponse = page.waitForResponse((response) =>
      response.request().method() === 'POST'
      && new URL(response.url()).pathname === '/graphql'
      && String(response.request().postData()).includes('platformAccounts'),
    );
    await page.goto('/accounts');
    await expect((await accountsResponse).ok()).toBeTruthy();

    await expect(page.getByRole('checkbox', { name: 'View all accounts' })).not.toBeChecked();
    await page.getByRole('button', { name: 'Next page' }).click();
    await expect(page.getByText('admin@sisdent.local', { exact: true })).toBeVisible();
  });
});
