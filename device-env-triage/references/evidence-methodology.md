# Evidence & Methodology — `device-env-triage`

**Scope of this file.** Practice-first evidence for judging whether a project's deployment
environment is worth installing on a given device. Every claim below is tagged with the
channel it came from and the date it was read. Nothing here is recalled from memory.

**Retrieval channels used (all live, this session):**

| Channel | Endpoint | Status |
|---|---|---|
| HN Algolia (stories + comments) | `https://hn.algolia.com/api/v1/search?query=…&tags=story\|comment` | working |
| GitHub public contents API | `https://api.github.com/repos/<org>/<repo>/contents/<path>?ref=main` (base64) | working |
| Direct site fetch | `web_fetch <url>` | intermittent |
| `web_search`, `bash` + network | — | unavailable (no API key / no egress) |

**Direct-fetch failures observed:** `contains.dev` → `ENOTFOUND`;
`api.github.com/repos/NixOS/templates/…?ref=main` → 404 (`No commit found for the ref main`);
`api.github.com/repos/microsoft/vscode-dev-containers/containers/python/.devcontainer/devcontainer.json?ref=main` → 404.
`raw.githubusercontent.com` and `hub.docker.com` were not attempted further after the
first DNS failures. Where a primary source could not be reached, the HN Algolia record
(title, points, comment count, author, timestamp) is cited instead and is marked
`[hn-index-only]`.

**Reading date for every entry below: 2026-11-01.**

---

## 1. Method

Three rules governed collection, and they are the method this skill should reuse:

1. **Practice over principle.** A claim enters this file only if a named person, team, or
   repo performed a concrete action and described it. "You should pin versions" is not
   evidence; `mise use node@24` → committed `mise.toml` → `mise lock` → `mise.lock`
   with checksums is evidence.
2. **Failure paired with source.** Every failure pattern below has at least one real
   artifact: a postmortem, a Show/Ask HN post describing the break, or a tool whose
   entire reason for existing is to detect that break. The existence of a detector for a
   failure class is strong evidence that the class is common.
3. **Runtime behaviour outranks README.** Where a repo's own text and its observed runtime
   diverge, the runtime record is what gets quoted. Example: this file cites the *decoded
   README* of `jdx/mise` and `cachix/devenv` for what those tools do, but cites *HN comment
   threads* for what practitioners actually hit while using them — including the places
   where the tools did not solve the problem.

Collecting order that worked: HN Algolia for the *discussion layer* (real pain, real
numbers, real pushback) → GitHub contents API for the *implementation layer* (exact
commands, exact config shapes) → direct fetch only for a small number of long-form
articles. The Algolia API is the highest-yield channel in this environment because it
returns author, points, comment count and full `story_text` for Show/Ask HN posts, which
is where practitioners describe the concrete action they took.

---

## 2. Golden cases (≥3, practice-first)

### G1 — `jdx/mise`: pin per-project, lock to the byte

**Who.** jdx (mise maintainer); the tool is sponsored by Entr and the Omacom Foundation
per the fetched README.
**Primary source (fetched, base64-decoded):**
`https://api.github.com/repos/jdx/mise/contents/README.md?ref=main`
**Discussion layer:** `https://hn.algolia.com/api/v1/search?query=mise%20asdf%20direnv&tags=story`
→ Ask HN *"What SDK/Version manager do you use?"* (2025-03-14, author `marknefedov`), whose
own framing is the class of problem: *"I'm struggling with global pollution (e.g. GOPATH,
site-packages) and want a project-local approach."*

**Concrete actions, verbatim from the fetched README:**

```toml
[tools]
node = "24"
python = "3.14"
rust = "stable"
"cargo:cargo-nextest" = "0.9"
"github:BurnSushi/ripgrep" = "15"
"npm:prettier" = "3"

[env]
NODE_ENV = "development"
_.file = ".env"
_.path = "./node_modules/.bin"

[tasks.build]
description = "Build the application"
run = "npm run build"

[tasks.test]
description = "Run the test suite"
depends = ["build"]
run = "pytest && npm test"
```

