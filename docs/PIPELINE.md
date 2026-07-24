# CI, quality, and deployment pipeline

## Purpose

This document records how Sisdent automation currently works so that a new
developer or agent can maintain it without reconstructing previous decisions.

Related files:

- `.github/workflows/ci.yml`: production deployment and release tagging.
- `.github/workflows/_render-deploy.yml`: shared Render deployment implementation.
- `.github/workflows/preprod.yml`: local pre-production deployment.
- `.github/workflows/pr-quality.yml`: pull-request quality checks.
- `.github/workflows/rollback-production.yml`: manual production rollback.
- `pom.xml`: Maven build, tests, and JaCoCo configuration.
- `Dockerfile`: container image deployed to Render.
- `render.yaml`: Render service definition.

## Current workflow

The automation is split into independent workflows so production-only jobs do
not appear as skipped in pre-production runs:

1. `Quality check` runs tests, produces coverage, submits the SonarCloud
   analysis, and waits for the Quality Gate on `master`.
2. `Deploy pre-production` is started manually with a branch, tag, or commit
   input. It resolves that reference to an immutable SHA, tests it, publishes
   the image and deployment bundle, and deploys through the `preprod`
   environment on the dedicated self-hosted runner.
4. `Deploy to Render` runs in the GitHub `production` environment only for a
   push to `master` and only after the quality job succeeds. It deploys the
   exact approved commit, waits for Render, and verifies application health.
5. `Tag deployed release` creates a SemVer tag only after the production health
   check succeeds.
6. `Rollback production` is a manual workflow that redeploys the immutable
   commit referenced by an existing release tag.

Triggers:

- `workflow_dispatch` on `CI` permits a manual quality run. Render deployment
  and release tagging still require a push to `master`.
- A pull request targeting `master` runs application and infrastructure checks.
- `workflow_dispatch` on `Deploy pre-production` permits an operator to select
  any branch, tag, or full commit for local validation. No push deploys locally.
- A push or merge to `master` runs quality checks and deploys to Render
  production. It does not deploy to the local pre-production machine.

```mermaid
flowchart LR
    F[Feature branch] --> Q[PR quality check]
    F -->|manual preprod selection| B[Build and push immutable GHCR image]
    B --> L[Deploy immutable SHA locally]
    L --> H[Verify health or roll back]
    H --> T[Manual pre-production testing]
    Q --> M[Reviewed merge to master]
    T --> M
    M --> R[Deploy to Render production]
    R --> C[Verify production health]
    C --> G[Create release tag]
```

Pre-production and production are separate environments. A local deployment
does not update Render. Only a successful push created on `master` can enter
the production workflow.

## Branch protection and environments

Create GitHub environments named `preprod` and `production`. The workflow
associates local deployment with `preprod` and Render deployment with
`production`, allowing environment-specific approvals and secrets when needed.
Allow selected branches and tags in `preprod`, because its workflow is manual.
Restrict `production` deployment branches and tags to `master`. Production may
additionally require a reviewer before the deployment job starts.

Configure repository branch rules as follows:

- For `master`, block deletion and force pushes, require pull requests, and
  require the `Quality check` and `Infrastructure check` status checks.
- Do not configure a required deployment on `master`: production deploys happen
  only after merge. The workflow itself also requires a push event whose ref is
  exactly `refs/heads/master`.
- Do not allow routine bypass of these rules. Pre-production evidence is a
  release decision and may be required through review policy when appropriate.

These settings are configured under `Settings > Rules > Rulesets` or branch
protection in GitHub. Workflow YAML cannot make a branch undeletable by itself.

### Required GitHub configuration

After merging the workflow change:

1. In `Settings > Environments > production`, set deployment branches and tags
   to selected branches and add only `master`. Keep `RENDER_API_KEY` and
   `RENDER_SERVICE_ID` available as repository secrets for the reusable
   workflow. An optional required reviewer creates a second production gate.
2. In `Settings > Environments > preprod`, allow the branches and tags that an
   operator may select for local testing. Add a required reviewer only if the
   operator starting the workflow must not approve their own deployment.
3. In the `master` ruleset, require both `Quality check` and
   `Infrastructure check`. Remove any rule that requires PRs to originate from
   `feat/preprod-deployment`.
4. Delete `PREPROD_RUNNER_TOKEN`; the pipeline no longer queries the runners
   administration API.
5. Keep Render automatic deployments disabled. No Render platform change is
   required for this iteration.

The workflow-level production condition and the environment branch policy are
independent barriers. The workflow requires a push to `refs/heads/master`; the
GitHub Environment independently rejects deployment from any other branch.

## `Quality check` job

The job uses `ubuntu-latest` with the
`maven:3.9.16-eclipse-temurin-25` container, fixing Maven and Java 25 in CI.

