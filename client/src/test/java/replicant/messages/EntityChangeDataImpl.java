package replicant.messages;

import java.util.HashMap;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * An implementation of EntityChangeData suitable for use within the JVM.
 */
@GwtIncompatible
public class EntityChangeDataImpl implements EntityChangeData {
    private final HashMap<String, Object> _data = new HashMap<>();

    public HashMap<String, Object> getData() {
        return _data;
    }

    @Override
    public boolean containsKey(@NonNull final String key) {
        return _data.containsKey(key);
    }

    @Override
    public boolean isNull(@NonNull final String key) {
        assert _data.containsKey(key);
        return null == _data.get(key);
    }

    @Override
    public int getIntegerValue(@NonNull final String key) {
        assert _data.containsKey(key);
        return (int) Objects.requireNonNull(_data.get(key));
    }

    @NonNull
    @Override
    public String getStringValue(@NonNull final String key) {
        assert _data.containsKey(key);
        return (String) Objects.requireNonNull(_data.get(key));
    }

    @Override
    public boolean getBooleanValue(@NonNull final String key) {
        assert _data.containsKey(key);
        return (Boolean) Objects.requireNonNull(_data.get(key));
    }
}
