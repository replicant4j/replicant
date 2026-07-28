package replicant.messages;

import java.util.HashMap;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * An implementation of EntityChangePayload suitable for use within the JVM.
 */
@GwtIncompatible
public class EntityChangePayloadImpl implements EntityChangePayload {
    private final HashMap<String, Object> _values = new HashMap<>();

    public HashMap<String, Object> getValues() {
        return _values;
    }

    @Override
    public boolean containsKey(@NonNull final String key) {
        return _values.containsKey(key);
    }

    @Override
    public boolean isNull(@NonNull final String key) {
        assert _values.containsKey(key);
        return null == _values.get(key);
    }

    @Override
    public int getIntegerValue(@NonNull final String key) {
        assert _values.containsKey(key);
        return (int) Objects.requireNonNull(_values.get(key));
    }

    @NonNull
    @Override
    public String getStringValue(@NonNull final String key) {
        assert _values.containsKey(key);
        return (String) Objects.requireNonNull(_values.get(key));
    }

    @Override
    public boolean getBooleanValue(@NonNull final String key) {
        assert _values.containsKey(key);
        return (Boolean) Objects.requireNonNull(_values.get(key));
    }
}
