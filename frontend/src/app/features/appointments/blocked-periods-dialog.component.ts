import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DateTime } from 'luxon';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';
import { AppointmentGraphqlService, BlockedPeriod } from '../../core/appointment-graphql.service';

export interface BlockedPeriodsDialogData { organizationId: string; clinicUnitId: string; timezone: string; practitioners: readonly { globalId: string; displayName: string }[]; from: string; to: string; onFinished: () => void; }
@Component({ selector: 'app-blocked-periods-dialog', imports: [FormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, TranslatePipe], templateUrl: './blocked-periods-dialog.component.html', styles: ['.visually-hidden { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }'] })
export class BlockedPeriodsDialogComponent {
  readonly data = inject<BlockedPeriodsDialogData>(MAT_DIALOG_DATA); private readonly ref = inject(MatDialogRef<BlockedPeriodsDialogComponent>);
  private readonly api = inject(AppointmentGraphqlService); private readonly dialog = inject(MatDialog);
  readonly periods = signal<BlockedPeriod[]>([]); readonly error = signal(false); readonly saving = signal(false);
  start = '';
  end = '';
  practitionerId: string | null = null;
  editing?: BlockedPeriod;
  private baseline = this.formState();
  constructor() { this.load(); this.ref.keydownEvents().subscribe(e => { if (e.key === 'Escape' && !this.saving()) this.close(); }); }
  load(): void { this.api.blockedPeriods(this.data.organizationId, this.data.clinicUnitId, this.data.from, this.data.to).subscribe({ next: x => this.periods.set(x), error: () => this.error.set(true) }); }
  edit(period?: BlockedPeriod): void {
    this.editing = period;
    this.practitionerId = period?.practitionerId ?? null;
    this.start = period ? this.local(period.startAt) : '';
    this.end = period ? this.local(period.endAt) : '';
    this.baseline = this.formState();
  }
  save(): void {
    const start = this.utc(this.start), end = this.utc(this.end); if (!start || !end || end <= start) { this.error.set(true); return; }
    this.saving.set(true); this.error.set(false); const input = { clinicUnitId: this.data.clinicUnitId, practitionerId: this.practitionerId, startAt: start, endAt: end };
    const request = this.editing ? this.api.updateBlockedPeriod(this.data.organizationId, this.editing.globalId, this.editing.version, input) : this.api.createBlockedPeriod(this.data.organizationId, input);
    request.subscribe({ next: () => { this.saving.set(false); this.edit(); this.load(); this.data.onFinished(); }, error: () => { this.saving.set(false); this.error.set(true); this.data.onFinished(); } });
  }
  remove(period: BlockedPeriod): void { this.dialog.open(BlockedPeriodConfirmDialog).afterClosed().subscribe(ok => { if (!ok) return; this.api.deleteBlockedPeriod(this.data.organizationId, this.data.clinicUnitId, period.globalId, period.version).subscribe({ next: () => { this.load(); this.data.onFinished(); }, error: () => { this.error.set(true); this.data.onFinished(); } }); }); }
  close(): void {
    if (!this.isDirty()) { this.ref.close(); return; }
    this.dialog.open(BlockedPeriodDiscardDialog, { autoFocus: 'dialog' }).afterClosed().subscribe(discard => {
      if (discard) this.ref.close();
    });
  }
  periodTime(period: BlockedPeriod): string { return `${this.local(period.startAt)} – ${this.local(period.endAt)}`; }
  private utc(value: string): string | undefined { const date = DateTime.fromFormat(value, "yyyy-LL-dd'T'HH:mm", { zone: this.data.timezone }); return date.isValid ? date.toUTC().toISO() ?? undefined : undefined; }
  private local(value: string): string { return DateTime.fromISO(value, { zone: 'utc' }).setZone(this.data.timezone).toFormat("yyyy-LL-dd'T'HH:mm"); }
  private isDirty(): boolean { return this.formState() !== this.baseline; }
  private formState(): string { return JSON.stringify({ start: this.start, end: this.end, practitionerId: this.practitionerId }); }
}
@Component({ selector: 'app-blocked-period-confirm', imports: [MatDialogModule, MatButtonModule, TranslatePipe], template: `<h2 mat-dialog-title>{{ 'APPOINTMENTS.BLOCKED.DELETE_TITLE' | translate }}</h2><mat-dialog-content>{{ 'APPOINTMENTS.BLOCKED.DELETE_TEXT' | translate }}</mat-dialog-content><mat-dialog-actions align="end"><button mat-button (click)="ref.close(false)">{{ 'APPOINTMENTS.CONFIRM.KEEP' | translate }}</button><button mat-flat-button (click)="ref.close(true)">{{ 'APPOINTMENTS.BLOCKED.DELETE' | translate }}</button></mat-dialog-actions>` })
export class BlockedPeriodConfirmDialog { readonly ref = inject(MatDialogRef<BlockedPeriodConfirmDialog>); }
@Component({ selector: 'app-blocked-period-discard', imports: [MatDialogModule, MatButtonModule, TranslatePipe], template: `<h2 mat-dialog-title>{{ 'APPOINTMENTS.BLOCKED.DISCARD_TITLE' | translate }}</h2><mat-dialog-content>{{ 'APPOINTMENTS.BLOCKED.DISCARD_TEXT' | translate }}</mat-dialog-content><mat-dialog-actions align="end"><button mat-button (click)="ref.close(false)">{{ 'APPOINTMENTS.FORM.KEEP_EDITING' | translate }}</button><button mat-flat-button (click)="ref.close(true)">{{ 'APPOINTMENTS.FORM.DISCARD' | translate }}</button></mat-dialog-actions>` })
export class BlockedPeriodDiscardDialog { readonly ref = inject(MatDialogRef<BlockedPeriodDiscardDialog, boolean>); }