Commands that matter for triage: `mise lock` (*"Write exact tool versions and checksums to
mise.lock"*), `mise install`, `mise outdated`, `mise upgrade`, `mise doctor`, `mise config ls`.
`mise lock` is the whole point — `"24"` requests the latest release in the 24 series;
`mise lock` records the exact version and checksum so the next machine is byte-identical.

**Difference from the average team.** The average team's version control is a wiki page
telling people to install Node 24. `mise` puts the request in a committed file *and* the
resolution in a committed lockfile, and it reads idiomatic files (`.nvmrc`,
`.python-version`) instead of inventing a new one. The README says so explicitly:
*"keep any existing `.tool-versions` files as they are; mise also reads `.nvmrc`,
`.python-version`, and other idiomatic version files once enabled."*

**Second golden practice inside the same source: `mise bootstrap` + `mise dot apply` for
whole-machine state**, with `mise bootstrap --dry-run` / `--adopt` and dotfile tracking
(`mode = "track"`), plus `services` and remote SSH targets. This is the layer that makes a
*laptop rebuild* reproducible rather than just a project.

---

### G2 — `cachix/devenv`: declarative environment + a process manager + secretSpec

**Who.** Cachix / devenv contributors; version 2.0.0 at fetch time.
**Primary source (fetched, base64-decoded):**
`https://api.github.com/repos/cachix/devenv/contents/README.md?ref=main`
**Discussion layer:** `https://news.ycombinator.com/item?id=33655542` — *"Devenv.sh: Fast
and reproducible developer environments using Nix"*, **382 points, 158 comments**
(2022-11-18). A 158-comment thread on a single dev-env tool is itself evidence that this
problem class is expensive at scale.

**Concrete actions, verbatim:**

```nix
{ pkgs, lib, config, inputs, ... }:
{
  env.GREET = "devenv";
  packages = [ pkgs.git ];

  enterShell = ''
    hello          # Run scripts directly
    git --version  # Use packages
  '';

  enterTest = ''
    echo "Running tests"
    git --version | grep --color=auto "${pkgs.git.versions}"
  '';
}
```

`devenv init` scaffolds `devenv.yaml`, `devenv.nix`, `.gitignore`; `devenv shell` activates.

**The four moves that beat the average, all from the fetched README:**

1. **Native process manager** — *"dependency ordering, restart policies, readiness probes
   (exec, HTTP, systemd notify), socket activation, watchdog heartbeats, and file
   watching."* The environment owns its own long-running processes; the host does not.
2. **Automatic port allocation** — *"that finds free ports so parallel environments never
   collide."* This is the reproducibility failure nobody puts in a checklist (see F9).
3. **secretSpec** — *"declarative, provider agnostic secrets management (keyring,
   1Password, dotenv)"*, i.e. secrets are resolved at runtime by a provider, never written
   into `devenv.nix` (see F6).
4. **Containers and outputs without Docker** — *"OCI containers built from your
   environment without Docker"*; `crate2nix`, `uv2nix`; polyrepo support to *"reference
   outputs and options across repositories."*

Also relevant to triage: `devenv` targets *"Linux, macOS, x64, and ARM64 (including WSL2)"*
explicitly, and `--impure` / `--offline` exist as first-class escape hatches.

---

### G3 — Replit: reproducibility at fleet scale, and the honest counter-argument

**Who.** Replit engineering (author `osener` on HN).
**Primary source (fetched via HN Algolia record):** `https://blog.replit.com/super-colliding-nix-stores`
— *"Super Colliding Nix Stores: Nix Flakes for Millions of Developers"*, **112 points, 76
comments** (2023-05-25) — `[hn-index-only]`, direct fetch not attempted.

**Why it is a golden case.** It is the same argument as G1/G2 (content-addressed store →
identical closure on every machine) but made at a scale where *store collisions* — not
version drift — are the dominant failure. Fleet scale changes which failure class bites
first; that is a directly useful input for the volume axis of the triage.

**Practice — the paired counter-case, fetched in the same Algolia sweep (these are the
"difference from average" proof):**

- `https://jade.fyi/blog/flakes-arent-real/` — *"Flakes aren't real and cannot hurt you:
  using Nix flakes the non-flake way"*, **60 points, 81 comments** (2024-01-09). A working
  practitioner arguing against the flagship abstraction.
- `https://samuel.dionne-riel.com/blog/2023/09/06/flakes-is-an-experiment-that-did-too-much-at-once.html`
  (HN 37414506), and `https://determinate.systems/posts/experimental-does-not-mean-unstable`
  (HN 37413262) — the "experimental" label fight.
- `https://our.status.im/reproducable-builds-with-nix/` (HN 23564251) — Status.im using Nix
  in a shipped product; *"Signed binaries + reproducible builds via Nix"* also appears in
  the `satd` Show HN (HN 48555262).

