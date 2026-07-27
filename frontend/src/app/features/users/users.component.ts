import { Component, inject, signal, ViewChild } from '@angular/core';
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
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../core/auth.service';
import { Permission, Role, User, UserWrite } from '../../core/models';
import { UserApiService } from '../../core/user-api.service';
import { LanguageSelectorComponent } from '../../shared/language-selector.component';
import { ThemeToggleComponent } from '../../shared/theme-toggle.component';

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
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    TranslatePipe,
    LanguageSelectorComponent,
    ThemeToggleComponent,
  ],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss',
})
export class UsersComponent {
  private readonly api = inject(UserApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);
  readonly auth = inject(AuthService);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly dataSource = new MatTableDataSource<User>([]);
  readonly displayedColumns = ['identification', 'role', 'permissions', 'status', 'actions'];

  @ViewChild(MatPaginator) set paginator(paginator: MatPaginator) {
    this.dataSource.paginator = paginator;
  }

  constructor() {
    this.load();
    this.dataSource.filterPredicate = (user, filter) =>
      `${user.identificationNumber} ${user.identificationType} ${user.role}`
        .toLowerCase()
        .includes(filter);
  }

  load(): void {
    this.loading.set(true);
    this.api.list().subscribe({
      next: (users) => {
        this.dataSource.data = users;
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.translate.instant('USERS.LOAD_ERROR'));
        this.loading.set(false);
      },
    });
  }

  filter(value: string): void {
    this.dataSource.filter = value.trim().toLowerCase();
    this.dataSource.paginator?.firstPage();
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
    'READ_STATES',
    'MAINTAIN_STATES',
  ];
  readonly selected = signal(new Set(this.user.permissions));
  readonly isAdmin = this.user.role === 'ADMIN';
  readonly labels: Record<Permission, string> = {
    READ_USERS: 'Utilizadores · Ler',
    MAINTAIN_USERS: 'Utilizadores · Manter',
    READ_PATIENTS: 'Pacientes · Ler',
    MAINTAIN_PATIENTS: 'Pacientes · Manter',
    READ_SPECIALITIES: 'Especialidades · Ler',
    MAINTAIN_SPECIALITIES: 'Especialidades · Manter',
    READ_ADDRESSES: 'Endereços · Ler',
    MAINTAIN_ADDRESSES: 'Endereços · Manter',
    READ_COUNTRIES: 'Países · Ler',
    MAINTAIN_COUNTRIES: 'Países · Manter',
    READ_STATES: 'Estados · Ler',
    MAINTAIN_STATES: 'Estados · Manter',
  };
  readonly descriptions: Record<Permission, string> = {
    READ_USERS: 'Consultar utilizadores',
    MAINTAIN_USERS: 'Criar, alterar e remover utilizadores',
    READ_PATIENTS: 'Consultar pacientes',
    MAINTAIN_PATIENTS: 'Criar, alterar e remover pacientes',
    READ_SPECIALITIES: 'Consultar especialidades',
    MAINTAIN_SPECIALITIES: 'Criar, alterar e remover especialidades',
    READ_ADDRESSES: 'Consultar endereços',
    MAINTAIN_ADDRESSES: 'Gerir endereços',
    READ_COUNTRIES: 'Consultar países',
    MAINTAIN_COUNTRIES: 'Gerir países',
    READ_STATES: 'Consultar estados',
    MAINTAIN_STATES: 'Gerir estados',
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
