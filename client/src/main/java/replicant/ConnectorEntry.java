package replicant;

import arez.Disposable;
import java.util.Objects;
import java.util.function.Consumer;
import org.jspecify.annotations.NonNull;

/**
 * Container describing the Container and state in client.
 */
final class ConnectorEntry implements Disposable {
    /**
     * The cost to attempt to modify action on DataLoader.
     */
    private static final int ACTION_COST = 10000;

    static final int REGEN_TIME_IN_SECONDS = 1;
    static final int REQUIRED_REGEN_PER_SECOND = REGEN_TIME_IN_SECONDS * ACTION_COST;
    static final int OPTIONAL_REGEN_PER_SECOND = REQUIRED_REGEN_PER_SECOND / 5;

    @NonNull
    private final Connector _connector;
    /**
     * Does the system require this DataLoader to be present to be operational.
     */
    private boolean _required;

    private final RateLimitedValue _rateLimiter;

    ConnectorEntry(@NonNull final Connector connector, final boolean required) {
        _connector = Objects.requireNonNull(connector);
        _required = required;

        final int regenRate = required ? REQUIRED_REGEN_PER_SECOND : OPTIONAL_REGEN_PER_SECOND;
        _rateLimiter = new RateLimitedValue(System.currentTimeMillis(), regenRate, ACTION_COST * 2);
    }

    boolean attemptAction(@NonNull final Consumer<Connector> action) {
        return getRateLimiter().attempt(System.currentTimeMillis(), ACTION_COST, () -> action.accept(getConnector()));
    }

    @NonNull
    RateLimitedValue getRateLimiter() {
        return _rateLimiter;
    }

    /**
     * Return the Connector the entry represents.
     *
     * @return the Connector the entry represents.
     */
    @NonNull
    Connector getConnector() {
        return _connector;
    }

    boolean isRequired() {
        return _required;
    }

    void setRequired(final boolean required) {
        _required = required;
    }

    @Override
    public void dispose() {
        Disposable.dispose(_connector);
    }

    @Override
    public boolean isDisposed() {
        return Disposable.isDisposed(_connector);
    }
}