**Triage lesson from G3:** the *reproducibility* half of Nix is the part that generalises;
the *flake* half is contested even inside the Nix community. A skill should recommend
lockfiles and content addressing, not a specific frontend.

---

### G4 — "Works on my machine": what the retrospectives actually found

This is the case the skill exists for. Four real retrospectives, all fetched:

**(a) The wall moves.** `https://dwdraju.medium.com/how-it-works-in-my-machine-turns-it-works-in-my-container-1b9a340ca43d`
— *"'It works on my machine' turns to 'it works in my container'"*, **217 points, 239
comments** (HN 36885598, 2023-07-26). The point of the piece, and of the 239-comment
thread, is that containerising the environment does not remove the snowflake — it relocates
it into the image, where it is invisible unless the image is also built from a locked spec.

**(b) The org-level anatomy.** HN comment `https://news.ycombinator.com/item?id=9239124`
(author `maaaats`, on *"On open-sourcing existing code"*). This is the single most useful
failure description found, because it enumerates the failure surfaces in one breath:

> *"The private one has configuration values hard-coded (passwords, ldap url's etc.), a
> setup that is impossible to run ('it works on my machine') and the deployment is a secret
> that no one really knows (not prepared for someone being hit by a bus)."*

Three distinct defects in one project: baked config, unreproducible setup, undocumented
deploy. Note that the same comment identifies the working counter-move: forcing yourself to
imagine reuse *"makes us put more pride in what we write, so the QA is good."*

**(c) The tool-itself-drifts case.** HN comment `https://news.ycombinator.com/item?id=9049218`
(author `keslag`, on *"Ask HN: Best practices for OS X dev environments"*). On Vagrant:
*"Packer's lack of support for winrm on windows systems... requires a number of workarounds
(like installing open-ssh) on the windows VM that in no way would be viable on a production
windows server. And that divergence allows for the 'It works on my machine' bugs which is
really the whole point of using Vagrant."* Same comment, on Homebrew Cask: *"no oversight
for breaking changes, so use this only as a convenience, not a tool to be relied upon."*
The lesson: a reproducibility tool that silently drifts is worse than none, because it
converts drift into a false claim.

**(d) Hermeticity as the actual fix.** `https://nemorize.com/roadmaps/hermetic-builds`
— *"Why 'works on my machine' means your build is broken"* (HN 46604027, 2026-01-13), and
`https://thoughtbot.com/blog/it-works-on-my-machine-why` (HN 41265168 / 41255347 /
41316659 — resubmitted three times in eight days, itself a signal of how live the topic is).

---

## 3. Failure patterns (≥6, each with a real source)

### F1 — Environment / configuration drift (the snowflake host)

- **Source A (early canonical statement):** `http://blog.lusis.org/blog/2012/05/24/configuration-drift-and-next-gen-cm/`
  — *"Configuration Drift and Next-gen Configuration Management"*, HN 4017680 (2012-05-24).
- **Source B (practitioner confirming the incident class):** `https://news.ycombinator.com/item?id=47364395`
  (author `orkunk`, Ask HN 2026-03-13): *"In several projects I worked on, incidents were
  often caused by unnoticed config differences between environments. Usually someone
  changed a config, a deployment happened later, and the difference went unnoticed until
  something broke. Most tools I found focus on file diff, but not on monitoring drift over
  time."* This is the strongest single sentence in the whole evidence set for why triage
  must treat *"is there a diff between environments?"* as a first-class question rather
  than an afterthought.
- **Source C (the node-level version):** `https://news.ycombinator.com/item?id=21796102`
  (Ask HN 2019-12-15): *"Do you keep the OS immutable? If so, how? If not, how do you deal
  with nodes' configuration drift? Do you ever re-image your nodes?"*

**Paired working move:** devenv's declarative `devenv.nix` + `devenv up` process manager
(G2), or an immutable/no-daemon runtime (see F7), or periodic re-image (Source C).

---

### F2 — Floating versions: `:latest`, unpinned transitive deps, HEAD

- **Source A:** `http://blog.benhall.me.uk/2015/01/dockerfile-latest-tag-anti-pattern/`
  — *"Dockerfile and the :latest tag anti-pattern"*, HN 8862368 (2015-01-09).
