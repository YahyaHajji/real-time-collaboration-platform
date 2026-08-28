# Scalability and failure roadmap

The delivered application intentionally targets one Spring Boot instance. Its simple broker and presence map are process-local, which keeps the learning project observable and correct without pretending to provide multi-node guarantees.

## Current guarantees and limits

- PostgreSQL is the durable source for users, notifications, chat, and read cursors.
- Publication occurs after commit and is ordered through one bounded worker inside one process.
- Live delivery is best effort; reconnect recovery is authoritative and ID-based.
- A process crash between commit and publication can omit a live event, but the record appears in the next REST recovery.
- Presence is accurate for sessions attached to this process. A crash removes the process without sending graceful offline frames.
- The in-memory simple broker provides no cross-instance routing or durable subscription queue.

## Scale-out sequence

1. Put stateless backend instances behind a load balancer that supports WebSocket upgrades. Sticky sessions may be a temporary bridge, but they do not solve cross-instance fan-out.
2. Replace the simple broker with a RabbitMQ STOMP broker relay. User destinations and chat topics then route across application instances and gain broker backpressure. Kafka would be considered only for replayable domain streams or analytics, not as a direct WebSocket replacement.
3. Move presence to shared ephemeral storage such as Redis. Each session writes a leased key refreshed by heartbeats; expiry handles crashed clients and instances. Presence changes travel through the shared broker.
4. Add a transactional outbox when the required guarantee becomes “every committed change eventually publishes.” A relay claims rows, publishes with an event ID, records completion, and retries safely.
5. Define per-channel ordering keys before parallelizing publishers. Consumers still deduplicate event IDs because retries remain possible.
6. Partition or archive chat and notification tables only after query latency, retention, and volume measurements justify it.

## Operations needed before production

- TLS termination and secure `wss://` only.
- Managed RSA keys with rotation/JWKS, secrets management, and refresh/session revocation policy.
- Metrics for active sessions, handshake rejection reasons, broker queue depth, executor saturation, reconnect rate, event recovery lag, database latency, and outbox age.
- Centralized logs with subject/session hashes and strict redaction; never JWTs, message bodies, or notification payloads.
- Load tests for connection churn, fan-out, slow consumers, maximum frames, and rolling deploys.
- Alerting and runbooks for database outage, broker outage, key rotation error, reconnect storm, and stale presence leases.
