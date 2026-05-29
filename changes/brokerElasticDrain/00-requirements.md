# Elastic Broker Drain Requirements

## Mission
Replace the fixed-rate broker drain with a demand-driven, container-managed scheduler that processes queued
`ReplicantSession` work concurrently while guaranteeing that no two workers process the same session at the same time.

## Scope Boundaries
- In scope:
  - `ReplicantMessageBroker` API shape.
  - `ReplicantMessageBrokerImpl` queueing, scheduling, batching, failure handling, and lifecycle behavior.
  - Package-private `ReplicantSession` helpers needed by broker scheduling.
  - Server-side unit tests for broker concurrency and scheduling behavior.
  - README documentation for deployer-visible broker tuning entries.
  - Concise `AGENTS.md` update for broker architecture/runtime guidance.
  - CHANGELOG documentation for the internal server broker API break unless implementation proves this is not
    externally visible.
- Out of scope:
  - Client protocol behavior.
  - `ReplicantSessionManagerImpl` routing semantics beyond preserving current `sendChangeMessage(...)` usage.
  - Raw Java thread or raw executor ownership inside Replicant.
  - Backwards-compatible shims for removed internal broker APIs.

## Locked Decisions
- The fixed-rate scheduled drain should be replaced by demand-driven drain tasks.
- Drain tasks must run on a container-managed executor, not raw application-created threads.
- Replicant owns the session scheduling semantics; the container owns physical thread execution.
- `queueChangeMessage(...)` remains the production entry point and schedules drain work after queueing a packet.
- `processPendingSessions()` does not need to remain on `ReplicantMessageBroker`; this is a hard API cut.
- Existing tests should be updated to the new API rather than preserved through compatibility shims.
- Broker queueing should coalesce session wakeups so a session is queued at most once while idle/queued/running.
- Per-session packets continue to accumulate in `ReplicantSession` packet queues.
- Broker scheduling state belongs in `ReplicantMessageBrokerImpl`, keyed by session id.
- Use a simple broker state machine: `IDLE -> QUEUED -> RUNNING -> IDLE`.
- The implementation can represent active states with a broker-owned enum, expected values `QUEUED` and `RUNNING`.
- A worker claims a queued session by atomically transitioning `QUEUED -> RUNNING`.
- Packets queued while a session is `RUNNING` should not enqueue duplicate session work; the worker catches this with a
  post-release pending-packet check.
- A worker should use `tryLock()` on `ReplicantSession.getLock()` and requeue at the tail if the lock is unavailable.
- Lock-contention requeue must not create a hot spin loop when no session made progress in a drain task.
- A worker should process a bounded number of packets per claimed session.
- `maxPacketsPerRun` applies per session claim and counts every popped packet.
- If packets remain after the per-session packet budget, requeue the session at the tail unconditionally.
- A single drain task must not reclaim the same session after requeueing it; remaining packets should be picked up by a
  later drain task.
- A drain task should process a bounded number of claimed sessions before yielding to the container.
- Replicant should gate logical concurrency with `maxConcurrentDrainTasks`.
- Active drain-task reservations must be atomic and must not exceed `maxConcurrentDrainTasks` under concurrent enqueue.
- Broker tuning should come from container configuration with built-in defaults when entries are absent.
- Resolve broker tuning once in `@PostConstruct`, validate once, and store primitive values for hot-path reads.
- Invalid configured values should fail startup with an `IllegalStateException`.
- Closed sessions dequeued by a worker should have broker scheduling state removed and should not be processed.
- If `sendChangeMessage(...)` closes a session without throwing while a batch is running, the worker must stop
  processing that session, remove broker scheduling state, and not requeue the session even if pending packets remain.
- Any throwable while processing a packet should log at `SEVERE`, close the session as an unexpected condition, remove
  broker state for that session, and allow the drain task to continue with other sessions.
- Submission failure should log at `SEVERE`, roll back active task accounting, leave queued work intact, and rely on a
  delayed retry unless the broker is stopping.
- Delayed retry should use a single outstanding retry guard so contention-only passes or submission failures cannot
  accumulate unbounded delayed callbacks.
- The broker retry delay should default to `20` milliseconds, matching the current scheduled drain period, and should
  remain an internal constant unless implementation discovers a deployment need for another runtime knob.
- `@PreDestroy` should mark the broker stopping, prevent new drain-task submissions, and allow already-running drain
  tasks to finish without waiting for them.
- After `@PreDestroy`, `queueChangeMessage(...)` may still queue packets and return a `Packet`, but it must not schedule
  new drain tasks.
