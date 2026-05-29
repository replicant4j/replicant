# Elastic Broker Drain Implementation Plan

Status: accepted after user review.

## Delivery Approach
Implement this as a server-side broker scheduler replacement. The container continues to own thread execution through
the managed executor. `ReplicantMessageBrokerImpl` owns logical scheduling: session coalescing, work-state transitions,
drain-task submission, per-session packet budgets, per-task session budgets, and failure isolation.

This is a hard internal API evolution. Remove the public/manual drain method from `ReplicantMessageBroker` and update
tests to exercise the new queue-driven behavior.

## Ordered Phases
1. Add the deterministic async test harness needed to drive submitted drain tasks.
2. Refactor broker API and session helpers.
3. Add broker configuration resolution, defaults, and validation.
4. Replace fixed-rate scheduled polling with demand-driven drain-task scheduling.
5. Implement coalesced session work-state transitions and missed-wakeup-safe release.
6. Add bounded drain-task and per-session packet processing.
7. Add lock-contention, closed-session, submission-failure, and packet-failure handling.
8. Update broker tests to the new API and add concurrency/fairness coverage.
9. Update README documentation for deployer-visible config entries, AGENTS guidance, and CHANGELOG for the broker API
   break.
10. Run targeted server tests and the full gate.

## Implementation Details
- `ReplicantMessageBroker`
  - Remove `processPendingSessions()`.
  - Keep `queueChangeMessage(...)`.
- `ReplicantSession`
  - Add package-private `boolean hasPendingPackets()`.
- `ReplicantMessageBrokerImpl`
  - Replace `LinkedBlockingDeque<ReplicantSession>` with `LinkedBlockingQueue<ReplicantSession>`.
  - Replace `_inProgress` with broker work state, probably:
    - `private enum WorkState { QUEUED, RUNNING }`
    - `ConcurrentHashMap<String, WorkState> _workStates`
  - Add active drain task accounting.
  - Active drain task accounting must use an `AtomicInteger` compare-and-set reservation loop so concurrent enqueues
    cannot exceed `maxConcurrentDrainTasks`.
  - Resolve and validate:
    - `maxConcurrentDrainTasks`
    - `maxPacketsPerRun`
    - `maxSessionsPerDrainTask`
  - Resolve config with explicit `InitialContext` lookup of `java:comp/env/<entry>`.
  - Treat `NameNotFoundException` as a missing entry and `NoInitialContextException` as unmanaged/test execution; both
    use defaults. Other `NamingException` failures fail startup.
  - Accept `Number` and numeric `String` values; fail startup for wrong types, non-numeric strings, and values below
    `1`.
  - Cancel fixed-rate scheduling; keep `@PostConstruct` for config resolution.
  - Add a stopping flag set by `@PreDestroy`; stopping disables future drain-task submission while allowing already
    running drain tasks to finish and account normally.
  - Add a package-private/test-visible seam for injecting a deterministic executor or capturing submitted drain tasks.
- Scheduling:
  - `queueChangeMessage(...)` queues the packet, transitions absent state to `QUEUED`, enqueues the session if it wins,
    then calls `scheduleDrainTasks()`.
  - `scheduleDrainTasks()` submits drain tasks while there is queued work and active task count is below the configured
    logical concurrency limit.
  - If executor submission fails before broker shutdown, roll back the active task reservation and schedule a delayed
    retry through the managed scheduled executor rather than waiting for a later enqueue.
  - Each drain task decrements active count in `finally` and asks the scheduler to continue if queued work remains,
    useful progress was made, and the broker is not stopping.
  - If queued work remains but a drain task made no useful progress because all claimed sessions were lock-contended,
    schedule a delayed retry rather than recursively resubmitting immediately.
  - Delayed retry uses one shared atomic retry-scheduled guard, a `20` millisecond default delay, and the same managed
    scheduled executor. The retry callback exits if stopping or empty, clears the guard before attempting normal
    scheduling, and logs/clears the guard if delayed scheduling itself is rejected.
- Session claim:
  - Poll a session from the queue.
  - Claim with `replace(id, QUEUED, RUNNING)`.
  - Skip stale entries that cannot be claimed.