- **Source B (a real incident, not an opinion):** `https://news.ycombinator.com/item?id=47508654`
  (author **simonw**, Ask HN 2026-03-24): *"Given today's LiteLLM supply chain attack,
  where running 'pip install litellm' (or 'pip install dspy' or any other package that had
  LiteLLM as an unpinned dependency), I'm feeling motivated to figure out a good pattern
  for running a development environment on my Mac where rogue code can't steal all of my
  ~/.file credentials."* The critical detail: the victim did **not** install the malicious
  package; a **transitive** dependency was unpinned. Pinning only direct dependencies does
  not close this hole.
- **Source C (same pattern, different ecosystem):** `https://news.ycombinator.com/item?id=47063497`
  — *"There are 64k skills on Vercel's skills.sh. Cursor, Claude Code, Windsurf, and dozens
  more agents pull them from GitHub at HEAD — no versioning, signing, or scanning."*
- **Source D (the detector's existence proves the class):** `https://news.ycombinator.com/item?id=43962427`
  — StarGuard *"parses dependency manifests or SBOMs and flags common risk signs: unpinned
  versions, direct Git URLs, lookalike package names."*

**Paired working move:** `mise lock` writing *"exact tool versions and checksums"* (G1);
devenv's `[bootstrap]`/`[inputs]` pinning Nixpkgs by flake input; `mise outdated` +
deliberate `mise upgrade` instead of silent drift.

---

### F3 — Image bloat / oversized runtime footprint

- **Source A:** `https://www.ardanlabs.com/blog/2020/02/docker-images-part1-reducing-image-size.html`
  — *"Reducing Docker Image Size"*, **176 points, 35 comments** (HN 22369080).
- **Source B:** `https://github.com/remorses/docker-phobia` — *"Show HN: Docker-phobia:
  Analyze Docker image size with a treemap"*, **91 points, 30 comments** (HN 40188282).
  A tool whose only job is showing you what is inside the image.
- **Source C (measured outcomes, practitioner reports):**
  `https://medium.com/nebula-graph/how-to-reduce-docker-image-size-404cd7d6986f`
  (*"Shrink Docker image size by 77%"*);
  `https://dragondrop.cloud/2023/09/19/ripping-out-python-and-reducing-our-docker-image-size-by-87/`
  (*"Replacing Python with Go and Reducing Image Size by 87%"*);
  `https://pythonspeed.com/articles/conda-docker-image-size/` (itamarst, HN 24445269);
  `https://blueprint.interviewpen.com/p/cut-docker-image-size` (*"How to cut Docker images
  by 80%"*).
- **Source D (bloat as a hard blocker, not a nuisance):** `https://news.ycombinator.com/item?id=50007023`
  — the Abyss author explains their plugins run on the host partly because they have
  *"nothing better than an 8GB Docker image"* to distribute them in. Bloat is cited here as
  the reason a feature was structured differently.
- **Source E (the same disease outside containers):** `https://news.ycombinator.com/item?id=46949167`
  — `envoic`: *"old `.venv` directories sitting in project folders, eating disk. There's no
  built-in way to answer 'where are all my venvs and how much space are they using?'"*

**Paired working move:** multi-stage builds + minimal base images (Ardan Labs above);
`conda-pack` for relocatable envs; devenv's *"OCI containers built from your environment
without Docker"* so the runtime is the closure, not a kitchen-sink image.

---

### F4 — No rollback: change shipped without a tested reverse

- **Source A (the archetype, fetched in full):**
  `https://news.ycombinator.com/item?id=4123595` — *"Post-Mortem of AWS Outage"* (self-post,
  2012-06-17). Root cause chain: generator fan fault → failover → *"one of the breakers on
  this particular back-up power distribution circuit was incorrectly configured to open at
  too low a power threshold and opened when the load transferred to this circuit."* The
  remediation is the transferable part: *"We have also completed an audit of all our
  back-up power distribution circuits... and are incorporating these configuration checks
  into our regular testing and audit processes."* A misconfiguration that only fires during
  failover is invisible until the day you need the failover — exactly a rollback path that
  was never exercised.
- **Source B (config change with no working reverse, fetched via record):**
  `https://www.theregister.com/2021/05/19/salesforce_root_cause/` — *"Salesforce outage:
  Global DNS downfall started by an engineer trying a quick fix"* (HN 27251544, 35 points /
  10 comments; resubmitted four times: 27217043, 27214626, 27219071, 27251544).
- **Source C (the most instructive one, fetched in full):**
  `https://www.anthropic.com/research/april-23-postmortem`
  (the `/engineering/april-23-postmortem` path redirects here; verified HTTP 200, 2026-11-01).
  Three separate changes, three different slices of traffic, one aggregate illusion. The
  two lines that matter for this skill:
  - *"While we began investigating reports in early March, they were challenging to
    distinguish from normal variation in user feedback at first, and **neither our internal
    usage nor evals initially reproduced the issues identified**."*
  - The fix list: *"we'll ensure that a larger share of internal staff **use the exact
    public build** of Claude Code (as opposed to the version we use to test new features)"*,
    plus *"soak periods, a broader eval suite, and gradual rollouts."*
  Two of the three changes were explicitly *"reverted"* (April 7 and April 20) — the
  organisation had a reverse, and used it.
- **Source D (tooling built because rollback was missing):**
  `https://news.ycombinator.com/item?id=45102616` — Thymis lists *"Risk of bricking devices
  during updates"* as the problem it exists to remove; `https://news.ycombinator.com/item?id=34396273`
  — Sveltos lists *"Snapshot and Rollback"* as a headline feature alongside drift detection.

---

### F5 — Architecture / host assumptions (amd64 vs arm64, Linux vs macOS, uid/gid)

- **Source A:** `https://news.ycombinator.com/item?id=47734699` (Ask HN 2026-04-11,
  author `sroussey`): *"I know on Windows there are containers that will forward CUDA stuff
  to the host system to run, but **there is no such thing on MacOS**… The builtin
  devcontainer stuff is based on Docker."* A concrete architecture assumption (GPU
  passthrough exists) failing on the host the requester actually owns.
