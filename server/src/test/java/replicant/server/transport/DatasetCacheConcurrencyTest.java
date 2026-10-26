package replicant.server.transport;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.websocket.Session;
import org.jspecify.annotations.Nullable;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import replicant.server.ChangeSet;
import replicant.server.DatasetAddress;
import replicant.server.EntityChange;
import replicant.server.EntityChangeCandidate;
import replicant.server.ServerConstants;
import replicant.server.SubscriptionChange;
import replicant.server.ee.RegistryUtil;

public final class DatasetCacheConcurrencyTest {
    @DataProvider
    public Object[][] invalidationModes() {
        return new Object[][] {{false}, {true}};
    }

    @Test(dataProvider = "invalidationModes")
    public void blockedCollectionAllowsOtherDatasetsAndDiscardsInvalidatedData(final boolean clearAll)
            throws ReflectiveOperationException, ExecutionException, InterruptedException, TimeoutException {
        final var started = new CountDownLatch(1);
        final var release = new CountDownLatch(1);
        final var firstLoads = new AtomicInteger();
        final var fixture = fixture((address, changes) -> {
            if (address.datasetId() == 0) {
                final var load = firstLoads.incrementAndGet();
                if (load == 1) {
                    started.countDown();
                    await(release);
                }
                fill(changes, address, load * 10);
            } else {
                fill(changes, address, 30);
            }
        });
        final var pool = Executors.newFixedThreadPool(2);
        try {
            final var blocked = pool.submit(() -> fixture.subscribe("blocked", 0, null));
            assertTrue(started.await(5, TimeUnit.SECONDS));
            final var other = pool.submit(() -> fixture.subscribe("other", 1, null));
            assertEquals(entityId(other.get(5, TimeUnit.SECONDS)), 30);
            final var invalidation = pool.submit(() -> {
                if (clearAll) {
                    fixture.manager().clearDatasetCacheEntries();
                } else {
                    invalidate(fixture.manager(), DatasetAddress.of(0));
                }
                return null;
            });
            invalidation.get(5, TimeUnit.SECONDS);
            release.countDown();
            final var packet = blocked.get(5, TimeUnit.SECONDS);
            assertEquals(firstLoads.get(), 2);
            assertEquals(entityId(packet), 20);
            assertNotNull(packet.datasetCacheVersion());
            assertSame(fixture.broker().packets.get("blocked"), packet);
        } finally {
            release.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void concurrentSameAddressLoadsShareOneInitialization()
            throws ReflectiveOperationException, ExecutionException, InterruptedException, TimeoutException {
        final var started = new CountDownLatch(1);
        final var release = new CountDownLatch(1);
        final var loads = new AtomicInteger();
        final var fixture = fixture((address, changes) -> {
            loads.incrementAndGet();
            started.countDown();
            await(release);
            fill(changes, address, 10);
        });
        final var pool = Executors.newFixedThreadPool(2);
        final var secondThread = new AtomicReference<Thread>();
        final var secondStarted = new CountDownLatch(1);
        try {
            final var first = pool.submit(() -> fixture.subscribe("first", 0, null));
            assertTrue(started.await(5, TimeUnit.SECONDS));
            final var second = pool.submit(() -> {
                secondThread.set(Thread.currentThread());
                secondStarted.countDown();
                return fixture.subscribe("second", 0, null);
            });
            assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
            @SuppressWarnings("unchecked")
            final var entries = (Map<DatasetAddress, DatasetCacheEntry>)
                    field("_datasetCacheEntries").get(fixture.manager());
            final var entry = Objects.requireNonNull(entries.get(DatasetAddress.of(0)));
            final var lock = (ReentrantReadWriteLock) entry.getLock();
            awaitQueued(lock, secondThread.get());
            release.countDown();
            final var firstPacket = first.get(5, TimeUnit.SECONDS);
            final var secondPacket = second.get(5, TimeUnit.SECONDS);
            assertEquals(loads.get(), 1);
            assertEquals(firstPacket.datasetCacheVersion(), secondPacket.datasetCacheVersion());
            assertEquals(entityId(firstPacket), 10);
            assertEquals(entityId(secondPacket), 10);
        } finally {
            release.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void invalidationCannotPassSubscriptionCommit()
            throws ReflectiveOperationException, ExecutionException, InterruptedException, TimeoutException {
        final var queued = new CountDownLatch(1);
        final var releaseQueue = new CountDownLatch(1);
        final var loads = new AtomicInteger();
        final var fixture = fixture((address, changes) -> fill(changes, address, loads.incrementAndGet()));
        fixture.broker().beforeQueue = packet -> {
            queued.countDown();
            await(releaseQueue);
        };
        final var pool = Executors.newFixedThreadPool(2);
        final var invalidatorThread = new AtomicReference<Thread>();
        final var invalidationStarted = new CountDownLatch(1);
        try {
            final var subscription = pool.submit(() -> fixture.subscribe("first", 0, null));
            assertTrue(queued.await(5, TimeUnit.SECONDS));
            final var invalidation = pool.submit(() -> {
                invalidatorThread.set(Thread.currentThread());
                invalidationStarted.countDown();
                fixture.manager().clearDatasetCacheEntries();
            });
            assertTrue(invalidationStarted.await(5, TimeUnit.SECONDS));
            final var lock =
                    (ReentrantReadWriteLock) field("_datasetCacheEntriesLock").get(fixture.manager());
            awaitQueued(lock, invalidatorThread.get());
            releaseQueue.countDown();
            final var first = subscription.get(5, TimeUnit.SECONDS);
            invalidation.get(5, TimeUnit.SECONDS);
            final var next = fixture.subscribe("next", 0, first.datasetCacheVersion());
            assertNotEquals(first.datasetCacheVersion(), next.datasetCacheVersion());
            assertEquals(entityId(next), 2);
        } finally {
            releaseQueue.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void invalidAddressCanBecomeAvailableAndMatchingVersionUsesReference() throws ReflectiveOperationException {
        final var loads = new AtomicInteger();
        final var fixture = fixture((address, changes) -> {
            if (loads.incrementAndGet() == 1) {
                changes.mergeSubscriptionChange(address, SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS);
            } else {
                fill(changes, address, 10);
            }
        });
        final var invalid = fixture.subscribe("invalid", 0, null);
        assertNull(invalid.datasetCacheVersion());
        assertEquals(
                invalid.changeSet().getSubscriptionChanges().get(0).type(),
                SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS);
        final var available = fixture.subscribe("available", 0, null);
        assertNotNull(available.datasetCacheVersion());
        final var reference = fixture.subscribe("reference", 0, available.datasetCacheVersion());
        assertEquals(reference.datasetCacheEntryAddress(), DatasetAddress.of(0));
        assertEquals(reference.datasetCacheVersion(), available.datasetCacheVersion());
        assertEquals(loads.get(), 2);
    }

    private static Fixture fixture(final BiConsumer<DatasetAddress, ChangeSet> collector)
            throws ReflectiveOperationException {
        final var manager = new ReplicantSessionManagerImpl();
        final var adapter = mock(ReplicantServerAdapter.class);
        final var first = new Dataset(
                0, "First", null, Dataset.FilterMode.UNFILTERED, null, false, true, Dataset.Visibility.UNIVERSAL);
        final var second = new Dataset(
                1, "Second", null, Dataset.FilterMode.UNFILTERED, null, false, true, Dataset.Visibility.UNIVERSAL);
        when(adapter.getSystemSchema()).thenReturn(new SystemSchema("CacheTest", first, second));
        doAnswer(invocation -> {
                    assertNull(invocation.getArgument(0));
                    final List<DatasetAddress> addresses = invocation.getArgument(1);
                    collector.accept(addresses.get(0), invocation.getArgument(3));
                    return null;
                })
                .when(adapter)
                .collectSubscriptionData(any(), anyList(), any(), any(), any());
        final var broker = new TestBroker();
        final var registry = new ThreadLocalRegistry();
        field("_serverAdapter").set(manager, adapter);
        field("_broker").set(manager, broker);
        field("_registry").set(manager, registry);
        return new Fixture(manager, broker, registry);
    }

    private static Field field(final String name) throws NoSuchFieldException {
        final var field = ReplicantSessionManagerImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void invalidate(final ReplicantSessionManagerImpl manager, final DatasetAddress address)
            throws ReflectiveOperationException {
        final var method = ReplicantSessionManagerImpl.class.getDeclaredMethod(
                "invalidateDatasetCacheEntry", DatasetAddress.class);
        method.setAccessible(true);
        method.invoke(manager, address);
    }

    private static void fill(final ChangeSet changes, final DatasetAddress address, final int id) {
        changes.mergeSubscriptionChange(address, SubscriptionChange.Type.SUBSCRIBE);
        changes.merge(
                new EntityChange(new EntityChangeCandidate(id, 1, 0, Map.of(), Map.of("value", id), null), address));
    }

    private static int entityId(final Packet packet) {
        return packet.changeSet()
                .getEntityChanges()
                .iterator()
                .next()
                .getEntityChangeCandidate()
                .getEntityId();
    }

    private static void await(final CountDownLatch release) {
        try {
            assertTrue(release.await(10, TimeUnit.SECONDS));
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static void awaitQueued(final ReentrantReadWriteLock lock, final Thread thread) {
        final var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!lock.hasQueuedThread(thread)) {
            assertTrue(System.nanoTime() < deadline, "Expected worker to wait on the owning cache lock");
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
        }
    }

    private record Fixture(ReplicantSessionManagerImpl manager, TestBroker broker, ThreadLocalRegistry registry) {
        Packet subscribe(final String id, final int datasetId, final @Nullable String cachedVersion)
                throws ReflectiveOperationException {
            final var webSocket = mock(Session.class);
            when(webSocket.getId()).thenReturn(id);
            final var session = new ReplicantSession(webSocket);
            final var address = DatasetAddress.of(datasetId);
            registry.prepare();
            session.getLock().lock();
            try {
                session.setDatasetCacheVersion(address, cachedVersion);
                final var method = ReplicantSessionManagerImpl.class.getDeclaredMethod(
                        "doSubscribe",
                        ReplicantSession.class,
                        List.class,
                        JsonObject.class,
                        ChangeSet.class,
                        SubscriptionMode.class);
                method.setAccessible(true);
                method.invoke(manager, session, List.of(address), null, new ChangeSet(), SubscriptionMode.EXPLICIT);
                final var packet = Objects.requireNonNull(broker.packets.get(id));
                if (null != packet.datasetCacheVersion()) {
                    assertNotNull(session.findSubscription(address));
                } else {
                    assertNull(session.findSubscription(address));
                }
                return packet;
            } finally {
                session.getLock().unlock();
            }
        }
    }

    private static final class ThreadLocalRegistry extends RegistryUtil.TestTransactionSynchronizationRegistry {
        private final ThreadLocal<RegistryUtil.TestTransactionSynchronizationRegistry> _registries =
                ThreadLocal.withInitial(RegistryUtil.TestTransactionSynchronizationRegistry::new);

        void prepare() {
            _registries.set(new RegistryUtil.TestTransactionSynchronizationRegistry());
            putResource(ServerConstants.REQUEST_ID_KEY, 1);
        }

        @Override
        public @Nullable Object getResource(final Object key) {
            return _registries.get().getResource(key);
        }

        @Override
        public void putResource(final Object key, final Object value) {
            _registries.get().putResource(key, value);
        }
    }

    private static final class TestBroker implements ReplicantMessageBroker {
        final ConcurrentHashMap<String, Packet> packets = new ConcurrentHashMap<>();
        Consumer<Packet> beforeQueue = packet -> {};

        @Override
        public Packet queueDatasetCacheEntryReference(
                final ReplicantSession session,
                final @Nullable Integer requestId,
                final DatasetAddress address,
                final String version) {
            return accept(session, Packet.datasetCacheEntryReference(requestId, address, version));
        }

        @Override
        public Packet queueChangeSet(
                final ReplicantSession session,
                final boolean fromSubscriptionRequest,
                final @Nullable Integer requestId,
                final @Nullable JsonValue commandResult,
                final @Nullable String datasetCacheVersion,
                final Collection<EntityChangeCandidate> candidates,
                final ChangeSet changeSet) {
            return accept(
                    session,
                    new Packet(
                            fromSubscriptionRequest,
                            requestId,
                            commandResult,
                            datasetCacheVersion,
                            candidates,
                            changeSet));
        }

        private Packet accept(final ReplicantSession session, final Packet packet) {
            beforeQueue.accept(packet);
            packets.put(session.getReplicantSessionId(), packet);
            return packet;
        }
    }
}
