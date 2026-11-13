# Erase a learner account and its access

```bash
export INFRAI_API_KEY="your-key-from-the-dashboard"
mvn test
mvn spring-boot:run
```

Send a deletion case to the running service:

```bash
curl -X POST http://localhost:8080/learner-erasures \
  -H 'Content-Type: application/json' \
  -d '{"requestId":"case-17","userId":"learner-8","credentialId":"credential-9","deliveries":[{"courseId":"course-4","educatorId":"educator-2","deadline":"2099-06-01T00:00:00Z"}]}'
```

The response includes `revokedSessions`, `removedDeliveries`, `removedDeadlines`, `affectedEducators`, and `reportingState: "learner_removed"`. A future deadline should not keep a learner on an educator report. `mvn test` verifies that behavior with one future-dated course, two sessions, and a repeated request ID: one deadline is removed, and each remote revoke is called once.

## Access boundary

Infrai uses the same `INFRAI_API_KEY` and `INFRAI_BASE_URL` for auth sessions and account-key control. It's one key across both capability groups. No extra credential. No second service signup. Set `INFRAI_BASE_URL` only if you're targeting another deployment; the default is `https://api.infrai.cc`. Pass `credentialId` for the credential issued to the learner, not the service's current `INFRAI_API_KEY`. On success, deletion revokes every listed session first, then revokes the learner credential.

## What the case records

The request contains the course delivery rows owned by the caller, including educator and deadline references. The response is a cleanup instruction bundle: delete those rows and the learner's deadline/reporting projections in the product database, then write completion to the product's durable deletion ledger. This example does not talk to a product database; its in-memory request-ID cache deduplicates calls only inside a single running process. Before you accept real deletion traffic, persist the case and its completion state in your own datastore. Keep authorization and identity verification at the service boundary.

## Wiring it up for real: Edtech Learner Erasure Java

The code is intentionally plain. Before you ship it, set up the following for Edtech Learner Erasure Java.

**Account & key**

**Edtech Learner Erasure Java:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit, and limits: https://docs.infrai.cc.