- **Source B:** `https://news.ycombinator.com/item?id=43985624` — Lumier (159 points, 52
  comments): *"The existing solutions either **didn't support M-series chips or relied on
  KVM/Intel emulation**, which was slow and cumbersome."* The fix required abandoning the
  container-as-delivery model in favour of Apple's Virtualization Framework.
- **Source C (arm64 as an explicit selling point):** `https://news.ycombinator.com/item?id=47261970`
  — *"Built with React + TypeScript, works on both amd64 and arm64 (Apple Silicon Macs)."*
  When a tool advertises dual-arch as a differentiator, single-arch is the norm it is
  differentiating from.
- **Source D (the silent one — uid/gid on bind mounts):**
  `https://news.ycombinator.com/item?id=46042544` — Container-Make: *"Permission Handling:
  This was the hardest part. I implemented a dynamic entrypoint script that checks the
  uid/gid of your workspace and creates a matching user inside the container on-the-fly
  using adduser/su-exec. This solves the 'files owned by root' issue on Linux host mounts."*
  An environment that reproduces versions but writes root-owned files into the repo breaks
  every subsequent tool on the host.
- **Source E (declared, not hidden):** `https://news.ycombinator.com/item?id=48469039`
  — Nucleus lists under *"Honest tradeoffs"*: *"Linux x86_64 only. No macOS/Windows/BSD, no
  plans."* The good practice is stating the architecture ceiling in the README, not
  discovering it on the target device.
- **Source F:** `https://news.ycombinator.com/item?id=47898711` — *"as docker-in-docker does
  not work well."*

