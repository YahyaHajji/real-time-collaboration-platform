# Engineering Notes

These postmortems capture two concurrency and transport bugs that shaped the final design and regression suite.

## Retaining the authenticated STOMP principal

**Symptom:** a valid JWT completed the STOMP handshake, but `/app/chat.send` produced no database row or broadcast.

**Root cause:** the inbound interceptor called `StompHeaderAccessor.wrap(message)`, set the user on the new accessor, and returned the original message. The authenticated principal was therefore absent from later frames in the same WebSocket session.

**Resolution:** the interceptor now obtains the existing mutable accessor through `MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class)` and attaches the principal there. Disconnect cleanup also runs before authentication checks because a closing transport may no longer carry a principal.

**Regression coverage:** a Testcontainers integration test exercises the complete path: REST login, STOMP `CONNECT` with JWT, subscription, `SEND`, post-commit broadcast, and persisted REST history.

## Reconciling stale presence snapshots

**Symptom:** a user appeared through a live presence event and disappeared when an older in-flight REST snapshot completed.

**Root cause:** the client store replaced its presence state with the snapshot and lost deltas received while that request was in progress. A superseding snapshot request could also discard changes buffered by the first request.

**Resolution:** the store buffers live deltas while a snapshot is active, carries the buffer into superseding requests, and replays those changes over the latest accepted snapshot.

**Regression coverage:** Vitest covers single and overlapping snapshots. Playwright deliberately holds a stale `/api/presence` response while a newer online event arrives and verifies that the live user remains visible.
