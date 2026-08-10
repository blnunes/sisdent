import { signal } from '@angular/core';

const UNAVAILABLE_KEY = 'sisdent.system-unavailable';
const unavailableState = signal(sessionStorage.getItem(UNAVAILABLE_KEY) === 'true');

export const systemUnavailable = unavailableState.asReadonly();
export function markSystemUnavailable(): void { sessionStorage.setItem(UNAVAILABLE_KEY, 'true'); unavailableState.set(true); }
export function clearSystemUnavailable(): void { sessionStorage.removeItem(UNAVAILABLE_KEY); unavailableState.set(false); }
export function wasSystemUnavailable(): boolean { return unavailableState(); }
