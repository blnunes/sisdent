import { Component, effect, inject, signal, untracked } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSidenavModule } from '@angular/material/sidenav';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { OrganizationMutationGraphqlService } from '../../core/organization-mutation-graphql.service';
import { OrganizationReadGraphqlService } from '../../core/organization-read-graphql.service';
import { AuthService } from '../../core/auth.service';
import { ClinicUnit } from '../../core/models';
import { AppHeaderComponent } from '../../core/layout/app-header/app-header.component';
import { ModuleNavigationComponent } from '../../core/layout/module-navigation/module-navigation.component';

@Component({
  selector: 'app-clinic-units', standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatIconModule, MatInputModule, MatProgressSpinnerModule, MatSidenavModule, TranslatePipe, AppHeaderComponent, ModuleNavigationComponent],
  templateUrl: './clinic-units.component.html', styleUrl: './organization-workspace.component.scss',
})
export class ClinicUnitsComponent {
  readonly auth = inject(AuthService);
  private readonly mutations = inject(OrganizationMutationGraphqlService);
  private readonly reads = inject(OrganizationReadGraphqlService);
  private readonly forms = inject(FormBuilder); private readonly translate = inject(TranslateService);
  readonly units = signal<ClinicUnit[]>([]); readonly loading = signal(true); readonly error = signal(''); readonly saving = signal(false);
  readonly form = this.forms.nonNullable.group({ name: ['', [Validators.required, Validators.maxLength(255)]] });

  constructor() { effect(() => { this.auth.activeMembership(); untracked(() => this.load()); }); }
  load(): void {
    const membership = this.auth.activeMembership();
    if (!membership || !this.auth.canAdministerOrganization()) return;
    this.loading.set(true); this.error.set('');
    this.reads.listClinicUnits(membership.organizationId).subscribe({ next: units => { this.units.set(units); this.loading.set(false); }, error: () => { this.error.set(this.translate.instant('ORGANIZATION.ERROR.LOAD_UNITS')); this.loading.set(false); } });
  }
  create(): void {
    const membership = this.auth.activeMembership();
    if (!membership || this.form.invalid || this.saving()) return;
    this.saving.set(true);
    this.mutations.createClinicUnit(membership.organizationId, this.form.getRawValue()).subscribe({ next: () => { this.form.reset(); this.saving.set(false); this.load(); }, error: () => { this.error.set(this.translate.instant('ORGANIZATION.ERROR.CREATE_UNIT')); this.saving.set(false); } });
  }
}