- Switch the broker queue from `LinkedBlockingDeque` to `LinkedBlockingQueue` unless deque behavior becomes necessary.
- Add a package-private `ReplicantSession.hasPendingPackets()` helper for scheduling decisions.
- Document deployer-facing broker tuning in `README.md`.
- Add a concise `AGENTS.md` note covering the broker's demand-driven container-managed scheduling model and runtime
  tuning knobs.

## Command Surface And Behavior Expectations
- `ReplicantMessageBroker` should expose only queueing behavior needed by production callers:
  - `queueChangeMessage(...)`
- `queueChangeMessage(...)` should:
  - create and queue a `Packet` on the session,
  - atomically transition absent broker state to `QUEUED`,
  - enqueue the session only when that transition wins,
  - call the scheduler to submit drain tasks when useful,
  - return the created `Packet`.
- Drain task scheduling should:
  - submit tasks while queued work exists and `activeDrainTasks < maxConcurrentDrainTasks`,
  - reserve active task slots with an atomic compare-and-set loop before submit,
  - roll back the reservation if submit fails,
  - decrement active task count in task `finally`,
  - reschedule after task completion if queued work remains and useful progress was made,
  - schedule a delayed retry when queued work remains but the task made no useful progress because every claimed
    session was lock-contended.
- Delayed retry scheduling should:
  - use one atomic retry-scheduled guard shared by contention-only passes and executor submission failures,
  - use a default delay of `20` milliseconds,
  - no-op if stopping is true,
  - no-op and clear the guard if the queue is empty when the callback fires,
  - clear the guard before attempting normal drain scheduling so new retry needs can be observed,
  - catch delayed-schedule rejection, log at `SEVERE`, clear the guard, and leave queued work intact for a later enqueue
    or container recovery.
- Each drain task should:
  - process at most `maxSessionsPerDrainTask` claimed sessions,
  - skip stale queue entries that are no longer `QUEUED`,
  - skip closed sessions after removing broker state,
  - requeue sessions whose lock cannot be acquired immediately,
  - process at most `maxPacketsPerRun` packets per locked session claim,
  - claim the same session at most once per drain task,
  - check `session.isOpen()` after each processed packet and stop the session batch if it became closed,
  - release the lock before requeueing or removing scheduling state,
  - continue to the next session after a session-specific processing failure.
- A drain task counts useful progress when it processes at least one packet, removes a closed session, or closes a
  failed session. Lock-contention-only passes are no-progress passes.
- Session release should avoid missed wakeups:
  - remove `RUNNING` state after the lock is released,
  - check `session.hasPendingPackets()`,
  - if pending packets remain, transition absent state back to `QUEUED` and enqueue the session when that transition wins.
- Lifecycle behavior:
  - `preDestroy()` sets a stopping flag,
  - no new drain tasks are submitted once stopping is true,
  - active drain tasks use normal `finally` accounting and do not reschedule after stopping,
  - queued packets are retained in memory until their session is closed or the broker is discarded by container shutdown.

## Runtime Configuration
- The broker should resolve these container environment entries with explicit JNDI lookup through `InitialContext`.
  Each name should be looked up as `java:comp/env/<entry>`:
  - `replicant/broker/maxConcurrentDrainTasks`
  - `replicant/broker/maxPacketsPerRun`
  - `replicant/broker/maxSessionsPerDrainTask`
- Missing entries use defaults.
- Lookup failure semantics:
  - `NameNotFoundException` for an individual entry means the entry is absent and the default should be used.
  - `NoInitialContextException` means the broker is running in an unmanaged/test environment and all unresolved entries
    should use defaults.
  - Other `NamingException` failures indicate a broken naming environment and should fail startup with
    `IllegalStateException`.
- Accepted configured value types:
  - `Number`, interpreted with `intValue()`,
  - `String`, parsed with `Integer.parseInt(...)`.
- Wrong-type values, non-numeric strings, and parsed values below `1` fail startup with `IllegalStateException` naming
  the entry and value.
- Defaults:
  - `maxConcurrentDrainTasks`: `max(2, Runtime.getRuntime().availableProcessors())`
  - `maxPacketsPerRun`: `64`
  - `maxSessionsPerDrainTask`: `64`
- Valid range:
  - each configured value must be `>= 1`

