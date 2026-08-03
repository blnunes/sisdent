import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSidenavModule } from '@angular/material/sidenav';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AccountSummary } from '../../core/models';
import { AccountApiService } from '../../core/account-api.service';
import { AppHeaderComponent } from '../../shared/app-header.component';
import { ModuleNavigationComponent } from '../../shared/module-navigation.component';
import { AuthService } from '../../core/auth.service';
import { ClinicUnit, OrganizationOption } from '../../core/models';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-accounts',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatCardModule,
    MatChipsModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatPaginatorModule,
    MatSidenavModule,
    TranslatePipe,
    AppHeaderComponent,
    ModuleNavigationComponent,
  ],
  templateUrl: './accounts.component.html',
  styleUrl: './accounts.component.scss',
})
export class AccountsComponent {
  private readonly api = inject(AccountApiService);
  private readonly translate = inject(TranslateService);
  private readonly forms = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  readonly auth = inject(AuthService);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly accounts = signal<AccountSummary[]>([]);
  readonly totalElements = signal(0);
  readonly page = signal(0);
  readonly pageSize = signal(10);
  readonly manageableOrganizations = signal<OrganizationOption[]>([]);
  readonly viewAllAccounts = signal(false);
  readonly creating = signal(false);
  readonly createForm = this.forms.nonNullable.group({
    displayName: ['', [Validators.required, Validators.maxLength(255)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
  });
  constructor() {
    effect(() => {
      this.auth.activeMembership();
      this.auth.isPlatformAdministrator();
      this.viewAllAccounts();
      untracked(() => this.load());
    });
  }
  load(): void {
    this.loading.set(true);
    this.error.set('');
    const query = {
      page: this.page(),
      size: this.pageSize(),
      sort: 'person.displayName',
      direction: 'asc' as const,
    };
    const activeMembership = this.auth.activeMembership();
    if (this.auth.isPlatformAdministrator()) {
      const page =
        this.viewAllAccounts() || !activeMembership
          ? this.api.listPlatform(query)
          : this.api.listOrganization(activeMembership.organizationId, query);
      forkJoin({ page, organizations: this.api.listPlatformOrganizations() }).subscribe({
        next: ({ page, organizations }) => {
          this.accounts.set(page.content);
          this.totalElements.set(page.totalElements);
          this.manageableOrganizations.set(organizations);
          this.loading.set(false);
        },
        error: () => {
          this.error.set(this.translate.instant('ACCOUNTS.LOAD_ERROR'));
          this.loading.set(false);
        },
      });
      return;
    }
    const managementOrganizationId = this.auth.session()?.accountManagementOrganizationId;
    if (
      !activeMembership ||
      activeMembership.organizationId !== managementOrganizationId ||
      activeMembership.role !== 'ORGANIZATION_ADMIN' ||
      activeMembership.clinicUnitId
    ) {
      this.error.set(this.translate.instant('ACCOUNTS.NO_MANAGEMENT_ACCESS'));
      this.loading.set(false);
      return;
    }
    const organization = {
      id: activeMembership.organizationId,
      name: activeMembership.organizationName,
      active: true,
    };
    this.manageableOrganizations.set([organization]);
    this.api.listOrganization(organization.id, query).subscribe({
      next: (page) => {
        this.accounts.set(page.content);
        this.totalElements.set(page.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.translate.instant('ACCOUNTS.LOAD_ERROR'));
        this.loading.set(false);
      },
    });
  }
  changePage(event: PageEvent): void {
    this.page.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }
  lifecycle(account: AccountSummary): void {
    this.api
      .changeLifecycle(account, !account.active)
      .subscribe({
        next: () => this.load(),
        error: () => this.error.set(this.translate.instant('ACCOUNTS.CHANGE_ERROR')),
      });
  }
  platformAdministration(account: AccountSummary): void {
    this.api
      .changePlatformAdministrator(account, !account.platformAdministrator)
      .subscribe({
        next: () => this.load(),
        error: () => this.error.set(this.translate.instant('ACCOUNTS.PLATFORM_ADMIN_ERROR')),
      });
  }
  create(): void {
    if (this.createForm.invalid) return;
    this.api.create(this.createForm.getRawValue()).subscribe({
      next: () => {
        this.createForm.reset();
        this.creating.set(false);
        this.load();
      },
      error: () => this.error.set(this.translate.instant('ACCOUNTS.CREATE_ERROR')),
    });
  }
  openAccess(account: AccountSummary): void {
    this.dialog
      .open(AccessDialog, {
        width: '680px',
        maxWidth: '94vw',
        data: { account, organizations: this.manageableOrganizations() },
      })
      .afterClosed()
      .subscribe((changed) => {
        if (changed) this.load();
      });
  }
  roleLabel(role: string): string {
    return this.translate.instant(`ACCOUNTS.ROLES.${role}`);
  }
}

interface AccessDialogData {
  account: AccountSummary;
  organizations: OrganizationOption[];
}
@Component({
  selector: 'app-access-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatChipsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    TranslatePipe,
  ],
  templateUrl: './access-dialog.component.html',
  styleUrl: './access-dialog.component.scss',
})
export class AccessDialog {
  readonly data = inject<AccessDialogData>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<AccessDialog, boolean>);
  private readonly api = inject(AccountApiService);
  private readonly forms = inject(FormBuilder);
  private readonly translate = inject(TranslateService);
  readonly auth = inject(AuthService);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly organizationId = signal(this.data.organizations[0]?.id ?? '');
  readonly clinicUnitId = signal<string | null>(null);
  readonly clinicUnits = signal<ClinicUnit[]>([]);
  readonly scopedMembership = computed(() =>
    this.data.account.memberships.find(
      (membership) =>
        membership.organizationId === this.organizationId() &&
        (membership.clinicUnitId ?? null) === this.clinicUnitId(),
    ),
  );
  readonly form = this.forms.nonNullable.group({ role: ['READ_ONLY', Validators.required] });
  readonly isOrganizationAdmin = computed(
    () => this.form.controls.role.value === 'ORGANIZATION_ADMIN',
  );
  readonly visibleMemberships = computed(() =>
    this.data.account.memberships.filter((membership) =>
      this.data.organizations.some((organization) => organization.id === membership.organizationId),
    ),
  );
  readonly canManageTarget = computed(
    () => this.auth.isPlatformAdministrator() || !this.data.account.platformAdministrator,
  );
  readonly roleOptions = computed(() =>
    [
      'READ_ONLY',
      'MANAGER',
      'PRACTITIONER_MANAGER',
      'APPOINTMENT_MANAGER',
      'APPOINTMENT_READER',
      'CLINICAL_READER',
      'CLINICAL_AUTHOR',
      'CLINICAL_MANAGER',
      'ORGANIZATION_ADMIN',
    ].filter((role) => !this.clinicUnitId() || role !== 'ORGANIZATION_ADMIN'),
  );
  constructor() {
    this.loadClinicUnits();
    this.syncRole();
    this.form.controls.role.valueChanges.subscribe((role) => {
      if (role === 'ORGANIZATION_ADMIN' && this.clinicUnitId()) this.selectClinicUnit('');
    });
  }
  selectOrganization(organizationId: string): void {
    this.organizationId.set(organizationId);
    this.clinicUnitId.set(null);
    this.loadClinicUnits();
    this.syncRole();
  }
  selectClinicUnit(clinicUnitId: string): void {
    this.clinicUnitId.set(clinicUnitId || null);
    this.syncRole();
  }
  private loadClinicUnits(): void {
    const organizationId = this.organizationId();
    if (!organizationId) return;
    this.api.listClinicUnits(organizationId).subscribe({
      next: (units) => this.clinicUnits.set(units),
      error: () => {
        this.clinicUnits.set([]);
        this.error.set(this.translate.instant('ACCOUNTS.LOAD_ERROR'));
      },
    });
  }
  private syncRole(): void {
    this.form.controls.role.setValue(this.scopedMembership()?.role ?? 'READ_ONLY', {
      emitEvent: false,
    });
  }
  save(): void {
    const organizationId = this.organizationId();
    if (!this.canManageTarget() || !organizationId || this.form.invalid) return;
    this.saving.set(true);
    const membership = this.scopedMembership();
    const request = membership
      ? this.api.changeMembershipRole(organizationId, membership.id, {
          ...this.form.getRawValue(),
          version: membership.version,
        })
      : this.api.grantMembership(organizationId, {
          email: this.data.account.email,
          clinicUnitId: this.clinicUnitId(),
          ...this.form.getRawValue(),
        });
    request.subscribe({
      next: () => this.ref.close(true),
      error: (response: HttpErrorResponse) => {
        this.error.set(this.errorMessage(response));
        this.saving.set(false);
      },
    });
  }
  revoke(membership: import('../../core/models').Membership): void {
    if (
      !this.canManageTarget() ||
      !window.confirm(
        this.translate.instant('ACCOUNTS.REVOKE_CONFIRM', {
          organization: membership.organizationName,
        }),
      )
    )
      return;
    this.saving.set(true);
    this.api
      .revokeMembership(membership.organizationId, membership.id, membership.version)
      .subscribe({
        next: () => this.ref.close(true),
        error: (response: HttpErrorResponse) => {
          this.error.set(this.errorMessage(response));
          this.saving.set(false);
        },
      });
  }
  private errorMessage(response: HttpErrorResponse): string {
    if (response.status === 403) return this.translate.instant('ACCOUNTS.ACCESS_DENIED');
    if (response.status === 409) return this.translate.instant('ACCOUNTS.STALE_MEMBERSHIP');
    if (response.status === 404)
      return this.translate.instant('ACCOUNTS.ACCOUNT_OR_SCOPE_NOT_FOUND');
    return this.translate.instant('ACCOUNTS.GRANT_ERROR');
  }
  close(): void {
    this.ref.close(false);
  }
  roleLabel(role: string): string {
    return this.translate.instant(`ACCOUNTS.ROLES.${role}`);
  }
}
