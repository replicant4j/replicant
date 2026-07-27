package replicant;

import org.jspecify.annotations.NonNull;

/**
 * An activation scope for one isolated {@link ReplicantContext}.
 *
 * <p>A Zone selects which Replicant Context is returned by {@link Replicant#context()} while code runs in the Zone.
 * The Zone is not Replicant Session state and does not itself own System Schemas, Areas of Interest, Subscriptions, or
 * Replicas.
 */
public final class Zone {
    /**
     * The Replicant Context selected by this Zone.
     */
    @NonNull
    private final ReplicantContext _context = new ReplicantContext();

    /**
     * Return the Replicant Context selected by this Zone.
     *
     * @return the Replicant Context selected by this Zone.
     */
    @NonNull
    public ReplicantContext getReplicantContext() {
        return _context;
    }

    /**
     * Create an activation scope containing a new, isolated Replicant Context.
     * Should only be done via {@link Replicant} methods.
     */
    Zone() {}

    public boolean isActive() {
        return Replicant.currentZone() == this;
    }

    /**
     * Run the specified function with this Zone's Replicant Context current.
     * Activate the Zone on entry and restore the previous Zone on exit.
     *
     * @param <T>    The type of the value returned from function.
     * @param action the function to execute.
     * @return the value returned from function.
     */
    public <T> T safeRun(@NonNull final SafeFunction<T> action) {
        Replicant.activateZone(this);
        try {
            return action.call();
        } finally {
            Replicant.deactivateZone(this);
        }
    }

    /**
     * Run the specified function with this Zone's Replicant Context current.
     * Activate the Zone on entry and restore the previous Zone on exit.
     *
     * @param <T>    The type of the value returned from function.
     * @param action the function to execute.
     * @return the value returned from function.
     * @throws Throwable if the function throws an exception.
     */
    public <T> T run(@NonNull final Function<T> action) throws Throwable {
        Replicant.activateZone(this);
        try {
            return action.call();
        } finally {
            Replicant.deactivateZone(this);
        }
    }

    /**
     * Run the specified procedure with this Zone's Replicant Context current.
     * Activate the Zone on entry and restore the previous Zone on exit.
     *
     * @param action the procedure to execute.
     */
    public void safeRun(@NonNull final SafeProcedure action) {
        Replicant.activateZone(this);
        try {
            action.call();
        } finally {
            Replicant.deactivateZone(this);
        }
    }

    /**
     * Run the specified procedure with this Zone's Replicant Context current.
     * Activate the Zone on entry and restore the previous Zone on exit.
     *
     * @param action the procedure to execute.
     * @throws Throwable if the procedure throws an exception.
     */
    public void run(@NonNull final Procedure action) throws Throwable {
        Replicant.activateZone(this);
        try {
            action.call();
        } finally {
            Replicant.deactivateZone(this);
        }
    }
}
