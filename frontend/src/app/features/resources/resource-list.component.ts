import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { TranslatePipe } from '@ngx-translate/core';
import { AppHeaderComponent } from '../../shared/app-header.component';
import { ModuleNavigationComponent } from '../../shared/module-navigation.component';

type ResourceConfig = { key: string; endpoint: string; title: string; description: string };

@Component({
  selector: 'app-resource-list',
  imports: [MatIconModule, MatProgressSpinnerModule, MatSidenavModule, TranslatePipe, AppHeaderComponent, ModuleNavigationComponent],
  templateUrl: './resource-list.component.html',
  styleUrl: './resource-list.component.scss',
})
export class ResourceListComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  readonly config = this.route.snapshot.data as ResourceConfig;
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly records = signal<Record<string, unknown>[]>([]);
  readonly title = computed(() => this.config.title);

  constructor() {
    this.http.get<Record<string, unknown>[]>(this.config.endpoint).subscribe({
      next: (records) => { this.records.set(records); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }

  closeMenu(drawer: MatSidenav): void { void drawer.close(); }
  primary(record: Record<string, unknown>): string { return String(record['name'] ?? record['street'] ?? record['code'] ?? '—'); }
  secondary(record: Record<string, unknown>): string { return Object.entries(record).filter(([key, value]) => !['id', 'name', 'street'].includes(key) && value != null && typeof value !== 'object').map(([, value]) => String(value)).join(' · '); }
}
