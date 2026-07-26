package replicant;

import static org.realityforge.braincheck.Guards.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.messages.EntityChangeData;

/**
 * Defines a kind of server-side Entity and how its client Replica is created and updated.
 */
public final class EntityType {
    /**
     * Function used to create a Replica on receipt of an initial Entity change.
     *
     * @param <T> the type of the Replica.
     */
    @FunctionalInterface
    public interface Creator<T> {
        /**
         * Create a Replica from the supplied Entity change data.
         *
         * @param id   the Entity identifier.
         * @param data the state to use to create the Replica.
         */
        @NonNull
        T createReplica(int id, @NonNull EntityChangeData data);
    }

    /**
     * Function used to update a Replica on receipt of subsequent Entity changes.
     *
     * @param <T> the type of the Replica.
     */
    @FunctionalInterface
    public interface Updater<T> {
        /**
         * Update the specified Replica from the supplied Entity change data.
         *
         * @param replica the Replica.
         * @param data    the state to apply to the Replica.
         */
        void updateReplica(@NonNull T replica, @NonNull EntityChangeData data);
    }

    /**
     * The id of the entity type. This is the value used when transmitting messages across network.
     */
    private final int _id;
    /**
     * A human consumable name for entity type. It should be non-null if {@link Replicant#areNamesEnabled()} returns
     * true and <tt>null</tt> otherwise.
     */
    @Nullable
    private final String _name;
    /**
     * The Java type of the Replica.
     */
    @NonNull
    private final Class<?> _type;
    /**
     * The function to create a Replica.
     */
    @NonNull
    private final Creator<?> _creator;
    /**
     * The function to update a Replica.
     * This may be null if the Replica has no fields that can be updated.
     */
    @Nullable
    private final Updater<?> _updater;

    @NonNull
    private final DatasetLink[] _datasetLinks;

    public <T> EntityType(
            final int id,
            @Nullable final String name,
            @NonNull final Class<T> type,
            @NonNull final Creator<T> creator,
            @Nullable final Updater<T> updater,
            @NonNull final DatasetLink[] datasetLinks) {
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    () -> Replicant.areNamesEnabled() || null == name,
                    () -> "Replicant-0049: EntityType passed a name '" + name
                            + "' but Replicant.areNamesEnabled() is false");
        }
        _id = id;
        _name = Replicant.areNamesEnabled() ? Objects.requireNonNull(name) : null;
        _type = Objects.requireNonNull(type);
        _creator = Objects.requireNonNull(creator);
        _updater = updater;
        _datasetLinks = Objects.requireNonNull(datasetLinks);
    }

    /**
     * Return the id of the entity type.
     *
     * @return the id of the entity type.
     */
    public int getId() {
        return _id;
    }

    /**
     * Return the name of the entity type.
     * This method should NOT be invoked unless {@link Replicant#areNamesEnabled()} returns true and will throw an
     * exception if invariant checking is enabled.
     *
     * @return the name of the entity type.
     */
    @NonNull
    public String getName() {
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    Replicant::areNamesEnabled,
                    () -> "Replicant-0050: EntityType.getName() invoked when Replicant.areNamesEnabled() is false");
        }
        return Objects.requireNonNull(_name);
    }

    /**
     * Return the Java type of the Replica.
     *
     * @return the Java type of the Replica.
     */
    @NonNull
    public Class<?> getType() {
        return _type;
    }

    /**
     * Return the function to create a Replica.
     *
     * @return the function to create a Replica.
     */
    @NonNull
    public Creator<?> getCreator() {
        return _creator;
    }

    /**
     * Return the function to update a Replica.
     *
     * @return the function to update a Replica.
     */
    @Nullable
    public Updater<?> getUpdater() {
        return _updater;
    }

    @NonNull
    public DatasetLink[] getDatasetLinks() {
        return _datasetLinks;
    }

    @NonNull
    public List<DatasetLink> getOutwardDatasetLinks(final int datasetId) {
        return Stream.of(getDatasetLinks())
                .filter(datasetLink -> datasetLink.getSourceDatasetId() == datasetId)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        if (Replicant.areNamesEnabled()) {
            return getName();
        } else {
            return super.toString();
        }
    }
}
