package replicant.server.transport;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.json.Json;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.testng.annotations.Test;
import replicant.server.ChangeSet;
import replicant.server.ValueUtil;

public class ReplicantMessageBrokerImplTest {
    @Test
    public void basicOperation() {
        final var session = newSession();
        final var broker = new TestReplicantMessageBrokerImpl();

        final var packet = queuePacket(broker, session);

        verifyNoSend(broker);
        assertEquals(broker.getDrainTaskCount(), 1);
        assertEquals(broker.getActiveDrainTaskCount(), 1);

        broker.runNextDrainTask();

        verifySendOnce(broker, session, packet);
        assertEquals(broker.getActiveDrainTaskCount(), 0);
        assertEquals(broker.getWorkStateCount(), 0);
    }

    @Test
    public void multipleSendsToSameSessionAreCoalesced() {
        final var session = newSession();
        final var broker = new TestReplicantMessageBrokerImpl();

        final var packet1 = queuePacket(broker, session);
        final var packet2 = queuePacket(broker, session);

        assertEquals(broker.getDrainTaskCount(), 1);
        assertEquals(broker.getQueuedSessionCount(), 1);

        broker.runNextDrainTask();

        verifySendOnce(broker, session, packet1);
        verifySendOnce(broker, session, packet2);
        assertEquals(broker.getWorkStateCount(), 0);
    }

    @Test
    public void multipleSendsToDifferentSessionsCanScheduleMultipleDrainTasks() {
        final var session1 = newSession();
        final var session2 = newSession();
        final var broker = new TestReplicantMessageBrokerImpl();

        final var packet1 = queuePacket(broker, session1);
        final var packet2 = queuePacket(broker, session2);

        assertEquals(broker.getDrainTaskCount(), 2);

        broker.runNextDrainTask();
        broker.runNextDrainTask();

        verifySendOnce(broker, session1, packet1);
        verifySendOnce(broker, session2, packet2);
    }

    @Test
    public void enqueueWhileSessionIsRunningSchedulesAdditionalDrainTask() throws InterruptedException {
        final var session1 = newSession();
        final var session2 = newSession();
        final var broker = new TestReplicantMessageBrokerImpl();
        broker.setMaxConcurrentDrainTasks(2);
        final var sendStarted = new CountDownLatch(1);
        final var releaseSend = new CountDownLatch(1);
        final var packet1 = queuePacket(broker, session1);
        doAnswer(invocation -> {
                    sendStarted.countDown();
                    assertTrue(releaseSend.await(10, TimeUnit.SECONDS));
                    return true;
                })
                .when(broker._sessionManager)
                .sendChangeMessage(eq(session1), eq(packet1));

        final var worker = broker.startNextDrainTaskInNewThread();

        assertTrue(sendStarted.await(10, TimeUnit.SECONDS));
        final var packet2 = queuePacket(broker, session2);

        assertEquals(broker.getDrainTaskCount(), 1);
        assertEquals(broker.getActiveDrainTaskCount(), 2);

        releaseSend.countDown();
        worker.join();
        broker.runNextDrainTask();

        verifySendOnce(broker, session1, packet1);
        verifySendOnce(broker, session2, packet2);
        assertEquals(broker.getWorkStateCount(), 0);
    }

    @Test
    public void concurrentEnqueuesRespectConfiguredMaxDrainTasks() throws InterruptedException {
        final var broker = new TestReplicantMessageBrokerImpl();
        broker.setMaxConcurrentDrainTasks(2);
        final var threadCount = 8;
        final var ready = new CountDownLatch(threadCount);
        final var start = new CountDownLatch(1);
        final var failure = new AtomicReference<Throwable>();
        final var threads = new ArrayList<Thread>();

        for (var i = 0; i < threadCount; i++) {
            final var thread = new Thread(() -> {
                ready.countDown();
                try {
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    queuePacket(broker, newSession());
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failure.compareAndSet(null, e);
                } catch (final Throwable t) {
                    failure.compareAndSet(null, t);
                }
            });
            threads.add(thread);
            thread.start();
        }

        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();
        for (final var thread : threads) {
            thread.join();
        }
        if (null != failure.get()) {
            throw new AssertionError(failure.get());
        }

        assertEquals(broker.getDrainTaskCount(), 2);
        assertEquals(broker.getActiveDrainTaskCount(), 2);
    }

