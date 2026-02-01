# Target architecture

1. Client services call your simulator with Authorization: Bearer <JWT>
2. Amazon API Gateway (HTTP API) in front
- JWT authorizer (validates tokens)
- throttling / quotas (optional)
3. Private integration to your service:
- API Gateway → VPC Link → internal ALB → Amazon ECS on AWS Fargate
4. Network: Fargate tasks in private subnets, only ALB can reach them.

# Current simulator features (already implemented)

- External `dsps.yml` config (local or `s3://bucket/key`)
- In-memory config store with atomic reload + "keep last good config" on error
- Polling-based hot reload on timestamp change + manual `POST /admin/reload-dsps`
- Request correlation and one-line request logs
- Metrics via `/actuator/prometheus`
- Basic error handling (clean 4xx/5xx, no stack traces in responses)
- Request size limit (1MB), response timeout, and in-flight limit (429)

# Deployment plan (clear steps)

## 1) Build & runtime
- Package container (Docker) for `bidbridge-simulator`.
- Set runtime profile: `SPRING_PROFILES_ACTIVE=aws`.
- Provide `dsps.file` as `s3://bucket/key`.
- Set AWS region: `AWS_REGION` or `AWS_DEFAULT_REGION`.
- Set JVM opts if needed: `JAVA_OPTS=-Xms256m -Xmx512m` (example).

## 1.1) Required env vars (minimum)
- `SPRING_PROFILES_ACTIVE=aws`
- `AWS_REGION=eu-central-1` (or your region)
- `DSPS_FILE=s3://your-bucket/path/dsps.yml` (if you override `dsps.file`)

## 2) Networking
- ECS Fargate tasks in private subnets.
- Internal ALB only (no public access to tasks).
- Security group: allow ALB → app port (8081).
 - Optional: separate management port (see section 4).

## 3) API Gateway + auth
- HTTP API with JWT authorizer (Cognito).
- Route `/openrtb2/{dsp}/bid` to VPC Link → internal ALB.
- Optional: per-route throttling.

## 4) Health + metrics exposure
- Public ALB health checks: `/actuator/health/liveness` and `/actuator/health/readiness`.
- Expose `/actuator/prometheus` only on an internal path/port (or via SG rules).
 - If using a separate management port:
   - `management.server.port=8082`
   - Allow 8082 only from VPC/internal monitoring.

## 5) Admin endpoint security
- Protect `POST /admin/reload-dsps` with stricter auth:
  - Route-level JWT scope (e.g., `simulator:admin`), or
  - Additional secret header checked at gateway.

## 6) Observability
- Logs already include request summary fields (requestId, dsp, status, latency, duration).
- Metrics already emitted (see README).
 - Send logs to CloudWatch (ECS log driver).
 - Optional alarms: 5xx rate, high latency, reload failures.

# Identity & token plan
1. Use Amazon Cognito User Pool as issuer for JWTs.
2. Each calling service gets its own client/app (so you can revoke/rotate per caller).
3. Token scopes/claims plan (MVP):
- aud = simulator
- scope includes simulator:bid
- stricter scope simulator:admin for admin endpoints

# App-level security plan (keep it light)
Even though API Gateway validates JWT, still do these in-app:
1. Trust boundary
- Accept traffic only from the ALB security group (no direct public access to tasks).
2. Admin endpoints
- Put /admin/** behind extra protection:

--> Option A: separate route in API Gateway that requires simulator:admin scope

--> Option B: additionally require X-Admin-Token (stored in Secrets Manager)

3. Disable / restrict actuator
- Don’t expose /actuator/** publicly, or only allow /actuator/health.

# Rate limiting + abuse protection plan
1. Add AWS WAF on API Gateway (or ALB) with:
- rate-based rule (basic DoS protection)
- IP allow/deny rules if you know partner IP ranges
2. Also set API Gateway per-route throttles.

# Config hot-reload plan (implemented)
- `dsps.yml` is external (local or S3).
- Polls on a fixed interval; reloads only when timestamp changes.
- Manual reload: `POST /admin/reload-dsps` (secure this).
- Invalid YAML does not break running config (keeps last good config).

# IAM permissions (S3)
Task role needs:
- `s3:GetObject` for the dsps.yml key
- `s3:ListBucket` if you want to validate bucket access

# Example ECS task/container env
- `SPRING_PROFILES_ACTIVE=aws`
- `AWS_REGION=eu-central-1`
- `DSPS_FILE=s3://your-bucket/dsps.yml`
- `JAVA_OPTS=-Xms256m -Xmx512m`

# Quick pre-deploy checklist
- ✅ dsps.yml uploaded to S3 and readable by task role
- ✅ Security group rules for app port (and management port if used)
- ✅ Health checks configured (liveness/readiness)
- ✅ Metrics endpoint internal or restricted

## Where to store dsps.yml in AWS (pick 1 later)
- S3 (most common): download on startup + poll every N seconds, or reload on admin call.
- SSM Parameter Store (small configs): store YAML as a parameter, fetch + cache.
- Secrets Manager if you treat it as sensitive (usually not needed for DSP sim rules).

# Observability (current)
- Logs include requestId, dspId, status, latencyMs, durationMs.
- Metrics include request counts, latency, reload success/fail, active DSPs, rejections.

# Implementation backlog (remaining)
- Dockerize simulator + run on ECS Fargate behind internal ALB.
- Add API Gateway HTTP API + VPC Link + JWT authorizer (Cognito).
- Add WAF rate limiting + API Gateway throttles.
- Lock down management endpoints (internal only).


  2. Add ECS task definition template
  3. Add short ECS quickstart doc