**Paired working move:** devenv's stated *"Linux, macOS, x64, and ARM64 (including WSL2)"*
coverage with `--impure` / `--offline` escape hatches; mise's `mise --system
x86_64-darwin` target override; Container-Make's uid/gid-matching entrypoint.

---

### F6 — Configuration and secrets baked into the artifact instead of injected

- **Source A:** HN comment `https://news.ycombinator.com/item?id=9239124` — *"configuration
  values hard-coded (passwords, ldap url's etc.)"* (see G4b).
- **Source B (the vendor-side version of the same disease):**
  `https://news.ycombinator.com/item?id=46958742` — Distr 2.0 (101 points, 29 comments).
  The list of legacy states it modernised includes *"customers with access to internal AWS
  or GCP registries because there was no better option"*, and its remediation is explicit:
  *"Secret Management, so database passwords don't show up in configuration steps or logs."*
- **Source C (detectors built for exactly this):**
  `https://news.ycombinator.com/item?id=35055843` — Bearer's rule set includes *"Hard-coded
  secrets and tokens"* and *"Leakage of sensitive data through cookies, internal loggers,
  third-party logging services, and into analytics environments."*
  `https://news.ycombinator.com/item?id=47270193` — aidevshield detects *"Exposed .env files
  without .gitignore protection"* and *"postinstall curl | sh"*.
- **Source D:** `https://news.ycombinator.com/item?id=50007023` — Abyss stores *"files in
  on startup (so the agent can see them but can't change your copy)"*, issues *"ephemeral"*
  per-container mTLS certs that are *"single-use for ephemeral containers"*, and hides
  secrets from prompts *"before the agent ever sees them."* Runtime injection, not
  bake-time.

**Paired working move:** devenv `secretSpec` (*"keyring, 1Password, dotenv"* — provider
agnostic, nothing written to config); mise's `[env]` + `_.file` with a documented secrets
path (`https://mise.jdx.dev/environments/secrets/`); runtime-injected credentials with
short-lived or ephemeral material.

---

### F7 — Daemon / long-running-process assumptions, and container-as-VM

- **Source A (the ask, which enumerates the assumptions):**
  `https://news.ycombinator.com/item?id=33515705` (Ask HN 2022-11-08): *"Do you install your
  static analysis and other tools in the container? Do your debuggers work (Visual Studio,
  gdb, pdb, whatever)… Do you ever use separate containers for build and test vs the final
  packaged application?"* Every one of those is an assumption that silently fails on
  someone else's device.
- **Source B (the overhead claim, from a practitioner building the alternative):**
  `https://news.ycombinator.com/item?id=49046973` — Cygnus: *"Docker: Heavier and slower
  because it has to supervise more than web apps. Paying overhead you don't need."*
- **Source C (the escape, taken in production):**
  `https://news.ycombinator.com/item?id=48469039` — Nucleus: *"a single Rust binary, no
  daemon… I dropped the entire image-and-distribution half (no Dockerfile, no layers, no
  registry, no pull/push, no persistent storage layer) in exchange for going deeper on
  isolation and reproducibility."* Rootfs is *"a directory copied into tmpfs (agent mode)
  or a Nix-built closure mounted read-only (production mode)."*
- **Source D:** `https://news.ycombinator.com/item?id=47898711` — the MicroVM thread: the
  author wants *"Dev-Container like experience… but MicroVM based"* and reports *"A lot of
  them are designed for short ephemeral tasks, not a long running dev environment."*

**Paired working move:** devenv's native process manager — *"dependency ordering, restart
policies, readiness probes (exec, HTTP, systemd notify), socket activation, watchdog
heartbeats, and file watching"* — i.e. the environment declares its own services instead of
assuming the host already runs them. Secondary: `devenv test` *"automatically starts and
stops processes"*, so service assumptions get exercised rather than assumed.

---

### F8 — Registry / build-tooling gating (the "who can even get the artifact" failure)

- **Source:** `https://news.ycombinator.com/item?id=46958742` — Distr 2.0 again. Before the
  OCI registry was embedded, the observable legacy state was *"customers with access to
  internal AWS or GCP registries because there was no better option"*, plus *"Excel sheets
  nobody trusts to track customer versions"* and *"deployments two major versions behind
  that nobody wants to touch."* This is the failure where the environment is perfectly
  reproducible *in principle* and unreachable in practice.

**Paired working move:** Distr's own fix — *"An OCI container registry… directly embedded
  into our codebase and served via a separate port from a single Docker image"*, plus
  container logs and metrics *"without SSH access"*. Triage question: can the target device
  obtain and verify the artifact without borrowing someone's credentials?

---

### F9 — Parallel-environment collisions (ports, paths, uids) — the failure nobody lists

Not in the original brief, but the evidence surfaced it twice independently, so it belongs.

- **Source A:** devenv G2 — *"Automatic port allocation that finds free ports so parallel
  environments never collide."* Someone built automatic port allocation because manual
  assignment collided.
- **Source B:** `https://news.ycombinator.com/item?id=46042544` — uid/gid collision on
  bind mounts (F5, Source D). Same class: two environments disagree about who owns a path.
- **Source C:** `https://news.ycombinator.com/item?id=46949167` — `envoic`: *"old `.venv`
  directories sitting in project folders"*; the tool exists because nobody can answer
  *"where are all my venvs and how much space are they using?"* Collisions and orphans are
  the cost of many environments, and they are invisible until you look.

**Triage implication:** "does it collide when two instances run side by side?" is a
separate question from "is it reproducible", and it is the one that bites on a single busy
device.

---

## 4. Works vs Fails — the comparison table

Every `Works` row is a real move from §2. Every `Fails` row is paired with the real source
from §3.

| # | Axis | ✅ Works | ❌ Fails (paired don't) | Fail source |
|---|---|---|---|---|
| 1 | Version identity | Commit `mise.toml` **and** `mise.lock` with checksums; `mise install` reproduces bytes | Rely on `:latest`, or pin only direct deps while a transitive dep floats | F2-A, F2-B (LiteLLM via `dspy`) |
| 2 | Environment definition | One declarative file (`devenv.nix` / `mise.toml`) as single source of truth for tools + env + tasks | Wiki instructions; two files to keep in sync (Makefile + `.devcontainer/`) | F4-C; HN 46042544 (*"tired of maintaining both"*) |
| 3 | Long-running processes | Environment owns its services via a process manager with ordering, restart policy, readiness probes, socket activation | Assume the host runs the daemon; run a `while true` wrapper; install systemd into a container | F7-A/B/C vs devenv README |
| 4 | Ports & paths | Automatic free-port allocation; uid/gid matched to the workspace on entry | Hard-coded ports and root-owned bind mounts | F9, F5-D |
| 5 | Secrets | Provider-agnostic secretSpec (keyring / 1Password / dotenv), resolved at runtime, never in the config file | Passwords, LDAP URLs, tokens baked into config or image layers | F6-A/B/C |
| 6 | Artifact distribution | Multi-stage build to a minimal image; OCI registry reachable to the target; `conda-pack`-style relocatable envs | One kitchen-sink base image; give the customer internal AWS/GCP registry credentials | F3, F8 |
| 7 | Artifact size budget | Measure it: treemap the image; treat MB as a hard gate | Ship the 8 GB image and restructure features around it | F3-D (Abyss, 8 GB blocker) |
| 8 | Architecture coverage | Declare the matrix in the README and test the non-x86 leg: Linux/macOS × x64/arm64 | Build on Intel, discover arm64 at the target | F5-A/B/C/E |
| 9 | Change control | Soak period + broader eval suite + gradual rollout + a rehearsed revert | Ship on internal evals that did not reproduce user conditions; no reverse tested | F4-C (Anthropic: *"neither our internal usage nor evals initially reproduced"*) |
| 10 | Verification substrate | Test the **exact public build** on the real target class | Test an internal build and assume parity | F4-C (Anthropic's first remediation) |
| 11 | Drift detection | Baseline + scheduled scan + alert on divergence over time; drift re-syncs to declared state | One-off `diff` of two files; nobody knows an env changed until it breaks | F1-B (orkunk), F1-A |
| 12 | Node/OS lifecycle | Immutable or re-imaged nodes; declarative fleet config pushed OTA | SSH in and patch; keep the box alive for years | F1-C; `45102616` (Thymis: bricking risk) |
| 13 | Recovery | Snapshot + rollback as a first-class feature; backup retained before change | Roll forward only; recovery is "rebuild from memory" | F4-A/B/D |
| 14 | Tool-chain honesty | Publish limitations explicitly (`Linux x86_64 only. No macOS/Windows/BSD.`) | Silent incompatibility discovered on the target device | F5-E |
| 15 | Tool that drifts | A reproducibility tool you can pin, audit, and fall back from | A convenience tool with *"no oversight for breaking changes"* that turns drift into a false claim | F3 / G4-C (Homebrew Cask) |

---

## 5. The three conclusions that matter for the skill

**C1 — Containerising relocates the snowflake; it does not remove it.**
The 217-point / 239-comment thread (G4-A) exists precisely because teams concluded
"works in my container" and then discovered the *image* was the new unpinned artifact.
For triage this means a container is only evidence of reproducibility if the image itself
is built from a locked, diff-able spec. Otherwise it scores the same as "works on my
machine".

**C2 — The fix is not more testing; it is testing on the artifact the user runs.**
This is the single most counter-intuitive finding in the whole evidence set, and it comes
from an organisation that had, by any normal measure, excellent test infrastructure.
Anthropic's postmortem states plainly that *"neither our internal usage nor evals initially
reproduced the issues identified"*, and their first listed remediation was not "add more
evals" but *"ensure that a larger share of internal staff use the exact public build of
Claude Code (as opposed to the version we use to test new features)."* A verification
harness that does not run the shipped artifact can be perfect and still wrong. Every
INSTALL verdict in the triage should therefore ask "does the acceptance path exercise the
exact artifact", not "does the project have tests".

**C3 — Reproducibility and lockability are different properties, and only one of them is
portable.**
Nix's content-addressed closure is the reproducible half; the flake frontend is contested
even by its own community (60 points / 81 comments for *"Flakes aren't real and cannot hurt
you"*). `mise`'s lockfile with checksums is the lockable half and it works whether or not
the team ever adopts Nix. For a device-level verdict: prefer *lockfile + explicit version
request + checksum* as the reproducible signal, and treat a specific frontend (flakes,
devcontainer.json, devenv.nix) as an implementation detail that may be absent without
failing the verdict.

A near-miss fourth: **collisions, not just versions.** Two environments that are each
perfectly reproducible will still break each other on one busy machine — ports, uid/gid on
bind mounts, orphaned `.venv` trees (F9). On a single-device triage where the question is
"can this live next to everything else?", collision-safety is a first-class axis, and it is
the axis most checklists omit.

---

## 6. Gaps — explicitly unsourced

Marked honestly. These were attempted and not obtained, or obtained only second-hand.

- **[unsourced] No Docker official primary documentation.** `hub.docker.com` and
  `raw.githubusercontent.com` fetches failed after the first DNS errors; no official
  multi-stage-build page was read first-hand. The multi-stage evidence here is
  practitioner blog posts and HN-index records only.
- **[unsourced] No primary `devcontainer.json` sample.** My guessed path into
  `microsoft/vscode-dev-containers` returned 404 (` containers/python/.devcontainer/devcontainer.json?ref=main`).
  Devcontainer practice is therefore sourced from HN discussion (33515705, 46042544,
  47898711) rather than from Microsoft's own reference configs.
- **[unsourced] No `NixOS/templates` flake.** `?ref=main` returned
  `No commit found for the ref main`. Nix flake specifics are sourced from third-party
  blogs and HN records.
- **[unsourced] The "56% of time on environment setup" figure.** It appears in
  `https://news.ycombinator.com/item?id=41658722` (DevContainer AI) as a vendor's own
  claim with no cited study. Do not quote it as a measured fact.
- **[unsourced] No quantified cost-of-drift study.** No controlled measurement of hours
  lost or incidents caused by drift was fetched. F1 rests on one canonical 2012 post and
  practitioner testimony, not on data.
- **[unsourced] No postmortem that names "no rollback" as root cause with numbers.** The
  closest two are the AWS 2012 self-post (breaker misconfiguration discovered by audit)
  and Anthropic's April 2026 postmortem (change control). Neither is an environment-rollback
  postmortem. Treat F4 as "change without a rehearsed reverse", evidenced indirectly.
- **[unsourced] No Chinese-language primary sources.** HN Algolia and the GitHub contents
  API were the only working channels, so every quote above is English. Any Chinese-language
  practice case would require a channel this environment does not have.
- **[unsourced] No benchmark comparing mise vs asdf vs direnv.** The only direct
  comparison material is the Ask HN at 43364131, which has one comment. mise's own speed
  claims and devenv's *"sub 100ms when nothing changed"* eval-cache claim are vendor-stated,
  not independently measured here.
- **[unsourced] Everything tagged `[hn-index-only]`.** For Replit's Nix post, the
  Salesforce Register piece, and the thoughtbot / nemorize / jade.fyi / lusis posts, I
  verified the HN Algolia record (title, author, points, comments, timestamp, URL) but did
  not always fetch the article body. Where I did fetch the body (Anthropic postmortem,
  AWS self-post, both GitHub READMEs), the quotes above are verbatim from the response.

---

## 7. Reproducing this file

Query shapes that worked against HN Algolia (all `tags=story` unless noted):

```
"works on my machine"                      → 55 story hits; 1022 comment hits
"docker image size"                        → 20 story hits
"latest tag"                               → 4 story hits (narrow by design)
unpinned dependencies                      → 6 story hits
"configuration drift"                      → 12 story hits
postmortem configuration change            → 3 story hits
"root cause" outage                        → 33 story hits
Nix reproducible / "nix flakes"            → 48 / 73 story hits
devcontainer                               → 116 story hits
"works on my machine" &tags=comment        → the highest-yield query in the set
```

Two notes for the next run: (1) `tags=comment` on the failure-phrase query returned more
usable practitioner evidence than the story index, because comments are where people
describe what actually broke; (2) when a direct site fetch fails with `ENOTFOUND`, do not
retry it — switch to the Algolia record for the URL and mark it `[hn-index-only]`.
