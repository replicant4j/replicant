package replicant;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Value equality and diagnostics for application Filter Parameters.
 */
public final class FilterParameterUtil {
    public static boolean filterParametersEqual(
            @Nullable final Object filterParameter1, @Nullable final Object filterParameter2) {
        return Objects.equals(filterParameter1, filterParameter2);
    }

    @NonNull
    public static String filterParameterToString(@Nullable final Object filterParameter) {
        return String.valueOf(filterParameter);
    }

    private FilterParameterUtil() {}
}
