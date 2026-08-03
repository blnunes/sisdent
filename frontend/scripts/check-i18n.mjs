import { readdir, readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const localesDirectory = fileURLToPath(new URL('../public/i18n/', import.meta.url));
const sourceDirectory = fileURLToPath(new URL('../src/', import.meta.url));

function flatten(value, prefix = '', result = {}) {
  for (const [key, child] of Object.entries(value)) {
    const path = prefix ? `${prefix}.${key}` : key;
    if (child && typeof child === 'object' && !Array.isArray(child)) flatten(child, path, result);
    else result[path] = child;
  }
  return result;
}

async function filesIn(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = await Promise.all(entries.map((entry) => {
    const path = join(directory, entry.name);
    return entry.isDirectory() ? filesIn(path) : [path];
  }));
  return files.flat();
}

const localeFiles = (await filesIn(localesDirectory)).filter((file) => file.endsWith('.json'));
const localeKeys = new Map(await Promise.all(localeFiles.map(async (file) => [
  file,
  flatten(JSON.parse(await readFile(file, 'utf8'))),
])));
const expectedKeys = new Set([...localeKeys.values()].flatMap(Object.keys));
const failures = [];

for (const [file, keys] of localeKeys) {
  const missing = [...expectedKeys].filter((key) => !(key in keys));
  if (missing.length) failures.push(`${file}: missing ${missing.join(', ')}`);
}

const source = (await Promise.all((await filesIn(sourceDirectory))
  .filter((file) => /\.(html|ts)$/.test(file))
  .map((file) => readFile(file, 'utf8')))).join('\n');
const usedKeys = new Set();
const keyExpression = /['"]([A-Z][A-Z0-9_]*(?:\.[A-Z0-9_]+)+)['"]\s*\|\s*translate|(?:instant|get)\(\s*['"]([A-Z][A-Z0-9_]*(?:\.[A-Z0-9_]+)+)['"]/g;
for (const match of source.matchAll(keyExpression)) usedKeys.add(match[1] ?? match[2]);

for (const [file, keys] of localeKeys) {
  const missing = [...usedKeys].filter((key) => !(key in keys));
  if (missing.length) failures.push(`${file}: missing used keys ${missing.join(', ')}`);
}

if (failures.length) {
  console.error(`Translation validation failed:\n${failures.join('\n')}`);
  process.exitCode = 1;
} else {
  console.log(`Translation validation passed for ${localeFiles.length} locales and ${usedKeys.size} used keys.`);
}
