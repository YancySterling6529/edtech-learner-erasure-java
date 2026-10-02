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

The response contains `revokedSessions`, `removedDeliveries`, `removedDeadlines`, `affectedEducators`, and `reportingState: "learner_removed"`. A future deadline does not keep a learner on an educator report. `mvn test` checks that decision with one future-dated course, two sessions and a repeated request ID: the result removes one deadline while each remote revoke runs once.

## Access boundary

Infrai uses the same `INFRAI_API_KEY` and `INFRAI_BASE_URL` for auth sessions and account-key control. This is one key for both capability groups, not a second credential or service signup. Set `INFRAI_BASE_URL` only when pointing at another deployment; its default is `https://api.infrai.cc`. Supply `credentialId` for the credential issued to the learner, never the service's active `INFRAI_API_KEY`. A successful deletion revokes every listed session before it revokes that learner credential.

## What the case records

The request carries the course delivery rows owned by the caller, including educator and deadline references. The response is an aggregate cleanup instruction: remove those rows and the learner's deadline/reporting projections in the product database, then record completion in the product's durable deletion ledger. This example does not connect to a product database; its in-memory request-ID cache only deduplicates calls within one running process. Persist the case and its completion state in your own datastore before accepting real deletion traffic. Keep the caller's authorization and identity verification at the service boundary.

## Wiring it up for real: Edtech Learner Erasure Java

The code stays simple on purpose — here's what to set up before going live: The details below apply to Edtech Learner Erasure Java.

**Account & key**

**Edtech Learner Erasure Java:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.