## Quality Gates
- Targeted tests:
  - queueing a packet schedules drain work without calling a public/manual drain API,
  - same-session packet bursts produce only one queued session wakeup while active,
  - different sessions can be processed by separate drain tasks up to `maxConcurrentDrainTasks`,
  - simultaneous enqueues cannot reserve more than `maxConcurrentDrainTasks` active drain tasks,
  - a running session is not processed by a second worker,
  - lock contention causes tail requeue rather than blocking a drain task,
  - a single lock-contended queued session does not cause a tight reschedule loop,
  - repeated no-progress passes coalesce to one outstanding delayed retry,
  - `maxPacketsPerRun` yields and requeues when more packets remain,
  - `maxSessionsPerDrainTask` bounds a drain task,
  - closed sessions are removed from broker state and not sent,
  - a session closed by `sendChangeMessage(...)` mid-batch is not processed further and is not requeued,
  - executor submission failure keeps queued work intact, restores active task accounting, and schedules a delayed retry
    when the broker is not stopping,
  - delayed retry callback exits cleanly when stopping or when the queue is empty,
  - delayed retry scheduling rejection is logged and does not corrupt retry or active-task accounting,
  - packet processing failure closes only the affected session and does not poison the whole drain loop,
  - `preDestroy()` prevents future drain-task submission without corrupting active task accounting,
  - invalid config fails startup,
  - absent config uses defaults,
  - JNDI string, numeric, wrong-type, and non-numeric values are handled according to the config contract.
  - JNDI missing-entry, no-initial-context, and naming-failure cases are covered by broker config tests or a broker
    env-entry test harness.
- Full gate:
  - `bundle exec buildr test`

## Intentional Divergences
- The public/manual `processPendingSessions()` drain method will be removed from `ReplicantMessageBroker`.
- Tests should use the new queue-driven scheduling hooks or test-only executor control rather than preserving the old
  manual drain API.
- Because `ReplicantMessageBroker` is a public type in the server artifact, the hard cut should be treated as a
  release-note-worthy internal server API change unless implementation discovers it is not included in published
  artifacts.

## Open Questions Register

### Q-01
- status: resolved
- question: Should the fixed-rate scheduled drain be replaced by demand-driven workers?
- context: The existing broker polls every 20ms using `ManagedScheduledExecutorService`.
- options:
  - Replace with demand-driven drain tasks.
  - Keep the fixed-rate scheduled drain.
- tradeoffs:
  - Demand-driven tasks remove polling latency and scale with actual backlog.
  - Fixed-rate polling is simpler but keeps latency and does not express elastic demand.
