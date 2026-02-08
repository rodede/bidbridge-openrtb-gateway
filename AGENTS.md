# AI Agent Guidelines — BidBridge

This repository contains a Java OpenRTB 2.6 gateway built with Spring Boot WebFlux.
The system is performance-critical and latency-sensitive.

## Context Index

Use this file as the entry point for task context.  
Use `docs/` for repo-wide context, then use module-local guidance.

- `docs/01-vision-and-scope.md` — product goals/scope
- `docs/02-roadmap.md` — milestones and sequencing
- `bidbridge-engine/AGENTS.md` — engine-specific rules and docs index
- `bidbridge-simulator/AGENTS.md` — simulator-specific rules
- `bidbridge-loadgen/README.md` — load generator usage

## Core Rules (Always On)

- Treat `AGENTS.md` as the **entry point for task context**
- Follow requirements and constraints from docs — do not invent behavior
- Present a short implementation plan before making code changes
- Prefer **MVP implementations**; avoid premature or speculative abstractions
- Use **non-blocking, reactive** patterns only (WebFlux / Reactor)
- Optimize for **low latency and low allocation**
- Keep module boundaries clear and avoid cross-module coupling unless required

## Code Expectations

- Language: Java 25, Spring Boot 4, WebFlux
- Follow existing package and naming conventions
- Write unit tests for core logic; add integration tests when relevant
- Keep comments minimal and code readable

## When Context Is Missing

- Do not guess requirements
- Refer back to this `AGENTS.md` index to identify the correct document(s)
- Ask for clarification only if the docs do not define the behavior

## Project Priorities

- This is a personal learning/playground project.
- `bidbridge-engine` is the primary service and should receive most design/implementation focus.
- `bidbridge-simulator` is a supporting test endpoint for DSP behavior and third-party communication scenarios.
- Keep simulator scope intentionally minimal; avoid complex features that do not improve engine testing.

## AWS Considerations

- Design with AWS deployment compatibility in mind.
- Prefer AWS Free Tier and minimize cost (ideally zero cost unless there is clear value).
- Treat paid AWS components as optional unless they are needed for a concrete testing or reliability gap.

## Plan Format

- 3-6 implementation steps
- Key risk/assumption notes
- Validation commands/tests to run
- Rollback note for risky changes

## Definition of Done

- Requested changes implemented
- Relevant tests pass
- Related docs/readmes updated when behavior/config/metrics change

## Observability Rule

- Document Prometheus timer series using exported names (`*_seconds_sum`, `*_seconds_count`, buckets when applicable)

## PR Safety Checks

- No secrets in code or docs
- No new background schedulers/threads unless explicitly required
