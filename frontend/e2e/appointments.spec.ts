import { expect, test, type Page } from '@playwright/test';

test.describe('Appointments calendar', () => {
  test('loads the clinic-timezone week, filters practitioners, and switches to day by keyboard', async ({ page }) => {
    await signIn(page);
    const rangeRequest = page.waitForResponse((response) =>
      new URL(response.url()).pathname === '/graphql'
      && String(response.request().postData()).includes('query Appointments'),
    );
    await page.goto('/appointments');
    await expect(page.getByRole('heading', { name: 'Appointments', exact: true })).toBeVisible();
    await expect(page.locator('full-calendar')).toBeVisible();
    await expect(page.locator('.fc-timeGridWeek-view')).toBeVisible();
    await expect(page.locator('.timezone')).toContainText('Europe/Lisbon');
    const request = await rangeRequest;
    const variables = JSON.parse(request.request().postData() ?? '{}').variables;
    expect(variables.from).toMatch(/Z$/);
    expect(variables.to).toMatch(/Z$/);
    expect(variables.to).not.toBe(variables.from);
    await page.screenshot({ path: 'test-results/appointments-calendar-visual-review.png', fullPage: true });
    await page.getByRole('button', { name: 'Day', exact: true }).focus();
    await page.keyboard.press('Enter');
    await expect(page.locator('.fc-timeGridDay-view')).toBeVisible();
    await page.getByLabel('Practitioner', { exact: true }).click();
    const option = page.getByRole('option').first();
    if (await option.isVisible()) {
      const filteredRequest = page.waitForResponse((response) =>
        new URL(response.url()).pathname === '/graphql'
        && String(response.request().postData()).includes('query Appointments'),
      );
      await option.click();
      const response = await filteredRequest;
      expect(JSON.parse(response.request().postData() ?? '{}').variables.practitionerIds).toHaveLength(1);
    }
  });

  test('remains usable in dark mode on a narrow viewport with only approved scheduling controls', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.addInitScript(() => {
      localStorage.setItem('sisdent.language', 'en');
      localStorage.setItem('sisdent-theme', 'dark');
    });
    await signIn(page);
    await mockAppointmentDetail(page);
    await page.goto('/appointments');
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
    await expect(page.locator('full-calendar')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Schedule appointment', exact: true })).toBeVisible();
    await page.locator('.fc-event:not(.fc-bg-event)').first().click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog.locator('dd').first()).toContainText(/2:30/);
    await expect(dialog.getByRole('button', { name: 'Reschedule appointment', exact: true })).toBeVisible();
    await expect(dialog.getByRole('button', { name: 'Complete', exact: true })).toBeVisible();
    await expect(dialog.getByRole('button', { name: 'Mark no show', exact: true })).toBeVisible();
    await expect(dialog.getByRole('button', { name: 'Cancel appointment', exact: true })).toBeVisible();
  });

  test('opens a selected clinic-local slot without creating an appointment until explicit save', async ({ page }) => {
    await signIn(page);
    await mockScheduling(page);
    let createRequests = 0;
    page.on('request', (request) => {
      if (String(request.postData()).includes('mutation CreateAppointment')) createRequests += 1;
    });
    await page.goto('/appointments');
    await page.getByRole('button', { name: 'Day', exact: true }).click();
    const slot = page.locator('.fc-timegrid-slot-lane').nth(8);
    await slot.focus();
    await page.keyboard.press('Enter');
    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog.getByLabel('Start date and time', { exact: true })).not.toHaveValue('');
    await expect(dialog.getByLabel('End date and time', { exact: true })).not.toHaveValue('');
    expect(createRequests).toBe(0);
    await expect(page.locator('.fc-event:not(.fc-bg-event)')).toBeVisible();
    await page.keyboard.press('Escape');
    const box = await slot.boundingBox();
    if (!box) throw new Error('Expected a selectable calendar slot');
    await page.mouse.move(box.x + 8, box.y + 8);
    await page.mouse.down();
    await page.mouse.move(box.x + 8, box.y + Math.min(20, box.height - 2));
    await page.mouse.up();
    await expect(page.getByRole('dialog')).toBeVisible();
    expect(createRequests).toBe(0);
  });

  test('confirms lifecycle actions from details, preserves the calendar, and keeps failures generic', async ({ page }) => {
    await signIn(page);
    await mockScheduling(page);
    await page.goto('/appointments');
    const event = page.locator('.fc-event:not(.fc-bg-event)').first();
    await event.click();
    await page.getByRole('button', { name: 'Complete', exact: true }).click();
    const confirmation = page.getByRole('dialog').last();
    await expect(confirmation).toContainText('cannot be undone');
    const mutation = page.waitForRequest((request) => String(request.postData()).includes('mutation TransitionAppointment'));
    await confirmation.getByRole('button', { name: 'Confirm action', exact: true }).click();
    const body = JSON.parse((await mutation).postData() ?? '{}');
    expect(body.variables).toMatchObject({ organizationId: expect.any(String), clinicUnitId: expect.any(String), status: 'COMPLETED' });
    await expect(page.locator('full-calendar')).toBeVisible();
    await expect(page.getByText('secret lifecycle detail')).toHaveCount(0);
  });

  test('creates from the explicit action and preserves the day view and filters', async ({ page }) => {
    await signIn(page);
    await mockScheduling(page);
    await page.goto('/appointments');
    await page.getByRole('button', { name: 'Day', exact: true }).click();
    await page.getByRole('button', { name: 'Schedule appointment', exact: true }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible();
    await dialog.getByLabel('Patient', { exact: true }).click();
    await page.getByRole('option', { name: 'Patient', exact: true }).click();
    await dialog.getByLabel('Practitioner', { exact: true }).click();
    await page.getByRole('option').first().click();
    await dialog.getByLabel('Start date and time', { exact: true }).fill('2026-10-25T01:30');
    await dialog.getByLabel('End date and time', { exact: true }).fill('2026-10-25T02:00');
    const mutation = page.waitForRequest((request) => String(request.postData()).includes('mutation CreateAppointment'));
    await dialog.getByRole('button', { name: 'Schedule appointment', exact: true }).click();
    const body = JSON.parse((await mutation).postData() ?? '{}');
    expect(body.variables.input).toMatchObject({ clinicUnitId: expect.any(String), schedulingTimezone: 'Europe/Lisbon' });
    expect(body.variables.input.startAt).toMatch(/Z$/);
    await expect(page.locator('.fc-timeGridDay-view')).toBeVisible();
  });

  test('reschedules from details and keeps a generic conflict safely recoverable', async ({ page }) => {
    await signIn(page);
    await mockScheduling(page, true);
    await page.goto('/appointments');
    await page.locator('.fc-event:not(.fc-bg-event)').first().click();
    await page.getByRole('button', { name: 'Reschedule appointment', exact: true }).click();
    const dialog = page.getByRole('dialog').last();
    await dialog.getByLabel('End date and time', { exact: true }).fill('2026-03-29T03:30');
    await dialog.getByRole('button', { name: 'Save new time', exact: true }).click();
    await expect(dialog.getByRole('alert')).toHaveText('The practitioner is unavailable for this interval.');
    await expect(page.getByText('secret conflict detail')).toHaveCount(0);
    await expect(page.locator('full-calendar')).toBeVisible();
  });

  test('opens read-only detail by keyboard, closes with Escape, and restores event focus', async ({ page }) => {
    await signIn(page);
    await mockAppointmentDetail(page);
    await page.goto('/appointments');
    const event = page.locator('.fc-event:not(.fc-bg-event)').first();
    await expect(event).toBeVisible();
    await event.focus();
    await page.keyboard.press('Enter');
    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Appointment details' })).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(event).toBeFocused();
    await expect(page).not.toHaveURL(/appointmentId=/);
    await page.getByRole('button', { name: 'Day', exact: true }).click();
    await expect(page.locator('.fc-timeGridDay-view')).toBeVisible();
    await page.locator('.fc-event:not(.fc-bg-event)').first().click();
    await expect(page.getByRole('dialog')).toBeVisible();
    await page.keyboard.press('Escape');
  });

  test('opens a direct deep link and preserves filters after closing detail', async ({ page }) => {
    await signIn(page);
    await mockAppointmentDetail(page);
    await page.goto('/appointments?appointmentId=11111111-1111-4111-8111-111111111111');
    await expect(page.locator('full-calendar')).toBeVisible();
    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByRole('dialog').locator('dd').filter({ hasText: 'Patient' })).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(page).not.toHaveURL(/appointmentId=/);
    const practitioner = page.getByRole('combobox', { name: 'Practitioner', exact: true });
    await practitioner.click();
    const option = page.getByRole('option').first();
    if (await option.isVisible()) await option.click();
    await page.keyboard.press('Escape');
    const selection = practitioner.locator('.mat-mdc-select-value-text');
    const selected = await selection.textContent();
    await page.locator('.fc-event:not(.fc-bg-event)').first().click();
    await expect(page.getByRole('dialog')).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(selection).toHaveText(selected ?? '');
  });

  test('clears a malformed deep link without querying a detail', async ({ page }) => {
    await signIn(page);
    await page.goto('/appointments?appointmentId=not-an-id');
    await expect(page.locator('full-calendar')).toBeVisible();
    await expect(page).not.toHaveURL(/appointmentId=/);
  });

  test('clears an unauthorized deep link without exposing GraphQL details', async ({ page }) => {
    await signIn(page);
    await mockAppointmentDetail(page);
    await page.goto('/appointments?appointmentId=22222222-2222-4222-8222-222222222222');
    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByRole('alert')).toHaveText("This appointment's details are unavailable.");
    await expect(page.getByText('Not permitted for this appointment')).toHaveCount(0);
    await expect(page).not.toHaveURL(/appointmentId=/);
  });

  test('manages unavailable periods accessibly on a narrow dark viewport without exposing opaque data', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.addInitScript(() => localStorage.setItem('sisdent-theme', 'dark'));
    await signIn(page);
    await page.route('**/graphql', async (route) => {
      const body = JSON.parse(route.request().postData() ?? '{}');
      const query = String(body.query ?? '');
      if (query.includes('mutation CreateBlockedPeriod')) {
        expect(body.variables.input.startAt).toMatch(/Z$/);
        expect(body.variables.input.endAt).toMatch(/Z$/);
        await route.fulfill({ json: { data: { createAppointmentBlockedPeriod: { globalId: '11111111-1111-4111-8111-111111111111', clinicUnitId: body.variables.input.clinicUnitId, practitionerId: null, startAt: body.variables.input.startAt, endAt: body.variables.input.endAt, version: 1 } } } });
        return;
      }
      if (query.includes('query BlockedPeriods')) {
        await route.fulfill({ json: { data: { appointmentBlockedPeriods: [] } } });
        return;
      }
      await route.continue();
    });
    await page.goto('/appointments');
    const opener = page.getByRole('button', { name: 'Manage unavailable periods', exact: true });
    await opener.focus();
    await opener.press('Enter');
    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Unavailable periods');
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
    await dialog.getByLabel('Start date and time', { exact: true }).fill('2026-03-29T09:00');
    await dialog.getByLabel('End date and time', { exact: true }).fill('2026-03-29T10:00');
    await dialog.getByRole('button', { name: 'Save period', exact: true }).click();
    await expect(dialog).not.toContainText(/11111111|version|raw stale|reason/i);
    await dialog.getByLabel('Start date and time', { exact: true }).fill('2026-10-25T01:30');
    await page.keyboard.press('Escape');
    const discard = page.getByRole('dialog').last();
    await expect(discard).toContainText('unsaved unavailable-period changes');
    await discard.getByRole('button', { name: 'Discard changes', exact: true }).click();
    await expect(opener).toBeFocused();
  });
});

