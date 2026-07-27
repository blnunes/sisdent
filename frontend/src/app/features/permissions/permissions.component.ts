import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { HttpErrorResponse } from '@angular/common/http';
import { User, Permission } from '../../core/models';
import { UserApiService } from '../../core/user-api.service';
import { AppHeaderComponent } from '../../shared/app-header.component';
import { ModuleNavigationComponent } from '../../shared/module-navigation.component';
import { AuthService } from '../../core/auth.service';

type PermissionGroup = { key: string; permissions: Permission[] };

@Component({
  selector: 'app-permissions',
  imports: [MatButtonModule, MatCardModule, MatCheckboxModule, MatExpansionModule, MatFormFieldModule,
    MatIconModule, MatInputModule, MatListModule, MatProgressSpinnerModule, MatSidenavModule,
    MatSnackBarModule, TranslatePipe, AppHeaderComponent, ModuleNavigationComponent],
  templateUrl: './permissions.component.html',
  styleUrl: './permissions.component.scss',
})
export class PermissionsComponent {
  private readonly api = inject(UserApiService);
  private readonly snack = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);
  readonly auth = inject(AuthService);
  readonly loading = signal(true); readonly saving = signal(false); readonly error = signal(false);
  readonly users = signal<User[]>([]); readonly selectedId = signal<number | null>(null);
  readonly search = signal(''); readonly revision = signal(0);
  readonly drafts = new Map<number, Set<Permission>>();
  readonly groups: PermissionGroup[] = [
    { key: 'USERS', permissions: ['READ_USERS', 'MAINTAIN_USERS'] },
    { key: 'PATIENTS', permissions: ['READ_PATIENTS', 'MAINTAIN_PATIENTS'] },
    { key: 'SPECIALITIES', permissions: ['READ_SPECIALITIES', 'MAINTAIN_SPECIALITIES'] },
    { key: 'ADDRESSES', permissions: ['READ_ADDRESSES', 'MAINTAIN_ADDRESSES'] },
    { key: 'COUNTRIES', permissions: ['READ_COUNTRIES', 'MAINTAIN_COUNTRIES'] },
    { key: 'STATES', permissions: ['READ_STATES', 'MAINTAIN_STATES'] },
    { key: 'PERMISSIONS', permissions: ['READ_PERMISSIONS', 'MAINTAIN_PERMISSIONS'] },
  ];
  readonly filteredGroups = computed(() => { this.revision(); const q = this.search().trim().toLowerCase();
    return this.groups.map(g => ({ ...g, permissions: g.permissions.filter(p => !q || this.permissionLabel(p).toLowerCase().includes(q)) })).filter(g => g.permissions.length); });
  readonly selected = computed(() => this.users().find(u => u.id === this.selectedId()) ?? null);
  readonly canManage = computed(() => this.auth.hasPermission('MAINTAIN_PERMISSIONS'));
  readonly hasChanges = computed(() => { this.revision(); const u = this.selected(); return !!u && !this.same(u.permissions, this.drafts.get(u.id)); });
  readonly changeCount = computed(() => { this.revision(); const u = this.selected(); if (!u) return 0; const draft = this.drafts.get(u.id) ?? new Set(); return new Set([...u.permissions, ...draft].filter(p => u.permissions.includes(p) !== draft.has(p))).size; });
  constructor() { this.load(); }
  load(): void { this.loading.set(true); this.api.list().subscribe({ next: users => { this.users.set(users); users.forEach(u => this.drafts.set(u.id, new Set(u.permissions))); if (!this.selectedId() && users.length) this.selectedId.set(users[0].id); this.loading.set(false); }, error: () => { this.error.set(true); this.loading.set(false); } }); }
  select(user: User): void { if (this.hasChanges()) { this.snack.open(this.translate.instant('PERMISSIONS_PAGE.UNSAVED_WARNING'), this.translate.instant('USERS.CLOSE'), { duration: 4500 }); return; } this.selectedId.set(user.id); }
  has(permission: Permission): boolean { this.revision(); return this.drafts.get(this.selectedId() ?? -1)?.has(permission) ?? false; }
  toggle(permission: Permission, checked: boolean): void { const set = this.drafts.get(this.selectedId() ?? -1); if (!set) return; checked ? set.add(permission) : set.delete(permission); this.revision.update(v => v + 1); }
  groupState(group: PermissionGroup): { checked: boolean; indeterminate: boolean } { const active = group.permissions.filter(p => this.has(p)).length; return { checked: active === group.permissions.length, indeterminate: active > 0 && active < group.permissions.length }; }
  toggleGroup(group: PermissionGroup, checked: boolean): void { group.permissions.forEach(p => this.toggle(p, checked)); }
  discard(): void { const u = this.selected(); if (u) { this.drafts.set(u.id, new Set(u.permissions)); this.revision.update(v => v + 1); } }
  save(): void { const u = this.selected(); if (!u || !this.canManage() || u.role === 'ADMIN' || !this.hasChanges()) return; this.saving.set(true); this.api.updatePermissions(u.id, [...(this.drafts.get(u.id) ?? [])]).subscribe({ next: () => { this.saving.set(false); this.snack.open(this.translate.instant('PERMISSIONS_PAGE.SAVE_SUCCESS'), this.translate.instant('USERS.CLOSE'), { duration: 3500 }); this.load(); }, error: (error: HttpErrorResponse) => { this.saving.set(false); const detail = typeof error.error?.detail === 'string' ? `: ${error.error.detail}` : ''; this.snack.open(`${this.translate.instant('PERMISSIONS_PAGE.SAVE_ERROR')}${detail}`, this.translate.instant('USERS.CLOSE'), { duration: 7000, panelClass: 'error-snackbar' }); } }); }
  roleLabel(role: User['role']): string { return this.translate.instant(`USERS.${role === 'USER' ? 'VIEWER' : role}`); }
  permissionLabel(permission: Permission): string { return this.translate.instant(`PERMISSIONS.${permission}`); }
  groupLabel(key: string): string { return this.translate.instant(`PERMISSION_GROUPS.${key}`); }
  private same(a: Permission[], b?: Set<Permission>): boolean { return a.length === (b?.size ?? 0) && a.every(p => b?.has(p)); }
}
