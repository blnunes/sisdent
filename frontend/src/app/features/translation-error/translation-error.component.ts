import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-translation-error',
  imports: [MatButtonModule, MatIconModule, RouterLink],
  template: `
    <main class="translation-error">
      <mat-icon>translate</mat-icon>
      <span class="code">I18N_LOAD_ERROR</span>
      <h1>Não foi possível carregar as traduções</h1>
      <p>O sistema não conseguiu carregar o ficheiro de idioma. Verifique se o asset foi publicado no servidor.</p>
      <dl>
        <dt>Idioma</dt><dd>{{ language }}</dd>
        <dt>Recurso esperado</dt><dd><code>{{ resource }}</code></dd>
        <dt>Detalhe técnico</dt><dd><code>{{ message }}</code></dd>
      </dl>
      <div class="actions">
        <button mat-flat-button (click)="retry()"><mat-icon>refresh</mat-icon>Tentar novamente</button>
        <a mat-button routerLink="/login">Voltar ao login</a>
      </div>
    </main>
  `,
  styles: [`
    :host { display:block; min-height:100%; background:#f4f9f9; color:#173e4b; }
    .translation-error { box-sizing:border-box; width:min(680px, calc(100% - 32px)); margin:0 auto; padding:12vh 0; }
    mat-icon { width:56px; height:56px; font-size:56px; color:#b34250; }
    .code { display:block; margin-top:22px; color:#b34250; font-size:.78rem; font-weight:800; letter-spacing:.12em; }
    h1 { margin:10px 0; font-size:clamp(2rem, 5vw, 3.4rem); line-height:1.05; }
    p { color:#627b83; line-height:1.6; }
    dl { margin:26px 0; padding:18px; border:1px solid #dbe9e9; border-radius:14px; background:#fff; }
    dt { margin-top:10px; color:#6b8188; font-size:.78rem; font-weight:700; } dt:first-child { margin-top:0; }
    dd { margin:4px 0 0; overflow-wrap:anywhere; } code { font-size:.85rem; }
    .actions { display:flex; gap:10px; align-items:center; }
  `],
})
export class TranslationErrorComponent {
  private readonly route = inject(ActivatedRoute);
  language = '';
  resource = '';
  message = '';

  constructor() { this.readDetails(); }

  retry(): void { window.location.reload(); }

  private readDetails(): void {
    const params = this.route.snapshot.queryParamMap;
    this.language = params.get('language') ?? 'unknown';
    this.resource = params.get('resource') ?? 'unknown';
    this.message = params.get('message') ?? 'No response details';
  }
}
