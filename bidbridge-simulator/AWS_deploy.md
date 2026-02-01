# BidBridge Simulator — AWS Deployment (MVP)

Concise, end-to-end plan for deploying the simulator to AWS with MVP auth.

---

## Target Architecture (MVP)

Client services
→ API Gateway (HTTP API, no JWT)
→ VPC Link
→ Internal ALB
→ ECS Fargate (private subnets)

Notes:
- ECS tasks are never public.
- Only API Gateway can reach the ALB.
- Auth is enforced in-app via shared-secret headers.

---

## Current Simulator Features (already implemented)

- External `dsps.yml` config (local or `s3://bucket/key`)
- In-memory config store with atomic reload + “keep last good config” on error
- Polling-based hot reload + manual `POST /admin/reload-dsps`
- Request correlation and one-line request logs
- Metrics via `/actuator/prometheus`
- Basic error handling (clean 4xx/5xx, no stack traces)
- Request size limit (1MB), response timeout, and in-flight limit (429)

---

## Shared-Secret Auth (MVP)

### Bid endpoints
- Paths: `/openrtb2/**`
- Header: `X-Api-Key: <shared-secret>`
- Missing/invalid → `401 Unauthorized`

### Admin endpoints
- Paths: `/admin/**`
- Header: `X-Admin-Token: <admin-secret>`
- Missing/invalid → `401 Unauthorized`

### Config (application-aws.yml)
```
simulator:
  auth:
    enabled: true
    bidApiKey: "${BID_API_KEY:}"
    adminApiToken: "${ADMIN_API_TOKEN:}"
```

---

## AWS Deployment Plan (step by step)

### 1) Container image & ECR
- Create an ECR repository (e.g., `bidbridge-simulator`).
- Build the image locally or via CI.
- Tag with a version (e.g., `v0.1.0` or Git SHA) and push to ECR.

### 2) IAM roles
- **Task execution role**: standard ECS execution policy (pull image, logs).
- **Task role**: grant S3 + Secrets Manager access (see policy below).
- Optional: if using GitHub Actions OIDC, create a role for CI/CD with ECR + ECS permissions.

### 3) Secrets Manager
- Store two secrets:
  - `BID_API_KEY`
  - `ADMIN_API_TOKEN`
- Use one shared key for bid endpoints and a separate admin secret.

### 4) ECS task definition
- Use Fargate.
- Set env vars (see example below).
- Inject secrets into env vars from Secrets Manager.
- Ensure `SPRING_PROFILES_ACTIVE=aws`.

### 5) ECS service
- Create service in private subnets.
- Attach to internal ALB target group.
- Health checks: `/actuator/health/liveness` and `/actuator/health/readiness`.

### 6) Networking
- Internal ALB only; no public access to tasks.
- Security group allows ALB → app port (8081).
- Optional: separate management port for actuator.

### 7) API Gateway
- HTTP API with no JWT authorizer (MVP).
- Route `/openrtb2/{dsp}/bid` and `/admin/**` to VPC Link → internal ALB.
- Optional: per-route throttling.

### 8) WAF
- Attach WAF to API Gateway.
- Add a rate-based rule for basic abuse protection.

### 9) Validate
- Call `/openrtb2/{dsp}/bid` with `X-Api-Key`.
- Call `/admin/dsps` with `X-Admin-Token`.
- Confirm metrics and logs.

---

## ECS / Secrets Manager wiring pattern

### Environment variables
- `SPRING_PROFILES_ACTIVE=aws`
- `AWS_REGION=eu-central-1` (or your region)
- `DSPS_FILE=s3://your-bucket/dsps.yml`
- `BID_API_KEY=<from Secrets Manager>`
- `ADMIN_API_TOKEN=<from Secrets Manager>`
- `JAVA_OPTS=-Xms256m -Xmx512m` (optional)

### IAM task role policy (snippet)
```
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject"],
      "Resource": "arn:aws:s3:::your-bucket/dsps.yml"
    },
    {
      "Effect": "Allow",
      "Action": ["s3:ListBucket"],
      "Resource": "arn:aws:s3:::your-bucket"
    },
    {
      "Effect": "Allow",
      "Action": ["secretsmanager:GetSecretValue"],
      "Resource": [
        "arn:aws:secretsmanager:REGION:ACCOUNT:secret:BID_API_KEY-*",
        "arn:aws:secretsmanager:REGION:ACCOUNT:secret:ADMIN_API_TOKEN-*"
      ]
    }
  ]
}
```

### ECS task definition (secrets section example)
```
"secrets": [
  {
    "name": "BID_API_KEY",
    "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT:secret:BID_API_KEY"
  },
  {
    "name": "ADMIN_API_TOKEN",
    "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT:secret:ADMIN_API_TOKEN"
  }
]
```

---

## App-level Security Plan (MVP)

1. Trust boundary
- Accept traffic only from the ALB security group (no direct public access to tasks).

2. Admin endpoints
- Require `X-Admin-Token` (stored in Secrets Manager).

3. Public bid endpoints
- Require `X-Api-Key` (stored in Secrets Manager).

4. Actuator
- Don’t expose `/actuator/**` publicly; allow only `/actuator/health`.

---

## Rate limiting + abuse protection

1. Add AWS WAF on API Gateway with a rate-based rule.
2. Optional: IP allow/deny if partner ranges are known.
3. Optional: API Gateway per-route throttles.

---

## Config hot-reload (implemented)

- `dsps.yml` is external (local or S3).
- Polls on a fixed interval; reloads only when timestamp changes.
- Manual reload: `POST /admin/reload-dsps` (secure this).
- Invalid YAML does not break running config (keeps last good config).

---

## Quick Pre-deploy Checklist (MVP)

- ✅ `dsps.yml` uploaded to S3 and readable by task role
- ✅ Secrets in Secrets Manager and injected into ECS task
- ✅ Security group rules for app port (and management port if used)
- ✅ Health checks configured (liveness/readiness)
- ✅ WAF rate-based rule enabled on API Gateway
- ✅ Metrics endpoint internal or restricted

---

## Identity & Token Plan (post-MVP)

1. Use Amazon Cognito User Pool as issuer for JWTs.
2. Each calling service gets its own client/app.
3. Token scopes:
- `aud = simulator`
- `scope` includes `simulator:bid`
- stricter `simulator:admin` for admin endpoints

---

## Where to store dsps.yml in AWS (pick 1 later)

- S3 (most common): download on startup + poll every N seconds, or reload on admin call.
- SSM Parameter Store (small configs): store YAML as a parameter, fetch + cache.
- Secrets Manager if you treat it as sensitive (usually not needed for DSP sim rules).

---

## Observability (current)

- Logs include requestId, dspId, status, latencyMs, durationMs.
- Metrics include request counts, latency, reload success/fail, active DSPs, rejections.

---

## Implementation Backlog (remaining)

- Dockerize simulator + run on ECS Fargate behind internal ALB.
- Add API Gateway HTTP API + VPC Link.
- Add WAF rate limiting + API Gateway throttles.
- Lock down management endpoints (internal only).