- Session processing:
  - If closed: remove `RUNNING` state and skip.
  - If lock unavailable: transition to `QUEUED`, enqueue at tail, and continue.
  - If locked: pop and process up to `maxPacketsPerRun` packets.
  - Count every popped packet.
  - Track session ids claimed by the current drain task; if a requeued session appears again in the same task, put it
    back on the queue and let a later task claim it.
  - After each processed packet, check `session.isOpen()`; if the session became closed without throwing, stop the
    batch, remove broker state, and do not requeue pending packets for that session.
  - On successful batch completion, unlock first, then remove `RUNNING`, check `hasPendingPackets()`, and requeue if
    still pending.
  - On packet-processing failure, log `SEVERE`, close the session with unexpected condition, remove broker state, and
    continue the task.
  - A task records useful progress when it processes a packet, removes a closed session, or closes a failed session.

## High-Risk Areas And Mitigations
- Risk: missed wakeup when a packet is queued while a worker releases `RUNNING`.
  - Mitigation: release by removing `RUNNING`, then checking `hasPendingPackets()`, then atomically transitioning to
    `QUEUED` if pending.
- Risk: duplicate processing of the same session.
  - Mitigation: claim only with `replace(id, QUEUED, RUNNING)` and require the session lock before packet processing.
- Risk: hot session monopolizes drain workers.
  - Mitigation: enforce `maxPacketsPerRun` per session, tail-requeue when packets remain, and prevent one drain task
    from reclaiming the same requeued session.
- Risk: drain tasks run too long under broad backlog.
  - Mitigation: enforce `maxSessionsPerDrainTask`.
- Risk: active-task accounting drifts after executor rejection or thrown task failures.
  - Mitigation: reserve with an atomic compare-and-set loop, roll back on submit failure, decrement in task `finally`.
- Risk: lock-contended sessions cause tight reschedule loops.
  - Mitigation: track whether a drain task made useful progress and use one coalesced delayed retry for
    contention-only passes.
- Risk: transient executor rejection strands queued work after fixed-rate polling is removed.
  - Mitigation: leave work queued and schedule one coalesced delayed retry when the broker is not stopping.
- Risk: shutdown races with demand scheduling.
  - Mitigation: set a stopping flag in `@PreDestroy`, prevent new submissions, and let active tasks finish without
    rescheduling.
- Risk: container config absence breaks deployment.
  - Mitigation: perform explicit JNDI lookup with defaults for missing entries rather than required field injection, and
    validate accepted value types.
- Risk: obsolete tests keep relying on manual drain behavior.
  - Mitigation: add the deterministic executor/task-capture test seam before removing `processPendingSessions()` from
    the public broker interface and rewriting broker tests.
- Risk: `sendChangeMessage(...)` closes a session without throwing and the broker keeps processing stale packets.
  - Mitigation: check `session.isOpen()` after every processed packet and treat mid-batch closure as terminal for that
    session's queued work.
- Risk: removing a public interface method is missed in release notes.
  - Mitigation: treat the broker method removal as a CHANGELOG-worthy server API break unless implementation proves the
    interface is not published externally.
- Risk: repo documentation expectations conflict with the earlier README-only preference.
  - Mitigation: Q-22 resolved this by adding a concise AGENTS update while keeping detailed config docs in README.

## Required Full-Gate Command
- `bundle exec buildr test`

## Decision Outcomes
- Q-01: fixed-rate scheduled drain is replaced by demand-driven drain tasks.
- Q-02: outer broker session wakeups are coalesced.
- Q-03: session processing uses a configurable per-session packet batch.
- Q-04: drain work uses bounded short-lived tasks on the container executor.
- Q-05: scheduling state lives in `ReplicantMessageBrokerImpl`.
- Q-06: packets queued while running are caught by hybrid coalescing and release checks.
- Q-07: `processPendingSessions()` is removed from the public broker interface.
- Q-08: tuning comes from container environment entries with built-in defaults.
- Q-09: invalid config fails startup.
- Q-10: `maxPacketsPerRun` applies per session claim.
- Q-11: `queueChangeMessage(...)` synchronously schedules drain tasks.
- Q-12: executor submission failures preserve queued work and restore active accounting.
- Q-13: closed sessions are removed from broker scheduling state and skipped.
- Q-14: drain tasks use `tryLock()` and requeue on lock contention.
- Q-15: sessions with remaining packets requeue at the tail.
- Q-16: add package-private `ReplicantSession.hasPendingPackets()`.
- Q-17: use enum work states with atomic map transitions.
- Q-18: switch to `LinkedBlockingQueue`.
- Q-19: packet processing failures close only the affected session and let the task continue.
- Q-20: count every popped packet toward `maxPacketsPerRun`.
- Q-21: resolve config once in `@PostConstruct`.
- Q-22: update `AGENTS.md` with a concise architecture/runtime note while README carries deployer-facing config
  details.

