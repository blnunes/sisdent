import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { TranslatePipe } from '@ngx-translate/core';
import { DataTableComponent } from '../../shared/data-table/data-table.component';
import { FilterBarComponent } from '../../shared/filters/filter-bar.component';
import { AppHeaderComponent } from '../../core/layout/app-header/app-header.component';
import { ModuleNavigationComponent } from '../../core/layout/module-navigation/module-navigation.component';

export const RESOURCE_PAGE_IMPORTS = [MatButtonModule, MatIconModule, MatSidenavModule, TranslatePipe, DataTableComponent, FilterBarComponent, AppHeaderComponent, ModuleNavigationComponent] as const;