async function signIn(page: Page): Promise<void> {
  await page.addInitScript(() => localStorage.setItem('sisdent.language', 'en'));
  await page.goto('/login');
  await page.getByLabel('Email address', { exact: true }).fill('northstar.scheduler@sisdent.demo');
  await page.getByLabel('Password', { exact: true }).fill('odonto2026@O');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL(/\/home$/);
}

async function mockAppointmentDetail(page: Page): Promise<void> {
  await page.route('**/graphql', async (route) => {
    const request = route.request();
    const body = JSON.parse(request.postData() ?? '{}');
    const query = String(body.query ?? '');
    if (query.includes('query Appointments(')) {
      const start = new Date(new Date(body.variables.from).getTime() + 9 * 60 * 60 * 1000).toISOString();
      const end = new Date(new Date(start).getTime() + 30 * 60 * 1000).toISOString();
      await route.fulfill({ json: { data: { appointments: { content: [{ globalId: '11111111-1111-4111-8111-111111111111', clinicUnitId: 'clinic-1', patientId: 'patient-1', patientName: 'Patient', practitionerId: 'practitioner-1', practitionerName: 'Practitioner', startAt: start, endAt: end, schedulingTimezone: 'Europe/Lisbon', status: 'SCHEDULED' }], page: 0, size: 100, totalElements: 1, totalPages: 1 } } } });
      return;
    }
    if (query.includes('query Appointment(')) {
      if (body.variables.appointmentId === '22222222-2222-4222-8222-222222222222') {
        await route.fulfill({ json: { errors: [{ message: 'Not permitted for this appointment', extensions: { code: 'AUTHORIZATION.DENIED' } }] } });
        return;
      }
      await route.fulfill({ json: { data: { appointment: { patientId: 'patient-1', practitionerId: 'practitioner-1', patientName: 'Patient', practitionerName: 'Practitioner', startAt: '2026-03-29T00:30:00Z', endAt: '2026-03-29T01:30:00Z', status: 'SCHEDULED' } } } });
      return;
    }
    await route.continue();
  });
}

