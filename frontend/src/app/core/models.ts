export type PatientDocumentType = 'NATIONAL_ID_CARD' | 'PASSPORT';
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
  email: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface JwtPayload {
  sub: string;
  accountId: string;
  email: string;
  platformAdministrator: boolean;
  memberships: Membership[];
  authorities: string[];
  exp: number;
}

export type MembershipRole = 'ORGANIZATION_ADMIN' | 'MANAGER' | 'READ_ONLY' | 'PRACTITIONER_MANAGER' | 'APPOINTMENT_MANAGER' | 'APPOINTMENT_READER' | 'CLINICAL_READER' | 'CLINICAL_AUTHOR' | 'CLINICAL_MANAGER';

export interface Practitioner { globalId: string; displayName: string; registrationNumber?: string; accountId?: string; active: boolean; specialityIds: number[]; }
export interface Appointment { globalId: string; clinicUnitId: string; patientId: string; patientName: string; practitionerId: string; practitionerName: string; startAt: string; endAt: string; schedulingTimezone: string; status: 'SCHEDULED' | 'CANCELLED' | 'COMPLETED' | 'NO_SHOW'; }
export interface ClinicalEncounter { globalId: string; clinicUnitId: string; patientId: string; appointmentId?: string; practitionerId?: string; careAt: string; careTimezone: string; narrative: string; administrativeNote?: string; status: 'DRAFT' | 'FINAL'; finalizedAt?: string; originalEncounterId?: string; amendmentReason?: string; version: number; }
export interface OdontogramFinding { globalId: string; clinicUnitId: string; patientId: string; replacementForId?: string; toothCode: string; surface: string; condition: string; observedAt: string; clinicalNote?: string; voidedAt?: string; voidReason?: string; version: number; }

export interface Membership {
  id: string;
  organizationId: string;
  organizationName: string;
  clinicUnitId?: string;
  clinicUnitName?: string;
  role: MembershipRole;
  version: number;
}

export interface OrganizationOption {
  id: string;
  name: string;
  active: boolean;
}

export interface ClinicUnit {
  id: string;
  organizationId: string;
  name: string;
  active: boolean;
}

export interface Session {
  accountId: string;
  email: string;
  displayName: string;
  platformAdministrator: boolean;
  accountManagementOrganizationId?: string;
  memberships: Membership[];
}

export interface CurrentAccountSettings { id: string; displayName: string; email: string; version: number; }

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AccountSummary {
  id: string; displayName: string; email: string; active: boolean; emailVerified: boolean;
  platformAdministrator: boolean; version: number; memberships: Membership[];
}
