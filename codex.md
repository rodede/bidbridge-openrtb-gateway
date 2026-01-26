# BidBridge — High-performance OpenRTB Gateway (Context Index)

This file is the entrypoint for AI-assisted development (Codex).  
Use it to select the right context document(s) for the task you are implementing.

## How to use with AI tools

- When starting a task, include **this file** plus the **relevant docs** from `docs/`.
- Prefer small context packs:
  - Always include: `codex.md`
  - Then include: only 1–3 docs relevant to the current task
- If using IntelliJ AI/Copilot:
  - Open `codex.md` + relevant docs + the target source file(s)
  - Ask the assistant to implement based on the open documents

---

## Document Map (single source of truth)

All requirements are split into purpose-focused documents:

- `docs/01-vision-and-scope.md` — Product vision, goals, scope, phases, out-of-scope
- `docs/02-architecture.md` — Components, data flow, responsibilities, constraints
- `docs/03-api-contract.md` — Endpoints, status codes, request/response rules, examples
- `docs/04-adapters.md` — Adapter model, interfaces, timeouts, fallback, examples
- `docs/05-rules-engine.md` — Rules concepts, evaluation order, config approach
- `docs/06-observability.md` — Metrics/tracing/logging conventions and expectations
- `docs/07-testing.md` — SSP simulator, test strategy, load/chaos testing approach
- `docs/08-performance-security.md` — Performance targets, security & compliance rules
- `docs/09-roadmap.md` — Implementation roadmap and milestones

---

## Deliverable Expectations (for generated code)

When implementing any module, produce:

- Production-style package layout
- Clear interfaces and boundaries (API / domain / adapters / rules / observability)
- Unit tests for core logic
- Integration tests where applicable
- Minimal docs/comments where needed (avoid verbose commentary in code)
- No premature overengineering: implement MVP first
