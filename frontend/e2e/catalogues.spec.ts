import { expect, test } from '@playwright/test';

const catalogues = [
  { path: 'specialities', title: 'Specialities', endpoint: '/api/specialities' },
  { path: 'addresses', title: 'Addresses', endpoint: '/api/addresses' },
  { path: 'administrative-divisions', title: 'Administrative divisions', endpoint: '/api/administrative-divisions' },
] as const;

test.describe('Catalogue feature boundaries', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => localStorage.setItem('sisdent.language', 'en'));
    await page.goto('/login');
    await page.getByLabel('Email address', { exact: true }).fill('admin@sisdent.local');
    await page.getByLabel('Password', { exact: true }).fill('admin');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page).toHaveURL(/\/home$/);
  });

  for (const catalogue of catalogues) {
    test(`${catalogue.title} loads its feature endpoint`, async ({ page }) => {
      const responsePromise = page.waitForResponse((response) =>
        new URL(response.url()).pathname === catalogue.endpoint
        && response.request().method() === 'GET',
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
  await page.addInitScript(() => localStorage.setItem('sisdent.language', 'en'));
  await page.goto('/login');
  await page.getByLabel('Email address', { exact: true }).fill('admin@sisdent.local');
  await page.getByLabel('Password', { exact: true }).fill('admin');
  await page.getByRole('button', { name: 'Sign in' }).click();

  const responsePromise = page.waitForResponse((response) =>
    new URL(response.url()).pathname === '/graphql'
    && response.request().method() === 'POST'
    && String(response.request().postData()).includes('query Countries'),
  );
  await page.goto('/countries');
  expect((await responsePromise).ok()).toBe(true);
  await expect(page.getByRole('heading', { name: 'Countries', exact: true })).toBeVisible();
});
