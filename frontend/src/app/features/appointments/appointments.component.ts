import { Component, DestroyRef, ViewChild, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  CalendarOptions,
  DateSelectArg,
  DatesSetArg,
  EventClickArg,
  EventInput,
} from '@fullcalendar/core';
import interactionPlugin from '@fullcalendar/interaction';
import { FullCalendarComponent, FullCalendarModule } from '@fullcalendar/angular';
import luxonPlugin, { toLuxonDateTime } from '@fullcalendar/luxon3';
import timeGridPlugin from '@fullcalendar/timegrid';
import { EMPTY, Subscription, distinctUntilChanged, expand, forkJoin, map, reduce } from 'rxjs';
import {
  AppointmentAvailabilityInterval,
  AppointmentGraphqlService,
} from '../../core/appointment-graphql.service';
import { AuthService } from '../../core/auth.service';
import { Appointment, ClinicUnit, Practitioner } from '../../core/models';
import { OrganizationReadGraphqlService } from '../../core/organization-read-graphql.service';
import { AppHeaderComponent } from '../../core/layout/app-header/app-header.component';
import { ModuleNavigationComponent } from '../../core/layout/module-navigation/module-navigation.component';
import { AppointmentDetailsDialogComponent } from './appointment-details-dialog.component';
import type { AppointmentDetail } from '../../core/appointment-graphql.service';
import {
  AppointmentFormDialogComponent,
  AppointmentCandidate,
} from './appointment-form-dialog.component';
import { BlockedPeriodsDialogComponent } from './blocked-periods-dialog.component';

