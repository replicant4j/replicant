package replicant;

import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * A System Schema rule declaring that encountering the configured Entity relationship in the source Dataset may require a
 * Subscription to the target Dataset.
 *
 * <p>This rule is not interpreted directly at runtime. Domgen generates the runtime integration while this
 * representation supports System Schema definition and validation.
 */
public final class DatasetLink {
    /**
     * The identifier of the source Dataset.
     */
    private final int _sourceDatasetId;
    /**
     * The identifier of the target Dataset.
     */
    private final int _targetDatasetId;
    /**
     * The path of attributes from the Entity in the source Dataset to the Dataset Root of the target Dataset.
     * It is expected that the attributes are immutable and all but the first are non-null.(Unlike Domgen where
     * the path omits the first attribute, this path includes the entire path)
     */
    @NonNull
    private final String[] _path;

    public DatasetLink(final int sourceDatasetId, final int targetDatasetId, @NonNull final String[] path) {
        _sourceDatasetId = sourceDatasetId;
        _targetDatasetId = targetDatasetId;
        _path = Objects.requireNonNull(path);
    }

    public int getSourceDatasetId() {
        return _sourceDatasetId;
    }

    public int getTargetDatasetId() {
        return _targetDatasetId;
    }

    @NonNull
    public String[] getPath() {
        return _path;
    }

    @Override
    public String toString() {
        if (Replicant.areNamesEnabled()) {
            return "DatasetLink[" + getSourceDatasetId() + "->" + getTargetDatasetId() + " via "
                    + Arrays.asList(getPath()) + "]";
        } else {
            return super.toString();
        }
    }
}
