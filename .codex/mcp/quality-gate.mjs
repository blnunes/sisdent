#!/usr/bin/env node

import { existsSync, readFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { resolve } from 'node:path';

const MINIMUM_INSTRUCTION_COVERAGE = 81;
const MINIMUM_BRANCH_COVERAGE = 60;

function repositoryRoot() {
  const result = spawnSync('git', ['rev-parse', '--show-toplevel'], { encoding: 'utf8' });
  if (result.status !== 0) throw new Error('The quality gate must run inside a Git repository.');
  return result.stdout.trim();
}

function changedJavaFiles(root) {
  const result = spawnSync('git', ['status', '--porcelain'], { cwd: root, encoding: 'utf8' });
  if (result.status !== 0) throw new Error(result.stderr || 'Unable to inspect the working tree.');
  return result.stdout.split('\n').some((line) => /(^|\s)src\/(main|test)\/java\/.*\.java$/.test(line));
}

function parseCoverage(root) {
  const csvPath = resolve(root, 'target/site/jacoco/jacoco.csv');
  if (!existsSync(csvPath)) throw new Error('JaCoCo did not produce target/site/jacoco/jacoco.csv.');
  const totals = readFileSync(csvPath, 'utf8').trim().split('\n').slice(1).reduce((result, row) => {
    const fields = row.split(',').map(Number);
    result.instructionMissed += fields[3];
    result.instructionCovered += fields[4];
    result.branchMissed += fields[5];
    result.branchCovered += fields[6];
    return result;
  }, { instructionMissed: 0, instructionCovered: 0, branchMissed: 0, branchCovered: 0 });
  const percentage = (covered, missed) => {
    const total = covered + missed;
    return total === 0 ? 100 : Number(((covered / total) * 100).toFixed(2));
  };
  return {
    instruction: percentage(totals.instructionCovered, totals.instructionMissed),
    branch: percentage(totals.branchCovered, totals.branchMissed),
  };
}

function run(command, args, root) {
  const result = spawnSync(command, args, { cwd: root, encoding: 'utf8', env: process.env, maxBuffer: 10 * 1024 * 1024 });
  if (result.status !== 0) {
    const output = `${result.stdout}\n${result.stderr}`.trim();
    throw new Error(`${command} ${args.join(' ')} failed.\n${output.slice(-6000)}`);
  }
}

function verifyQuality({ runSonar = false } = {}) {
  const root = repositoryRoot();
  run('./mvnw', ['--batch-mode', '--no-transfer-progress', 'verify'], root);
  const coverage = parseCoverage(root);
  const failures = [];
  if (coverage.instruction < MINIMUM_INSTRUCTION_COVERAGE) failures.push(`instruction coverage ${coverage.instruction}% is below ${MINIMUM_INSTRUCTION_COVERAGE}%`);
  if (coverage.branch < MINIMUM_BRANCH_COVERAGE) failures.push(`branch coverage ${coverage.branch}% is below ${MINIMUM_BRANCH_COVERAGE}%`);
  if (runSonar) {
    if (!process.env.SONAR_TOKEN) throw new Error('SONAR_TOKEN is required to run SonarCloud analysis.');
    run('./mvnw', ['--batch-mode', '--no-transfer-progress', 'org.sonarsource.scanner.maven:sonar-maven-plugin:5.7.0.6970:sonar', '-Dsonar.organization=blnunes', '-Dsonar.projectKey=blnunes_sisdent', '-Dsonar.qualitygate.wait=true'], root);
  }
  if (failures.length > 0) throw new Error(failures.join('; '));
  return { coverage, sonar: runSonar ? 'passed' : 'not run' };
}

function toolResult(value) {
  return { content: [{ type: 'text', text: JSON.stringify(value, null, 2) }] };
}

function respond(message) {
  process.stdout.write(`${JSON.stringify(message)}\n`);
}

const tool = {
  name: 'verify_quality',
  description: 'Runs Maven tests, enforces JaCoCo coverage, and optionally waits for the SonarCloud Quality Gate. Call before completing Java work.',
  inputSchema: { type: 'object', properties: { runSonar: { type: 'boolean', description: 'Run SonarCloud too; requires SONAR_TOKEN.' } }, additionalProperties: false },
};

if (process.argv.includes('--stop-hook')) {
  try {
    const root = repositoryRoot();
    if (!changedJavaFiles(root)) process.exit(0);
    verifyQuality();
    process.exit(0);
  } catch (error) {
    process.stderr.write(`Quality gate failed: ${error.message}\n`);
    process.exit(2);
  }
}

let buffer = '';
process.stdin.setEncoding('utf8');
process.stdin.on('data', (chunk) => {
  buffer += chunk;
  let newline;
  while ((newline = buffer.indexOf('\n')) >= 0) {
    const line = buffer.slice(0, newline).trim();
    buffer = buffer.slice(newline + 1);
    if (!line) continue;
    const request = JSON.parse(line);
    if (request.method === 'initialize') {
      respond({ jsonrpc: '2.0', id: request.id, result: { protocolVersion: request.params?.protocolVersion ?? '2025-03-26', capabilities: { tools: {} }, serverInfo: { name: 'sisdent-quality-gate', version: '1.0.0' }, instructions: 'Before completing Java work, call verify_quality. Add focused tests for every changed behaviour and maintain at least 81% JaCoCo instruction coverage. When Sonar coverage is below 80%, invoke coverage-recovery before another remote analysis: measure the deficit, rank changed files by uncovered local lines, and make one high-impact test batch expected to exceed the required gain by 2x. Do not repeat Sonar after a marginal sub-1-point increase that cannot clear the deficit. Fix coverage and Sonar findings rather than suppressing them. For Angular and TypeScript, never render or interpolate an unknown API value with String(value), template interpolation, or implicit coercion. Narrow values to a supported primitive first; for UI text, use shared/text-value.ts textValue(value, fallback) so null, arrays, and malformed objects render the explicit fallback rather than [object Object]. Add regression tests for malformed API values whenever a new UI mapping is introduced. NON-NEGOTIABLE: constructors and methods with more than seven parameters are unacceptable. Do not suppress Sonar java:S107; redesign the API with cohesive value objects, commands, or builders and cover the refactor with focused tests.' } });
    } else if (request.method === 'notifications/initialized') {
      // MCP notification; no response required.
    } else if (request.method === 'tools/list') {
      respond({ jsonrpc: '2.0', id: request.id, result: { tools: [tool] } });
    } else if (request.method === 'tools/call' && request.params?.name === tool.name) {
      try {
        respond({ jsonrpc: '2.0', id: request.id, result: toolResult(verifyQuality(request.params.arguments)) });
      } catch (error) {
        respond({ jsonrpc: '2.0', id: request.id, result: { ...toolResult({ error: error.message }), isError: true } });
      }
    } else {
      respond({ jsonrpc: '2.0', id: request.id, error: { code: -32601, message: 'Method not found' } });
    }
  }
});
