package replicant.shared;

public final class SharedConstants {
    /**
     * The url relative to base from which replicant event stream data is retrieved.
     */
    @SuppressWarnings("unused")
    public static final String REPLICANT_URL_FRAGMENT = "/replicant";
    /**
     * The url relative to base from which initial token is generated and the replicant session created.
     */
    public static final String CONNECTION_URL_FRAGMENT = "/session";
    /**
     * The url relative to the session that controls Subscriptions.
     */
    public static final String SUBSCRIPTION_URL_FRAGMENT = "/subscription";

    private SharedConstants() {}
}
