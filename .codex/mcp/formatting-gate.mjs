#!/usr/bin/env node

import { existsSync, readFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { resolve } from 'node:path';

const JAVA_MAX_LINE_LENGTH = 120;
const PRETTIER_EXTENSIONS = new Set(['.ts', '.html', '.scss', '.json']);

function repositoryRoot() {
  const result = spawnSync('git', ['rev-parse', '--show-toplevel'], { encoding: 'utf8' });
  if (result.status !== 0) {
    throw new Error('The formatting gate must run inside a Git repository.');
  }
  return result.stdout.trim();
}

function run(command, args, root) {
  const result = spawnSync(command, args, {
    cwd: root,
    encoding: 'utf8',
    env: process.env,
    maxBuffer: 10 * 1024 * 1024,
  });
  if (result.status !== 0) {
    const output = `${result.stdout}\n${result.stderr}`.trim();
    throw new Error(`${command} ${args.join(' ')} failed.\n${output.slice(-6000)}`);
  }
}

function changedFiles(root) {
  const tracked = spawnSync('git', ['diff', '--name-only', 'HEAD'], {
    cwd: root,
    encoding: 'utf8',
  });
  const untracked = spawnSync('git', ['ls-files', '--others', '--exclude-standard'], {
    cwd: root,
    encoding: 'utf8',
  });
  if (tracked.status !== 0 || untracked.status !== 0) {
    throw new Error('Unable to inspect changed files.');
  }
  return [...tracked.stdout.split('\n'), ...untracked.stdout.split('\n')]
    .map((file) => file.trim())
    .filter(Boolean)
    .map((file) => resolve(root, file));
}

function extension(file) {
  const lastDot = file.lastIndexOf('.');
  return lastDot < 0 ? '' : file.slice(lastDot);
}

function checkWhitespace(root) {
  run('git', ['diff', '--check', 'HEAD'], root);
}

function checkJava(files) {
  const failures = [];
  for (const file of files.filter((item) => item.endsWith('.java'))) {
    const lines = readFileSync(file, 'utf8').split('\n');
    lines.forEach((line, index) => {
      const lineNumber = index + 1;
      const indent = line.match(/^[ \t]*/)[0];
      if (indent.includes('\t')) {
        failures.push(`${file}:${lineNumber}: use spaces instead of tabs.`);
      }
      if (indent.replaceAll('\t', '').length % 4 !== 0) {
        failures.push(`${file}:${lineNumber}: Java indentation must be a multiple of 4 spaces.`);
      }
      if (line.length > JAVA_MAX_LINE_LENGTH) {
        failures.push(`${file}:${lineNumber}: line exceeds ${JAVA_MAX_LINE_LENGTH} characters.`);
      }
    });
  }
  if (failures.length > 0) {
    throw new Error(`Java formatting failed.\n${failures.slice(0, 50).join('\n')}`);
  }
}

function checkPrettier(root, files) {
  const prettierFiles = files.filter((file) => PRETTIER_EXTENSIONS.has(extension(file)));
  if (prettierFiles.length === 0) {
    return;
  }
  const prettier = resolve(root, 'frontend', 'node_modules', '.bin', 'prettier');
  if (!existsSync(prettier)) {
    throw new Error('Prettier is required to validate frontend formatting. Run npm install in frontend/.');
  }
  run(prettier, ['--check', ...prettierFiles], root);
}

function checkPython(root, files) {
  const pythonFiles = files.filter((file) => file.endsWith('.py'));
  if (pythonFiles.length === 0) {
    return;
  }
  const result = spawnSync('ruff', ['format', '--check', ...pythonFiles], {
    cwd: root,
    encoding: 'utf8',
    env: process.env,
  });
  if (result.error?.code === 'ENOENT') {
    throw new Error('Ruff is required to validate Python formatting. Install Ruff before changing Python files.');
  }
  if (result.status !== 0) {
    const output = `${result.stdout}\n${result.stderr}`.trim();
    throw new Error(`ruff format --check failed.\n${output.slice(-6000)}`);
  }
}

function verifyFormatting() {
  const root = repositoryRoot();
  const files = changedFiles(root);
  checkWhitespace(root);
  checkJava(files);
  checkPrettier(root, files);
  checkPython(root, files);
  return {
    checkedFiles: files.length,
    java: files.filter((file) => file.endsWith('.java')).length,
    prettier: files.filter((file) => PRETTIER_EXTENSIONS.has(extension(file))).length,
    python: files.filter((file) => file.endsWith('.py')).length,
  };
}

function toolResult(value) {
  return { content: [{ type: 'text', text: JSON.stringify(value, null, 2) }] };
}

function respond(message) {
  process.stdout.write(`${JSON.stringify(message)}\n`);
}

const tool = {
  name: 'verify_formatting',
  description: 'Checks indentation and formatting for changed Java, frontend, and Python files. Call before completing code changes.',
  inputSchema: { type: 'object', properties: {}, additionalProperties: false },
};

if (process.argv.includes('--stop-hook')) {
  try {
    verifyFormatting();
    process.exit(0);
  } catch (error) {
    process.stderr.write(`Formatting gate failed: ${error.message}\n`);
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
    if (!line) {
      continue;
    }
    const request = JSON.parse(line);
    if (request.method === 'initialize') {
      respond({
        jsonrpc: '2.0',
        id: request.id,
        result: {
          protocolVersion: request.params?.protocolVersion ?? '2025-03-26',
          capabilities: { tools: {} },
          serverInfo: { name: 'sisdent-formatting-gate', version: '1.0.0' },
          instructions: 'Before completing code changes, call verify_formatting and fix any reported indentation or formatting issue.',
        },
      });
    } else if (request.method === 'notifications/initialized') {
      // MCP notification; no response required.
    } else if (request.method === 'tools/list') {
      respond({ jsonrpc: '2.0', id: request.id, result: { tools: [tool] } });
    } else if (request.method === 'tools/call' && request.params?.name === tool.name) {
      try {
        respond({ jsonrpc: '2.0', id: request.id, result: toolResult(verifyFormatting()) });
      } catch (error) {
        respond({
          jsonrpc: '2.0',
          id: request.id,
          result: { ...toolResult({ error: error.message }), isError: true },
        });
      }
    } else {
      respond({
        jsonrpc: '2.0',
        id: request.id,
        error: { code: -32601, message: 'Method not found' },
      });
    }
  }
});
