import { expect, test, type Page } from '@playwright/test';

const catalogues = [
  { path: 'specialities', title: 'Specialities', operation: 'query Specialities' },
  { path: 'addresses', title: 'Addresses', operation: 'query Addresses' },
  { path: 'administrative-divisions', title: 'Administrative divisions', operation: 'query AdministrativeDivisions' },
] as const;

test.describe('Catalogue feature boundaries', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  for (const catalogue of catalogues) {
    test(`${catalogue.title} loads its feature endpoint`, async ({ page }) => {
      const responsePromise = page.waitForResponse((response) =>
        new URL(response.url()).pathname === '/graphql'
        && response.request().method() === 'POST'
        && String(response.request().postData()).includes(catalogue.operation),
      );

      await page.goto(`/${catalogue.path}`);

      const response = await responsePromise;
      expect(response.ok()).toBe(true);
      await expect(page.getByRole('heading', { name: catalogue.title, exact: true })).toBeVisible();
      await expect(page.getByRole('table')).toBeVisible();
    });
  }
});

test('Countries loads through GraphQL', async ({ page }) => {
  await loginAsAdmin(page);

  const responsePromise = page.waitForResponse((response) =>
    new URL(response.url()).pathname === '/graphql'
    && response.request().method() === 'POST'
    && String(response.request().postData()).includes('query Countries'),
  );
  await page.goto('/countries');
  expect((await responsePromise).ok()).toBe(true);
  await expect(page.getByRole('heading', { name: 'Countries', exact: true })).toBeVisible();
});

async function loginAsAdmin(page: Page): Promise<void> {
  await page.addInitScript(() => localStorage.setItem('sisdent.language', 'en'));
  await page.goto('/login');
  await page.getByLabel('Email address', { exact: true }).fill('admin@sisdent.local');
  await page.getByLabel('Password', { exact: true }).fill('admin');
  const loginResponse = page.waitForResponse((response) =>
    new URL(response.url()).pathname === '/api/auth/login' && response.request().method() === 'POST',
  );
  const sessionResponse = page.waitForResponse((response) =>
    new URL(response.url()).pathname === '/api/session' && response.request().method() === 'GET',
  );
  await page.getByRole('button', { name: 'Sign in' }).click();
  expect((await loginResponse).ok()).toBe(true);
  expect((await sessionResponse).ok()).toBe(true);
  await expect(page).toHaveURL(/\/home$/);
}
