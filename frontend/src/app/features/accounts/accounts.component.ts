import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSidenavModule } from '@angular/material/sidenav';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AccountSummary } from '../../core/models';
import { AccountApiService } from '../../core/account-api.service';
import { AppHeaderComponent } from '../../shared/app-header.component';
import { ModuleNavigationComponent } from '../../shared/module-navigation.component';
import { AuthService } from '../../core/auth.service';
import { ClinicUnit, OrganizationOption } from '../../core/models';
import { forkJoin } from 'rxjs';

@Component({ selector: 'app-accounts', standalone: true, imports: [ReactiveFormsModule, MatButtonModule, MatCardModule, MatChipsModule, MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatProgressSpinnerModule, MatSidenavModule, TranslatePipe, AppHeaderComponent, ModuleNavigationComponent], templateUrl: './accounts.component.html', styleUrl: './accounts.component.scss' })
export class AccountsComponent {
  private readonly api = inject(AccountApiService); private readonly translate = inject(TranslateService); private readonly forms = inject(FormBuilder); private readonly dialog = inject(MatDialog); readonly auth = inject(AuthService);
  readonly loading = signal(true); readonly error = signal(''); readonly accounts = signal<AccountSummary[]>([]); readonly manageableOrganizations = signal<OrganizationOption[]>([]);
  readonly creating = signal(false);
  readonly createForm = this.forms.nonNullable.group({ displayName: ['', [Validators.required, Validators.maxLength(255)]], email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]], password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]] });
  constructor() { this.load(); }
  load(): void { this.loading.set(true); this.error.set(''); if (this.auth.isPlatformAdministrator()) { forkJoin({ page: this.api.listPlatform(), organizations: this.api.listPlatformOrganizations() }).subscribe({ next: ({ page, organizations }) => { this.accounts.set(page.content); this.manageableOrganizations.set(organizations); this.loading.set(false); }, error: () => { this.error.set(this.translate.instant('ACCOUNTS.LOAD_ERROR')); this.loading.set(false); } }); return; } const organizations = [...new Map((this.auth.session()?.memberships ?? []).filter(membership => membership.role === 'ORGANIZATION_ADMIN' && !membership.clinicUnitId).map(membership => [membership.organizationId, { id: membership.organizationId, name: membership.organizationName, active: true }])).values()]; this.manageableOrganizations.set(organizations); const activeOrganizationId = this.auth.activeMembership()?.organizationId; const organization = organizations.find(item => item.id === activeOrganizationId) ?? organizations[0]; if (!organization) { this.error.set(this.translate.instant('ACCOUNTS.NO_MANAGEMENT_ACCESS')); this.loading.set(false); return; } this.api.listOrganization(organization.id).subscribe({ next: page => { this.accounts.set(page.content); this.loading.set(false); }, error: () => { this.error.set(this.translate.instant('ACCOUNTS.LOAD_ERROR')); this.loading.set(false); } }); }
  lifecycle(account: AccountSummary): void { this.api.changeLifecycle(account, !account.active).subscribe({ next: () => this.load(), error: () => this.error.set(this.translate.instant('ACCOUNTS.CHANGE_ERROR')) }); }
  platformAdministration(account: AccountSummary): void { this.api.changePlatformAdministrator(account, !account.platformAdministrator).subscribe({ next: () => this.load(), error: () => this.error.set(this.translate.instant('ACCOUNTS.PLATFORM_ADMIN_ERROR')) }); }
  create(): void { if (this.createForm.invalid) return; this.api.create(this.createForm.getRawValue()).subscribe({ next: () => { this.createForm.reset(); this.creating.set(false); this.load(); }, error: () => this.error.set(this.translate.instant('ACCOUNTS.CREATE_ERROR')) }); }
  openAccess(account: AccountSummary): void { this.dialog.open(AccessDialog, { width: '680px', maxWidth: '94vw', data: { account, organizations: this.manageableOrganizations() } }).afterClosed().subscribe(changed => { if (changed) this.load(); }); }
  roleLabel(role: string): string { return this.translate.instant(`ACCOUNTS.ROLES.${role}`); }
}

