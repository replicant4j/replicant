package replicant.server;

import static org.testng.Assert.*;

import org.jspecify.annotations.NonNull;
import org.testng.annotations.Test;

public final class SubscriptionDependencyCandidateTest {
    @Test
    public void basicOperation() {
        final var subscriptionDependencyCandidate =
                new SubscriptionDependencyCandidate(DatasetAddress.of(22, 44), DatasetAddress.of(1, 2));
        assertEquals(
                subscriptionDependencyCandidate.sourceDatasetAddressCandidate().datasetId(), 22);
        assertEquals(
                subscriptionDependencyCandidate.sourceDatasetAddressCandidate().datasetRootId(), (Integer) 44);
        assertEquals(
                subscriptionDependencyCandidate.targetDatasetAddressCandidate().datasetId(), 1);
        assertEquals(
                subscriptionDependencyCandidate.targetDatasetAddressCandidate().datasetRootId(), (Integer) 2);
        assertEquals(subscriptionDependencyCandidate.toString(), "[22.44=>1.2]");
    }

    @Test
    public void candidateWithDatasetAddressTemplate() {
        final var subscriptionDependencyCandidate =
                new SubscriptionDependencyCandidate(DatasetAddressTemplate.of(22, 44), DatasetAddress.of(1, 2), null);

        assertEquals(subscriptionDependencyCandidate.toString(), "[22.44?=>1.2]");
    }

    @Test
    public void hashcodeAndEquals() {
        final var subscriptionDependency1 =
                new SubscriptionDependencyCandidate(DatasetAddress.of(22, 44), DatasetAddress.of(1, 2));
        final var subscriptionDependency2 =
                new SubscriptionDependencyCandidate(DatasetAddress.of(22, 44), DatasetAddress.of(1, 3));
        final var subscriptionDependency3 =
                new SubscriptionDependencyCandidate(DatasetAddress.of(22, 77), DatasetAddress.of(1, 2));
        final var subscriptionDependency4 =
                new SubscriptionDependencyCandidate(DatasetAddress.of(27), DatasetAddress.of(1, 2));
        final var subscriptionDependency5 =
                new SubscriptionDependencyCandidate(DatasetAddress.of(27), DatasetAddress.of(1, 3));
        final var subscriptionDependency6 =
                new SubscriptionDependencyCandidate(DatasetAddressTemplate.of(22, 44), DatasetAddress.of(1, 2));

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
            @NonNull final SubscriptionDependencyCandidate subscriptionDependency1,
            @NonNull final SubscriptionDependencyCandidate subscriptionDependency2) {
        assertEquals(subscriptionDependency1, subscriptionDependency2);
        assertEquals(subscriptionDependency1.hashCode(), subscriptionDependency2.hashCode());
    }

    private void assertSubscriptionDependencyNotEqual(
            @NonNull final SubscriptionDependencyCandidate subscriptionDependency1,
            @NonNull final SubscriptionDependencyCandidate subscriptionDependency2) {
        assertNotEquals(subscriptionDependency1, subscriptionDependency2);
        assertNotEquals(subscriptionDependency1.hashCode(), subscriptionDependency2.hashCode());
    }
}