    @Test
    public void maxPacketsPerRunYieldsAndRequeuesSession() {
        final var session = newSession();
        final var broker = new TestReplicantMessageBrokerImpl();
        broker.setMaxPacketsPerRun(1);
        assertEquals(broker.getMaxPacketsPerRun(), 1);

        final var packet1 = queuePacket(broker, session);
        final var packet2 = queuePacket(broker, session);
        assertEquals(broker.getDrainTaskCount(), 1);

        broker.runNextDrainTask();

        verifySendOnce(broker, session, packet1);
        verify(broker._sessionManager, never()).sendChangeMessage(eq(session), eq(packet2));
        assertEquals(broker.getDrainTaskCount(), 1);

        broker.runNextDrainTask();

        verifySendOnce(broker, session, packet2);
        assertEquals(broker.getWorkStateCount(), 0);
    }

    @Test
    public void maxSessionsPerDrainTaskYieldsAndSchedulesAnotherTask() {
        final var session1 = newSession();
        final var session2 = newSession();
        final var broker = new TestReplicantMessageBrokerImpl();
        broker.setMaxConcurrentDrainTasks(1);
        broker.setMaxSessionsPerDrainTask(1);

        final var packet1 = queuePacket(broker, session1);
        final var packet2 = queuePacket(broker, session2);

        assertEquals(broker.getDrainTaskCount(), 1);

        broker.runNextDrainTask();

        verifySendOnce(broker, session1, packet1);
        verify(broker._sessionManager, never()).sendChangeMessage(eq(session2), eq(packet2));
        assertEquals(broker.getDrainTaskCount(), 1);

        broker.runNextDrainTask();

        verifySendOnce(broker, session2, packet2);
    }

    @Test
    public void lockContentionRequeuesSessionAndSchedulesDelayedRetry() throws InterruptedException {
        final var session = newSession();
        final var broker = new TestReplicantMessageBrokerImpl();

        session.getLock().lock();
        try {
            queuePacket(broker, session);

            broker.runNextDrainTaskInNewThread();

            verifyNoSend(broker);
            assertEquals(broker.getQueuedSessionCount(), 1);
            assertEquals(broker.getRetryTaskCount(), 1);
            assertTrue(broker.isRetryScheduled());
        } finally {
            session.getLock().unlock();
        }

        broker.runNextRetryTask();
        broker.runNextDrainTask();

        verify(broker._sessionManager, times(1)).sendChangeMessage(eq(session), any());
        assertEquals(broker.getWorkStateCount(), 0);
    }

    @Test
    public void repeatedNoProgressPassesCoalesceDelayedRetries() throws InterruptedException {
        final var session1 = newSession();
        final var session2 = newSession();
        final var broker = new TestReplicantMessageBrokerImpl();

        session1.getLock().lock();
        session2.getLock().lock();
        try {
            queuePacket(broker, session1);
            queuePacket(broker, session2);

            broker.runNextDrainTaskInNewThread();
            broker.runNextDrainTaskInNewThread();

            assertEquals(broker.getRetryTaskCount(), 1);
            assertTrue(broker.isRetryScheduled());
        } finally {
            session2.getLock().unlock();
            session1.getLock().unlock();
        }
    }

    @Test
    public void delayedRetryCallbackDoesNothingWhenStopping() throws InterruptedException {
        final var session = newSession();
        final var broker = new TestReplicantMessageBrokerImpl();

        session.getLock().lock();
        try {
            queuePacket(broker, session);
            broker.runNextDrainTaskInNewThread();
            assertEquals(broker.getRetryTaskCount(), 1);
            broker.preDestroy();
        } finally {
            session.getLock().unlock();
        }

        broker.runNextRetryTask();

        assertEquals(broker.getDrainTaskCount(), 0);
        assertEquals(broker.getActiveDrainTaskCount(), 0);
        assertFalse(broker.isRetryScheduled());
    }

    @Test
    public void delayedRetryCallbackDoesNothingWhenQueueIsEmpty() {
        final var broker = new TestReplicantMessageBrokerImpl();

        broker.runDelayedRetry();

        assertEquals(broker.getDrainTaskCount(), 0);
        assertFalse(broker.isRetryScheduled());
    }

    @Test
    public void closedSessionIsRemovedFromBrokerState() {
        final var open = new AtomicBoolean(false);
        final var session = newSession(open);
        final var broker = new TestReplicantMessageBrokerImpl();

        queuePacket(broker, session);
        broker.runNextDrainTask();

        verifyNoSend(broker);
        assertEquals(broker.getWorkStateCount(), 0);
        assertEquals(broker.getQueuedSessionCount(), 0);
    }

