package replicant.server;

import static org.testng.Assert.*;

import org.jspecify.annotations.NonNull;
import org.testng.annotations.Test;

public final class SubscriptionDependencyTest {
    @Test
    public void basicOperation() {
        final var subscriptionDependency =
                new SubscriptionDependency(DatasetAddress.of(22, 44), DatasetAddress.of(1, 2));
        assertEquals(subscriptionDependency.sourceDatasetAddress().datasetId(), 22);
        assertEquals(subscriptionDependency.sourceDatasetAddress().datasetRootId(), (Integer) 44);
        assertEquals(subscriptionDependency.targetDatasetAddress().datasetId(), 1);
        assertEquals(subscriptionDependency.targetDatasetAddress().datasetRootId(), (Integer) 2);
        assertFalse(subscriptionDependency.partial());
        assertEquals(subscriptionDependency.toString(), "[22.44=>1.2]");
    }

    @Test
    public void partialOperation() {
        final var subscriptionDependency =
                new SubscriptionDependency(DatasetAddress.partial(22, 44), DatasetAddress.of(1, 2), null, true);

        assertTrue(subscriptionDependency.partial());
        assertEquals(subscriptionDependency.toString(), "[22.44?=>1.2?]");
    }

    @Test
    public void hashcodeAndEquals() {
        final var subscriptionDependency1 =
                new SubscriptionDependency(DatasetAddress.of(22, 44), DatasetAddress.of(1, 2));
        final var subscriptionDependency2 =
                new SubscriptionDependency(DatasetAddress.of(22, 44), DatasetAddress.of(1, 3));
        final var subscriptionDependency3 =
                new SubscriptionDependency(DatasetAddress.of(22, 77), DatasetAddress.of(1, 2));
        final var subscriptionDependency4 = new SubscriptionDependency(DatasetAddress.of(27), DatasetAddress.of(1, 2));
        final var subscriptionDependency5 = new SubscriptionDependency(DatasetAddress.of(27), DatasetAddress.of(1, 3));
        final var subscriptionDependency6 =
                new SubscriptionDependency(DatasetAddress.partial(22, 44), DatasetAddress.of(1, 2), null, true);

        assertSubscriptionDependencyEqual(subscriptionDependency1, subscriptionDependency1);
        assertSubscriptionDependencyEqual(subscriptionDependency2, subscriptionDependency2);
        assertSubscriptionDependencyEqual(subscriptionDependency3, subscriptionDependency3);
        assertSubscriptionDependencyEqual(subscriptionDependency4, subscriptionDependency4);
        assertSubscriptionDependencyEqual(subscriptionDependency5, subscriptionDependency5);
        assertSubscriptionDependencyEqual(subscriptionDependency6, subscriptionDependency6);

        assertSubscriptionDependencyNotEqual(subscriptionDependency1, subscriptionDependency2);
        assertSubscriptionDependencyNotEqual(subscriptionDependency1, subscriptionDependency3);
        assertSubscriptionDependencyNotEqual(subscriptionDependency1, subscriptionDependency4);
        assertSubscriptionDependencyNotEqual(subscriptionDependency1, subscriptionDependency5);
        assertSubscriptionDependencyNotEqual(subscriptionDependency1, subscriptionDependency6);

        assertSubscriptionDependencyNotEqual(subscriptionDependency2, subscriptionDependency3);
        assertSubscriptionDependencyNotEqual(subscriptionDependency2, subscriptionDependency4);
        assertSubscriptionDependencyNotEqual(subscriptionDependency2, subscriptionDependency5);
        assertSubscriptionDependencyNotEqual(subscriptionDependency2, subscriptionDependency6);

        assertSubscriptionDependencyNotEqual(subscriptionDependency3, subscriptionDependency4);
        assertSubscriptionDependencyNotEqual(subscriptionDependency3, subscriptionDependency5);
        assertSubscriptionDependencyNotEqual(subscriptionDependency3, subscriptionDependency6);

        assertSubscriptionDependencyNotEqual(subscriptionDependency4, subscriptionDependency5);
        assertSubscriptionDependencyNotEqual(subscriptionDependency4, subscriptionDependency6);

        assertSubscriptionDependencyNotEqual(subscriptionDependency5, subscriptionDependency6);
    }

    private void assertSubscriptionDependencyEqual(
            @NonNull final SubscriptionDependency subscriptionDependency1,
            @NonNull final SubscriptionDependency subscriptionDependency2) {
        assertEquals(subscriptionDependency1, subscriptionDependency2);
        assertEquals(subscriptionDependency1.hashCode(), subscriptionDependency2.hashCode());
    }

    private void assertSubscriptionDependencyNotEqual(
            @NonNull final SubscriptionDependency subscriptionDependency1,
            @NonNull final SubscriptionDependency subscriptionDependency2) {
        assertNotEquals(subscriptionDependency1, subscriptionDependency2);
        assertNotEquals(subscriptionDependency1.hashCode(), subscriptionDependency2.hashCode());
    }
}
