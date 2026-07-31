import { Component, ViewChild, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../core/auth.service';
import { AppHeaderComponent } from '../../shared/app-header.component';
import { ModuleNavigationComponent } from '../../shared/module-navigation.component';

@Component({
  selector: 'app-home',
  imports: [MatIconModule, MatSidenavModule, TranslatePipe, AppHeaderComponent, ModuleNavigationComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  readonly auth = inject(AuthService);
  @ViewChild(AppHeaderComponent) private readonly header?: AppHeaderComponent;

  onDrawerChange(opened: boolean, drawerScroll: HTMLElement): void {
    if (opened) {
      drawerScroll.scrollTop = 0;
      return;
    }
    queueMicrotask(() => this.header?.focusMenuButton());
  }

  closeMenu(drawer: MatSidenav): void {
    void drawer.close();
  }

  hasAccessibleModules(): boolean {
    return this.auth.hasAnyPermission(
      'READ_PATIENTS', 'MAINTAIN_PATIENTS', 'READ_SPECIALITIES', 'MAINTAIN_SPECIALITIES',
      'READ_ADDRESSES', 'MAINTAIN_ADDRESSES', 'READ_COUNTRIES', 'MAINTAIN_COUNTRIES',
      'READ_ADMINISTRATIVE_DIVISIONS', 'MAINTAIN_ADMINISTRATIVE_DIVISIONS',
    ) || this.auth.canReadClinical();
  }
}
