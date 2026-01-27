# Implementation Details (MVP)

This document captures MVP-level internal behavior that goes beyond the public API contract.
Refer to `docs/02-architecture.md` for component-level context.

---

## Error Handling (MVP)

- Parsing errors → 400
- Validation errors → 400
- Internal timeout → 204 or 503
- Error responses return a JSON body: `{ "error": "<message>" }`

---

## Normalization

The gateway normalizes incoming OpenRTB requests into a 2.6-first internal model.

### Required fields

- `id` must be present and non-blank
- `imp` must exist and contain at least one impression
- Exactly one of `site` or `app` must be present

### Impression rules

- `imp.id` must be present and non-blank
- At least one of `banner`, `video`, `audio`, or `native` must be present
- If multiple types are present, select deterministically: `video` > `audio` > `banner` > `native`
- `imp.bidfloor` defaults to `0` if missing

### Context and defaults

- `inventoryType` is derived from `site` vs `app`
- `tmax`:
  - Default to `100ms` if missing
  - Clamp to the range `10–1000ms`

### Pass-through (lossless for partner-specific data)

- Preserve `ext` on:
  - top-level request
  - `imp`
  - `site` / `app`
  - `device`
  - `user`
  - `regs`

### Device fields kept

- `ua`, `ip`, `os`, `devicetype` (plus `device.ext`)
