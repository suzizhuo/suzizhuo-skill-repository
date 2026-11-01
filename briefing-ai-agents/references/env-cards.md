# Environment Fact-Cards — Operator's Two Standing Targets

The operator dispatches work across TWO different environments. Attach the matching card to every brief. Cards are hypotheses until probed output is pasted.

## Probe First, Always

Before any code/command that depends on machine, version, path, permission, or network, run on the ACTUAL target and paste output:

```bash
uname -mrs
sw_ver 2>/dev/null || cat /etc/os-release | head -2
bash --version | head -1
python3 --version 2>/dev/null; java -version 2>&1 | head -1
docker version --format '{{.Server.Version}}' 2>&1 | head -1
df -h ~ | tail -1
```

Rules:
- "It's probably macOS" is not a fact. `sw_ver` output is.
- If you cannot run the probe, say so in the assumption ledger and stop at destructive steps.
- Re-probe after any environment switch (local ↔ container).

## Card A — MAC_LOCAL (老 Mac 本地)

| Field | Standing value | Verify by |
|---|---|---|
| Machine | Intel Mac, operator describes it as slow ("特别垃圾") | `uname -m` → x86_64 |
| OS | macOS 12.6 (Monterey) | `sw_ver` |
| Default shell | bash 3.2 (no GNU-only flags, no `sed -i` without suffix) | `bash --version` |
| Typical work dirs | `~/Downloads` and personal project dirs | `pwd` before edits |
| Docker | assume absent unless probe proves a server | `docker version` |
| Constraints | limited RAM/CPU — avoid heavy local builds; no root assumptions | `df -h ~`, `sysctl -n hw.memsize` |

Known traps on this card: `bash 3.2` (no associative arrays, no `mapfile`), BSD `sed/stat/find` semantics, `~/Downloads` may contain `.crdownload/.download` partials and hidden dotfiles — never move those.

## Card B — MONKEY_CONTAINER (远程云主机容器)

| Field | Standing value | Verify by |
|---|---|---|
| Type | Remote cloud host, containerized agent execution (Monkey Code), sometimes desktop version | session tool inventory (below) |
| OS | assume Linux until probe says otherwise | `cat /etc/os-release` |
| Access | tool-mediated only — you see execution output ONLY if a tool exists that returns it | list this session's tools; prove one round-trip |
| Network | unknown until probed; security-lab tasks often need OAST_DOMAIN exfil or restricted egress | actual DNS/HTTP test |
| Persistence | never assume the container, its filesystem, or its state survives across sessions | ask; persist artifacts to declared RESULT_DIR |

## Capability Checklist (Beat 2, before any plan)

Answer each with EVIDENCE (a tool exists + one successful call), not assumption:

- [ ] Can I write files in the target? (proved by a real write/echo)
- [ ] Can I see command output from the target? (proved by a real round-trip)
- [ ] Can I reach the network from the target? (proved by a real DNS/HTTP test)
- [ ] Which tools in THIS session touch the target? (name them)
- [ ] Where do deliverables persist? (declared path, not "it stays in the container")

If any box is unproved → it goes in the assumption ledger and blocks destructive/deploy steps until the operator confirms.

## Assumption Ledger Template

```
A1: target = MAC_LOCAL | probed: <paste sw_ver output> | status: CONFIRMED
A2: container execution output visible to me | status: UNPROVED (no tool with round-trip) → needs operator confirmation
A3: ~/Downloads contains partial downloads | status: ASSUMED → script must skip .crdownload/.download
```