Steps:

1. `actions/checkout` pinned to the reviewed v6 commit checks out the full
   history with `fetch-depth: 0`, which Sonar needs for accurate SCM analysis.
2. `mvn --batch-mode --no-transfer-progress verify` compiles the application,
   runs all tests, and generates `target/site/jacoco/jacoco.xml`.
3. Maven Sonar Scanner submits the analysis to project `blnunes_sisdent` in
   organization `blnunes`.
4. `-Dsonar.qualitygate.wait=true` waits for SonarCloud. A rejected Quality Gate
   makes the command fail and prevents deployment.

The Maven property `sonar.exclusions` excludes `.github/workflows/**` from
SonarCloud analysis. GitHub Actions workflow files are validated by GitHub when
the workflows run and are intentionally outside the Java quality gate.

The `contents: read` permission belongs only to this job because it is the only
job that checks out the repository. The deployment job performs no checkout,
avoiding duplicate work and following least privilege.

Coverage thresholds and severity rules are configured in SonarCloud, not in
the workflow YAML. On July 21, 2026, JaCoCo line coverage was 97.54%, and the
gate required at least 90%. Update this document if the external gate changes.

## `Deploy to Render` job

The job has three controls:

- `if` requires a push to `refs/heads/master`; pull requests and manual
  pre-production runs never deploy to Render.
- `needs: quality-check`: tests and Sonar must pass first.
- `timeout-minutes: 25`: the workflow cannot wait indefinitely.

Flow:

1. Validate `RENDER_API_KEY` and `RENDER_SERVICE_ID`.
2. Call the private reusable `_render-deploy.yml` workflow with `GITHUB_SHA`.
3. Call `POST /v1/services/{serviceId}/deploys` with that SHA, ensuring
   Render deploys exactly the commit that passed quality checks.
4. Read the returned deploy ID and poll Render every 10 seconds.
5. Treat `live` as success. Fail immediately for `build_failed`,
   `update_failed`, `pre_deploy_failed`, `canceled`, or `deactivated`.
6. Request `https://sisdent-yhze.onrender.com/actuator/health`, with retries to
   allow for startup time on the free plan.

`render.yaml` deliberately uses `autoDeployTrigger: off`. Enabling Render auto
deploy as well would allow a push to create two deployments. GitHub Actions is
the single deployment orchestrator.

## Release tags, hotfixes, and production rollback

After Render reports `live` and the application health endpoint succeeds, the
`Tag deployed release` job creates an annotated `vMAJOR.MINOR.PATCH` tag for
the exact deployed commit. The release tag is the project version in `pom.xml`
with `-SNAPSHOT` removed. A tag with that version must not already point to
another commit. Re-running a completed workflow is idempotent: an already
tagged commit receives no second tag.

After tagging, `Prepare next development version` calculates the next patch,
creates an `automation/prepare-<version>-SNAPSHOT` branch, changes only
`pom.xml`, and opens a pull request to `master`. Merge that pull request after
its checks pass. This keeps `master` one development-version commit ahead of
the production tag, so new branches inherit the next `-SNAPSHOT` version.
Re-running the release workflow detects an existing open version PR and exits
successfully without creating a duplicate.

Both the pull-request and push quality workflows classify a change as an
automatic version bump only when `pom.xml` is the sole changed file and its
version advances by exactly one patch. Such a change runs `mvn validate` but
does not run the full tests, SonarCloud, Render deployment, or release tagging.
Any additional file or unexpected version change follows the complete release
pipeline.

The push classifier fetches the previous `master` commit when it is absent
from the checkout. If either commit or the diff cannot be inspected,
classification fails and production deployment remains blocked. Deploy and
tag jobs run only when classification explicitly reports a regular release
change.

The tag job needs `contents: write` on `GITHUB_TOKEN`. In repository settings,
ensure Actions can use read/write workflow permissions and that any tag ruleset
allows `github-actions[bot]` to create release tags.

The version preparation job uses `RELEASE_AUTOMATION_TOKEN`, a fine-grained
personal access token or GitHub App installation token with repository Contents
and Pull requests read/write permissions. The associated identity must be able
to create branches and pull requests. Auto-merge is deliberately not requested:
GitHub rejects it when the base branch has no compatible protection rule, and
the version PR is small enough to review and merge explicitly.

To prepare a correction from an older production version:

```bash
git fetch origin --tags
git switch --create hotfix/short-description v0.0.1
```

Commit the correction, deploy that branch manually to pre-production, and
promote the approved change to `master` through the normal pull-request flow.
A successful production deploy receives the next patch tag automatically.

To roll production back:

