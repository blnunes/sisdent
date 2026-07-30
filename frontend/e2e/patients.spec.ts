import { expect, test, type Page } from '@playwright/test';

const patient = {
  name: `E2E Patient ${Date.now()}`,
  updatedName: `E2E Patient ${Date.now()} Updated`,
  taxId: uniqueElevenDigits(),
  identificationNumber: `E2E-${Date.now()}`,
};

test.describe('Patient management', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => localStorage.setItem('sisdent.language', 'en'));
    await page.goto('/login');
    await page.getByLabel('Identification', { exact: true }).fill('ADMIN');
    await page.getByLabel('Password', { exact: true }).fill('admin');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page).toHaveURL(/\/home$/);
    await page.goto('/patients');
    await expect(page.getByRole('heading', { name: 'Patients', exact: true })).toBeVisible();
  });

  test('creates, updates and deletes a patient', async ({ page }) => {
    const filters = page.getByRole('region', { name: 'Patient filters' });
    await filters.getByLabel('Patient name').fill(patient.name);
    await filters.getByRole('button', { name: 'Filter', exact: true }).click();

    await page.getByRole('button', { name: 'New' }).click();
    await expect(page.getByRole('heading', { name: 'New record' })).toBeVisible();

    await page.getByLabel('Full name').fill(patient.name);
    await selectBirthDate(page);
    await selectOption(page, 'Status', 'Active');
    await selectOption(page, 'Gender', 'Female');
    await page.getByLabel('Tax ID').fill(patient.taxId);
    await selectOption(page, 'Identification type', 'National ID card');
    await page.getByLabel('Identification number').fill(patient.identificationNumber);
    await page.getByLabel('Document issuer country code').fill('PT');
    await page.getByLabel('Nationality country code').fill('PT');
    await page.getByLabel('Street').fill('E2E Test Street');
    await page.getByLabel('District').fill('Lisbon');
    await page.getByLabel('City').fill('Lisbon');
    await page.getByLabel('Postal code').fill('1000-001');
    await page.getByLabel('Administrative division name').fill('Lisbon');
    await page.getByLabel('Administrative division code').fill('11');
    await page.getByLabel('Administrative division type').fill('DISTRICT');
    await page.getByLabel('Address country code').fill('PT');

    const createResponse = page.waitForResponse(
      (response) =>
        response.url().includes('/api/patients') && response.request().method() === 'POST',
    );
    await page.getByRole('button', { name: 'Save changes' }).click();
    const create = await createResponse;
    expect(create.status(), `Request: ${create.request().postData()}`).toBe(201);
    await expect(page.getByRole('cell', { name: patient.name })).toBeVisible();

    const row = page.getByRole('row', { name: new RegExp(patient.name) });
    await row.getByRole('button', { name: 'Edit record' }).click();
    await expect(page.getByRole('heading', { name: 'Edit record' })).toBeVisible();
    await page.getByLabel('Full name').fill(patient.updatedName);

    const updateResponse = page.waitForResponse(
      (response) =>
        response.url().match(/\/api\/patients\/\d+$/) !== null &&
        response.request().method() === 'PUT' &&
        response.ok(),
    );
    await page.getByRole('button', { name: 'Save changes' }).click();
    await updateResponse;
    await expect(page.getByRole('cell', { name: patient.updatedName })).toBeVisible();

    page.once('dialog', (dialog) => dialog.accept());
    const deleteResponse = page.waitForResponse(
      (response) =>
        response.url().match(/\/api\/patients\/\d+$/) !== null &&
        response.request().method() === 'DELETE' &&
        response.status() === 204,
    );
    await page
      .getByRole('row', { name: new RegExp(patient.updatedName) })
      .getByRole('button', { name: 'Deactivate patient' })
      .click();
    await deleteResponse;
    await expect(page.getByRole('cell', { name: patient.updatedName })).toHaveCount(0);
  });
});

async function selectOption(page: Page, label: string, option: string): Promise<void> {
  await page.getByRole('dialog').getByRole('combobox', { name: label, exact: true }).click();
  await page.getByRole('option', { name: option, exact: true }).click();
}

async function selectBirthDate(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Open calendar' }).click();
  const calendar = page.locator('mat-calendar');
  await expect(calendar).toBeVisible();
  await calendar.locator('.mat-calendar-period-button').click();
  await calendar.getByText('2020', { exact: true }).click();
  await calendar.getByRole('button', { name: 'July 2020', exact: true }).click();
  await calendar.getByRole('button', { name: /July 15, 2020/ }).click();
}

function uniqueElevenDigits(): string {
  return String(Date.now()).slice(-11).padStart(11, '0');
}
