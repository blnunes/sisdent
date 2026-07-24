export type IdentificationType = 'NATIONAL_ID' | 'PASSPORT';
export type Role = 'ADMIN' | 'MANAGER' | 'USER';
export type Permission = 'CREATE' | 'UPDATE' | 'READ' | 'DELETE';

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