## Decision Log
- Q-01:
  - Plan change: remove fixed-rate executor scheduling and move scheduling to `queueChangeMessage(...)`.
- Q-02:
  - Plan change: outer queue entries represent session wakeups, not individual packets.
- Q-03:
  - Plan change: introduce `maxPacketsPerRun` and tail requeue.
- Q-04:
  - Plan change: introduce active drain task accounting instead of long-lived app-owned workers.
- Q-05:
  - Plan change: keep `ReplicantSession` free of scheduler state except for the pending-packet helper.
- Q-06:
  - Plan change: implement release logic that catches packets added while a session was running.
- Q-07:
  - Plan change: remove `processPendingSessions()` and rewrite tests against queue-driven scheduling.
- Q-08:
  - Plan change: add startup config lookup with defaults for broker limits.
- Q-09:
  - Plan change: validate resolved values and fail startup on invalid input.
- Q-10:
  - Plan change: nested budgets are per-session packet limit and per-task session limit.
- Q-11:
  - Plan change: schedule after every packet enqueue attempt, with coalescing guarding duplicate work.
- Q-12:
  - Plan change: keep queued state intact on submit failure.
- Q-13:
  - Plan change: closed sessions are terminal from the broker's perspective.
- Q-14:
  - Plan change: lock contention is a yield signal, not a reason to block a drain task.
- Q-15:
  - Plan change: no queue-empty shortcut for hot sessions.
- Q-16:
  - Plan change: add a narrow session helper instead of exposing packet queues.
- Q-17:
  - Plan change: use `ConcurrentHashMap` atomic transitions instead of counters/generations.
- Q-18:
  - Plan change: simplify queue type to FIFO-only.
- Q-19:
  - Plan change: isolate poison sessions and keep drain tasks alive.
- Q-20:
  - Plan change: batch accounting happens when a packet is popped.
- Q-21:
  - Plan change: config is startup-only, not dynamically refreshed.
- Q-22:
  - Plan change: documentation task includes README, AGENTS, and CHANGELOG updates.
  - Plan change: README remains the detailed deployer-facing config reference; AGENTS gets concise repo guidance only.
- Round 1 plan review:
  - Plan change: active drain task reservations must use atomic compare-and-set accounting.
  - Plan change: lock-contention-only drain passes and transient executor rejection use delayed retry for liveness
    without tight loops.
  - Plan change: lifecycle behavior is explicit: `@PreDestroy` marks stopping and disables future submissions.
  - Plan change: config lookup is explicit JNDI with accepted types and startup failure cases.
  - Plan change: implementation must add a deterministic async test seam before rewriting broker tests.
  - Plan change: the `ReplicantMessageBroker` API removal is treated as CHANGELOG-worthy unless proven unpublished.
- Round 2 plan review:
  - Plan change: deterministic async test seam is sequenced before demand scheduling tests depend on it.
  - Plan change: delayed retry policy has a concrete default delay, single-outstanding guard, stopping behavior, and
    rejection behavior.
- Round 3 plan review:
  - Plan change: expanded broker tests depend on both the deterministic test seam and the scheduler/failure behavior
    implementation tasks they validate.
- Repeat plan review round 1:
  - Plan change: mid-batch session closure stops processing and prevents requeue.
  - Plan change: deterministic async test seam is implemented before the broker API cut so each task can stay green.
  - Plan change: JNDI lookup failure semantics distinguish missing entries, unmanaged/test execution, and broken naming
    infrastructure.
- Implementation refinement:
  - Plan change: one drain task must not reclaim the same session after a batch-limit requeue.

## Planned Tasks
- PLAN-01: Create and review requirements, implementation plan, and task board.
- Q22: Resolve documentation target conflict.
- T6: Add deterministic async test seam.
- T1: Refactor broker API and add session pending-packet helper.
- T2: Add broker config resolution and validation.
- T3: Implement demand-driven drain-task scheduling and active task accounting.
- T4: Implement coalesced session state transitions and bounded processing.
- T5: Add failure, lock-contention, closed-session, and lifecycle handling.
- T7: Rewrite broker tests and add concurrency/fairness/failure coverage.
- T8: Update README, AGENTS, and CHANGELOG docs.
- T9: Run targeted tests and full gate.
