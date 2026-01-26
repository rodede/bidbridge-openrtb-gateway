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
- Design and plan the work first, after confirmation start the implementation and coding 

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