interface AccessDialogData { account: AccountSummary; organizations: OrganizationOption[]; }
@Component({ selector: 'app-access-dialog', standalone: true, imports: [ReactiveFormsModule, MatButtonModule, MatChipsModule, MatDialogModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule, TranslatePipe], templateUrl: './access-dialog.component.html', styleUrl: './access-dialog.component.scss' })
export class AccessDialog {
  readonly data = inject<AccessDialogData>(MAT_DIALOG_DATA); private readonly ref = inject(MatDialogRef<AccessDialog, boolean>); private readonly api = inject(AccountApiService); private readonly forms = inject(FormBuilder); private readonly translate = inject(TranslateService);
  readonly saving = signal(false); readonly error = signal('');
  readonly organizationId = signal(this.data.organizations[0]?.id ?? ''); readonly clinicUnitId = signal<string | null>(null); readonly clinicUnits = signal<ClinicUnit[]>([]);
  readonly scopedMembership = computed(() => this.data.account.memberships.find(membership => membership.organizationId === this.organizationId() && (membership.clinicUnitId ?? null) === this.clinicUnitId()));
  readonly form = this.forms.nonNullable.group({ role: ['READ_ONLY', Validators.required] });
  readonly isOrganizationAdmin = computed(() => this.form.controls.role.value === 'ORGANIZATION_ADMIN');
  readonly roleOptions = computed(() => ['READ_ONLY','MANAGER','PRACTITIONER_MANAGER','APPOINTMENT_MANAGER','APPOINTMENT_READER','CLINICAL_READER','CLINICAL_AUTHOR','CLINICAL_MANAGER','ORGANIZATION_ADMIN'].filter(role => !this.clinicUnitId() || role !== 'ORGANIZATION_ADMIN'));
  constructor() { this.loadClinicUnits(); this.syncRole(); this.form.controls.role.valueChanges.subscribe(role => { if (role === 'ORGANIZATION_ADMIN' && this.clinicUnitId()) this.selectClinicUnit(''); }); }
  selectOrganization(organizationId: string): void { this.organizationId.set(organizationId); this.clinicUnitId.set(null); this.loadClinicUnits(); this.syncRole(); }
  selectClinicUnit(clinicUnitId: string): void { this.clinicUnitId.set(clinicUnitId || null); this.syncRole(); }
  private loadClinicUnits(): void { const organizationId = this.organizationId(); if (!organizationId) return; this.api.listClinicUnits(organizationId).subscribe({ next: units => this.clinicUnits.set(units), error: () => { this.clinicUnits.set([]); this.error.set(this.translate.instant('ACCOUNTS.LOAD_ERROR')); } }); }
  private syncRole(): void { this.form.controls.role.setValue(this.scopedMembership()?.role ?? 'READ_ONLY', { emitEvent: false }); }
  save(): void { const organizationId = this.organizationId(); if (!organizationId || this.form.invalid) return; this.saving.set(true); const membership = this.scopedMembership(); const request = membership ? this.api.changeMembershipRole(organizationId, membership.id, { ...this.form.getRawValue(), version: membership.version }) : this.api.grantMembership(organizationId, { email: this.data.account.email, clinicUnitId: this.clinicUnitId(), ...this.form.getRawValue() }); request.subscribe({ next: () => this.ref.close(true), error: (response: HttpErrorResponse) => { this.error.set(this.errorMessage(response)); this.saving.set(false); } }); }
  private errorMessage(response: HttpErrorResponse): string { if (response.status === 403) return this.translate.instant('ACCOUNTS.ACCESS_DENIED'); if (response.status === 409) return this.translate.instant('ACCOUNTS.MEMBERSHIP_EXISTS'); if (response.status === 404) return this.translate.instant('ACCOUNTS.ACCOUNT_OR_SCOPE_NOT_FOUND'); return this.translate.instant('ACCOUNTS.GRANT_ERROR'); }
  close(): void { this.ref.close(false); }
  roleLabel(role: string): string { return this.translate.instant(`ACCOUNTS.ROLES.${role}`); }
}
