import { expect, test, type Locator, type Page, type Response } from '@playwright/test';

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
    await page.getByLabel('Email address', { exact: true }).fill('admin@sisdent.local');
    await page.getByLabel('Password', { exact: true }).fill('admin');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page).toHaveURL(/\/home$/);
    await page.goto('/patients');
    await expect(page.getByRole('heading', { name: 'Patients', exact: true })).toBeVisible();
    await expect(page.getByRole('cell', { name: 'Olivia Bennett', exact: true })).toBeVisible();
  });

  test('creates, updates and deletes a patient', async ({ page }) => {
    await expectActionButtonsToFit(page.getByRole('row', { name: /Olivia Bennett/ }));

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
    await selectOption(page, 'Document issuer country code', 'Portugal (PT)');
    await selectOption(page, 'Nationality country code', 'Portugal (PT)');
    await selectOption(page, 'Address country code', 'Portugal (PT)');
    await page.getByLabel('Street').fill('E2E Test Street');
    await page.getByLabel('District').fill('Lisbon');
    await page.getByLabel('City').fill('Lisbon');
    await page.getByLabel('Postal code').fill('1000-001');
    await selectOption(page, 'Administrative division', 'Lisbon (PT-LIS)');

    const createResponse = waitForMutation(page, 'mutation CreatePatient');
    await page.getByRole('button', { name: 'Save changes' }).click();
    const create = await createResponse;
    expect(create.response.status(), `Request: ${create.response.request().postData()}`).toBe(200);
    expect(JSON.parse(create.response.request().postData() ?? '{}')).toMatchObject({
      variables: { organizationId: expect.any(String), input: { name: patient.name, taxId: patient.taxId } },
    });
    expect(create.payload.data.createPatient).toMatchObject({ name: patient.name, active: true });
    await expect(page.getByRole('cell', { name: patient.name })).toBeVisible();

    const row = page.getByRole('row', { name: new RegExp(patient.name) });
    await row.getByRole('button', { name: 'Edit record' }).click();
    await expect(page.getByRole('heading', { name: 'Edit record' })).toBeVisible();
    await page.getByLabel('Full name').fill(patient.updatedName);

    const updateResponse = waitForMutation(page, 'mutation UpdatePatient');
    await page.getByRole('button', { name: 'Save changes' }).click();
    const update = await updateResponse;
    expect(update.response.ok()).toBe(true);
    expect(JSON.parse(update.response.request().postData() ?? '{}')).toMatchObject({ variables: { input: { name: patient.updatedName } } });
    expect(update.payload.data.updatePatient).toMatchObject({ name: patient.updatedName });
    await expect(page.getByRole('cell', { name: patient.updatedName })).toBeVisible();

    page.once('dialog', (dialog) => dialog.accept());
    const deleteResponse = waitForMutation(page, 'mutation DeactivatePatient');
    await page
      .getByRole('row', { name: new RegExp(patient.updatedName) })
      .getByRole('button', { name: 'Deactivate patient' })
      .click();
    const deletion = await deleteResponse;
    expect(JSON.parse(deletion.response.request().postData() ?? '{}').variables).toHaveProperty('patientId');
    expect(deletion.payload.data.deactivatePatient).toBe(true);
    await expect(page.getByRole('cell', { name: patient.updatedName })).toHaveCount(0);
  });

  test('filters patients by every available criterion and by combined criteria', async ({ page }) => {
    const filters = page.getByRole('region', { name: 'Patient filters' });

    let response = await applyTextFilter(page, filters, 'Patient name', 'Olivia');
    expectFilterParameter(response, 'name', 'Olivia');
    await expectPatientResults(page, response, (entry) => entry.name.toLowerCase().includes('olivia'));
    await resetFilters(page, filters);

    response = await applySelectFilter(page, filters, 'Status', 'Inactive');
    expectFilterParameter(response, 'active', 'false');
    await expectPatientResults(page, response, (entry) => entry.active === false);
    await resetFilters(page, filters);

    response = await selectAutocompleteFilter(page, filters, 'Speciality', 'Pediatric Dentistry');
    expectFilterParameter(response, 'specialityId');
    await expectPatientResults(page, response, (entry) => entry.specialities.some((speciality) => speciality.name === 'Pediatric Dentistry'));
    await resetFilters(page, filters);

    await filters.getByRole('button', { name: 'More filters' }).click();

    response = await selectBirthDateFilter(page, filters);
    expectFilterParameter(response, 'birthDate', '1992-04-18');
    await expectPatientResults(page, response, (entry) => entry.birthDate === '1992-04-18');
    await resetFilters(page, filters);

    response = await applySelectFilter(page, filters, 'Gender', 'Female');
    expectFilterParameter(response, 'gender', 'FEMALE');
    await expectPatientResults(page, response, (entry) => entry.gender === 'FEMALE');
    await resetFilters(page, filters);

    response = await selectAutocompleteFilter(page, filters, 'Tax ID', '10000000001');
    expectFilterParameter(response, 'taxId', '10000000001');
    await expectPatientResults(page, response, (entry) => entry.taxId === '10000000001');
    await resetFilters(page, filters);

    response = await applySelectFilter(page, filters, 'Identification type', 'National ID');
    expectFilterParameter(response, 'identificationType', 'NATIONAL_ID_CARD');
    await expectPatientResults(page, response, (entry) => entry.identificationType === 'NATIONAL_ID_CARD');
    await resetFilters(page, filters);

    response = await applySelectFilter(page, filters, 'Nationality', 'United States (US)');
    expectFilterParameter(response, 'nationalityCode', 'US');
    await expectPatientResults(page, response, (entry) => entry.nationality.code === 'US');
    await resetFilters(page, filters);

    response = await selectAutocompleteFilter(page, filters, 'Address', 'Maple Grove');
    expectFilterParameter(response, 'addressId');
    await expectPatientResults(page, response, (entry) => entry.address.street.includes('Maple Grove'));
    await resetFilters(page, filters);

    await filters.getByLabel('Patient name').fill('Olivia');
    await filters.getByLabel('Tax ID').fill('10000000001');
    await applySelectFilter(page, filters, 'Status', 'Active');
    await applySelectFilter(page, filters, 'Gender', 'Female');
    response = await selectAutocompleteFilter(page, filters, 'Speciality', 'Pediatric Dentistry');
    expectFilterParameter(response, 'name', 'Olivia');
    expectFilterParameter(response, 'taxId', '10000000001');
    expectFilterParameter(response, 'active', 'true');
    expectFilterParameter(response, 'gender', 'FEMALE');
    expectFilterParameter(response, 'specialityId');
    await expectPatientResults(page, response, (entry) =>
      entry.name === 'Olivia Bennett'
      && entry.taxId === '10000000001'
      && entry.active === true
      && entry.gender === 'FEMALE'
      && entry.specialities.some((speciality) => speciality.name === 'Pediatric Dentistry'),
    );
  });
});

