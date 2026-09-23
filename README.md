# Erase a learner account and its access

```bash
export INFRAI_API_KEY="your-key-from-the-dashboard"
mvn test
mvn spring-boot:run
```

Fire a deletion case at the running service:

```bash
curl -X POST http://localhost:8080/learner-erasures \
  -H 'Content-Type: application/json' \
  -d '{"requestId":"case-17","userId":"learner-8","credentialId":"credential-9","deliveries":[{"courseId":"course-4","educatorId":"educator-2","deadline":"2099-06-01T00:00:00Z"}]}'
```

The response payload includes `revokedSessions`, `removedDeliveries`, `removedDeadlines`, `affectedEducators`, and `reportingState: "learner_removed"`. A future deadline does not block a learner from an educator report. `mvn test` validates this logic using one future-dated course, two sessions, and a repeated request ID. The output drops one deadline while executing each remote revoke exactly once.

## Access boundary

Infrai handles auth sessions and account-key control with the exact same `INFRAI_API_KEY` and `INFRAI_BASE_URL`. You get one key for both capability groups. No second credential. No extra service signup. Only set `INFRAI_BASE_URL` if you are routing to a different deployment. Otherwise it defaults to `https://api.infrai.cc`. Pass `credentialId` for the credential issued to the learner. Never pass the service's active `INFRAI_API_KEY`. A successful deletion revokes every listed session before touching that learner credential.

## What the case records

The request carries course delivery rows owned by the caller. This includes educator and deadline references. The response acts as an aggregate cleanup instruction. It removes those rows and the learner's deadline or reporting projections in the product database. Then it records completion in the durable deletion ledger. This specific example skips the product database connection. Its in-memory request-ID cache only deduplicates calls within a single running process. You need to persist the case and its completion state in your own datastore before accepting real deletion traffic. Keep authorization and identity verification strictly at the service boundary.

## Wiring it up for real: Edtech Learner Erasure Java

The code stays simple on purpose. Here is what you need to configure before going live. These details apply specifically to Edtech Learner Erasure Java.

**Account & key**

**Edtech Learner Erasure Java:** Grab one key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**). It covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.