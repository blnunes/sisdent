import { textValue } from './text-value';

describe('textValue', () => {
  it('renders supported primitive values', () => {
    expect(textValue('Lisbon')).toBe('Lisbon');
    expect(textValue(42)).toBe('42');
    expect(textValue(false)).toBe('false');
    expect(textValue(42n)).toBe('42');
  });

  it('uses the explicit fallback for absent or malformed values', () => {
    expect(textValue(undefined, '—')).toBe('—');
    expect(textValue(null, '—')).toBe('—');
    expect(textValue({ name: 'Lisbon' }, '—')).toBe('—');
    expect(textValue(['Lisbon'], '—')).toBe('—');
  });
});
