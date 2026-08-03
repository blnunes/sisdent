import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatListModule } from '@angular/material/list';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Permission, Role, User, UserWrite } from '../../core/models';
import { UserApiService } from '../../core/user-api.service';
import { AppHeaderComponent } from '../../shared/app-header.component';
import { ModuleNavigationComponent } from '../../shared/module-navigation.component';
import { TableQueryService } from '../../core/table-query.service';

@Component({
  selector: 'app-users',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatSidenavModule,
    MatListModule,
    MatSortModule,
    TranslatePipe,
    AppHeaderComponent,
    ModuleNavigationComponent,
  ],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss',
})
export class UsersComponent {
  private readonly api = inject(UserApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);
  private readonly tableQuery = inject(TableQueryService);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly page = signal(0);
  readonly pageSize = signal(5);
  readonly totalElements = signal(0);
  readonly sort = signal('id');
  readonly sortDirection = signal<'asc' | 'desc'>('asc');
  readonly dataSource = new MatTableDataSource<User>([]);
  readonly displayedColumns = ['identification', 'role', 'permissions', 'status', 'actions'];

  constructor() {
    this.load();
    this.dataSource.filterPredicate = (user, filter) =>
      `${user.identificationNumber} ${user.identificationType} ${user.role}`
        .toLowerCase()
        .includes(filter);
  }

  load(): void {
    this.loading.set(true);
    this.api.list({ page: this.page(), size: this.pageSize(), sort: this.sort(), direction: this.sortDirection() }).subscribe({
      next: (response) => {
        this.dataSource.data = response.content;
        this.totalElements.set(response.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.translate.instant('USERS.LOAD_ERROR'));
        this.loading.set(false);
      },
    });
  }

  changePage(event: PageEvent): void {
    this.page.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  changeSort(change: Sort): void {
    const next = this.tableQuery.nextSort({ page: this.page(), size: this.pageSize(), sort: this.sort(), direction: this.sortDirection() }, change);
    this.page.set(next.page); this.sort.set(next.sort); this.sortDirection.set(next.direction);
    this.load();
  }

  filter(value: string): void {
    this.dataSource.filter = value.trim().toLowerCase();
  }

  create(): void {
    this.dialog
      .open<UserFormDialog, UserFormData, UserWrite>(UserFormDialog, {
        width: '560px',
        maxWidth: '94vw',
        data: { mode: 'create' },
      })
      .afterClosed()
      .subscribe((request) => {
        if (!request) return;
        this.api.create(request).subscribe({
          next: () => this.changed('USERS.CREATE_SUCCESS'),
          error: () => this.failed('USERS.CREATE_ERROR'),
        });
      });
  }

  edit(user: User): void {
    this.dialog
      .open<UserFormDialog, UserFormData, UserWrite>(UserFormDialog, {
        width: '560px',
        maxWidth: '94vw',
        data: { mode: 'edit', user },
      })
      .afterClosed()
      .subscribe((request) => {
        if (!request) return;
        this.api.update(user.id, request).subscribe({
          next: () => this.changed('USERS.UPDATE_SUCCESS'),
          error: () => this.failed('USERS.UPDATE_ERROR'),
        });
      });
  }

  permissions(user: User): void {
    this.dialog
      .open<PermissionDialog, User, Permission[]>(PermissionDialog, {
        width: '500px',
        maxWidth: '94vw',
        data: user,
      })
      .afterClosed()
      .subscribe((permissions) => {
        if (!permissions) return;
        this.api.updatePermissions(user.id, permissions).subscribe({
          next: () => this.changed('USERS.PERMISSIONS_SUCCESS'),
          error: () => this.failed('USERS.PERMISSIONS_ERROR'),
        });
      });
  }

  remove(user: User): void {
    this.dialog
      .open<ConfirmDialog, User, boolean>(ConfirmDialog, {
        width: '440px',
        maxWidth: '94vw',
        data: user,
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) return;
        this.api.delete(user.id).subscribe({
          next: () => this.changed('USERS.DEACTIVATE_SUCCESS'),
          error: () => this.failed('USERS.DEACTIVATE_ERROR'),
        });
      });
  }

  roleLabel(role: Role): string {
    return this.translate.instant(
      `USERS.${{ ADMIN: 'ADMIN', MANAGER: 'MANAGER', USER: 'VIEWER' }[role]}`,
    );
  }

  permissionLabel(permission: Permission): string {
    return this.translate.instant(`PERMISSIONS.${permission}`);
  }

  private changed(message: string): void {
    this.snackBar.open(this.translate.instant(message), this.translate.instant('USERS.CLOSE'), {
      duration: 3500,
    });
    this.load();
  }

  private failed(message: string): void {
    this.snackBar.open(this.translate.instant(message), this.translate.instant('USERS.CLOSE'), {
      duration: 5000,
      panelClass: 'error-snackbar',
    });
  }
}

interface UserFormData {
  mode: 'create' | 'edit';
  user?: User;
}