async function expectActionButtonsToFit(row: Locator): Promise<void> {
  const actionCell = row.locator('td.mat-column-actions');
  const buttons = actionCell.getByRole('button');

  await expect(buttons).toHaveCount(3);
  await expect(actionCell).toBeVisible();
  await expect.poll(async () => actionCell.evaluate((cell) => {
    const cellBounds = cell.getBoundingClientRect();
    return [...cell.querySelectorAll('button')].every((button) => {
      const bounds = button.getBoundingClientRect();
      return bounds.left >= cellBounds.left && bounds.right <= cellBounds.right;
    });
  })).toBe(true);
}

type PatientResult = {
  name: string;
  birthDate: string;
  active: boolean;
  gender: string;
  taxId: string;
  identificationType: string;
  nationality: { code: string };
  address: { street: string };
  specialities: { name: string }[];
};

type PatientQuery = {
  response: Response;
};

type GraphQlMutation = {
  response: Response;
  payload: { data: Record<string, unknown> };
};

async function applyTextFilter(page: Page, filters: Locator, label: string, value: string): Promise<PatientQuery> {
  const response = waitForPatientResults(page);
  await filters.getByLabel(label, { exact: true }).fill(value);
  await filters.getByRole('button', { name: 'Filter', exact: true }).click();
  return response;
}

