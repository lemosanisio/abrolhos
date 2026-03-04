# Agent Roster

| Agent | File | Purpose |
|---|---|---|
| Product Owner | `.claude/agents/po.md` | Spec new features before any code is written. Produces requirements, design, and tasks docs. Invoked with `/po [idea]` or `/po review [feature-name]`. |
| Tech Lead | `.claude/agents/tech-lead.md` | Plans non-trivial changes — approaches, tradeoffs, file scope — before implementation starts. Never writes code. |
| QA | `.claude/agents/qa.md` | Writes tests and finds edge cases. Invoked before marking features complete or when assessing coverage. |
| Reviewer | `.claude/agents/reviewer.md` | Reviews implementation against CLAUDE.md patterns after code is written, before committing. |
| Memory Keeper | `.claude/agents/memory-keeper.md` | Keeps CLAUDE.md, docs/, and agent files accurate after sessions that establish new patterns or resolve inconsistencies. |

## Typical Workflow

```
/po [idea]
  → requirements.md (confirm)
  → design.md (confirm)
  → tasks.md

Tech Lead  →  Implementation  →  QA  →  Reviewer  →  Memory Keeper
```
