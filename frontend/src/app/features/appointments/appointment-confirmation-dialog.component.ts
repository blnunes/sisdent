import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { TranslatePipe } from '@ngx-translate/core';

export interface AppointmentConfirmationDialogData {
  titleKey: string;
  messageKey: string;
  cancelKey: string;
  confirmKey: string;
}

@Component({
  selector: 'app-appointment-confirmation-dialog',
  imports: [MatButtonModule, MatDialogModule, TranslatePipe],
  template: `
    <h2 mat-dialog-title>{{ data.titleKey | translate }}</h2>
    <mat-dialog-content><p>{{ data.messageKey | translate }}</p></mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" (click)="close(false)">{{ data.cancelKey | translate }}</button>
      <button mat-flat-button type="button" (click)="close(true)">{{ data.confirmKey | translate }}</button>
    </mat-dialog-actions>
  `,
})
export class AppointmentConfirmationDialogComponent {
  readonly data = inject<AppointmentConfirmationDialogData>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<AppointmentConfirmationDialogComponent, boolean>);

  close(confirmed: boolean): void { this.ref.close(confirmed); }
}
