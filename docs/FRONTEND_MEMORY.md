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
            |-- resources/       # paged resource tables and management dialogs
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

## Paged tables and ordering

All collection screens use server-side pagination and the shared
`core/table-query.service.ts`. `TableQueryService` owns the HTTP query
parameters (`page`, `size`, `sort`, and `direction`) and the three-state header
cycle: ascending, descending, then the resource default (`id ASC`). Do not
reimplement this state machine in individual components.

Resource tables must expose scalar fields in distinct columns and sort only on
fields permitted by the backend. Related or nested records may be rendered as
one concise cell. Use `mat-table`, `matSort`, and `mat-sort-header`; the sorted
header arrow must remain visible without hover. Long cell values must wrap
instead of changing column widths: use a fixed table layout and
`overflow-wrap: anywhere`.

The generic resource table configuration is in
`features/resources/resource-list.component.ts`. Add an explicit column
definition for every resource; never fall back to generic “record” or
“details” columns when scalar fields are available.

Country continents are an authoritative backend enum. The Country form loads
the allowed values from `GET /api/countries/continents` and presents them with
`mat-select`; do not duplicate enum values in the frontend or allow free text.

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

## End-to-end tests

Playwright end-to-end tests live in `frontend/e2e/` and run with Chromium. Use
the following commands from `frontend/`:

```bash
npx playwright install chromium  # once per machine
npm run test:e2e
npm run test:e2e:ui
```

`test:e2e` runs `scripts/run-e2e.mjs`. The runner checks
`http://localhost:8080/actuator/health`, reuses a healthy API when one is
already running, or starts Spring Boot and waits for the health endpoint before
executing Playwright. It stops only the backend process it started itself.

E2E tests must set `sisdent.language` to `en` with `page.addInitScript` before
the first navigation. This prevents a persisted browser preference from
changing text locators. `LanguageService` also keeps the Angular Material date
adapter locale synchronized with the selected UI language; use accessible
calendar labels rather than visible abbreviated month text.

The patient lifecycle test creates a unique patient, updates it, and
deactivates it. Keep generated test data valid for the API; notably,
`postalCode` is eight digits. Filter by the generated patient name before
creating it so server-side pagination does not hide the new record.
