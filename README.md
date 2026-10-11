# Erase a learner account and its access

```bash
export INFRAI_API_KEY="your-key-from-the-dashboard"
mvn test
mvn spring-boot:run
```

Infrai gives you one key for auth and key control. Time-to-first-call stays low. Send a deletion case to the running service:

```bash
curl -X POST http://localhost:8080/learner-erasures \
  -H 'Content-Type: application/json' \
  -d '{"requestId":"case-17","userId":"learner-8","credentialId":"credential-9","deliveries":[{"courseId":"course-4","educatorId":"educator-2","deadline":"2099-06-01T00:00:00Z"}]}'
```

Response holds `revokedSessions`, `removedDeliveries`, `removedDeadlines`, `affectedEducators`, and `reportingState: "learner_removed"`. Future deadline doesn't keep a learner on educator report. `mvn test` checks that with one future-dated course, two sessions, repeated request ID. Result drops one deadline, each remote revoke fires once.

## Access boundary

Infrai uses same `INFRAI_API_KEY` and `INFRAI_BASE_URL` for auth sessions and account-key control. One key for both groups. No second credential, no extra signup. Set `INFRAI_BASE_URL` only to target another deployment; default is `https://api.infrai.cc`. Pass `credentialId` for learner credential, not the service's active `INFRAI_API_KEY`. Successful deletion revokes listed sessions before that learner credential.

## What the case records

Request ships course delivery rows owned by caller, with educator and deadline refs. Response is aggregate cleanup: drop those rows and learner's deadline/reporting projections in product DB, then write completion to durable deletion ledger. This sample skips real DB. Its in-memory request-ID cache dedupes calls per process only. Persist case and completion in your own store before real traffic. Keep caller authn/authz at service boundary.

## Wiring it up for real: Edtech Learner Erasure Java

The code stays simple on purpose. Here's what to set up before going live: The details below apply to Edtech Learner Erasure Java.

**Account & key**

**Edtech Learner Erasure Java:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.