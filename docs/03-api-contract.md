# API Contract

## Endpoint
The gateway is a **bidder endpoint** for SSPs; it does **not** communicate directly with players.

Endpoint: `POST /openrtb2/bid`
Content-Type: `application/json`

For audio/video, creative is typically embedded as **VAST XML in `adm`** and returned to SSP.

---

## Request

- Must comply with OpenRTB 2.6
- Must contain at least one impression
- Supports banner, audio, and video

Backward compatibility:
- OpenRTB **2.5 requests are fully accepted**
- 2.5 is treated as a **subset of 2.6**
- Unknown fields are ignored and preserved via `ext`
- Requests MUST NOT be rejected based on minor version

---

## Responses

| Case       | Status | Body             |
|------------|--------|------------------|
| Bid        | 200    | BidResponse JSON |
| No-bid     | 204    | Empty            |
| Bad input  | 400    | Error message    |
| Overload   | 503    | Empty            |

---

## Headers

- X-OpenRTB-Version: 2.6
- Accept-Encoding: gzip

---

## BidResponse Rules

- id must match request id
- impid must match impression id
- price must be > 0
- adm contains creative or VAST

---

## Error Handling

- Parsing errors → 400
- Validation errors → 400
- Internal timeout → 204 or 503

---

## Normalization (MVP)

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
