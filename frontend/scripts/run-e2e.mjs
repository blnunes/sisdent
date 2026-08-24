import { spawn } from 'node:child_process';
import { closeSync, openSync } from 'node:fs';
import { readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';
import { dirname, join, resolve } from 'node:path';

const frontendDirectory = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const repositoryDirectory = resolve(frontendDirectory, '..');
const backendPort = process.env.E2E_BACKEND_PORT ?? '8081';
const backendUrl = `http://localhost:${backendPort}`;
const healthUrl = `${backendUrl}/actuator/health`;
const backendPidFile = join(tmpdir(), `sisdent-e2e-backend-${backendPort}.pid`);
const backendLogFile = join(tmpdir(), `sisdent-e2e-backend-${backendPort}.log`);
let backend;
let stoppingBackend;
let backendLogDescriptor;

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.once(signal, () => {
    void stopBackend().finally(() => process.exit(signal === 'SIGINT' ? 130 : 143));
  });
}

try {
  await stopStaleBackend();
  if (await isHealthy()) {
    throw new Error(`The dedicated E2E backend port ${backendPort} is already in use. Stop that E2E process and retry.`);
  }
  console.log(`Starting a fresh Spring Boot backend for Playwright (log: ${backendLogFile})...`);
  backendLogDescriptor = openSync(backendLogFile, 'a');
  backend = spawn(
    './mvnw',
    ['-q', 'spring-boot:run', '-Dspring-boot.run.profiles=e2e', `-Dspring-boot.run.arguments=--server.port=${backendPort}`],
    {
    cwd: repositoryDirectory,
    stdio: ['ignore', backendLogDescriptor, backendLogDescriptor],
    // The Maven wrapper starts Java as a child. A dedicated process group lets
    // the runner reliably stop both processes after failed Playwright runs.
    detached: process.platform !== 'win32',
    },
  );
  await writeFile(backendPidFile, String(backend.pid), { mode: 0o600 });
  await waitForHealth();

  const exitCode = await runPlaywright(process.argv.slice(2));
  process.exitCode = exitCode;
} finally {
  await stopBackend();
  closeBackendLog();
}

async function stopBackend() {
  if (stoppingBackend) return stoppingBackend;
  if (!backend || backend.exitCode !== null || backend.signalCode !== null) return;

  stoppingBackend = new Promise((resolvePromise) => {
    const timeout = setTimeout(() => {
      if (backend.exitCode === null && backend.signalCode === null) {
        terminateBackend('SIGKILL');
      }
    }, 10_000);
    backend.once('exit', () => {
      clearTimeout(timeout);
      closeBackendLog();
      void rm(backendPidFile, { force: true });
      resolvePromise();
    });
    terminateBackend('SIGTERM');
  });
  return stoppingBackend;
}

function closeBackendLog() {
  if (backendLogDescriptor === undefined) return;
  closeSync(backendLogDescriptor);
  backendLogDescriptor = undefined;
}

async function stopStaleBackend() {
  let stalePid;
  try {
    stalePid = Number.parseInt((await readFile(backendPidFile, 'utf8')).trim(), 10);
  } catch (error) {
    if (error.code === 'ENOENT') return;
    throw error;
  }

  if (!Number.isSafeInteger(stalePid) || stalePid <= 0) {
    await rm(backendPidFile, { force: true });
    return;
  }
  try {
    process.kill(-stalePid, 'SIGTERM');
  } catch (error) {
    if (error.code !== 'ESRCH') throw error;
  }
  await waitForBackendStop();
  await rm(backendPidFile, { force: true });
}

async function waitForBackendStop() {
  const deadline = Date.now() + 10_000;
  while (Date.now() < deadline) {
    if (!await isListening()) return;
    await delay(100);
  }
  throw new Error(`The previous E2E backend did not stop on port ${backendPort}.`);
}

function terminateBackend(signal) {
  if (process.platform === 'win32') {
    backend.kill(signal);
    return;
  }
  try {
    process.kill(-backend.pid, signal);
  } catch (error) {
    if (error.code !== 'ESRCH') throw error;
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

async function isListening() {
  try {
    await fetch(healthUrl, { signal: AbortSignal.timeout(500) });
    return true;
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
    env: { ...process.env, E2E_BACKEND_URL: backendUrl },
  });
  return new Promise((resolvePromise, reject) => {
    child.once('error', reject);
    child.once('exit', (code, signal) => resolvePromise(code ?? (signal ? 1 : 0)));
  });
}

function delay(milliseconds) {
  return new Promise((resolvePromise) => setTimeout(resolvePromise, milliseconds));
}