async function mockScheduling(page: Page, conflict = false): Promise<void> {
  await mockAppointmentDetail(page);
  await page.route('**/graphql', async (route) => {
    const body = JSON.parse(route.request().postData() ?? '{}');
    const query = String(body.query ?? '');
    if (query.includes('query Appointments(')) {
      const start = new Date(new Date(body.variables.from).getTime() + 9 * 60 * 60 * 1000).toISOString();
      const end = new Date(new Date(start).getTime() + 30 * 60 * 1000).toISOString();
      await route.fulfill({ json: { data: { appointments: { content: [{ globalId: '11111111-1111-4111-8111-111111111111', clinicUnitId: body.variables.clinicUnitId, patientId: 'patient-1', patientName: 'Patient', practitionerId: 'practitioner-1', practitionerName: 'Practitioner', startAt: start, endAt: end, schedulingTimezone: 'Europe/Lisbon', status: 'SCHEDULED' }], page: 0, size: 100, totalElements: 1, totalPages: 1 } } } });
      return;
    }
    if (query.includes('query AppointmentPatients(')) {
      await route.fulfill({ json: { data: { patients: { content: [{ globalId: 'patient-1', name: 'Patient', active: true }], page: 0, size: 100, totalElements: 1, totalPages: 1 } } } });
      return;
    }
    if (query.includes('query Appointment(')) {
      await route.fulfill({ json: { data: { appointment: { patientId: 'patient-1', practitionerId: 'practitioner-1', patientName: 'Patient', practitionerName: 'Practitioner', startAt: '2026-03-29T00:30:00Z', endAt: '2026-03-29T01:30:00Z', status: 'SCHEDULED' } } } });
      return;
    }
    if (query.includes('mutation CreateAppointment')) {
      await route.fulfill({ json: { data: { createAppointment: { ...body.variables.input, patientName: 'Patient', practitionerName: 'Practitioner', status: 'SCHEDULED' } } } });
      return;
    }
    if (query.includes('mutation RescheduleAppointment')) {
      await route.fulfill({ json: conflict ? { errors: [{ message: 'secret conflict detail', extensions: { code: 'SCHEDULING.PRACTITIONER_UNAVAILABLE' } }] } : { data: { rescheduleAppointment: { ...body.variables.input, patientName: 'Patient', practitionerName: 'Practitioner', status: 'SCHEDULED' } } } });
      return;
    }
    if (query.includes('mutation TransitionAppointment')) {
      await route.fulfill({ json: { data: { transitionAppointment: { status: body.variables.status } } } });
      return;
    }
    await route.continue();
  });
}
