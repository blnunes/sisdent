# Angular Frontend Memory

This document guides agents modifying the Sisdent frontend. The Angular
application is located in `frontend/` and uses standalone components, Angular
Material, SCSS, and `@ngx-translate/core`.

## Current structure

```text
frontend/
|-- angular.json              # build, serve, tests, and schematics
|-- public/i18n/              # pt-PT, en, and nl translations
`-- src/
    |-- main.ts               # application bootstrap
    |-- styles.scss           # global styles
    `-- app/
        |-- app.ts/html/scss  # shell and router-outlet
        |-- app.config.ts     # global providers
        |-- app.routes.ts     # routes
        |-- core/             # services, models, authentication, and interceptor
        |-- shared/           # reusable components
        `-- features/         # components grouped by feature
            |-- home/            # authenticated home and independent navigation
            |-- login/
            |-- not-found/
            |-- permissions/
            |-- resources/       # read-only module listings
            `-- users/
```

Feature components should remain alongside their template and style files. A
feature should use `name.component.ts`, `name.component.html`, and
`name.component.scss`. Shared services and models belong in `core`; reusable
components belong in `shared`.

## Light and dark themes

The frontend supports `light` and `dark` themes. State is centralized in
`ThemeService` (`core/theme.service.ts`), which applies `data-theme` to the
`html` element, updates `color-scheme`, and persists the choice in
`localStorage` under `sisdent-theme`. The reusable visual control is
`shared/theme-toggle.component.ts`.

New interface colors must use global theme variables/selectors or work in both
themes. When creating a page, expose `app-theme-toggle` in the header or action
area. Do not duplicate theme-switching logic inside features.

The dark palette is scoped to `html[data-theme='dark']`; do not change light
theme rules while refining it. The brand reference is dentistry and healthcare:
navy background (`#081c24`), blue-teal surfaces (`#102b35`), blue borders
(`#28515c`), light aqua text (`#e4f5f5`), and teal actions
(`#12a99b`/`#46d7c8`). Blue and teal communicate trust, cleanliness, and calm;
use red only for errors or disabled states, never as the primary color.

On dark surfaces, chips, labels, and metadata must not use low-contrast gray.
Use a distinct teal surface, medium teal border, and light aqua text; check
text and border contrast in every Material component. Dark form fields should
use an elevated surface, a readable aqua placeholder, and a sufficiently
contrasting outline; never rely on Angular Material's default placeholder.
In `mat-form-field`, do not use `mat-label` and `placeholder` for the same
instruction: Material displays the label as the placeholder while the field is
empty, causing overlap. In dark mode, provide contrast for both the native
`::placeholder` and `.mdc-floating-label`, since the latter is visible when a
`mat-label` occupies an empty field.

Controls positioned over decorative elements must be placed in a flexible
container with explicit alignment and a `z-index` above the decoration.
Purely visual elements, such as background bubbles, must use
`pointer-events: none` so they never block clicks.

The `/login` and `/not-found` pages must also use `--app-*` theme tokens so
their light and dark palettes remain consistent with the rest of the system.
In encapsulated styles, use `:host-context(html[data-theme='dark'])` when the
component needs to react to the theme applied to the `html` element.

The shared header lives in `shared/app-header.component.*`, and the authorized
module list lives in `shared/module-navigation.component.*`. Features that
need a menu must reuse these components to preserve the same structure,
spacing, and focus behavior.

## Mandatory template rule

Never create HTML inside a TypeScript component. Do not use `template:` or a
template string with markup in `@Component`, even for small templates. Every
component must point to a separate file:

```ts
@Component({
  selector: 'app-example',
  templateUrl: './example.component.html',
  styleUrl: './example.component.scss',
})
```

The same principle applies to styles: prefer `styleUrl` pointing to an SCSS
file instead of `styles: [...]`. HTML should contain only the view; business
rules, state, and service calls belong in `.ts`.

Before completing a change, search for violations with:

```bash
rg -n "template\\s*:|styles\\s*:" frontend/src --glob '*.ts'
```

The expected result is empty. When converting an existing component, move the
`template` content to `.html` and the `styles` content to `.scss`, preserving
Angular bindings, events, control flow, and encapsulated styles.

## Implementation conventions

- Use standalone components and declare dependencies in the `imports` array.
- Use `inject()` for services and component dependencies when consistent with
  the surrounding file.
- Use signals for local reactive state where the feature already follows that
  pattern.
- Keep HTTP calls in `core` services, not directly in templates.
- Reuse models from `core/models.ts` and the `AuthService`/interceptor for
  authentication.
- Use translations from `public/i18n/` for new interface text, keeping all
  three locales synchronized.
- The official language for routes and identifiers is English: use `/home`,
  `/users`, `/permissions`, `/patients`, `/specialities`, `/addresses`,
  `/countries`, and `/states`. Do not create Portuguese route aliases.
- `/home` requires authentication only and has no dedicated permission. After
  login, `AuthService.destination()` must direct authenticated users to
  `/home`.
- Module routes must use guards based on read or management permissions. The
  `/permissions` page accepts `READ_PERMISSIONS` in read-only mode and
  `MAINTAIN_PERMISSIONS` for editing; the API must still require the
  management permission to save changes.
- The translation loader uses the absolute `/i18n/<language>.json` path. Keep
  files in `frontend/public/i18n/` so the build copies them to the published
  asset root. The backend must keep `/i18n/**` public because the login page
  loads translations before authentication.
- English (`en`) is the default language and is also embedded in the bundle as
  an offline fallback. `/login` must not display keys such as `LOGIN.TITLE`
  even if `/i18n/en.json` fails; additional languages continue to load from
  `/i18n/`.
- `LanguageService` must handle HTTP failures while loading a saved language
  and explicitly fall back to `en`; never leave the exception unhandled.
  Pre-production deployment must verify that `/i18n/en.json` returns the
  expected content, in addition to checking the `/login` page.
- For Material components, explicitly import the modules used by the
  standalone component.
- Place tests next to the implementation when a feature has non-trivial
  behavior.

## Verification

Run from `frontend/` after changes:

```bash
npm install       # only when dependencies are not installed yet
npm run build
npm test -- --watch=false
```

If installation is already prepared, `npm run build` is the minimum check.
Fix CSS budget, binding, import, or template failures before delivering the
change.
