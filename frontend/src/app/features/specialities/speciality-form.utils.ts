export type ProcedureOption = {
  id?: number;
  name: string;
  displayName?: string;
  translations?: Readonly<Record<string, string>>;
};
export function parseProcedures(value: string): ProcedureOption[] {
  try {
    const procedures = JSON.parse(value) as unknown;
    if (!Array.isArray(procedures)) return [];
    return procedures.flatMap((procedure) => {
      if (!procedure || typeof procedure !== 'object') return [];
      const { id, name, displayName, translations } = procedure as Record<string, unknown>;
      const normalizedName = String(name ?? '').trim();
      if (!normalizedName) return [];
      return [
        {
          ...(typeof id === 'number' ? { id } : {}),
          name: normalizedName,
          ...(typeof displayName === 'string' ? { displayName } : {}),
          ...(translations && typeof translations === 'object'
            ? { translations: translations as Record<string, string> }
            : {}),
        },
      ];
    });
  } catch {
    return [];
  }
}
