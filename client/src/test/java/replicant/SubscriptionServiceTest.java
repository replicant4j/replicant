package replicant;

import static org.testng.Assert.*;

import arez.Disposable;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;
import replicant.spy.SubscriptionCreatedEvent;
import replicant.spy.SubscriptionDisposedEvent;

public class SubscriptionServiceTest extends AbstractReplicantTest {
    @Test
    public void typeDatasetSubscriptions() {
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 1);
        final DatasetAddress datasetAddress3 = new DatasetAddress(1, 2);

        final SubscriptionService service = SubscriptionService.create(null);

        final AtomicInteger findSubscriptionAddress1CallCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(service)) {
                // Access observable next line
                service.findSubscription(datasetAddress1);
            }

            findSubscriptionAddress1CallCount.incrementAndGet();
        });

        final AtomicInteger findSubscriptionAddress2CallCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(service)) {
                // Access observable next line
                service.findSubscription(datasetAddress2);
            }
            findSubscriptionAddress2CallCount.incrementAndGet();
        });

        final AtomicInteger getInstanceDatasetSubscriptionsCallCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(service)) {
                service.getInstanceDatasetSubscriptions();
            }
            getInstanceDatasetSubscriptionsCallCount.incrementAndGet();
        });

        final AtomicInteger getTypeDatasetSubscriptionsCallCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(service)) {
                service.getTypeDatasetSubscriptions();
            }
            getTypeDatasetSubscriptionsCallCount.incrementAndGet();
        });

        assertEquals(findSubscriptionAddress1CallCount.get(), 1);
        assertEquals(findSubscriptionAddress2CallCount.get(), 1);
        assertEquals(getInstanceDatasetSubscriptionsCallCount.get(), 1);
        assertEquals(getTypeDatasetSubscriptionsCallCount.get(), 1);
        assertNull(service.findSubscription(datasetAddress1));
        assertNull(service.findSubscription(datasetAddress2));
        safeAction(() -> assertEquals(service.getTypeDatasetSubscriptions().size(), 0));

        // Add subscription on datasetAddress1
        {
            safeAction(() -> service.createSubscription(datasetAddress1, null, SubscriptionMode.EXPLICIT));

            assertEquals(findSubscriptionAddress1CallCount.get(), 2);
            assertEquals(findSubscriptionAddress2CallCount.get(), 2);
            assertEquals(getInstanceDatasetSubscriptionsCallCount.get(), 1);
            assertEquals(getTypeDatasetSubscriptionsCallCount.get(), 2);
            assertNotNull(service.findSubscription(datasetAddress1));
            assertNull(service.findSubscription(datasetAddress2));
            safeAction(() -> assertEquals(service.getTypeDatasetSubscriptions().size(), 1));
        }

        // Add subscription on datasetAddress2
        {
            safeAction(() -> service.createSubscription(datasetAddress2, null, SubscriptionMode.EXPLICIT));

            assertEquals(findSubscriptionAddress1CallCount.get(), 2);
            assertEquals(findSubscriptionAddress2CallCount.get(), 3);
            assertEquals(getInstanceDatasetSubscriptionsCallCount.get(), 1);
            assertEquals(getTypeDatasetSubscriptionsCallCount.get(), 3);
            assertNotNull(service.findSubscription(datasetAddress1));
            assertNotNull(service.findSubscription(datasetAddress2));
            safeAction(() -> assertEquals(service.getTypeDatasetSubscriptions().size(), 2));
        }

        // Add subscription on datasetAddress3
        {
            safeAction(() -> service.createSubscription(datasetAddress3, null, SubscriptionMode.EXPLICIT));

            assertEquals(findSubscriptionAddress1CallCount.get(), 2);
            assertEquals(findSubscriptionAddress2CallCount.get(), 3);
            assertEquals(getInstanceDatasetSubscriptionsCallCount.get(), 1);
            assertEquals(getTypeDatasetSubscriptionsCallCount.get(), 4);
            assertNotNull(service.findSubscription(datasetAddress1));
            assertNotNull(service.findSubscription(datasetAddress2));
            safeAction(() -> assertEquals(service.getTypeDatasetSubscriptions().size(), 3));
        }

        // Dispose subscription on datasetAddress3
        // Should only reschedule `getTypeDatasetSubscriptions()`
        {
            safeAction(() -> {
                final Subscription subscription = service.findSubscription(datasetAddress3);
                assertNotNull(subscription);
                Disposable.dispose(subscription);

                // Check that subscription count is updated from within the subscription
                // to ensure not possible that disposed is returned
                assertEquals(service.getTypeDatasetSubscriptions().size(), 2);
                assertNull(service.findSubscription(datasetAddress3));
            });

            assertEquals(findSubscriptionAddress1CallCount.get(), 2);
            assertEquals(findSubscriptionAddress2CallCount.get(), 3);
            assertEquals(getInstanceDatasetSubscriptionsCallCount.get(), 1);
            assertEquals(getTypeDatasetSubscriptionsCallCount.get(), 5);
            assertNotNull(service.findSubscription(datasetAddress1));
            assertNotNull(service.findSubscription(datasetAddress2));
            safeAction(() -> assertEquals(service.getTypeDatasetSubscriptions().size(), 2));
        }

        // Dispose subscription on datasetAddress2
        // Should reschedule `getTypeDatasetSubscriptions()` and findSubscription( datasetAddress2 )
        {
            safeAction(() -> {
                final Subscription subscription = service.findSubscription(datasetAddress2);
                assertNotNull(subscription);
                Disposable.dispose(subscription);

                // Check that subscription count is updated from within the subscription
                // to ensure not possible that disposed is returned
                assertEquals(service.getTypeDatasetSubscriptions().size(), 1);
                assertNull(service.findSubscription(datasetAddress2));
            });

            assertEquals(findSubscriptionAddress1CallCount.get(), 2);
            assertEquals(findSubscriptionAddress2CallCount.get(), 4);
            assertEquals(getInstanceDatasetSubscriptionsCallCount.get(), 1);
            assertEquals(getTypeDatasetSubscriptionsCallCount.get(), 6);
            assertNotNull(service.findSubscription(datasetAddress1));
            assertNull(service.findSubscription(datasetAddress2));
            safeAction(() -> assertEquals(service.getTypeDatasetSubscriptions().size(), 1));
        }

        // Dispose service
        {
            Disposable.dispose(service);

            assertEquals(findSubscriptionAddress1CallCount.get(), 3);
            assertEquals(findSubscriptionAddress2CallCount.get(), 5);
            assertEquals(getInstanceDatasetSubscriptionsCallCount.get(), 2);
            assertEquals(getTypeDatasetSubscriptionsCallCount.get(), 7);
        }
    }

    @Test
    public void typeDatasetSubscriptions_withDatasetKey() {
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, null, "fi1");
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 0, null, "fi2");

        final SubscriptionService service = SubscriptionService.create(null);

        safeAction(() -> {
            service.createSubscription(datasetAddress1, null, SubscriptionMode.EXPLICIT);
            service.createSubscription(datasetAddress2, null, SubscriptionMode.EXPLICIT);
        });

        assertNotNull(service.findSubscription(datasetAddress1));
        assertNotNull(service.findSubscription(datasetAddress2));
        safeAction(() -> assertEquals(service.getTypeDatasetSubscriptions().size(), 2));

        safeAction(() -> {
            final Subscription subscription = service.findSubscription(datasetAddress1);
            assertNotNull(subscription);
            Disposable.dispose(subscription);
        });

        assertNull(service.findSubscription(datasetAddress1));
        assertNotNull(service.findSubscription(datasetAddress2));
        safeAction(() -> assertEquals(service.getTypeDatasetSubscriptions().size(), 1));
    }

    @Test
    public void typeDatasetSubscriptions_emptyDatasetKey() {
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, null, "");
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 0, null, null);

        final SubscriptionService service = SubscriptionService.create(null);

        safeAction(() -> {
            service.createSubscription(datasetAddress1, null, SubscriptionMode.EXPLICIT);
            service.createSubscription(datasetAddress2, null, SubscriptionMode.EXPLICIT);
        });

        assertNotNull(service.findSubscription(datasetAddress1));
        assertNotNull(service.findSubscription(datasetAddress2));
        safeAction(() -> assertEquals(service.getTypeDatasetSubscriptions().size(), 2));
    }

    @Test
    public void instanceDatasetSubscriptions() {
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 0, 2);
        final DatasetAddress datasetAddress3 = new DatasetAddress(1, 1, ValueUtil.randomInt());

        final SubscriptionService service = SubscriptionService.create(null);

        final AtomicInteger findSubscriptionAddress1CallCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(service)) {
                // Access observable next line
                service.findSubscription(datasetAddress1);
            }

            findSubscriptionAddress1CallCount.incrementAndGet();
        });

        final AtomicInteger findSubscriptionAddress2CallCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(service)) {
                // Access observable next line
                service.findSubscription(datasetAddress2);
            }
            findSubscriptionAddress2CallCount.incrementAndGet();
        });

        final AtomicInteger getInstanceDatasetSubscriptionsCallCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(service)) {
                service.getInstanceDatasetSubscriptions();
            }
            getInstanceDatasetSubscriptionsCallCount.incrementAndGet();
        });

        final AtomicInteger getTypeDatasetSubscriptionsCallCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(service)) {
                service.getTypeDatasetSubscriptions();
            }
            getTypeDatasetSubscriptionsCallCount.incrementAndGet();
        });

        assertEquals(findSubscriptionAddress1CallCount.get(), 1);
        assertEquals(findSubscriptionAddress2CallCount.get(), 1);
        assertEquals(getInstanceDatasetSubscriptionsCallCount.get(), 1);
        assertEquals(getTypeDatasetSubscriptionsCallCount.get(), 1);
        assertNull(service.findSubscription(datasetAddress1));
        assertNull(service.findSubscription(datasetAddress2));
        safeAction(() -> assertEquals(service.getInstanceDatasetSubscriptions().size(), 0));
        safeAction(() -> assertEquals(service.getSubscribedDatasetRootIds(1, 0).size(), 0));
        safeAction(() -> assertEquals(service.getSubscribedDatasetRootIds(1, 1).size(), 0));

        // Add subscription on datasetAddress1
        {
            safeAction(() -> service.createSubscription(datasetAddress1, null, SubscriptionMode.EXPLICIT));

            assertEquals(findSubscriptionAddress1CallCount.get(), 2);
            assertEquals(findSubscriptionAddress2CallCount.get(), 2);
            assertEquals(getInstanceDatasetSubscriptionsCallCount.get(), 2);
            assertEquals(getTypeDatasetSubscriptionsCallCount.get(), 1);
            assertNotNull(service.findSubscription(datasetAddress1));
            assertNull(service.findSubscription(datasetAddress2));
            safeAction(
                    () -> assertEquals(service.getInstanceDatasetSubscriptions().size(), 1));
            safeAction(
                    () -> assertEquals(service.getSubscribedDatasetRootIds(1, 0).size(), 1));
            safeAction(
                    () -> assertEquals(service.getSubscribedDatasetRootIds(1, 1).size(), 0));
        }

        // Add subscription on datasetAddress2
        {
            safeAction(() -> service.createSubscription(datasetAddress2, null, SubscriptionMode.EXPLICIT));

            assertEquals(findSubscriptionAddress1CallCount.get(), 2);
            assertEquals(findSubscriptionAddress2CallCount.get(), 3);
            assertEquals(getInstanceDatasetSubscriptionsCallCount.get(), 3);
            assertEquals(getTypeDatasetSubscriptionsCallCount.get(), 1);
            assertNotNull(service.findSubscription(datasetAddress1));
            assertNotNull(service.findSubscription(datasetAddress2));
            safeAction(
                    () -> assertEquals(service.getInstanceDatasetSubscriptions().size(), 2));
            safeAction(
                    () -> assertEquals(service.getSubscribedDatasetRootIds(1, 0).size(), 2));
            safeAction(
                    () -> assertEquals(service.getSubscribedDatasetRootIds(1, 1).size(), 0));
        }

        // Add subscription on datasetAddress3
        {
            safeAction(() -> service.createSubscription(datasetAddress3, null, SubscriptionMode.EXPLICIT));

            assertEquals(findSubscriptionAddress1CallCount.get(), 2);
            assertEquals(findSubscriptionAddress2CallCount.get(), 3);
            assertEquals(getInstanceDatasetSubscriptionsCallCount.get(), 4);
            assertEquals(getTypeDatasetSubscriptionsCallCount.get(), 1);
            assertNotNull(service.findSubscription(datasetAddress1));
            assertNotNull(service.findSubscription(datasetAddress2));
            safeAction(
                    () -> assertEquals(service.getInstanceDatasetSubscriptions().size(), 3));
            safeAction(
                    () -> assertEquals(service.getSubscribedDatasetRootIds(1, 0).size(), 2));
            safeAction(
                    () -> assertEquals(service.getSubscribedDatasetRootIds(1, 1).size(), 1));
        }

        // Dispose subscription on datasetAddress3
        // Should only reschedule `getInstanceDatasetSubscriptions()`
        {
            safeAction(() -> {
                final Subscription subscription = service.findSubscription(datasetAddress3);
                assertNotNull(subscription);
                Disposable.dispose(subscription);

                // Check that subscription count is updated from within the subscription
                // to ensure not possible that disposed is returned
                assertEquals(service.getInstanceDatasetSubscriptions().size(), 2);
                assertNull(service.findSubscription(datasetAddress3));
                assertEquals(service.getSubscribedDatasetRootIds(1, 0).size(), 2);
            });

            assertEquals(findSubscriptionAddress1CallCount.get(), 2);
            assertEquals(findSubscriptionAddress2CallCount.get(), 3);
            assertEquals(getInstanceDatasetSubscriptionsCallCount.get(), 5);
            assertEquals(getTypeDatasetSubscriptionsCallCount.get(), 1);
            assertNotNull(service.findSubscription(datasetAddress1));
            assertNotNull(service.findSubscription(datasetAddress2));
            safeAction(
                    () -> assertEquals(service.getInstanceDatasetSubscriptions().size(), 2));
            safeAction(
                    () -> assertEquals(service.getSubscribedDatasetRootIds(1, 0).size(), 2));
            safeAction(
                    () -> assertEquals(service.getSubscribedDatasetRootIds(1, 1).size(), 0));
        }

        // Dispose subscription on datasetAddress2
        // Should reschedule `getInstanceDatasetSubscriptions()` and findSubscription( datasetAddress2 )
        {
            safeAction(() -> {
                final Subscription subscription = service.findSubscription(datasetAddress2);
                assertNotNull(subscription);
                Disposable.dispose(subscription);

                // Check that subscription count is updated from within the subscription
                // to ensure not possible that disposed is returned
                assertEquals(service.getInstanceDatasetSubscriptions().size(), 1);
                assertNull(service.findSubscription(datasetAddress2));
                assertEquals(service.getSubscribedDatasetRootIds(1, 0).size(), 1);
            });

            assertEquals(findSubscriptionAddress1CallCount.get(), 2);
            assertEquals(findSubscriptionAddress2CallCount.get(), 4);
            assertEquals(getInstanceDatasetSubscriptionsCallCount.get(), 6);
            assertEquals(getTypeDatasetSubscriptionsCallCount.get(), 1);
            assertNotNull(service.findSubscription(datasetAddress1));
            assertNull(service.findSubscription(datasetAddress2));
            safeAction(
                    () -> assertEquals(service.getInstanceDatasetSubscriptions().size(), 1));
            safeAction(
                    () -> assertEquals(service.getSubscribedDatasetRootIds(1, 0).size(), 1));
            safeAction(
                    () -> assertEquals(service.getSubscribedDatasetRootIds(1, 1).size(), 0));
        }

        // Dispose service
        {
            Disposable.dispose(service);

            assertEquals(findSubscriptionAddress1CallCount.get(), 3);
            assertEquals(findSubscriptionAddress2CallCount.get(), 5);
            assertEquals(getInstanceDatasetSubscriptionsCallCount.get(), 7);
            assertEquals(getTypeDatasetSubscriptionsCallCount.get(), 2);
        }
    }

    @Test
    public void instanceDatasetSubscriptions_withDatasetKey() {
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 7, "fi1");
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 0, 7, "fi2");
        final DatasetAddress datasetAddress3 = new DatasetAddress(1, 0, 8, "fi1");

        final SubscriptionService service = SubscriptionService.create(null);

        safeAction(() -> {
            service.createSubscription(datasetAddress1, null, SubscriptionMode.EXPLICIT);
            service.createSubscription(datasetAddress2, null, SubscriptionMode.EXPLICIT);
            service.createSubscription(datasetAddress3, null, SubscriptionMode.EXPLICIT);
        });

        assertNotNull(service.findSubscription(datasetAddress1));
        assertNotNull(service.findSubscription(datasetAddress2));
        assertNotNull(service.findSubscription(datasetAddress3));
        safeAction(() -> assertEquals(service.getInstanceDatasetSubscriptions().size(), 3));
        safeAction(() -> assertEquals(service.getSubscribedDatasetRootIds(1, 0).size(), 2));

        safeAction(() -> {
            final Subscription subscription = service.findSubscription(datasetAddress2);
            assertNotNull(subscription);
            Disposable.dispose(subscription);
        });

        assertNull(service.findSubscription(datasetAddress2));
        assertNotNull(service.findSubscription(datasetAddress1));
        assertNotNull(service.findSubscription(datasetAddress3));
        safeAction(() -> assertEquals(service.getInstanceDatasetSubscriptions().size(), 2));
        safeAction(() -> assertEquals(service.getSubscribedDatasetRootIds(1, 0).size(), 2));
    }

    @Test
    public void createSubscription_instanceDataset_NoFilter_ExplicitMode() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 0, 1);

        final SubscriptionService service = SubscriptionService.create(null);

        // Instance Dataset, no Filter Parameter, Explicit Subscription Mode
        safeAction(() -> {
            final Subscription subscription =
                    service.createSubscription(datasetAddress, null, SubscriptionMode.EXPLICIT);
            assertEquals(subscription.datasetAddress(), datasetAddress);
            assertNull(subscription.getFilterParameter());
            assertEquals(subscription.getMode(), SubscriptionMode.EXPLICIT);
        });
    }

    @Test
    public void createSubscription_instanceDataset_Filter_ImplicitMode() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 0, 2);

        final SubscriptionService service = SubscriptionService.create(null);

        // Instance Dataset, Filter Parameter, Implicit Subscription Mode
        safeAction(() -> {
            final String filterParameter = ValueUtil.randomString();
            final SubscriptionMode mode = SubscriptionMode.IMPLICIT;
            final Subscription subscription = service.createSubscription(datasetAddress, filterParameter, mode);
            assertEquals(subscription.datasetAddress(), datasetAddress);
            assertEquals(subscription.getFilterParameter(), filterParameter);
            assertEquals(subscription.getMode(), mode);
        });
    }

    @Test
    public void createSubscription_typeDataset_NoFilter_ImplicitMode() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 1);

        final SubscriptionService service = SubscriptionService.create(null);

        // Type Dataset, no Filter Parameter, Implicit Subscription Mode
        safeAction(() -> {
            final String filterParameter = null;
            final SubscriptionMode mode = SubscriptionMode.IMPLICIT;
            final Subscription subscription = service.createSubscription(datasetAddress, filterParameter, mode);
            assertEquals(subscription.datasetAddress(), datasetAddress);
            assertEquals(subscription.getFilterParameter(), filterParameter);
            assertEquals(subscription.getMode(), mode);
        });
    }

    @Test
    public void createSubscription_typeDataset_Filter_ExplicitMode() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 1);

        final SubscriptionService service = SubscriptionService.create(null);

        // Type Dataset, Filter Parameter, Explicit Subscription Mode
        safeAction(() -> {
            final String filterParameter = ValueUtil.randomString();
            final SubscriptionMode mode = SubscriptionMode.EXPLICIT;
            final Subscription subscription = service.createSubscription(datasetAddress, filterParameter, mode);
            assertEquals(subscription.datasetAddress(), datasetAddress);
            assertEquals(subscription.getFilterParameter(), filterParameter);
            assertEquals(subscription.getMode(), mode);
        });
    }

    @Test
    public void createSubscription_generatesSpyEvent() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 1);

        final SubscriptionService service = SubscriptionService.create(null);
        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final Subscription subscription = safeAction(
                () -> service.createSubscription(datasetAddress, ValueUtil.randomString(), SubscriptionMode.EXPLICIT));

        handler.assertEventCount(1);

        final SubscriptionCreatedEvent event = handler.assertNextEvent(SubscriptionCreatedEvent.class);
        assertEquals(event.getSubscription(), subscription);
    }

    @Test
    public void disposeSubscription_generatesSpyEvent() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 1);

        final SubscriptionService service = SubscriptionService.create(null);

        final Subscription subscription = safeAction(
                () -> service.createSubscription(datasetAddress, ValueUtil.randomString(), SubscriptionMode.EXPLICIT));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        Disposable.dispose(subscription);

        handler.assertEventCount(1);

        final SubscriptionDisposedEvent event = handler.assertNextEvent(SubscriptionDisposedEvent.class);
        assertEquals(event.getSubscription(), subscription);
    }

    @Test
    public void createSubscription_alreadyExists() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);

        final SubscriptionService service = SubscriptionService.create(null);

        safeAction(() -> service.createSubscription(datasetAddress, null, SubscriptionMode.EXPLICIT));

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> safeAction(() -> service.createSubscription(datasetAddress, null, SubscriptionMode.EXPLICIT)));

        assertEquals(
                exception.getMessage(),
                "Replicant-0064: createSubscription invoked with Dataset Address 1.0 but a subscription with that"
                        + " Dataset Address already exists.");
    }

    @Test
    public void removeSubscription_typeDatasetSubscription_noExist() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);

        final SubscriptionService service = SubscriptionService.create(null);

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class, () -> safeAction(() -> service.unlinkSubscription(datasetAddress)));

        assertEquals(
                exception.getMessage(),
                "Replicant-0062: unlinkSubscription invoked with Dataset Address 1.0 but no subscription with that"
                        + " Dataset Address exists.");
    }

    @Test
    public void removeSubscription_typeDatasetSubscription_notDisposed() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);

        final SubscriptionService service = SubscriptionService.create(null);

        safeAction(() -> service.createSubscription(datasetAddress, null, SubscriptionMode.EXPLICIT));

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class, () -> safeAction(() -> service.unlinkSubscription(datasetAddress)));

        assertEquals(
                exception.getMessage(),
                "Replicant-0063: unlinkSubscription invoked with Dataset Address 1.0 but subscription has not already"
                        + " been disposed.");
    }

    @Test
    public void removeSubscription_instanceDatasetSubscription_noExist() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 0, 1);

        final SubscriptionService service = SubscriptionService.create(null);

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class, () -> safeAction(() -> service.unlinkSubscription(datasetAddress)));

        assertEquals(
                exception.getMessage(),
                "Replicant-0060: unlinkSubscription invoked with Dataset Address 1.0.1 but no subscription with that"
                        + " Dataset Address exists.");
    }

    @Test
    public void removeSubscription_instanceDatasetSubscription_noExist_butSameInstanceDatasetSubscriptionExists() {
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 0, 2);

        final SubscriptionService service = SubscriptionService.create(null);

        safeAction(() -> service.createSubscription(datasetAddress2, null, SubscriptionMode.EXPLICIT));

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class, () -> safeAction(() -> service.unlinkSubscription(datasetAddress1)));

        assertEquals(
                exception.getMessage(),
                "Replicant-0060: unlinkSubscription invoked with Dataset Address 1.0.1 but no subscription with that"
                        + " Dataset Address exists.");
    }

    @Test
    public void removeSubscription_instanceDatasetSubscription_notDisposed() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 0, 2);

        final SubscriptionService service = SubscriptionService.create(null);

        safeAction(() -> service.createSubscription(datasetAddress, null, SubscriptionMode.EXPLICIT));

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class, () -> safeAction(() -> service.unlinkSubscription(datasetAddress)));

        assertEquals(
                exception.getMessage(),
                "Replicant-0061: unlinkSubscription invoked with Dataset Address 1.0.2 but subscription has not already"
                        + " been disposed.");
    }

    @Test
    public void createSubscriptionServicePassingContextWhenNoZones() {
        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> SubscriptionService.create(Replicant.context()));

        assertEquals(
                exception.getMessage(),
                "Replicant-0037: ReplicantService passed a context but Replicant.areZonesEnabled() is false");
    }

    @Test
    public void dispose_delinksFromEntity() {
        createConnector();

        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();
        final SubscriptionService subscriptionService = Replicant.context().getSubscriptionService();

        final Subscription subscription1 = safeAction(() ->
                subscriptionService.createSubscription(new DatasetAddress(1, 0, 1), null, SubscriptionMode.EXPLICIT));
        final Subscription subscription2 = safeAction(() ->
                subscriptionService.createSubscription(new DatasetAddress(1, 0, 2), null, SubscriptionMode.EXPLICIT));

        final ReplicaEntry replicaEntry1 =
                safeAction(() -> replicaRegistry.findOrCreateReplicaEntry("A/1", A.class, 1));
        final ReplicaEntry replicaEntry2 =
                safeAction(() -> replicaRegistry.findOrCreateReplicaEntry("A/2", A.class, 2));

        safeAction(() -> replicaEntry1.linkToSubscription(subscription1));
        safeAction(() -> replicaEntry2.linkToSubscription(subscription1));
        safeAction(() -> replicaEntry1.linkToSubscription(subscription2));

        safeAction(() -> assertEquals(subscription1.findAllReplicaTypes().size(), 1));
        safeAction(() -> assertEquals(subscription2.findAllReplicaTypes().size(), 1));
        safeAction(() -> assertEquals(replicaEntry1.getSubscriptions().size(), 2));

        assertFalse(Disposable.isDisposed(subscription1));
        assertFalse(Disposable.isDisposed(subscription2));
        assertFalse(Disposable.isDisposed(replicaEntry1));
        assertFalse(Disposable.isDisposed(replicaEntry2));

        Disposable.dispose(subscription1);

        assertTrue(Disposable.isDisposed(subscription1));
        assertFalse(Disposable.isDisposed(subscription2));
        // replicaEntry2 is associated with subscription2 so it stays
        assertFalse(Disposable.isDisposed(replicaEntry1));
        // replicaEntry2 had no other subscriptions so it went away
        assertTrue(Disposable.isDisposed(replicaEntry2));
    }

    @Test
    public void disposeService_disposesKeyedSubscriptions() {
        final SubscriptionService service = SubscriptionService.create(null);

        final Subscription typeDatasetSubscription = safeAction(() ->
                service.createSubscription(new DatasetAddress(1, 0, null, "fi"), null, SubscriptionMode.EXPLICIT));
        final Subscription instanceDatasetSubscription = safeAction(
                () -> service.createSubscription(new DatasetAddress(1, 0, 2, "fi"), null, SubscriptionMode.EXPLICIT));

        assertFalse(Disposable.isDisposed(typeDatasetSubscription));
        assertFalse(Disposable.isDisposed(instanceDatasetSubscription));

        Disposable.dispose(service);

        assertTrue(Disposable.isDisposed(typeDatasetSubscription));
        assertTrue(Disposable.isDisposed(instanceDatasetSubscription));
    }

    static class A {}
}