@Component({
  selector: 'app-appointments',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSidenavModule,
    TranslatePipe,
    FullCalendarModule,
    AppHeaderComponent,
    ModuleNavigationComponent,
  ],
  templateUrl: './appointments.component.html',
  styleUrl: './appointments.component.scss',
})
export class AppointmentsComponent {
  @ViewChild('calendar') private readonly calendar?: FullCalendarComponent;
  readonly auth = inject(AuthService);
  private readonly appointmentApi = inject(AppointmentGraphqlService);
  private readonly organizationReads = inject(OrganizationReadGraphqlService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly translate = inject(TranslateService);
  private readonly dialog = inject(MatDialog);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly membership = this.auth.activeMembership;
  readonly clinicUnits = signal<ClinicUnit[]>([]);
  readonly practitioners = signal<Practitioner[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly selectedView = signal<'timeGridWeek' | 'timeGridDay'>('timeGridWeek');
  readonly calendarOptions = signal<CalendarOptions>(this.options('UTC', []));
  clinicUnitId = '';
  practitionerIds: string[] = [];
  private visibleRange?: DatesSetArg;
  private rangeSubscription?: Subscription;
  private detailDialog?: MatDialogRef<AppointmentDetailsDialogComponent>;
  private detailOrigin?: HTMLElement;
  private slotOrigin?: HTMLElement;
  private deepLinkAppointmentId?: string;
  private requestVersion = 0;

  selectedClinic(): ClinicUnit | undefined {
    return this.clinicUnits().find((unit) => unit.id === this.clinicUnitId && unit.active);
  }

  constructor() {
    toObservable(this.auth.activeMembership)
      .pipe(
        distinctUntilChanged((previous, current) => previous?.id === current?.id),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((membership) => {
        this.clinicUnitId = membership?.clinicUnitId ?? '';
        this.practitionerIds = [];
        this.clinicUnits.set([]);
        this.practitioners.set([]);
        this.error.set('');
        this.loadContext();
      });
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      this.deepLinkAppointmentId = params.get('appointmentId') ?? undefined;
      this.openDeepLinkedAppointment();
    });
  }

  changeView(view: 'timeGridWeek' | 'timeGridDay'): void {
    this.selectedView.set(view);
    this.calendar?.getApi().changeView(view);
  }
  refresh(): void {
    if (this.visibleRange) this.loadRange(this.visibleRange);
  }
  onDatesSet(range: DatesSetArg): void {
    this.visibleRange = range;
    this.loadRange(range);
  }
  onClinicChanged(): void {
    this.practitionerIds = [];
    this.loadPractitioners();
    this.applyCalendarOptions([]);
    this.refresh();
  }
  onPractitionerChanged(): void {
    this.refresh();
  }
  onEventClick(event: EventClickArg): void {
    event.jsEvent.preventDefault();
    this.detailOrigin = event.el;
    this.setAppointmentSelection(event.event.id);
  }
  onSlotSelect(selection: Pick<DateSelectArg, 'start' | 'end' | 'view' | 'jsEvent'>): void {
    const clinic = this.selectedClinic();
    if (!clinic) return;
    this.slotOrigin = this.selectionOrigin(selection);
    const start = toLuxonDateTime(selection.start, selection.view.calendar).setZone(
      clinic.timezone,
    );
    const end = toLuxonDateTime(selection.end, selection.view.calendar).setZone(clinic.timezone);
    this.calendar?.getApi().unselect();
    this.openForm(undefined, undefined, undefined, {
      startLocal: start.toFormat("yyyy-LL-dd'T'HH:mm"),
      endLocal: end.toFormat("yyyy-LL-dd'T'HH:mm"),
    });
  }
  openCreate(): void {
    this.openForm();
  }
  canManageAppointments(): boolean {
    return ['ORGANIZATION_ADMIN', 'MANAGER', 'APPOINTMENT_MANAGER'].includes(
      this.membership()?.role ?? '',
    );
  }
  openBlockedPeriods(): void {
    const membership = this.membership(),
      clinic = this.selectedClinic();
    if (!membership || !clinic || !this.visibleRange || !this.canManageAppointments()) return;
    this.dialog.open(BlockedPeriodsDialogComponent, {
      data: {
        organizationId: membership.organizationId,
        clinicUnitId: clinic.id,
        timezone: clinic.timezone,
        practitioners: this.practitioners(),
        from: this.asUtcIso(this.visibleRange.start, this.visibleRange),
        to: this.asUtcIso(this.visibleRange.end, this.visibleRange),
        onFinished: () => this.refresh(),
      },
      autoFocus: 'dialog',
      restoreFocus: true,
      disableClose: true,
      width: 'min(620px, calc(100vw - 32px))',
    });
  }

  private loadContext(): void {
    const membership = this.membership();
    if (!membership) return;
    this.organizationReads
      .listClinicUnits(membership.organizationId, membership.clinicUnitId)
      .subscribe({
        next: (units) => {
          this.clinicUnits.set(units);
          this.clinicUnitId ||= units[0]?.id ?? '';
          this.loadPractitioners();
          this.applyCalendarOptions([]);
          this.refresh();
          this.openDeepLinkedAppointment();
        },
        error: () => this.error.set(this.translate.instant('APPOINTMENTS.ERROR.LOAD_CLINICS')),
      });
  }

  private loadPractitioners(): void {
    const membership = this.membership();
    if (!membership || !this.clinicUnitId) {
      this.practitioners.set([]);
      return;
    }
    this.organizationReads
      .listPractitioners(membership.organizationId, this.clinicUnitId)
      .subscribe({
        next: (records) =>
          this.practitioners.set(records.filter((practitioner) => practitioner.active)),
        error: () => this.error.set(this.translate.instant('APPOINTMENTS.ERROR.LOAD_OPTIONS')),
      });
  }

  private loadRange(range: DatesSetArg): void {
    const membership = this.membership();
    const clinic = this.selectedClinic();
    if (!membership || !clinic) return;
    this.rangeSubscription?.unsubscribe();
    const version = ++this.requestVersion;
    const from = this.asUtcIso(range.start, range);
    const to = this.asUtcIso(range.end, range);
    const practitionerIds = this.practitionerIds.length ? this.practitionerIds : undefined;
    this.loading.set(true);
    this.error.set('');
    this.rangeSubscription = forkJoin({
      appointments: this.loadAppointmentPages(
        membership.organizationId,
        clinic.id,
        from,
        to,
        practitionerIds,
      ),
      availability: this.appointmentApi.availabilityIntervals(
        membership.organizationId,
        clinic.id,
        from,
        to,
        practitionerIds,
      ),
    }).subscribe({
      next: ({ appointments, availability }) => {
        if (version !== this.requestVersion) return;
        this.applyCalendarOptions(this.events(appointments, availability));
        this.loading.set(false);
      },
      error: () => {
        if (version !== this.requestVersion) return;
        this.error.set(this.translate.instant('APPOINTMENTS.ERROR.LOAD_SCHEDULE'));
        this.loading.set(false);
      },
    });
  }

  private loadAppointmentPages(
    organizationId: string,
    clinicUnitId: string,
    from: string,
    to: string,
    practitionerIds?: readonly string[],
  ) {
    return this.appointmentApi
      .list(organizationId, clinicUnitId, from, to, 0, 100, practitionerIds)
      .pipe(
        expand((page) =>
          page.page + 1 < page.totalPages
            ? this.appointmentApi.list(
                organizationId,
                clinicUnitId,
                from,
                to,
                page.page + 1,
                100,
                practitionerIds,
              )
            : EMPTY,
        ),
        map((page) => page.content),
        reduce((all, content) => [...all, ...content], [] as Appointment[]),
      );
  }

  private applyCalendarOptions(events: EventInput[]): void {
    const timezone =
      this.clinicUnits().find((unit) => unit.id === this.clinicUnitId)?.timezone ?? 'UTC';
    this.calendarOptions.set(this.options(timezone, events));
  }
  private options(timeZone: string, events: EventInput[]): CalendarOptions {
    const locale = this.translate.currentLang() ?? 'en-gb';
    return {
      plugins: [timeGridPlugin, interactionPlugin, luxonPlugin],
      initialView: this.selectedView(),
      timeZone,
      locale: locale === 'en' ? 'en-gb' : locale,
      allDaySlot: false,
      editable: false,
      selectable: true,
      selectMirror: true,
      eventStartEditable: false,
      eventDurationEditable: false,
      eventInteractive: true,
      headerToolbar: false,
      height: 'auto',
      nowIndicator: true,
      slotMinTime: '06:00:00',
      slotMaxTime: '22:00:00',
      events,
      datesSet: (range) => this.onDatesSet(range),
      eventClick: (event) => this.onEventClick(event),
      select: (selection) => this.onSlotSelect(selection),
      slotLaneDidMount: (slot) => {
        slot.el.tabIndex = 0;
        slot.el.setAttribute('role', 'button');
        slot.el.setAttribute('aria-label', this.translate.instant('APPOINTMENTS.SELECT_SLOT'));
        slot.el.addEventListener('keydown', (event) => {
          if (event.key !== 'Enter' && event.key !== ' ') return;
          event.preventDefault();
          if (!slot.date) return;
          const end = toLuxonDateTime(slot.date, slot.view.calendar)
            .plus({ minutes: 30 })
            .toJSDate();
          this.onSlotSelect({ start: slot.date, end, view: slot.view, jsEvent: null });
        });
      },
      eventDidMount: (event) => {
        event.el.setAttribute(
          'aria-label',
          this.translate.instant('APPOINTMENTS.DETAILS.OPEN_EVENT', { title: event.event.title }),
        );
      },
    };
  }
  private events(
    appointments: Appointment[],
    availability: AppointmentAvailabilityInterval[],
  ): EventInput[] {
    return [
      ...availability.map((interval) => ({
        start: interval.startAt,
        end: interval.endAt,
        display: 'background' as const,
        classNames: [`availability-${interval.category.toLowerCase()}`],
        title:
          interval.category === 'BLOCKED' ? this.translate.instant('APPOINTMENTS.UNAVAILABLE') : '',
      })),
      ...appointments.map((appointment) => ({
        id: appointment.globalId,
        start: appointment.startAt,
        end: appointment.endAt,
        title: `${appointment.practitionerName} · ${this.translate.instant(`APPOINTMENTS.STATUS.${appointment.status}`)}`,
        classNames: [`appointment-${appointment.status.toLowerCase()}`],
      })),
    ];
  }
  private asUtcIso(date: Date, range: DatesSetArg): string {
    return toLuxonDateTime(date, range.view.calendar)
      .toUTC()
      .toISO({ suppressMilliseconds: false })!;
  }

  private setAppointmentSelection(appointmentId: string): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { appointmentId },
      queryParamsHandling: 'merge',
    });
  }

  private openDeepLinkedAppointment(): void {
    const membership = this.membership();
    const appointmentId = this.deepLinkAppointmentId;
    const clinic = this.selectedClinic();
    if (!appointmentId || !membership || !clinic || this.detailDialog) return;
    if (!this.isUuid(appointmentId)) {
      this.clearAppointmentSelection();
      return;
    }
    this.detailDialog = this.dialog.open(AppointmentDetailsDialogComponent, {
      data: {
        organizationId: membership.organizationId,
        clinicUnitId: clinic.id,
        appointmentId,
        clinicName: clinic.name,
        timezone: clinic.timezone,
        canManageAppointments: this.auth.canManageAppointments(),
        onUnavailable: () => this.clearAppointmentSelection(),
        onReschedule: (detail: AppointmentDetail, onSaved: (updated: AppointmentDetail) => void) =>
          this.openForm(appointmentId, detail, onSaved),
        onLifecycleFinished: () => this.refresh(),
      },
      autoFocus: 'dialog',
      restoreFocus: false,
      disableClose: true,
      width: 'min(480px, calc(100vw - 32px))',
    });
    this.detailDialog.afterClosed().subscribe(() => {
      this.detailDialog = undefined;
      this.clearAppointmentSelection();
      requestAnimationFrame(() => this.detailOrigin?.focus());
      this.detailOrigin = undefined;
    });
  }

  private openForm(
    appointmentId?: string,
    detail?: AppointmentDetail,
    onSaved?: (updated: AppointmentDetail) => void,
    prefill?: { startLocal: string; endLocal: string },
  ): void {
    const membership = this.membership();
    const clinic = this.selectedClinic();
    if (!membership || !clinic) return;
    // Both lists are existing authorized, clinic-scoped GraphQL reads; IDs remain form-internal.
    forkJoin({
      patients: this.appointmentApi.patients(membership.organizationId, clinic.id),
      practitioners: this.organizationReads.listPractitioners(membership.organizationId, clinic.id),
    }).subscribe({
      next: ({ patients, practitioners }) => {
        const dialog = this.dialog.open(AppointmentFormDialogComponent, {
          data: {
            organizationId: membership.organizationId,
            clinicUnitId: clinic.id,
            timezone: clinic.timezone,
            patients: patients.map((patient) => ({
              id: patient.globalId,
              name: patient.name,
            })) as AppointmentCandidate[],
            practitioners: practitioners
              .filter((practitioner) => practitioner.active)
              .map((practitioner) => ({
                id: practitioner.globalId,
                name: practitioner.displayName,
              })),
            appointmentId,
            detail,
            prefill,
            onMutationFinished: () => this.refresh(),
            onSaved,
          },
          autoFocus: 'dialog',
          restoreFocus: false,
          disableClose: true,
          width: 'min(520px, calc(100vw - 32px))',
        });
        dialog.afterClosed().subscribe(() => {
          requestAnimationFrame(() => this.slotOrigin?.focus());
          this.slotOrigin = undefined;
        });
      },
      error: () => this.error.set(this.translate.instant('APPOINTMENTS.ERROR.LOAD_OPTIONS')),
    });
  }

  private clearAppointmentSelection(): void {
    if (!this.deepLinkAppointmentId) return;
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { appointmentId: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  private isUuid(value: string): boolean {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
  }

  private selectionOrigin(selection: Pick<DateSelectArg, 'jsEvent'>): HTMLElement | undefined {
    if (selection.jsEvent?.target instanceof HTMLElement) {
      return selection.jsEvent.target;
    }
    return document.activeElement instanceof HTMLElement ? document.activeElement : undefined;
  }
}
