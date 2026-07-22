# Prompt for the Ubuntu pre-production agent

Copy everything inside the block below to the agent running on the freshly
installed Ubuntu Desktop machine.

```text
You are preparing this Ubuntu Desktop machine as the dedicated local
pre-production host for the Sisdent project. Perform the authorized reversible
setup work, ask for sudo approval when required, and finish with the requested
handoff report.

Architecture and constraints
----------------------------
- GitHub-hosted Actions runners test and build the application.
- CI publishes an immutable image to ghcr.io/blnunes/sisdent:<commit-sha>.
- This machine runs a repository-scoped GitHub Actions self-hosted runner.
- The self-hosted runner downloads only a deployment artifact containing
  Compose, Caddy configuration, and a deploy script.
- Do not clone the Sisdent source repository on this machine.
- Java, Maven, Caddy, and H2 must not be installed on the host.
- Do not install Docker Desktop, Kubernetes, Portainer, a database server, an
  IDE, or unrelated development packages.
- Do not expose SSH, HTTP, or the Docker daemon to the public internet.
- Never print or store passwords, private SSH keys, GitHub tokens, or recovery
  keys in the report.
- Do not repartition disks, format storage, clear TPM, or change Secure Boot.
- Do not disable SSH password login until key authentication has been tested.
- Do not reboot without explicit approval.
- Ask for confirmation before disabling suspend or hibernation.

Phase 1: inspect and update
---------------------------
1. Record the Ubuntu version, kernel, architecture, hostname, current
   administrator username, CPU, RAM, root filesystem capacity, LAN IPv4
   address, and active network interface. Do not report serial numbers, MAC
   addresses, public IP addresses, or secrets.
2. Run the normal Ubuntu package metadata refresh and install available OS and
   security updates.
3. Report whether /var/run/reboot-required exists. Ask before rebooting.

Phase 2: install the minimal host software
------------------------------------------
1. Ensure these packages are installed:
   - ca-certificates
   - curl
   - jq
   - openssh-server
   - ufw
2. Install Docker Engine from Docker's official APT repository for the Ubuntu
   release detected in Phase 1 (Ubuntu 24.04 and 26.04 are both supported).
   Install only docker-ce, docker-ce-cli, containerd.io,
   docker-buildx-plugin, and docker-compose-plugin plus their required
   dependencies.
3. Enable and start Docker and OpenSSH.
4. Verify Docker with the official hello-world image.
5. Never configure the Docker daemon to listen on a TCP socket.

Phase 3: create the deployment identity and directories
-------------------------------------------------------
1. Create a dedicated non-root user named github-runner with a home directory
   and disabled password, unless it already exists.
2. Add github-runner to the docker group. Do not grant it sudo access.
   Explicitly note in the report that Docker group membership is
   root-equivalent and is limited to this deployment identity.
3. Create /opt/actions-runner owned by github-runner:github-runner with mode
   0750. Do not download or register the runner yet.
4. Create /srv/sisdent owned by github-runner:github-runner with mode 0750.
5. Create /srv/sisdent/runtime.env owned by github-runner:github-runner with
   mode 0600 and exactly these non-secret values:

   SISDENT_IMAGE_REPOSITORY=ghcr.io/blnunes/sisdent
   SISDENT_BIND_ADDRESS=127.0.0.1

6. Do not create a Git checkout under /srv/sisdent or /opt/actions-runner.

Phase 4: administration and network safety
------------------------------------------
1. Keep SSH on port 22 and prohibit root login. Preserve temporary password
   authentication until the administrator's SSH public key has been added and
   tested from the other computer.
2. Create ~/.ssh mode 0700 and ~/.ssh/authorized_keys mode 0600 for the current
   administrator. Do not invent or insert a key.
3. Determine the trusted LAN subnet, but do not guess it. Configure UFW with
   default deny incoming and default allow outgoing, then allow SSH only from
   the confirmed LAN subnet. If the subnet cannot be confirmed, do not enable
   UFW yet; report the exact blocker.
4. Do not open port 80 yet. The application initially binds only to
   127.0.0.1:80 and will be reached through an SSH tunnel.
5. Explain that Docker-published ports can bypass UFW and must later be
   restricted with the DOCKER-USER chain before LAN exposure.
6. Check automatic suspend, hibernation, and lid-close behavior. Ask before
   changing them. A pre-production host eventually needs to stay awake with
   the lid in its intended operating position.
7. Enable the standard Ubuntu unattended security-updates mechanism if it is
   already available. Do not add a third-party update manager.

Phase 5: connectivity checks
----------------------------
Verify outbound HTTPS connectivity to github.com, api.github.com, and ghcr.io.
Do not authenticate to GHCR manually and do not create a personal access token.
The deployment workflow will use its short-lived GITHUB_TOKEN.

GitHub runner stopping point
----------------------------
Do not install or register the GitHub Actions runner until the user provides
the current commands from:

Repository Settings > Actions > Runners > New self-hosted runner

The registration token expires quickly and must not be copied into your final
report. When those commands are later provided:
- execute the download and configuration steps as github-runner in
  /opt/actions-runner;
- use the custom label sisdent-preprod;
- use the default work directory unless there is a concrete conflict;
- install and start it as a system service running as github-runner;
- do not give the runner sudo privileges;
- report only runner name, labels, service state, and GitHub online/offline
  state, never the token.

Final validation and report
---------------------------
Do not deploy Sisdent yet. Produce a structured report with:

A. Actions completed
B. Actions skipped or awaiting approval
C. Blockers and warnings
D. Connection details
E. Exact command outputs
F. HANDOFF SUMMARY

Include exact output from these read-only commands in section E. If a command
is unavailable, write "not available" and explain why:

hostnamectl
whoami
uname -a
cat /etc/os-release
dpkg --print-architecture
ip -4 -brief address
lscpu | grep -E 'Architecture|Model name|CPU\(s\)'
free -h
lsblk -o NAME,SIZE,TYPE,FSTYPE,MOUNTPOINTS
df -h /
ssh -V
systemctl is-enabled ssh
systemctl is-active ssh
sudo ufw status verbose
docker --version
docker compose version
systemctl is-enabled docker
systemctl is-active docker
sudo -u github-runner docker ps
id github-runner
getent group docker
ls -ld /srv/sisdent /opt/actions-runner
ls -l /srv/sisdent/runtime.env
systemctl --failed --no-pager
test -f /var/run/reboot-required && echo "REBOOT REQUIRED" || echo "NO REBOOT REQUIRED"
curl -sS -o /dev/null -w 'github.com: %{http_code}\n' https://github.com
curl -sS -o /dev/null -w 'api.github.com: %{http_code}\n' https://api.github.com
curl -sS -o /dev/null -w 'ghcr.io: %{http_code}\n' https://ghcr.io/v2/

Do not display the contents of runtime.env; only report its owner and mode.
An HTTP 401 from ghcr.io/v2/ still proves that the registry is reachable.

The HANDOFF SUMMARY must contain only:
- Ubuntu version
- Hostname
- Administrator username
- LAN IPv4 address and confirmed LAN subnet
- SSH command for the administrator
- UFW state and permitted sources/ports
- Docker and Compose versions
- Docker service state
- Whether github-runner can execute docker ps without sudo
- Ownership and readiness of /srv/sisdent and /opt/actions-runner
- Whether runtime.env exists with mode 0600
- Whether logout or reboot is required
- Suspend/hibernation status
- GitHub and GHCR connectivity result
- Whether the GitHub runner is not installed, installed offline, or online
- Every remaining blocker

Stop after delivering the report and wait for the next instruction.
```
