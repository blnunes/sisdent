import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, ViewChild, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { distinctUntilChanged } from 'rxjs';
import { Appointment, PageResponse } from '../../core/models';
import { AuthService } from '../../core/auth.service';
import { AppHeaderComponent } from '../../core/layout/app-header/app-header.component';
import { ModuleNavigationComponent } from '../../core/layout/module-navigation/module-navigation.component';

@Component({
  selector: 'app-home',
  imports: [DatePipe, MatButtonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule, MatSidenavModule, RouterLink, TranslatePipe, AppHeaderComponent, ModuleNavigationComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  readonly membership = this.auth.activeMembership;
  readonly appointments = signal<Appointment[]>([]);
  readonly recentAppointments = signal<Appointment[]>([]);
  readonly loading = signal(false);
  readonly loadError = signal(false);
  readonly today = new Date();
  readonly todayAppointments = computed(() => this.appointments().filter((appointment) => this.isToday(appointment.startAt)));
  readonly scheduledToday = computed(() => this.todayAppointments().filter(({ status }) => status === 'SCHEDULED').length);
  readonly nextAppointment = computed(() => this.appointments().find((appointment) => appointment.status === 'SCHEDULED' && new Date(appointment.startAt).getTime() >= Date.now()) ?? null);
  @ViewChild(AppHeaderComponent) private readonly header?: AppHeaderComponent;

  constructor() {
    toObservable(this.auth.activeMembership)
      .pipe(distinctUntilChanged((previous, current) => previous?.id === current?.id), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadDashboard());
  }

  onDrawerChange(opened: boolean, drawerScroll: HTMLElement): void {
    if (opened) { drawerScroll.scrollTop = 0; return; }
    queueMicrotask(() => this.header?.focusMenuButton());
  }

  closeMenu(drawer: MatSidenav): void { void drawer.close(); }

  loadDashboard(): void {
    const membership = this.membership();
    this.appointments.set([]);
    this.recentAppointments.set([]);
    this.loadError.set(false);
    if (!membership || !this.auth.canReadAppointments()) return;
    this.loading.set(true);
    const from = this.startOfToday();
    const to = new Date(from.getTime() + (7 * 86_400_000));
    const params: Record<string, string> = { from: from.toISOString(), to: to.toISOString(), size: '12' };
    if (membership.clinicUnitId) params['clinicUnitId'] = membership.clinicUnitId;
    this.http.get<PageResponse<Appointment>>(`/api/organizations/${membership.organizationId}/appointments`, { params }).subscribe({
      next: (page) => { this.appointments.set(page.content); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
    const recentFrom = new Date(from.getTime() - (7 * 86_400_000));
    this.http.get<PageResponse<Appointment>>(`/api/organizations/${membership.organizationId}/appointments`, { params: { ...params, from: recentFrom.toISOString(), to: new Date().toISOString(), size: '4' } }).subscribe({
      next: (page) => this.recentAppointments.set(page.content.slice().reverse()),
    });
  }

  hasAccessibleModules(): boolean {
    return this.auth.hasAnyPermission('READ_SPECIALITIES', 'MAINTAIN_SPECIALITIES', 'READ_ADDRESSES', 'MAINTAIN_ADDRESSES', 'READ_COUNTRIES', 'MAINTAIN_COUNTRIES', 'READ_ADMINISTRATIVE_DIVISIONS', 'MAINTAIN_ADMINISTRATIVE_DIVISIONS') || this.auth.canReadClinical();
  }

  private startOfToday(): Date {
    const start = new Date();
    start.setHours(0, 0, 0, 0);
    return start;
  }

  private isToday(value: string): boolean {
    const date = new Date(value);
    return date.getFullYear() === this.today.getFullYear() && date.getMonth() === this.today.getMonth() && date.getDate() === this.today.getDate();
  }
}
