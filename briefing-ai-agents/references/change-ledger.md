# Change Ledger & Rework Ledger — Templates

## Change Ledger (当前有效约束) — one per task, append-only

Mid-task requirements are where deliveries die: a Replit agent deleted 1,206 production records during an explicit "code freeze" because the late constraint lived only in chat memory (crackr.dev/vibe-coding-failures). Rule: the ledger is the agent's memory; chat is not.

```markdown
## 当前有效约束 — <task-id> (<date>)
C1: <constraint> — source: <operator / spec / earlier ledger entry> — status: ACTIVE
C2: ...

## 变更登记
E<k> (<date-time>): 新约束 <text> — 废止 C<n> "<old decision>" — 触发: operator 中途想起 / probe 结果 / 失败复盘
     不可逆? no/yes → yes 需单独人工批准 + 回滚: <command/state>
```

Rules:
- New constraint MUST name the old decision it voids (or state it conflicts with none).
- Reversible/irreversible tag on every entry; irreversible entries block until explicit operator approval with a named rollback.
- At every work session start for the task, re-read the ACTIVE constraints before any action.

## Rework Ledger (翻车复盘入账) — one per skill folder, grows forever

Every rework appends one line. At dispatch time, the agent reads this file for the same task type BEFORE planning. Purpose: incidents become the eval set (Claude Code review practice: reverts −40% once incidents scored daily — claude.dev/blog/solving-code-review-at-anthropic/).

```markdown
## Rework Ledger — briefing-ai-agents
R1 | 2026-10-31 | 任务类型: 脚本整理 | 派了啥: "按扩展名分类下载文件夹" | 翻车点: <what broke> | 根因: <unprobed env / missing DoD / drift / bloat> | 下次禁令: <one-line rule>
```

Fields in one line each; never edit old lines, only append.

## Where they live (operator's machine)

- Per-task change ledger: inside the task's working dir, file `CHANGE_LEDGER.md`.
- Rework ledger for this skill: `~/.agents/skills/briefing-ai-agents/references/rework-log.md` (create on first rework; read before planning any dispatch).
