import { expect, Page, test } from '@playwright/test';

const accounts = {
  admin: ['admin@collaboration.local', 'Admin123!'],
  manager: ['manager@collaboration.local', 'Manager123!'],
  user: ['user@collaboration.local', 'User123!']
} as const;

async function login(page: Page, account: keyof typeof accounts): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('Work email').fill(accounts[account][0]);
  await page.getByLabel('Password').fill(accounts[account][1]);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.locator('.connection-pill:visible').filter({ hasText: 'Connected' })).toBeVisible();
}

test('invalid credentials return the generic sign-in error', async ({ page }) => {
  await page.goto('/login');
  await expect(page.getByText('Your team, in the moment.', { exact: true })).toBeVisible();
  await expect(page.getByText('Development roles')).toHaveCount(0);
  await expect(page.getByLabel('Work email')).toHaveValue('');
  await expect(page.getByLabel('Password')).toHaveValue('');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('alert')).toHaveText('Enter your work email and password.');

  await page.getByLabel('Work email').fill(accounts.user[0]);
  await page.getByLabel('Password').fill('wrong-password');
  await page.getByRole('button', { name: 'Sign in' }).click();

  await expect(page.getByRole('alert')).toHaveText('The email or password is incorrect.');
});

test('a stale presence snapshot cannot overwrite a newer live delta', async ({ browser }) => {
  const managerContext = await browser.newContext();
  const adminContext = await browser.newContext();
  const manager = await managerContext.newPage();
  const admin = await adminContext.newPage();
  let releaseSnapshot = (): void => undefined;
  const snapshotGate = new Promise<void>((resolve) => { releaseSnapshot = resolve; });
  let snapshotCaptured = (): void => undefined;
  const capturedSnapshot = new Promise<void>((resolve) => { snapshotCaptured = resolve; });

  await manager.route('**/api/presence', async (route) => {
    const staleResponse = await route.fetch();
    snapshotCaptured();
    await snapshotGate;
    await route.fulfill({ response: staleResponse });
  });

  try {
    await login(manager, 'manager');
    await capturedSnapshot;
    await login(admin, 'admin');

    const managerPresence = manager.locator('.people-section');
    await expect(managerPresence.getByText('Amina Admin')).toBeVisible();

    const deliveredSnapshot = manager.waitForResponse('**/api/presence');
    releaseSnapshot();
    await deliveredSnapshot;
    await manager.evaluate(() => new Promise<void>((resolve) =>
      requestAnimationFrame(() => requestAnimationFrame(() => resolve()))
    ));

    await expect(managerPresence.getByText('Amina Admin')).toBeVisible();
  } finally {
    releaseSnapshot();
    await adminContext.close();
    await managerContext.close();
  }
});

test('three roles collaborate, recover durable state, and preserve multi-session presence', async ({ browser }) => {
  const suffix = Date.now().toString();
  const offlineNotice = `Offline notice ${suffix}`;
  const liveNotice = `Live notice ${suffix}`;
  const chatText = `Secured group message ${suffix}`;

  const adminContext = await browser.newContext();
  const managerContext = await browser.newContext();
  const userContext = await browser.newContext();
  const secondUserContext = await browser.newContext();
  const admin = await adminContext.newPage();
  const manager = await managerContext.newPage();
  let user = await userContext.newPage();
  let secondUser = await secondUserContext.newPage();

  await login(admin, 'admin');
  await login(manager, 'manager');
  await expect(manager.getByRole('heading', { name: 'Send update' })).toBeVisible();

  await admin.getByLabel('Notification recipient').selectOption({ label: 'Youssef User · USER' });
  await admin.getByLabel('Notification message').fill(offlineNotice);
  await admin.getByRole('button', { name: 'Send notification' }).click();

  await login(user, 'user');
  await expect(user.getByRole('heading', { name: 'Send update' })).toHaveCount(0);
  await expect(user.getByText(offlineNotice)).toBeVisible();

  await manager.getByLabel('Notification recipient').selectOption({ label: 'Youssef User · USER' });
  await manager.getByLabel('Notification message').fill(liveNotice);
  await manager.getByRole('button', { name: 'Send notification' }).click();
  await expect(user.getByText(liveNotice)).toBeVisible();

  await user.getByLabel('Chat message').fill(chatText);
  await user.locator('.composer-region').getByRole('button', { name: 'Send' }).click();
  await expect(manager.getByText(chatText)).toBeVisible();

  await user.reload();
  await expect(user.getByText(chatText)).toBeVisible();

  await login(secondUser, 'user');
  const managerPresence = manager.locator('.people-section');
  await expect(managerPresence.getByText('Youssef User')).toBeVisible();

  await userContext.close();
  await expect(managerPresence.getByText('Youssef User')).toBeVisible();

  await secondUserContext.close();
  await expect(managerPresence.getByText('Youssef User')).toHaveCount(0);

  await adminContext.close();
  await managerContext.close();
});

test('mobile navigation exposes chat, activity, and people without horizontal overflow', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await login(page, 'manager');

  await expect(page.getByRole('navigation', { name: 'Mobile workspace navigation' })).toBeVisible();
  await page.getByRole('button', { name: 'Activity' }).click();
  await expect(page.getByRole('heading', { name: 'Activity' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Notifications' })).toBeVisible();

  await page.getByRole('button', { name: 'People' }).click();
  await expect(page.getByRole('heading', { name: 'People' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Online now' })).toBeVisible();

  await page.getByRole('button', { name: 'Chat' }).click();
  await expect(page.getByLabel('Chat message')).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
});
