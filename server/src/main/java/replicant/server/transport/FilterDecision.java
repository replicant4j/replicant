package replicant.server.transport;

/**
 * A decision describing how a Filter affects an Entity Change Candidate.
 */
public enum FilterDecision {
    /**
     * Forward the Entity Change Candidate.
     */
    FORWARD,
    /**
     * Replace the Entity Change Candidate with a Replica removal because the Entity moved out of the Subscription.
     */
    REMOVE,
    /**
     * Ignore the Entity Change Candidate.
     */
    IGNORE
}