@Component({
  selector: 'app-user-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    TranslatePipe,
  ],
  templateUrl: './user-form-dialog.component.html',
  styleUrl: './user-form-dialog.component.scss',
})
export class UserFormDialog {
  readonly data = inject<UserFormData>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<UserFormDialog, UserWrite>);
  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.nonNullable.group({
    identificationType: [this.data.user?.identificationType ?? 'NATIONAL_ID', Validators.required],
    identificationNumber: [this.data.user?.identificationNumber ?? '', Validators.required],
    password: [
      '',
      this.data.mode === 'create'
        ? [Validators.required, Validators.minLength(8)]
        : [Validators.minLength(8)],
    ],
    role: [this.data.user?.role ?? 'USER', Validators.required],
  });
  readonly rolePermissions: Record<Role, Permission[]> = {
    ADMIN: [
      'READ_USERS', 'MAINTAIN_USERS', 'READ_PATIENTS', 'MAINTAIN_PATIENTS',
      'READ_SPECIALITIES', 'MAINTAIN_SPECIALITIES', 'READ_ADDRESSES',
      'MAINTAIN_ADDRESSES', 'READ_COUNTRIES', 'MAINTAIN_COUNTRIES',
      'READ_ADMINISTRATIVE_DIVISIONS', 'MAINTAIN_ADMINISTRATIVE_DIVISIONS',
      'READ_PERMISSIONS', 'MAINTAIN_PERMISSIONS',
    ],
    MANAGER: [
      'READ_PATIENTS', 'MAINTAIN_PATIENTS', 'READ_SPECIALITIES',
      'MAINTAIN_SPECIALITIES', 'READ_ADDRESSES', 'READ_COUNTRIES', 'READ_ADMINISTRATIVE_DIVISIONS',
    ],
    USER: ['READ_PATIENTS', 'READ_SPECIALITIES', 'READ_ADDRESSES', 'READ_COUNTRIES', 'READ_ADMINISTRATIVE_DIVISIONS'],
  };

  permissionLabel(permission: Permission): string {
    return `PERMISSIONS.${permission}`;
  }

  save(): void {
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    this.ref.close({
      identificationType: value.identificationType,
      identificationNumber: value.identificationNumber,
      password: value.password || undefined,
      role: value.role,
    } as UserWrite);
  }
}

@Component({
  selector: 'app-permission-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatIconModule,
    TranslatePipe,
  ],
  templateUrl: './permission-dialog.component.html',
  styleUrl: './permission-dialog.component.scss',
})
export class PermissionDialog {
  readonly user = inject<User>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<PermissionDialog, Permission[]>);
  readonly allPermissions: Permission[] = [
    'READ_USERS',
    'MAINTAIN_USERS',
    'READ_PATIENTS',
    'MAINTAIN_PATIENTS',
    'READ_SPECIALITIES',
    'MAINTAIN_SPECIALITIES',
    'READ_ADDRESSES',
    'MAINTAIN_ADDRESSES',
    'READ_COUNTRIES',
    'MAINTAIN_COUNTRIES',
    'READ_ADMINISTRATIVE_DIVISIONS',
    'MAINTAIN_ADMINISTRATIVE_DIVISIONS',
    'READ_PERMISSIONS',
    'MAINTAIN_PERMISSIONS',
  ];
  readonly selected = signal(new Set(this.user.permissions));
  readonly isAdmin = this.user.role === 'ADMIN';
  readonly labels: Record<Permission, string> = {
    READ_USERS: 'Users · Read',
    MAINTAIN_USERS: 'Users · Maintain',
    READ_PATIENTS: 'Patients · Read',
    MAINTAIN_PATIENTS: 'Patients · Maintain',
    READ_SPECIALITIES: 'Specialities · Read',
    MAINTAIN_SPECIALITIES: 'Specialities · Maintain',
    READ_ADDRESSES: 'Addresses · Read',
    MAINTAIN_ADDRESSES: 'Addresses · Maintain',
    READ_COUNTRIES: 'Countries · Read',
    MAINTAIN_COUNTRIES: 'Countries · Maintain',
    READ_ADMINISTRATIVE_DIVISIONS: 'Administrative divisions · Read',
    MAINTAIN_ADMINISTRATIVE_DIVISIONS: 'Administrative divisions · Maintain',
    READ_PERMISSIONS: 'Permissions · Read',
    MAINTAIN_PERMISSIONS: 'Permissions · Maintain',
  };
  readonly descriptions: Record<Permission, string> = {
    READ_USERS: 'View users',
    MAINTAIN_USERS: 'Create, update, and deactivate users',
    READ_PATIENTS: 'View patients',
    MAINTAIN_PATIENTS: 'Create, update, and deactivate patients',
    READ_SPECIALITIES: 'View specialities',
    MAINTAIN_SPECIALITIES: 'Create, update, and deactivate specialities',
    READ_ADDRESSES: 'View addresses',
    MAINTAIN_ADDRESSES: 'Manage addresses',
    READ_COUNTRIES: 'View countries',
    MAINTAIN_COUNTRIES: 'Manage countries',
    READ_ADMINISTRATIVE_DIVISIONS: 'View administrative divisions',
    MAINTAIN_ADMINISTRATIVE_DIVISIONS: 'Manage administrative divisions',
    READ_PERMISSIONS: 'View permissions',
    MAINTAIN_PERMISSIONS: 'Manage permissions',
  };

  toggle(permission: Permission, checked: boolean): void {
    if (this.isAdmin) {
      return;
    }
    const next = new Set(this.selected());
    checked ? next.add(permission) : next.delete(permission);
    this.selected.set(next);
  }
  save(): void {
    if (this.isAdmin) {
      return;
    }
    this.ref.close([...this.selected()]);
  }
}

@Component({
  selector: 'app-confirm-dialog',
  imports: [MatButtonModule, MatDialogModule, MatIconModule, TranslatePipe],
  templateUrl: './confirm-dialog.component.html',
  styleUrl: './confirm-dialog.component.scss',
})
export class ConfirmDialog {
  readonly user = inject<User>(MAT_DIALOG_DATA);
}