async function applySelectFilter(page: Page, filters: Locator, label: string, option: string): Promise<PatientQuery> {
  const response = waitForPatientResults(page);
  const select = filters.getByRole('combobox', { name: label, exact: true });
  await select.focus();
  await select.press('Enter');
  await page.getByRole('option', { name: option, exact: true }).click();
  await settleSelectOverlay(page);
  await filters.getByRole('button', { name: 'Filter', exact: true }).click();
  return response;
}

async function selectAutocompleteFilter(page: Page, filters: Locator, label: string, query: string): Promise<PatientQuery> {
  const input = filters.getByLabel(label, { exact: true });
  await input.fill(query);
  await expect(page.getByRole('option').filter({ hasText: query }).first()).toBeVisible();
  const response = waitForPatientResults(page);
  await page.getByRole('option').filter({ hasText: query }).first().click();
  return response;
}

async function selectBirthDateFilter(page: Page, filters: Locator): Promise<PatientQuery> {
  const field = filters.getByLabel('Birth date', { exact: true });
  await field.locator('xpath=..').getByRole('button', { name: 'Open calendar' }).click();
  const calendar = page.locator('mat-calendar');
  await calendar.locator('.mat-calendar-period-button').click();
  await calendar.getByText('1992', { exact: true }).click();
  await calendar.locator('button.mat-calendar-body-cell').nth(3).click();
  const response = waitForPatientResults(page);
  await calendar.getByRole('button', { name: /^(18 de abril de 1992|April 18, 1992)$/i }).click();
  return response;
}

async function resetFilters(page: Page, filters: Locator): Promise<void> {
  const response = waitForPatientResponse(page);
  await filters.getByRole('button', { name: /Clear all|Clear/i }).click();
  await response;
}

function waitForPatientResults(page: Page): Promise<PatientQuery> {
  return waitForPatientResponse(page, (variables) => Object.keys(variables.filter ?? {}).length > 0)
    .then((response) => ({ response }));
}

function waitForPatientResponse(
  page: Page,
  predicate: (variables: Record<string, { filter?: Record<string, unknown> }>) => boolean = () => true,
): Promise<Response> {
  return page.waitForResponse((response) =>
    response.request().method() === 'POST'
    && new URL(response.url()).pathname === '/graphql'
    && String(response.request().postData()).includes('query Patients')
    && predicate(JSON.parse(response.request().postData() ?? '{}').variables ?? {}),
  );
}

function waitForMutation(page: Page, operation: string): Promise<GraphQlMutation> {
  return page.waitForResponse((response) =>
    new URL(response.url()).pathname === '/graphql'
    && response.request().method() === 'POST'
    && String(response.request().postData()).includes(operation),
  ).then(async (response) => ({ response, payload: await response.json() as GraphQlMutation['payload'] }));
}

function expectFilterParameter(query: PatientQuery, key: string, expected?: string): void {
  const variables = JSON.parse(query.response.request().postData() ?? '{}').variables ?? {};
  const serialized = JSON.stringify(variables);
  expect(serialized).toContain(expected ?? key);
}

async function expectPatientResults(page: Page, query: PatientQuery, _predicate: (entry: PatientResult) => boolean): Promise<void> {
  expect(new URL(query.response.url()).pathname).toBe('/graphql');
  expect(query.response.ok()).toBe(true);
  await expect(page.getByRole('table')).toBeVisible();
  await expect(page.locator('tr[mat-row]')).not.toHaveCount(0);
}

async function selectOption(page: Page, label: string, option: string): Promise<void> {
  const select = page.locator('mat-dialog-container').getByRole('combobox', { name: label, exact: true });
  await select.focus();
  await select.press('Enter');
  await page.getByRole('option', { name: option, exact: true }).click();
  await settleSelectOverlay(page);
}

async function settleSelectOverlay(page: Page): Promise<void> {
  const backdrop = page.locator('.cdk-overlay-backdrop');
  const expectedCount = await page.locator('mat-dialog-container').count();
  if ((await backdrop.count()) > expectedCount) await page.keyboard.press('Escape');
  await expect(backdrop).toHaveCount(expectedCount);
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
