# Local pre-production environment

## Purpose

The local pre-production environment runs Sisdent on a dedicated Ubuntu
Desktop machine. The host does not build the application and does not keep a
source checkout. GitHub-hosted Actions runners test the project, build an
immutable container image, and publish it to GitHub Container Registry (GHCR).
A self-hosted runner on the Ubuntu machine downloads only the deployment
bundle and starts the approved image.

```text
push to master
  -> tests and SonarCloud Quality Gate
  -> build image on a GitHub-hosted runner
  -> push ghcr.io/blnunes/sisdent:<commit SHA>
  -> send Compose, Caddy, and deploy script as an Actions artifact
  -> self-hosted Ubuntu runner pulls the image and runs Compose
  -> verify /actuator/health
  -> keep the new image, or roll back to the last healthy image
```

Render remains a separate deployment target. A failure or unavailable local
runner does not block the Render deployment job.

## Host responsibilities

The Ubuntu machine contains only operational state:

- Docker Engine and the Docker Compose plugin;
- a repository-scoped GitHub Actions runner;
- `/srv/sisdent/runtime.env`, which is created during bootstrap and is not
  stored in Git;
- the deployment bundle downloaded by Actions;
- the persistent `sisdent-preprod-data` Docker volume.

Java, Maven, Caddy, and H2 are provided by containers. Git and a source checkout
are not required on the host.

## Network defaults

The checked-in Compose file binds Caddy to `127.0.0.1:80` by default. This is
deliberately private. During initial validation, reach it from an administrator
machine with an SSH tunnel:

```bash
ssh -L 8080:127.0.0.1:80 <ubuntu-user>@<ubuntu-lan-ip>
curl http://127.0.0.1:8080/actuator/health
```

To expose pre-production to the trusted LAN, set `SISDENT_BIND_ADDRESS` in
`/srv/sisdent/runtime.env` to the Ubuntu machine's fixed LAN address. Review
Docker's `DOCKER-USER` firewall chain before doing so: published Docker ports
can bypass ordinary UFW rules. Never expose this prototype directly to the
internet.

## Required runtime configuration

The host bootstrap creates `/srv/sisdent/runtime.env` from this template:

```dotenv
SISDENT_IMAGE_REPOSITORY=ghcr.io/blnunes/sisdent
SISDENT_BIND_ADDRESS=127.0.0.1
```

The image tag is supplied by the workflow and always equals the Git commit SHA
that passed the Quality Gate.

## GitHub runner registration

After the Ubuntu bootstrap report is reviewed:

1. Open `Settings > Actions > Runners > New self-hosted runner` in the Sisdent
   repository.
2. Select Linux and the architecture reported by the host.
3. Run GitHub's generated download and registration commands as the dedicated
   `github-runner` user in `/opt/actions-runner`.
4. Register the custom label `sisdent-preprod`.
5. Install the runner as a system service for `github-runner`.
6. Confirm that GitHub reports the runner as online and idle.

The registration token is short-lived. Do not store it in Git, documentation,
shell history, or chat transcripts. The runner needs outbound HTTPS access to
GitHub and GHCR; it does not require an inbound internet port.

The runner has Docker access and therefore must be treated as a privileged
deployment identity. It must run deployment jobs only from trusted pushes to
this repository. Pull-request jobs stay on GitHub-hosted runners.

## Deployment contents

`compose.preprod.yml` runs:

- `data-init`, which gives container user `1001` ownership of the data volume;
- `app`, using the immutable GHCR image and a file-backed H2 database;
- `proxy`, using Caddy on port 80 with compression and defensive headers.

The named volume `sisdent-preprod-data` survives container recreation and new
deployments. Do not remove it during routine cleanup.

## Health check and rollback

`deploy/preprod/deploy.sh` waits up to three minutes for
`http://127.0.0.1/actuator/health`. A successful tag is recorded in
`/srv/sisdent/.last-successful-image`. If a later image fails its health check,
the script recreates the services with the last successful tag and leaves the
workflow red so the failure is visible.

The first deployment has no rollback target. If it fails, inspect:

```bash
cd /srv/sisdent
SISDENT_IMAGE_TAG=<commit-sha> docker compose \
  --env-file runtime.env \
  -f compose.preprod.yml ps
SISDENT_IMAGE_TAG=<commit-sha> docker compose \
  --env-file runtime.env \
  -f compose.preprod.yml logs --tail 200
```

## Bootstrap handoff

Copy the complete prompt from
[`PREPROD_UBUNTU_AGENT_PROMPT.md`](PREPROD_UBUNTU_AGENT_PROMPT.md) to the agent
running on the Ubuntu machine. Bring its final handoff report back before
registering the runner or exposing the HTTP port to the LAN.
