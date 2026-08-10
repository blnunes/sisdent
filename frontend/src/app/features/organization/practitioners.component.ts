import { Component, effect, inject, signal, untracked } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSidenavModule } from '@angular/material/sidenav';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AccountApiService, PractitionerWrite } from '../../core/account-api.service';
import { AuthService } from '../../core/auth.service';
import { Practitioner } from '../../core/models';
import { AppHeaderComponent } from '../../core/layout/app-header/app-header.component';
import { ModuleNavigationComponent } from '../../core/layout/module-navigation/module-navigation.component';

@Component({ selector: 'app-practitioners', standalone: true, imports: [MatButtonModule, MatCardModule, MatDialogModule, MatIconModule, MatProgressSpinnerModule, MatSidenavModule, TranslatePipe, AppHeaderComponent, ModuleNavigationComponent], templateUrl: './practitioners.component.html', styleUrl: './organization-workspace.component.scss' })
export class PractitionersComponent {
  readonly auth = inject(AuthService); private readonly api = inject(AccountApiService); private readonly dialog = inject(MatDialog); private readonly translate = inject(TranslateService);
  readonly practitioners = signal<Practitioner[]>([]); readonly loading = signal(true); readonly error = signal('');
  constructor() { effect(() => { this.auth.activeMembership(); untracked(() => this.load()); }); }
  load(): void { const membership = this.auth.activeMembership(); if (!membership || !this.auth.canManagePractitioners()) return; this.loading.set(true); this.error.set(''); this.api.listPractitioners(membership.organizationId).subscribe({ next: records => { this.practitioners.set(records); this.loading.set(false); }, error: () => { this.error.set(this.translate.instant('ORGANIZATION.ERROR.LOAD_PRACTITIONERS')); this.loading.set(false); } }); }
  open(practitioner?: Practitioner): void { this.dialog.open(PractitionerDialog, { width: '520px', maxWidth: '94vw', data: practitioner }).afterClosed().subscribe(changed => { if (changed) this.load(); }); }
  deactivate(practitioner: Practitioner): void { const membership = this.auth.activeMembership(); if (!membership || !practitioner.active) return; this.api.deactivatePractitioner(membership.organizationId, practitioner.globalId).subscribe({ next: () => this.load(), error: () => this.error.set(this.translate.instant('ORGANIZATION.ERROR.DEACTIVATE_PRACTITIONER')) }); }
}

@Component({ selector: 'app-practitioner-dialog', standalone: true, imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatIconModule, MatInputModule, TranslatePipe], templateUrl: './practitioner-dialog.component.html' })
export class PractitionerDialog {
  readonly practitioner = inject<Practitioner | undefined>(MAT_DIALOG_DATA); private readonly ref = inject(MatDialogRef<PractitionerDialog, boolean>); private readonly api = inject(AccountApiService); private readonly auth = inject(AuthService); private readonly forms = inject(FormBuilder); private readonly translate = inject(TranslateService); readonly saving = signal(false); readonly error = signal('');
  readonly form = this.forms.nonNullable.group({ displayName: [this.practitioner?.displayName ?? '', [Validators.required, Validators.maxLength(255)]], registrationNumber: [this.practitioner?.registrationNumber ?? '', Validators.maxLength(128)] });
  save(): void { const membership = this.auth.activeMembership(); if (!membership || this.form.invalid || this.saving()) return; this.saving.set(true); const request: PractitionerWrite = { ...this.form.getRawValue(), specialityIds: this.practitioner?.specialityIds ?? [], accountId: this.practitioner?.accountId ?? null }; const response = this.practitioner ? this.api.updatePractitioner(membership.organizationId, this.practitioner.globalId, request) : this.api.createPractitioner(membership.organizationId, request); response.subscribe({ next: () => this.ref.close(true), error: () => { this.error.set(this.translate.instant('ORGANIZATION.ERROR.SAVE_PRACTITIONER')); this.saving.set(false); } }); }
  close(): void { this.ref.close(false); }
}
