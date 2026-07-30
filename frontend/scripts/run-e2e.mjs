import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const frontendDirectory = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const repositoryDirectory = resolve(frontendDirectory, '..');
const healthUrl = process.env.BACKEND_HEALTH_URL ?? 'http://localhost:8080/actuator/health';
let backend;

try {
  if (await isHealthy()) {
    throw new Error(
      `A backend is already running at ${healthUrl}. Stop it before E2E so the suite cannot use stale code.`,
    );
  }
  console.log(`Starting a fresh Spring Boot backend for Playwright...`);
  backend = spawn('./mvnw', ['-q', 'spring-boot:run'], {
    cwd: repositoryDirectory,
    stdio: 'inherit',
  });
  await waitForHealth();

  const exitCode = await runPlaywright(process.argv.slice(2));
  process.exitCode = exitCode;
} finally {
  if (backend && !backend.killed) {
    backend.kill('SIGTERM');
  }
}

async function isHealthy() {
  try {
    const response = await fetch(healthUrl, { signal: AbortSignal.timeout(2_000) });
    return response.ok;
  } catch {
    return false;
  }
}

async function waitForHealth() {
  const deadline = Date.now() + 120_000;
  while (Date.now() < deadline) {
    if (await isHealthy()) {
      console.log('Backend is healthy. Starting Playwright...');
      return;
    }
    await delay(1_000);
  }
  throw new Error(`Backend did not become healthy within 120 seconds: ${healthUrl}`);
}

function runPlaywright(args) {
  const command = process.platform === 'win32' ? 'npx.cmd' : 'npx';
  const child = spawn(command, ['playwright', 'test', ...args], {
    cwd: frontendDirectory,
    stdio: 'inherit',
  });
  return new Promise((resolvePromise, reject) => {
    child.once('error', reject);
    child.once('exit', (code, signal) => resolvePromise(code ?? (signal ? 1 : 0)));
  });
}

function delay(milliseconds) {
  return new Promise((resolvePromise) => setTimeout(resolvePromise, milliseconds));
}
