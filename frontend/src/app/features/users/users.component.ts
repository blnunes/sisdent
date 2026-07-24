import { Component, inject, signal, ViewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';
import { AuthService } from '../../core/auth.service';
import { Permission, Role, User, UserWrite } from '../../core/models';
import { UserApiService } from '../../core/user-api.service';

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
  ],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss',
})
export class UsersComponent {
  private readonly api = inject(UserApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
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
        this.error.set('Não foi possível carregar os utilizadores.');
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
          next: () => this.changed('Utilizador criado com sucesso.'),
          error: () => this.failed('Não foi possível criar o utilizador. Verifique se a identificação já existe.'),
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
          next: () => this.changed('Utilizador atualizado com sucesso.'),
          error: () => this.failed('Não foi possível atualizar o utilizador.'),
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
          next: () => this.changed('Permissões atualizadas.'),
          error: () => this.failed('Não foi possível atualizar as permissões.'),
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
          next: () => this.changed('Utilizador desativado com sucesso.'),
          error: () => this.failed('Não foi possível desativar o utilizador.'),
        });
      });
  }

  roleLabel(role: Role): string {
    return { ADMIN: 'Administrador', MANAGER: 'Gestor', USER: 'Consulta' }[role];
  }

  permissionLabel(permission: Permission): string {
    return { CREATE: 'Criar', UPDATE: 'Alterar', READ: 'Consultar', DELETE: 'Eliminar' }[permission];
  }

  private changed(message: string): void {
    this.snackBar.open(message, 'Fechar', { duration: 3500 });
    this.load();
  }

  private failed(message: string): void {
    this.snackBar.open(message, 'Fechar', { duration: 5000, panelClass: 'error-snackbar' });
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
  ],
  template: `
    <div class="dialog-heading">
      <span class="dialog-icon"><mat-icon>{{ data.mode === 'create' ? 'person_add' : 'edit' }}</mat-icon></span>
      <div>
        <h2 mat-dialog-title>{{ data.mode === 'create' ? 'Novo utilizador' : 'Alterar utilizador' }}</h2>
        <p>{{ data.mode === 'create' ? 'Defina a identificação e o nível de acesso.' : 'Atualize os dados necessários.' }}</p>
      </div>
    </div>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline">
          <mat-label>Tipo de identificação</mat-label>
          <mat-select formControlName="identificationType">
            <mat-option value="NATIONAL_ID">Documento nacional</mat-option>
            <mat-option value="PASSPORT">Passaporte</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Identificação</mat-label>
          <input matInput formControlName="identificationNumber" />
          <mat-hint>Será guardada em maiúsculas, sem espaços ou hífens.</mat-hint>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>{{ data.mode === 'create' ? 'Password' : 'Nova password (opcional)' }}</mat-label>
          <input matInput type="password" formControlName="password" autocomplete="new-password" />
          <mat-hint>Mínimo de 8 caracteres.</mat-hint>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Perfil</mat-label>
          <mat-select formControlName="role">
            <mat-option value="ADMIN">Administrador</mat-option>
            <mat-option value="MANAGER">Gestor</mat-option>
            <mat-option value="USER">Consulta</mat-option>
          </mat-select>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-flat-button class="primary-action" [disabled]="form.invalid" (click)="save()">
        {{ data.mode === 'create' ? 'Criar utilizador' : 'Guardar alterações' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-heading { display:flex; gap:14px; align-items:center; padding:24px 24px 4px; }
    .dialog-heading h2 { padding:0; margin:0; color:#123d4c; font-weight:750; }
    .dialog-heading p { margin:3px 0 0; color:#637b83; }
    .dialog-icon { display:grid; place-items:center; width:46px; height:46px; border-radius:14px; color:#087b79; background:#dcfaf5; }
    .dialog-form { display:grid; gap:5px; padding-top:14px; }
    mat-form-field { width:100%; }
    .primary-action { background:#087b79 !important; color:white !important; }
  `],
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
      this.data.mode === 'create' ? [Validators.required, Validators.minLength(8)] : [Validators.minLength(8)],
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
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatDialogModule, MatIconModule],
  template: `
    <div class="dialog-heading">
      <span class="dialog-icon"><mat-icon>admin_panel_settings</mat-icon></span>
      <div><h2 mat-dialog-title>Gerir permissões</h2><p>{{ user.identificationNumber }} · {{ user.role }}</p></div>
    </div>
    <mat-dialog-content>
      <p class="helper">Escolha as ações permitidas. O perfil define o limite máximo de acesso.</p>
      <div class="permission-grid">
        @for (permission of allPermissions; track permission) {
          <mat-checkbox [checked]="selected().has(permission)" (change)="toggle(permission, $event.checked)">
            <span class="permission-name">{{ labels[permission] }}</span>
            <small>{{ descriptions[permission] }}</small>
          </mat-checkbox>
        }
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-flat-button class="primary-action" (click)="save()">Guardar permissões</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-heading { display:flex; gap:14px; align-items:center; padding:24px 24px 4px; }
    .dialog-heading h2 { padding:0; margin:0; color:#123d4c; font-weight:750; }
    .dialog-heading p,.helper { margin:3px 0 0; color:#637b83; }
    .dialog-icon { display:grid; place-items:center; width:46px; height:46px; border-radius:14px; color:#087b79; background:#dcfaf5; }
    .helper { margin:12px 0 18px; }
    .permission-grid { display:grid; gap:8px; }
    mat-checkbox { padding:10px; border:1px solid #dbe9e9; border-radius:12px; }
    .permission-name, small { display:block; }
    .permission-name { font-weight:700; color:#234a57; }
    small { color:#6b8188; margin-top:2px; }
    .primary-action { background:#087b79 !important; color:white !important; }
  `],
})
export class PermissionDialog {
  readonly user = inject<User>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<PermissionDialog, Permission[]>);
  readonly allPermissions: Permission[] = ['CREATE', 'UPDATE', 'READ', 'DELETE'];
  readonly selected = signal(new Set(this.user.permissions));
  readonly labels: Record<Permission, string> = { CREATE: 'Criar', UPDATE: 'Alterar', READ: 'Consultar', DELETE: 'Eliminar' };
  readonly descriptions: Record<Permission, string> = {
    CREATE: 'Adicionar novos registos',
    UPDATE: 'Editar registos existentes',
    READ: 'Visualizar informação',
    DELETE: 'Efetuar exclusão lógica',
  };

  toggle(permission: Permission, checked: boolean): void {
    const next = new Set(this.selected());
    checked ? next.add(permission) : next.delete(permission);
    this.selected.set(next);
  }
  save(): void { this.ref.close([...this.selected()]); }
}

@Component({
  selector: 'app-confirm-dialog',
  imports: [MatButtonModule, MatDialogModule, MatIconModule],
  template: `
    <div class="confirm-icon"><mat-icon>person_remove</mat-icon></div>
    <h2 mat-dialog-title>Desativar utilizador?</h2>
    <mat-dialog-content>
      <p><strong>{{ user.identificationNumber }}</strong> deixará de conseguir iniciar sessão.</p>
      <p class="helper">O registo será mantido para histórico.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-flat-button color="warn" [mat-dialog-close]="true">Desativar</button>
    </mat-dialog-actions>
  `,
  styles: [`
    :host { display:block; padding-top:22px; }
    .confirm-icon { display:grid; place-items:center; margin:0 auto; width:58px; height:58px; border-radius:18px; background:#fff0f1; color:#b7293e; }
    h2 { text-align:center; color:#173e4b; }
    mat-dialog-content { text-align:center; }
    .helper { color:#6b8188; }
  `],
})
export class ConfirmDialog {
  readonly user = inject<User>(MAT_DIALOG_DATA);
}
