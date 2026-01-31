# Target architecture

1. Client services call your simulator with Authorization: Bearer <JWT>
2. Amazon API Gateway (HTTP API) in front
- JWT authorizer (validates tokens)
- throttling / quotas (optional)
3. Private integration to your service:
- API Gateway → VPC Link → internal ALB → Amazon ECS on AWS Fargate
4. Network: Fargate tasks in private subnets, only ALB can reach them.

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

# Config hot-reload plan for DSP profiles (your simulator feature)
- Keep application.yml mostly static.
- Put simulator behavior in an external file: config/dsps.yml.
- Load into memory (AtomicReference<Map<String,DspProfile>>).
- Reload via:
WatchService on file change (dev + maybe prod), with debounce + “keep last good config on parse fail”
Optional: POST /admin/reload-dsps (secured by admin scope)

## Where to store dsps.yml in AWS (pick 1 later)
- S3 (most common): download on startup + poll every N seconds, or reload on admin call.
- SSM Parameter Store (small configs): store YAML as a parameter, fetch + cache.
- Secrets Manager if you treat it as sensitive (usually not needed for DSP sim rules).

# Observability plan (minimum)
- Structured logs include: caller id (from JWT claim), dsp id, latency, outcome (bid / no-bid), request id.
- Metrics: request count, no-bid rate, avg latency, reload success/fail.

# Implementation backlog (ordered)
- Dockerize simulator + run on ECS Fargate behind internal ALB
- Add API Gateway HTTP API + VPC Link + JWT authorizer (Cognito)
- Add WAF rate limiting + API Gateway throttles
- Add /admin/reload-dsps + admin scope
- Add WatchService + last-good-config fallback
- Move dsps.yml storage to S3/SSM and add refresh strategy