    @Test
    public void sessionClosedMidBatchIsNotProcessedFurtherOrRequeued() {
        final var open = new AtomicBoolean(true);
        final var session = newSession(open);
        final var broker = new TestReplicantMessageBrokerImpl();
        final var packet1 = queuePacket(broker, session);
        final var packet2 = queuePacket(broker, session);
        doAnswer(invocation -> {
                    open.set(false);
                    return true;
                })
                .when(broker._sessionManager)
                .sendChangeMessage(eq(session), eq(packet1));

        broker.runNextDrainTask();

        verifySendOnce(broker, session, packet1);
        verify(broker._sessionManager, never()).sendChangeMessage(eq(session), eq(packet2));
        assertEquals(broker.getWorkStateCount(), 0);
        assertEquals(broker.getQueuedSessionCount(), 0);
    }

    @Test
    public void packetProcessingFailureClosesOnlyAffectedSession() throws Exception {
        final var session1 = newSession();
        final var session2 = newSession();
        final var broker = new TestReplicantMessageBrokerImpl();
        final var packet1 = queuePacket(broker, session1);
        final var packet2 = queuePacket(broker, session2);
        doThrow(new IllegalStateException("boom"))
                .when(broker._sessionManager)
                .sendChangeMessage(eq(session1), eq(packet1));

        broker.runNextDrainTask();
        broker.runNextDrainTask();

        verifySendOnce(broker, session1, packet1);
        verifySendOnce(broker, session2, packet2);
        verify(session1.getWebSocketSession(), times(1)).close(any(CloseReason.class));
        verify(session2.getWebSocketSession(), never()).close(any(CloseReason.class));
    }

    @Test
    public void packetProcessingFailureDoesNotStopCurrentDrainTask() throws Exception {
        final var session1 = newSession();
        final var session2 = newSession();
        final var broker = new TestReplicantMessageBrokerImpl();
        broker.setMaxConcurrentDrainTasks(1);
        final var packet1 = queuePacket(broker, session1);
        final var packet2 = queuePacket(broker, session2);
        doThrow(new IllegalStateException("boom"))
                .when(broker._sessionManager)
                .sendChangeMessage(eq(session1), eq(packet1));

        broker.runNextDrainTask();

        verifySendOnce(broker, session1, packet1);
        verifySendOnce(broker, session2, packet2);
        verify(session1.getWebSocketSession(), times(1)).close(any(CloseReason.class));
        verify(session2.getWebSocketSession(), never()).close(any(CloseReason.class));
        assertEquals(broker.getWorkStateCount(), 0);
    }

    @Test
    public void executorSubmissionFailureKeepsWorkQueuedAndSchedulesRetry() {
        final var session = newSession();
        final var broker = new TestReplicantMessageBrokerImpl();
        broker._submitFailure = new IllegalStateException("reject");

        queuePacket(broker, session);

        assertEquals(broker.getActiveDrainTaskCount(), 0);
        assertEquals(broker.getDrainTaskCount(), 0);
        assertEquals(broker.getQueuedSessionCount(), 1);
        assertEquals(broker.getRetryTaskCount(), 1);

        broker.runNextRetryTask();
        broker.runNextDrainTask();

        verify(broker._sessionManager, times(1)).sendChangeMessage(eq(session), any());
    }

