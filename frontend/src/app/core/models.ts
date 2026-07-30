export type IdentificationType = 'NATIONAL_ID' | 'PASSPORT';
export type PatientDocumentType = 'NATIONAL_ID_CARD' | 'PASSPORT';
export type Role = 'ADMIN' | 'MANAGER' | 'USER';
export type Permission =
  | 'READ_USERS'
  | 'MAINTAIN_USERS'
  | 'READ_PATIENTS'
  | 'MAINTAIN_PATIENTS'
  | 'READ_SPECIALITIES'
  | 'MAINTAIN_SPECIALITIES'
  | 'READ_ADDRESSES'
  | 'MAINTAIN_ADDRESSES'
  | 'READ_COUNTRIES'
  | 'MAINTAIN_COUNTRIES'
  | 'READ_ADMINISTRATIVE_DIVISIONS'
  | 'MAINTAIN_ADMINISTRATIVE_DIVISIONS'
  | 'READ_PERMISSIONS'
  | 'MAINTAIN_PERMISSIONS';

export interface LoginRequest {
  identificationType: IdentificationType;
  identificationNumber: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface User {
  id: number;
  identificationType: IdentificationType;
  identificationNumber: string;
  role: Role;
  permissions: Permission[];
  active: boolean;
}

export interface UserWrite {
  identificationType: IdentificationType;
  identificationNumber: string;
  password?: string;
  role: Role;
}

export interface JwtPayload {
  sub: string;
  userId: number;
  authorities: string[];
  exp: number;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