- recommended_default: Replace with demand-driven drain tasks.
- user_decision: Replace the fixed-rate scheduled drain with demand-driven workers.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`
  - `changes/brokerElasticDrain/20-task-board.yaml`

### Q-02
- status: resolved
- question: Should broker queue entries be coalesced by session?
- context: Packets are already stored in per-session queues; duplicate outer queue entries create scheduler noise.
- options:
  - Coalesce outer session wakeups.
  - Queue duplicate session wakeups for every packet.
- tradeoffs:
  - Coalescing avoids unbounded duplicate queue entries for hot sessions.
  - Duplicate wakeups are simpler but distort backlog and scaling signals.
- recommended_default: Coalesce outer session wakeups.
- user_decision: Coalesce session wakeups.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-03
- status: resolved
- question: Should a worker drain all packets for a session or use a bounded batch?
- context: `sendChangeMessage(...)` can do meaningful routing and expansion work while holding the session lock.
- options:
  - Drain all packets.
  - Bound the per-session packet batch.
- tradeoffs:
  - Draining all packets maximizes locality but can let a hot session monopolize workers.
  - A bounded batch improves fairness while preserving per-session serialization.
- recommended_default: Bound the per-session packet batch.
- user_decision: Use a configurable per-session packet batch.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-04
- status: resolved
- question: Should Replicant manage long-lived worker loops or bounded short-lived drain tasks on the container executor?
- context: The container already provides a managed executor and should own physical threads.
- options:
  - Long-lived worker loops.
  - Bounded short-lived drain tasks.
- tradeoffs:
  - Long-lived loops duplicate pool lifecycle concerns already owned by the container.
  - Short-lived drain tasks let Replicant control logical concurrency while the container owns threads.
- recommended_default: Use bounded short-lived drain tasks.
- user_decision: Use bounded short-lived drain tasks on the container executor.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-05
- status: resolved
- question: Should scheduling state live in the broker or in `ReplicantSession`?
- context: Queued/running state is scheduler state rather than domain session state.
- options:
  - Store state in `ReplicantMessageBrokerImpl`.
  - Store state in `ReplicantSession`.
- tradeoffs:
  - Broker state keeps scheduling policy localized.
  - Session state would widen the session object's responsibilities.
- recommended_default: Store state in the broker.
- user_decision: Store scheduling state in `ReplicantMessageBrokerImpl`.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-06
- status: resolved
- question: How should packets queued while a session is already running be handled?
- context: Immediate duplicate enqueueing can inflate the outer queue, while suppressing enqueueing risks missed wakeups
  unless release is carefully designed.
- options:
  - Queue immediately even when running.
  - Hybrid coalescing: enqueue only when transitioning idle to queued, then check pending packets after running.
- tradeoffs:
  - Immediate enqueueing is simple but can create many stale queue entries.
  - Hybrid coalescing avoids duplicate wakeups but requires correct release checks.
- recommended_default: Hybrid coalescing.
- user_decision: Use hybrid coalescing.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-07
- status: resolved
- question: Should `processPendingSessions()` remain on the public broker interface?
- context: Existing tests call it, but the new production model schedules from `queueChangeMessage(...)`.
- options:
  - Keep it as a compatibility hook.
  - Remove it and update tests to the new API.
- tradeoffs:
  - Keeping it reduces test churn but preserves an obsolete internal API.
  - Removing it matches direct API evolution and avoids compatibility shims.
- recommended_default: Remove it and update tests.
- user_decision: Remove it from the public interface and update tests; no compatibility shims.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-08
- status: resolved
- question: Where should broker tuning values come from?
- context: These are runtime deployment values, while physical pool ownership remains with the container executor.
- options:
  - JVM system properties.
  - Container environment entries with defaults.
  - Container executor configuration only.
- tradeoffs:
  - JVM properties match some existing client config style but are less container-native for server deployment.
  - Environment entries are container-native but require careful default handling.
  - Executor configuration alone cannot express Replicant-specific scheduling budgets.
- recommended_default: Container environment entries with built-in defaults.
- user_decision: Use container environment entries with built-in defaults.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-09
- status: resolved
- question: Should invalid configured values fail startup or be clamped?
- context: Zero or negative concurrency/budget values can make the broker stuck or unfair.
- options:
  - Fail startup.
  - Clamp to defaults and log a warning.
- tradeoffs:
  - Failing startup makes bad deployments obvious.
  - Clamping is more forgiving but can hide deployment errors.
- recommended_default: Fail startup.
- user_decision: Fail startup with `IllegalStateException`.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-10
- status: resolved
- question: Should `maxPacketsPerRun` apply per session or globally across a drain task?
- context: Fairness must be controlled at both the hot-session level and task-duration level.
- options:
  - Per session claim.
  - Global drain-task budget.
- tradeoffs:
  - Per-session budget gives each claimed session a fair slice.
  - Global budget can still let one hot session consume an entire task.
- recommended_default: Per session claim.
- user_decision: `maxPacketsPerRun` applies per session claim.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-11
- status: resolved
- question: Should `queueChangeMessage(...)` synchronously schedule drain tasks?
- context: Demand-driven behavior needs a wakeup path when new work arrives.
- options:
  - Schedule synchronously after queueing.
  - Rely on a timer or external trigger.
- tradeoffs:
  - Synchronous scheduling is immediate and removes polling.
  - Timer/external scheduling preserves delay and adds another moving part.
- recommended_default: Schedule synchronously after queueing.
- user_decision: Schedule synchronously after queueing.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-12
- status: resolved
- question: How should executor submission failures be handled?
- context: Submission can fail if the container rejects work during shutdown or overload.
- options:
  - Drop work.
  - Throw to caller.
  - Log, roll back accounting, leave work queued for later retry.
- tradeoffs:
  - Dropping work loses replication packets.
  - Throwing changes caller semantics and can fail unrelated service work.
  - Leaving work queued delays replication but preserves state.
- recommended_default: Log, roll back accounting, leave work queued.
- user_decision: Log at `SEVERE`, restore active task accounting, and leave queued work intact.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-13
- status: resolved
- question: Should closed sessions be removed from broker scheduling state?
- context: A closed session cannot receive queued packets.
- options:
  - Remove scheduling state and skip processing.
  - Keep state until explicit session-manager cleanup.
- tradeoffs:
  - Removing state avoids leaks and pointless sends.
  - Keeping state risks retained packets for sessions that cannot drain.
- recommended_default: Remove scheduling state and skip processing.
- user_decision: Remove state and do not process closed sessions.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-14
- status: resolved
- question: Should a worker block on the session lock or use `tryLock()`?
- context: Subscribe/unsubscribe paths also hold the session lock.
- options:
  - Block with `lockInterruptibly()`.
  - Use `tryLock()` and requeue if unavailable.
- tradeoffs:
  - Blocking is simpler but can occupy a drain task doing no useful work.
  - `tryLock()` lets the task make progress on other sessions.
- recommended_default: Use `tryLock()` and requeue.
- user_decision: Use `tryLock()` and requeue at the tail if the lock is unavailable.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-15
- status: resolved
- question: Should a session with remaining packets requeue at the tail or continue immediately when no other work is waiting?
- context: Requeue behavior controls hot-session fairness.
- options:
  - Requeue at the tail unconditionally.
  - Continue immediately if the queue appears empty.
- tradeoffs:
  - Tail requeue is simple and fair.
  - Continuing immediately can improve locality but risks monopolization and depends on racy queue observations.
- recommended_default: Requeue at the tail unconditionally.
- user_decision: Requeue at the tail unconditionally.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-16
- status: resolved
- question: Does `ReplicantSession` need a pending-packet helper?
- context: The broker needs to know whether it should requeue a session after a run.
- options:
  - Add package-private `hasPendingPackets()`.
  - Inspect queues indirectly or avoid the check.
- tradeoffs:
  - A helper keeps broker code direct and avoids exposing packet queues.
  - Avoiding the helper makes missed wakeup handling harder.
- recommended_default: Add package-private `hasPendingPackets()`.
- user_decision: Add package-private `ReplicantSession.hasPendingPackets()`.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-17
- status: resolved
- question: Is a simple enum state enough for broker work state?
- context: The broker must avoid missed wakeups without adding unnecessary counters.
- options:
  - Use enum states with atomic map transitions.
  - Use counters or generation versions.
- tradeoffs:
  - Enum state is simpler and enough with remove-then-check release ordering.
  - Counters add complexity unless the enum protocol proves insufficient.
- recommended_default: Use enum states with atomic map transitions.
- user_decision: Use enum states with atomic map transitions.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-18
- status: resolved
- question: Should the broker queue remain a deque?
- context: The design only needs FIFO operations.
- options:
  - Keep `LinkedBlockingDeque`.
  - Switch to `LinkedBlockingQueue`.
- tradeoffs:
  - Deque support is unused.
  - Queue better communicates FIFO-only behavior.
- recommended_default: Switch to `LinkedBlockingQueue`.
- user_decision: Switch to `LinkedBlockingQueue`.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-19
- status: resolved
- question: How should packet processing failures be handled?
- context: A thrown exception from one session must not poison all broker scheduling.
- options:
  - Close the affected session and continue.
  - Keep retrying later packets.
  - Stop the whole drain task.
- tradeoffs:
  - Closing the affected session avoids infinite poison retries.
  - Retrying can loop forever if the packet is deterministic.
  - Stopping the task reduces isolation.
- recommended_default: Close the affected session and continue.
- user_decision: Close the affected session, remove broker state, and continue the drain task.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-20
- status: resolved
- question: Should `maxPacketsPerRun` count packets that produce no outgoing WebSocket message?
- context: A popped packet may still do work before `changeSet.hasContent()` decides no send is needed.
- options:
  - Count every popped packet.
  - Count only packets that send a WebSocket message.
- tradeoffs:
  - Counting popped packets makes budgets deterministic.
  - Counting sent messages makes budgets content-dependent.
- recommended_default: Count every popped packet.
- user_decision: Count every popped packet.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-21
- status: resolved
- question: Should broker config be resolved once or dynamically?
- context: These values define scheduler semantics and are read on the hot path.
- options:
  - Resolve once in `@PostConstruct`.
  - Resolve dynamically on every enqueue or task.
- tradeoffs:
  - Startup resolution is predictable and cheap.
  - Dynamic resolution is container-specific and harder to reason about.
- recommended_default: Resolve once in `@PostConstruct`.
- user_decision: Resolve once in `@PostConstruct`.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`

### Q-22
- status: resolved
- question: Should implementation update `AGENTS.md` despite the earlier preference for README-only documentation?
- context: The repository guidelines say `AGENTS.md` must be updated when runtime flags or architectural concepts change.
  The user also chose README-only when asked where to document the broker config entries.
- options:
  - Update README only and leave `AGENTS.md` unchanged.
  - Update README for deployer-facing config and add a concise `AGENTS.md` note to satisfy the repository rule.
- tradeoffs:
  - README-only follows the narrow documentation preference but conflicts with the repository guideline.
  - Updating both preserves repo governance while keeping deployer details in README.
- recommended_default: Update README for config details and add only a concise `AGENTS.md` architecture/runtime note.
- user_decision: Update `AGENTS.md` as part of the plan, with README retaining deployer-facing configuration details.
- artifacts_updated:
  - `changes/brokerElasticDrain/00-requirements.md`
  - `changes/brokerElasticDrain/10-implementation-plan.md`
  - `changes/brokerElasticDrain/20-task-board.yaml`
