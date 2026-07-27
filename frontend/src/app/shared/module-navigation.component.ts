import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-module-navigation',
  imports: [RouterLink, MatIconModule, MatListModule, TranslatePipe],
  templateUrl: './module-navigation.component.html',
})
export class ModuleNavigationComponent {
  readonly auth = inject(AuthService);
  @Input() active = 'home';
  @Output() readonly navigate = new EventEmitter<void>();
}
