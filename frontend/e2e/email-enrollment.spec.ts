import { expect, test } from '@playwright/test';

test('legacy user verifies email and permanently moves to email login', async ({ page }) => {
  const verifiedEmail = `e2e.phase3.${Date.now()}@example.com`;
  await page.addInitScript(() => localStorage.setItem('sisdent.language', 'en'));
  await page.goto('/login');
  await page.getByRole('button', { name: 'Use legacy identification' }).click();
  await selectPassport(page);
  await page.getByLabel('Identification', { exact: true }).fill('E2ELEGACY');
  await page.getByLabel('Password', { exact: true }).fill('e2e-password');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL(/\/email-enrollment$/);

  await page.getByLabel('Email address').fill(verifiedEmail);
  const enrollmentResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith('/api/account/email-enrollment') &&
      response.request().method() === 'POST',
  );
  await page.getByRole('button', { name: 'Send verification email' }).click();
  expect((await enrollmentResponse).status()).toBe(200);
  await expect(page.getByRole('heading', { name: 'Check your inbox' })).toBeVisible();

  const verificationToken = await page.evaluate(async () => {
    const accessToken = localStorage.getItem('sisdent.access-token');
    const response = await fetch('/api/test-support/email-verifications/latest', {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    if (!response.ok) throw new Error(`Test delivery lookup failed: ${response.status}`);
    return (await response.json()).token as string;
  });
  await page.goto(`/verify-email?token=${encodeURIComponent(verificationToken)}`);
  await expect(page.getByRole('heading', { name: 'Email verified' })).toBeVisible();
  await page.getByRole('link', { name: 'Sign in with email' }).click();

  await page.getByLabel('Email address').fill(verifiedEmail);
  await page.getByLabel('Password', { exact: true }).fill('e2e-password');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL(/\/home$/);

  await page.evaluate(() => {
    localStorage.removeItem('sisdent.access-token');
    localStorage.removeItem('sisdent.active-membership');
  });
  await page.goto('/login');
  await page.getByRole('button', { name: 'Use legacy identification' }).click();
  await selectPassport(page);
  await page.getByLabel('Identification', { exact: true }).fill('E2ELEGACY');
  await page.getByLabel('Password', { exact: true }).fill('e2e-password');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('alert')).toContainText('Invalid');
  await expect(page).toHaveURL(/\/login$/);
});

async function selectPassport(page: import('@playwright/test').Page): Promise<void> {
  await page
    .locator('select[formcontrolname="identificationType"]')
    .selectOption('PASSPORT');
}
