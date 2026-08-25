declare module 'luxon' {
  export class DateTime {
    static now(): DateTime;
    static fromISO(value: string, options?: { zone?: string }): DateTime;
    static fromFormat(value: string, format: string, options?: { zone?: string; setZone?: boolean }): DateTime;
    readonly isValid: boolean;
    setZone(zone: string): DateTime;
    plus(duration: { minutes: number }): DateTime;
    toFormat(format: string): string;
    toJSDate(): Date;
    toUTC(): DateTime;
    toISO(options?: { suppressMilliseconds?: boolean }): string | null;
  }
}