    @Test
    public void delayedRetrySubmissionFailureClearsRetryState() throws InterruptedException {
        final var session = newSession();
        final var broker = new TestReplicantMessageBrokerImpl();
        broker._retryFailure = new IllegalStateException("reject");

        session.getLock().lock();
        try {
            queuePacket(broker, session);
            broker.runNextDrainTaskInNewThread();

            assertFalse(broker.isRetryScheduled());
            assertEquals(broker.getRetryTaskCount(), 0);
            assertEquals(broker.getQueuedSessionCount(), 1);
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void preDestroyPreventsFutureDrainTaskSubmission() {
        final var session = newSession();
        final var broker = new TestReplicantMessageBrokerImpl();

        broker.preDestroy();
        queuePacket(broker, session);

        assertEquals(broker.getDrainTaskCount(), 0);
        assertEquals(broker.getActiveDrainTaskCount(), 0);
        assertEquals(broker.getQueuedSessionCount(), 1);
    }

    @Test
    public void activeDrainTasksDoNotExceedConfiguredMaximum() {
        final var broker = new TestReplicantMessageBrokerImpl();
        broker.setMaxConcurrentDrainTasks(2);

        queuePacket(broker, newSession());
        queuePacket(broker, newSession());
        queuePacket(broker, newSession());

        assertEquals(broker.getDrainTaskCount(), 2);
        assertEquals(broker.getActiveDrainTaskCount(), 2);
    }

    private void verifySendOnce(
            @NonNull final ReplicantMessageBroker broker,
            @NonNull final ReplicantSession session,
            @NonNull final Packet packet) {
        verify(((TestReplicantMessageBrokerImpl) broker)._sessionManager, times(1))
                .sendChangeMessage(eq(session), eq(packet));
    }

    private void verifyNoSend(@NonNull final ReplicantMessageBroker broker) {
        verify(((TestReplicantMessageBrokerImpl) broker)._sessionManager, never())
                .sendChangeMessage(any(), any());
    }

    @NonNull
    private Packet queuePacket(@NonNull final ReplicantMessageBroker broker, @NonNull final ReplicantSession session) {
        return broker.queueChangeMessage(
                session,
                false,
                ValueUtil.randomInt(),
                Json.createObjectBuilder()
                        .add(ValueUtil.randomString(), ValueUtil.randomString())
                        .build(),
                ValueUtil.randomString(),
                Collections.emptyList(),
                new ChangeSet());
    }

    @NonNull
    private ReplicantSession newSession() {
        return newSession(new AtomicBoolean(true));
    }

    @NonNull
    private ReplicantSession newSession(@NonNull final AtomicBoolean open) {
        final var session = mock(Session.class);
        when(session.isOpen()).thenAnswer(invocation -> open.get());
        when(session.getId()).thenReturn(ValueUtil.randomString());
        try {
            doAnswer(invocation -> {
                        open.set(false);
                        return null;
                    })
                    .when(session)
                    .close();
            doAnswer(invocation -> {
                        open.set(false);
                        return null;
                    })
                    .when(session)
                    .close(any(CloseReason.class));
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        return new ReplicantSession(session);
    }

    private static class TestReplicantMessageBrokerImpl extends ReplicantMessageBrokerImpl {
        @NonNull
        private final List<Runnable> _drainTasks = Collections.synchronizedList(new ArrayList<>());

        @NonNull
        private final List<Runnable> _retryTasks = Collections.synchronizedList(new ArrayList<>());

        @Nullable
        private RuntimeException _submitFailure;

        @Nullable
        private RuntimeException _retryFailure;

        public TestReplicantMessageBrokerImpl() {
            _sessionManager = mock(ReplicantSessionManager.class);
            when(_sessionManager.sendChangeMessage(any(), any())).thenReturn(true);
            setMaxConcurrentDrainTasks(Math.max(2, Runtime.getRuntime().availableProcessors()));
            setMaxPacketsPerRun(64);
            setMaxSessionsPerDrainTask(64);
        }

        @Override
        void submitDrainTask(@NonNull final Runnable task) {
            if (null != _submitFailure) {
                final var failure = _submitFailure;
                _submitFailure = null;
                throw failure;
            }
            _drainTasks.add(task);
        }

        @Override
        void scheduleRetryTask(@NonNull final Runnable task) {
            if (null != _retryFailure) {
                final var failure = _retryFailure;
                _retryFailure = null;
                throw failure;
            }
            _retryTasks.add(task);
        }

        int getDrainTaskCount() {
            return _drainTasks.size();
        }

        int getRetryTaskCount() {
            return _retryTasks.size();
        }

        void runNextDrainTask() {
            _drainTasks.remove(0).run();
        }

        void runNextDrainTaskInNewThread() throws InterruptedException {
            startNextDrainTaskInNewThread().join();
        }

        @NonNull
        DrainTaskThread startNextDrainTaskInNewThread() {
            final var failure = new AtomicReference<Throwable>();
            final var thread = new Thread(() -> {
                try {
                    runNextDrainTask();
                } catch (final Throwable t) {
                    failure.set(t);
                }
            });
            thread.start();
            return new DrainTaskThread(thread, failure);
        }

        private static class DrainTaskThread {
            @NonNull
            private final Thread _thread;

            @NonNull
            private final AtomicReference<Throwable> _failure;

            DrainTaskThread(@NonNull final Thread thread, @NonNull final AtomicReference<Throwable> failure) {
                _thread = thread;
                _failure = failure;
            }

            void join() throws InterruptedException {
                _thread.join();
                if (null != _failure.get()) {
                    throw new AssertionError(_failure.get());
                }
            }
        }

        void runNextRetryTask() {
            _retryTasks.remove(0).run();
        }
    }
}
