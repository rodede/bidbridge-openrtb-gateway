# AI Agent Guidelines — BidBridge

This repository contains a Java OpenRTB 2.6 gateway built with Spring Boot WebFlux.
The system is performance-critical and latency-sensitive.

## Core Rules (Always On)

- Treat `codex.md` as the **entry point for task context**
- Follow requirements and constraints from `docs/` — do not invent behavior
- Prefer **MVP implementations**; avoid premature or speculative abstractions
- Use **non-blocking, reactive** patterns only (WebFlux / Reactor)
- Optimize for **low latency and low allocation**
- Keep modules cleanly separated (API / domain / rules / adapters / observability)

## Code Expectations

- Language: Java 25, Spring Boot 4, WebFlux
- Follow existing package and naming conventions
- Fail fast on invalid OpenRTB input
- Write unit tests for core logic; add integration tests when relevant
- Keep comments minimal and code readable

## When Context Is Missing

- Do not guess requirements
- Refer back to `codex.md` to identify the correct document(s)
- Ask for clarification only if the docs do not define the behavior

## Security & Reliability

- Validate inbound OpenRTB requests early
- Never block on I/O
- Do not add background threads or schedulers unless explicitly required
- Keep secrets out of source control

## Project Priorities

- This is a personal learning/playground project.
- `bidbridge-engine` is the primary service and should receive most design/implementation focus.
- `bidbridge-simulator` is a supporting test endpoint for DSP behavior and third-party communication scenarios.
- Keep simulator scope intentionally minimal; avoid complex features that do not improve engine testing.

## AWS Considerations

- Design with AWS deployment compatibility in mind.
- Prefer AWS Free Tier and minimize cost (ideally zero cost unless there is clear value).
- Treat paid AWS components as optional unless they are needed for a concrete testing or reliability gap.
