package replicant;

/**
 * The current reason a Subscription is retained.
 */
public enum SubscriptionMode {
    /**
     * The Subscription is backed by an Area of Interest.
     */
    EXPLICIT,
    /**
     * The Subscription is retained only through one or more Subscription Dependencies.
     */
    IMPLICIT
}