1. Open `Actions > Rollback production > Run workflow`.
2. Enter an existing tag such as `v0.0.1`.
3. Approve the `production` environment if protection requires it.
4. Confirm the selected deployment becomes `live` and passes the health check.

The rollback workflow accepts only SemVer release tags that point to a commit
in `master` history. It resolves the tag to a commit SHA and sends that SHA to
Render. It does not move `master`, delete tags, or create a new tag. A later
push to `master` remains the authoritative forward deployment.

Production deploys and rollbacks share the `render-production` concurrency
group and never run simultaneously. Rollback is an application rollback only:
database changes must remain backward-compatible or have a separate,
explicitly tested rollback procedure.

## Local pre-production jobs

These jobs run only after an operator starts `Deploy pre-production` and
supplies a Git reference. The validation job resolves it to a commit SHA before
the build job publishes two GHCR tags:

- `ghcr.io/blnunes/sisdent:<commit SHA>` is immutable and is the deployment
  input;
- `ghcr.io/blnunes/sisdent:preprod` is a convenience pointer and is never used
  as the authoritative rollback record.

Start the workflow only while the local runner is online. If it is offline,
the deployment job remains queued; the already published immutable image can
be reused by rerunning the failed or queued deployment.

The build job packages only `compose.preprod.yml`, the Caddy configuration, and
`deploy/preprod/deploy.sh`. The self-hosted job downloads that artifact directly
to `/srv/sisdent`; it does not run `actions/checkout` and keeps no source tree.

The self-hosted runner must have the standard `self-hosted`, `linux`, and `x64`
labels plus the custom `sisdent-preprod` label. Host bootstrap, network policy,
runtime files, rollback behavior, and registration steps are documented in
`docs/PREPROD.md`.

The workflow authenticates to GHCR with its short-lived `GITHUB_TOKEN`. No
long-lived registry token belongs on the Ubuntu host. The build job receives
`packages: write`; the deployment job receives `packages: read`.

### Adding a future local development environment

Do not share the pre-production runner label, environment, volume names, or
concurrency group with development. Create:

- a `dev` GitHub Environment;
- a dedicated runner label such as `sisdent-dev`;
- environment-specific Compose project, bind address, volumes, and runtime
  directory;
- a manual caller workflow using the same validate, immutable-image, health,
  and rollback sequence.

The selected Git reference must still be resolved to a full commit SHA before
building. Load testing should use the immutable SHA tag and record that SHA with
the test results so a run can be reproduced.

## Required secrets

Repository secrets live under
`GitHub > Settings > Secrets and variables > Actions`.

| Secret | Source | Purpose |
| --- | --- | --- |
| `SONAR_TOKEN` | SonarCloud | Authenticate code analysis |
| `RENDER_API_KEY` | Render Account Settings > API Keys | Authenticate deployment API calls |
| `RENDER_SERVICE_ID` | Render service settings; value starts with `srv-` | Identify the Sisdent service |
| `RELEASE_AUTOMATION_TOKEN` | Fine-grained PAT or GitHub App token | Create the post-release branch and pull request |

Never store secret values in source files, logs, commits, or documentation.
GitHub can show a secret name and update date but cannot reveal its value.

## Diagnosing a failed pipeline

1. Open `GitHub > Actions > CI` and find the first failing step.
2. For `Unit tests`, reproduce with `./mvnw verify`.
3. If `SonarCloud` ends with `QUALITY GATE STATUS: FAILED`, inspect the Sonar
   dashboard. Intermediate warnings such as SLF4J messages are not necessarily
   the cause.
4. For `Trigger deploy`, verify both Render secrets and API-key permissions.
5. For `Wait for deploy`, inspect the corresponding Render deployment logs.
6. For `Verify application health`, inspect `/actuator/health`, `${PORT}`, and
   application startup logs.

Useful commands:

```bash
./mvnw verify
gh run list --branch master --limit 5
gh run view <RUN_ID> --log-failed
curl --fail https://sisdent-yhze.onrender.com/actuator/health
```

## Notifications

GitHub sends Actions notifications according to the account settings under
`GitHub > Settings > Notifications > Actions`. There is no separate email
integration in the workflow. Email notifications require a verified address
and enabled Actions notifications.

## Evolution guidelines

- Pin every third-party GitHub Action to a reviewed full commit SHA and retain
  the major version only as an explanatory comment.
- Do not add checkout to the deployment job without a concrete need.
- Do not re-run the full Maven build during Sonar analysis when the existing
  JaCoCo output can be reused.
- Do not enable Render auto deploy while GitHub controls deployments.
- For staging and production, use GitHub Environments, environment-specific
  secrets, and separate Render service IDs.
- Before PostgreSQL deployment, introduce migrations and never use
  `ddl-auto=create-drop` in production.
