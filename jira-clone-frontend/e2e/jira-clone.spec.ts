import { test, expect } from '@playwright/test';

const BASE = 'http://localhost:4200';
const API = 'http://localhost:8080/api';

/** Generates a unique 8-letter uppercase key (letters only, satisfies [A-Z]{2,10}) */
function uniqKey(prefix = ''): string {
  const alpha = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
  let key = prefix.toUpperCase().replace(/[^A-Z]/g, '').substring(0, 4);
  while (key.length < 8) key += alpha[Math.floor(Math.random() * 26)];
  return key;
}

test.describe('JIRA Clone E2E Tests', () => {

  test.beforeEach(async ({ page }) => {
    // Navigate to the app
    await page.goto(BASE);
    await page.waitForLoadState('networkidle');
  });

  // ─── Projects Page ──────────────────────────────────────────────────────────

  test('TC-01: Projects page loads and shows empty state or project grid', async ({ page }) => {
    await page.goto(`${BASE}/projects`);
    await page.waitForLoadState('networkidle');

    const title = page.locator('.page-title');
    await expect(title).toBeVisible();
    await expect(title).toHaveText('Projects');

    // Either empty state or project cards
    const emptyState = page.locator('.empty-state');
    const projectGrid = page.locator('.project-grid');
    const hasEmpty = await emptyState.isVisible();
    const hasGrid = await projectGrid.isVisible();
    expect(hasEmpty || hasGrid).toBeTruthy();
  });

  test('TC-02: Can open Create Project dialog', async ({ page }) => {
    await page.goto(`${BASE}/projects`);
    await page.waitForLoadState('networkidle');

    await page.locator('p-button:has-text("New Project"), p-button:has-text("Create Project")').first().click();

    const dialog = page.locator('.p-dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog.locator('.p-dialog-title')).toContainText('Create Project');
  });

  test('TC-03: Can create a new project', async ({ page }) => {
    await page.goto(`${BASE}/projects`);
    await page.waitForLoadState('networkidle');

    // Open dialog
    await page.locator('p-button').filter({ hasText: /new project|create project/i }).first().click();
    await page.waitForSelector('.p-dialog', { state: 'visible' });

    // Fill form
    const timestamp = Date.now();
    const projectName = `Test Project ${timestamp}`;
    const projectKey = `TP${timestamp}`;

    await page.locator('.p-dialog input[placeholder="My Project"]').fill(projectName);
    await page.waitForTimeout(300);
    await page.locator('.p-dialog input[placeholder="PROJ"]').fill(projectKey);

    // Submit
    await page.locator('.p-dialog .p-button').filter({ hasText: 'Create' }).click();
    await page.waitForTimeout(1500);

    // Project should appear in grid or we get success toast
    const toast = page.locator('.p-toast-message');
    const projectCard = page.locator('.project-card').filter({ hasText: projectName });
    const hasToast = await toast.isVisible().catch(() => false);
    const hasCard = await projectCard.isVisible().catch(() => false);
    expect(hasToast || hasCard).toBeTruthy();
  });

  // ─── Layout ─────────────────────────────────────────────────────────────────

  test('TC-04: Sidebar is visible with navigation links', async ({ page }) => {
    await page.goto(`${BASE}/projects`);
    await page.waitForLoadState('networkidle');

    const sidebar = page.locator('.sidebar');
    await expect(sidebar).toBeVisible();

    const projectsLink = page.locator('.nav-item').filter({ hasText: 'Projects' });
    await expect(projectsLink).toBeVisible();
  });

  test('TC-05: Sidebar toggle collapses and expands', async ({ page }) => {
    await page.goto(`${BASE}/projects`);
    await page.waitForLoadState('networkidle');

    const toggleBtn = page.locator('.sidebar-toggle');
    await toggleBtn.click();
    await page.waitForTimeout(300);

    const shell = page.locator('.app-shell');
    await expect(shell).toHaveClass(/sidebar-collapsed/);

    // Toggle back
    await toggleBtn.click();
    await page.waitForTimeout(300);
    await expect(shell).not.toHaveClass(/sidebar-collapsed/);
  });

  // ─── Board ───────────────────────────────────────────────────────────────────

  test('TC-06: Board page shows kanban columns when project exists', async ({ page }) => {
    // First create a project via API
    const projResp = await page.request.post(`${API}/projects`, {
      data: {
        name: 'E2E Board Project',
        key: uniqKey('EB'),
        type: 'SCRUM'
      }
    });
    if (!projResp.ok()) throw new Error(`Create project failed: ${await projResp.text()}`);
    const { data: project } = await projResp.json();

    await page.goto(`${BASE}/projects/${project.id}/board`);
    await page.waitForLoadState('networkidle');

    const columns = page.locator('.board-column');
    const count = await columns.count();
    expect(count).toBeGreaterThan(0);
  });

  test('TC-07: Can create an issue from Board page', async ({ page }) => {
    // Create project via API
    const projResp = await page.request.post(`${API}/projects`, {
      data: {
        name: 'E2E Issue Project',
        key: uniqKey('EI'),
        type: 'SCRUM'
      }
    });
    if (!projResp.ok()) throw new Error(`Create project failed: ${await projResp.text()}`);
    const { data: project } = await projResp.json();

    await page.goto(`${BASE}/projects/${project.id}/board`);
    await page.waitForLoadState('networkidle');

    // Open create dialog
    await page.locator('p-button').filter({ hasText: 'Create Issue' }).click();
    await page.waitForSelector('.p-dialog', { state: 'visible' });

    // Fill summary
    const issueSummary = `E2E Test Issue ${Date.now()}`;
    await page.locator('.p-dialog input[placeholder="Issue summary"]').fill(issueSummary);

    // Submit
    await page.locator('.p-dialog .p-button').filter({ hasText: 'Create' }).click();
    await page.waitForTimeout(1500);

    // Issue card should appear
    const issueCard = page.locator('.issue-summary').filter({ hasText: issueSummary });
    await expect(issueCard).toBeVisible({ timeout: 5000 });
  });

  // ─── Backlog ─────────────────────────────────────────────────────────────────

  test('TC-08: Backlog page shows sprints section', async ({ page }) => {
    const projResp = await page.request.post(`${API}/projects`, {
      data: {
        name: 'E2E Backlog Project',
        key: uniqKey('EBL'),
        type: 'SCRUM'
      }
    });
    if (!projResp.ok()) throw new Error(`Create project failed: ${await projResp.text()}`);
    const { data: project } = await projResp.json();

    await page.goto(`${BASE}/projects/${project.id}/backlog`);
    await page.waitForLoadState('networkidle');

    // Backlog section should always exist
    const backlogSection = page.locator('.sprint-section').last();
    await expect(backlogSection).toBeVisible();
  });

  test('TC-09: Can create a sprint from Backlog', async ({ page }) => {
    const projResp = await page.request.post(`${API}/projects`, {
      data: {
        name: 'E2E Sprint Project',
        key: uniqKey('ESP'),
        type: 'SCRUM'
      }
    });
    if (!projResp.ok()) throw new Error(`Create project failed: ${await projResp.text()}`);
    const { data: project } = await projResp.json();

    await page.goto(`${BASE}/projects/${project.id}/backlog`);
    await page.waitForLoadState('networkidle');

    await page.locator('p-button').filter({ hasText: 'Create Sprint' }).click();
    await page.waitForSelector('.p-dialog', { state: 'visible' });

    const sprintName = `Sprint 1 - ${Date.now()}`;
    await page.locator('.p-dialog input[placeholder="Sprint 1"]').fill(sprintName);

    await page.locator('.p-dialog .p-button').filter({ hasText: 'Create' }).click();
    await page.waitForTimeout(1500);

    const sprintSection = page.locator('.sprint-name').filter({ hasText: sprintName });
    await expect(sprintSection).toBeVisible({ timeout: 5000 });
  });

  // ─── Issue Detail ─────────────────────────────────────────────────────────

  test('TC-10: Issue detail page loads correctly', async ({ page }) => {
    // Create project + issue via API
    const projResp = await page.request.post(`${API}/projects`, {
      data: { name: 'E2E Detail Project', key: uniqKey('ED'), type: 'SCRUM' }
    });
    if (!projResp.ok()) throw new Error(`Create project failed: ${await projResp.text()}`);
    const { data: project } = await projResp.json();

    const issueResp = await page.request.post(`${API}/projects/${project.id}/issues`, {
      data: {
        type: 'TASK',
        summary: 'E2E Detail Test Issue',
        priority: 'MEDIUM'
      }
    });
    const { data: issue } = await issueResp.json();

    await page.goto(`${BASE}/issues/${issue.id}`);
    await page.waitForLoadState('networkidle');

    const summary = page.locator('.issue-summary');
    await expect(summary).toBeVisible();
    await expect(summary).toContainText('E2E Detail Test Issue');
  });

  test('TC-11: Can add a comment on Issue detail', async ({ page }) => {
    const projResp = await page.request.post(`${API}/projects`, {
      data: { name: 'E2E Comment Project', key: uniqKey('EC'), type: 'SCRUM' }
    });
    if (!projResp.ok()) throw new Error(`Create project failed: ${await projResp.text()}`);
    const { data: project } = await projResp.json();

    const issueResp = await page.request.post(`${API}/projects/${project.id}/issues`, {
      data: { type: 'TASK', summary: 'Issue for Comment Test', priority: 'LOW' }
    });
    const { data: issue } = await issueResp.json();

    await page.goto(`${BASE}/issues/${issue.id}`);
    await page.waitForLoadState('networkidle');

    const commentText = `Test comment ${Date.now()}`;
    await page.locator('.add-comment textarea').fill(commentText);
    await page.locator('p-button').filter({ hasText: 'Add Comment' }).click();
    await page.waitForTimeout(1500);

    const comment = page.locator('.comment-text').filter({ hasText: commentText });
    await expect(comment).toBeVisible({ timeout: 5000 });
  });

  // ─── API Health ───────────────────────────────────────────────────────────

  test('TC-12: Backend API is healthy', async ({ request }) => {
    const resp = await request.get(`${API}/projects`);
    expect(resp.ok()).toBeTruthy();
    const body = await resp.json();
    expect(body.success).toBe(true);
    expect(Array.isArray(body.data)).toBe(true);
  });

  test('TC-13: Navigation breadcrumb works on board', async ({ page }) => {
    const projResp = await page.request.post(`${API}/projects`, {
      data: { name: 'E2E Nav Project', key: uniqKey('EN'), type: 'SCRUM' }
    });
    if (!projResp.ok()) throw new Error(`Create project failed: ${await projResp.text()}`);
    const { data: project } = await projResp.json();

    await page.goto(`${BASE}/projects/${project.id}/board`);
    await page.waitForLoadState('networkidle');

    const breadcrumb = page.locator('.breadcrumb');
    await expect(breadcrumb).toContainText('Projects');
    await expect(breadcrumb).toContainText('Board');

    // Click Projects in breadcrumb
    await breadcrumb.locator('a').first().click();
    await expect(page).toHaveURL(/\/projects$/);
  });

});
