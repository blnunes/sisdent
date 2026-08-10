import { Component, effect, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { systemUnavailable } from './core/system-availability';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly router = inject(Router);

  constructor() {
    effect(() => {
      if (systemUnavailable() && this.router.url !== '/unavailable') void this.router.navigateByUrl('/unavailable');
    });
  }
}
