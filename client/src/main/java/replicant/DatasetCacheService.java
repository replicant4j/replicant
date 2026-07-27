package replicant;

import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.messages.ChangeSetMessage;

/**
 * Optional client service for storing Dataset Cache Entries by Dataset Address.
 *
 * <p>Each stored representation is identified by an opaque Dataset Cache Version supplied by the server. Callers
 * compare Dataset Cache Versions only for equality and treat absent, unreadable, corrupt, or mismatched entries as
 * recoverable cache misses.
 *
 * <p>A Replicant Context owns its association with this service. Implementations may reuse external persistence
 * across Contexts, but each Context materializes independent Subscription and Replica state from a stored Change Set.
 */
public interface DatasetCacheService {
    /**
     * Return the Dataset Addresses with locally stored entries for the System Schema.
     *
     * @param systemSchemaId the System Schema identifier.
     * @return the Dataset Addresses with stored entries.
     */
    @NonNull
    Set<DatasetAddress> getDatasetAddresses(int systemSchemaId);

    /**
     * Return the Dataset Cache Version for the specified Dataset Address.
     *
     * @param datasetAddress the Dataset Address.
     * @return the opaque Dataset Cache Version, or null if no readable version is stored.
     */
    @Nullable
    String lookupDatasetCacheVersion(@NonNull DatasetAddress datasetAddress);

    /**
     * Return the Dataset Cache Entry for the specified Dataset Address.
     *
     * @param datasetAddress the Dataset Address.
     * @return the Dataset Cache Entry, or null if no readable entry is stored.
     */
    @Nullable
    DatasetCacheEntry lookupDatasetCacheEntry(@NonNull DatasetAddress datasetAddress);

    /**
     * Store a Change Set as a Dataset Cache Entry.
     *
     * @param datasetAddress      the concrete Dataset Address owning the representation.
     * @param datasetCacheVersion the opaque Dataset Cache Version supplied by the server.
     * @param changeSet           the Change Set to store.
     * @return true if the Change Set was stored, false otherwise.
     */
    boolean storeDatasetCacheEntry(
            @NonNull DatasetAddress datasetAddress,
            @NonNull String datasetCacheVersion,
            @NonNull ChangeSetMessage changeSet);

    /**
     * Invalidate the Dataset Cache Entry for the specified Dataset Address.
     *
     * @param datasetAddress the Dataset Address.
     * @return true if an entry was removed, false if no entry was stored.
     */
    boolean invalidateDatasetCacheEntry(@NonNull DatasetAddress datasetAddress);
}
