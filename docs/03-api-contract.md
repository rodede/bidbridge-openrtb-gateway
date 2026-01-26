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
