package replicant.server.runtime;

import javax.transaction.TransactionSynchronizationRegistry;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.server.ChangeSet;
import replicant.server.EntityChangeCandidateSet;
import replicant.server.ServerConstants;

/**
 * Utility methods for accessing Replication Invocation resources in the TransactionSynchronizationRegistry.
 */
public final class EntityChangeCandidateCacheUtil {
    /**
     * Key used to reference the set of changes in the TransactionSynchronizationRegistry.
     */
    private static final String KEY = EntityChangeCandidateSet.class.getName();
    /**
     * Key used to look up the Initiating Session Change Set. Its changes are not routed.
     */
    private static final String INITIATING_SESSION_CHANGE_SET_KEY = KEY + "/InitiatingSessionChangeSet";

    private EntityChangeCandidateCacheUtil() {}

    @NonNull
    public static EntityChangeCandidateSet getEntityChangeCandidateSet(
            @NonNull final TransactionSynchronizationRegistry r) {
        var entityChangeCandidateSet = EntityChangeCandidateCacheUtil.<EntityChangeCandidateSet>lookup(r, KEY);
        if (null == entityChangeCandidateSet) {
            entityChangeCandidateSet = new EntityChangeCandidateSet();
            r.putResource(KEY, entityChangeCandidateSet);
        }
        return entityChangeCandidateSet;
    }

    @Nullable
    public static EntityChangeCandidateSet lookupEntityChangeCandidateSet() {
        return lookup(TransactionSynchronizationRegistryUtil.lookup(), KEY);
    }

    @Nullable
    public static EntityChangeCandidateSet removeEntityChangeCandidateSet() {
        return removeEntityChangeCandidateSet(TransactionSynchronizationRegistryUtil.lookup());
    }

    @Nullable
    public static EntityChangeCandidateSet removeEntityChangeCandidateSet(
            @NonNull final TransactionSynchronizationRegistry r) {
        return remove(r, KEY);
    }

    /**
     * Return the Initiating Session Change Set, creating it if necessary.
     *
     * @return the Initiating Session Change Set.
     */
    @NonNull
    public static ChangeSet getInitiatingSessionChangeSet() {
        return getInitiatingSessionChangeSet(TransactionSynchronizationRegistryUtil.lookup());
    }

    /**
     * Return the Initiating Session Change Set, creating it if necessary.
     *
     * @param r the TransactionSynchronizationRegistry containing the Replication Invocation resources.
     * @return the Initiating Session Change Set.
     */
    @NonNull
    public static ChangeSet getInitiatingSessionChangeSet(@NonNull final TransactionSynchronizationRegistry r) {
        var changeSet = EntityChangeCandidateCacheUtil.<ChangeSet>lookup(r, INITIATING_SESSION_CHANGE_SET_KEY);
        if (null == changeSet) {
            changeSet = new ChangeSet();
            r.putResource(INITIATING_SESSION_CHANGE_SET_KEY, changeSet);
        }
        return changeSet;
    }

    /**
     * Return the Initiating Session Change Set if one exists.
     *
     * @return the Initiating Session Change Set, or null if none exists.
     */
    @Nullable
    public static ChangeSet lookupInitiatingSessionChangeSet() {
        return lookupInitiatingSessionChangeSet(TransactionSynchronizationRegistryUtil.lookup());
    }

    /**
     * Return the Initiating Session Change Set if one exists.
     *
     * @param r the TransactionSynchronizationRegistry containing the Replication Invocation resources.
     * @return the Initiating Session Change Set, or null if none exists.
     */
    @Nullable
    public static ChangeSet lookupInitiatingSessionChangeSet(@NonNull final TransactionSynchronizationRegistry r) {
        return lookup(r, INITIATING_SESSION_CHANGE_SET_KEY);
    }

    /**
     * Remove and return the Initiating Session Change Set if one exists.
     *
     * @return the removed Initiating Session Change Set, or null if none exists.
     */
    @Nullable
    public static ChangeSet removeInitiatingSessionChangeSet() {
        return removeInitiatingSessionChangeSet(TransactionSynchronizationRegistryUtil.lookup());
    }

    /**
     * Remove and return the Initiating Session Change Set if one exists.
     *
     * @param r the TransactionSynchronizationRegistry containing the Replication Invocation resources.
     * @return the removed Initiating Session Change Set, or null if none exists.
     */
    @Nullable
    public static ChangeSet removeInitiatingSessionChangeSet(@NonNull final TransactionSynchronizationRegistry r) {
        return remove(r, INITIATING_SESSION_CHANGE_SET_KEY);
    }

    private static <T> T remove(@NonNull final TransactionSynchronizationRegistry r, @NonNull final String key) {
        final var resource = EntityChangeCandidateCacheUtil.<T>lookup(r, key);
        if (null != resource) {
            r.putResource(key, null);
        }
        return resource;
    }

    @SuppressWarnings("unchecked")
    private static <T> T lookup(@NonNull final TransactionSynchronizationRegistry r, @NonNull final String key) {
        final var invocationContext = r.getResource(ServerConstants.REPLICATION_INVOCATION_KEY);
        if (null == invocationContext) {
            final var message = "Attempting to look up replication resource '" + key
                    + "' but there is no active Replication Invocation. This probably means you are attempting to"
                    + " update replicated Entities outside a valid Replication Invocation. Make sure the Entity is"
                    + " modified within a Replication Invocation.";
            throw new IllegalStateException(message);
        }
        return (T) r.getResource(key);
    }
}
