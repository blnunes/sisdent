/// <reference types="vite/client" />

describe('shared import boundary', () => {
  const sources = import.meta.glob('./shared/**/*.ts', { query: '?raw', import: 'default', eager: true }) as Record<string, string>;

  it('forbids shared code from importing feature code', () => {
    const violations = Object.entries(sources).filter(([, source]) => /from\s+['"][^'"]*features\//.test(source));
    expect(violations.map(([path]) => path)).toEqual([]);
  });

  it('keeps shared components free from routing and HTTP orchestration', () => {
    const violations = Object.entries(sources).filter(([, source]) => /ActivatedRoute|HttpClient/.test(source));
    expect(violations.map(([path]) => path)).toEqual([]);
  });
});
