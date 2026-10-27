package replicant;

import java.util.Objects;
import java.util.function.Function;

/**
 * Converts non-null Filter Parameter values between application and transport representations.
 * Null parameters pass through the Dataset boundary without invoking either function.
 *
 * @param <T> the application Filter Parameter type.
 */
public final class FilterParameterCodec<T> {
    private final Function<T, Object> _encoder;
    private final Function<Object, T> _decoder;

    /**
     * Create the paired conversions for one Dataset's Filter Parameter.
     *
     * @param encoder converts an application value to its JSON-compatible transport value.
     * @param decoder converts a transport value to its application value.
     */
    public FilterParameterCodec(final Function<T, Object> encoder, final Function<Object, T> decoder) {
        _encoder = Objects.requireNonNull(encoder);
        _decoder = Objects.requireNonNull(decoder);
    }

    Object encode(final T value) {
        return _encoder.apply(value);
    }

    T decode(final Object value) {
        return _decoder.apply(value);
    }
}
