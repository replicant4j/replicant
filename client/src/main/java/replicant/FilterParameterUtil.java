package replicant;

import akasha.core.JSON;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Simple utility that is extracted so it can be replaced by the GWT compiler.
 */
public final class FilterParameterUtil {
    private static final FilterParameterSupport c_support = new FilterParameterSupport();

    public static boolean filterParametersEqual(
            @Nullable final Object filterParameter1, @Nullable final Object filterParameter2) {
        return c_support.filterParametersEqual(filterParameter1, filterParameter2);
    }

    @NonNull
    public static String filterParameterToString(@Nullable final Object filterParameter) {
        return c_support.filterParameterToString(filterParameter);
    }

    /**
     * Abstract support class with methods used by GWT.
     */
    @SuppressWarnings("unused")
    private abstract static class AbstractFilterParameterSupport {
        boolean filterParametersEqual(
                @Nullable final Object filterParameter1, @Nullable final Object filterParameter2) {
            final String filterParameter1String =
                    null == filterParameter1 ? null : filterParameterToString(filterParameter1);
            final String filterParameter2String =
                    null == filterParameter2 ? null : filterParameterToString(filterParameter2);
            return Objects.equals(filterParameter1String, filterParameter2String);
        }

        @NonNull
        String filterParameterToString(@Nullable final Object filterParameter) {
            return null == filterParameter ? "" : JSON.stringify(filterParameter);
        }
    }

    /**
     * Concrete support class with methods used by JVM.
     */
    private static final class FilterParameterSupport extends AbstractFilterParameterSupport {
        @GwtIncompatible
        @Override
        boolean filterParametersEqual(
                @Nullable final Object filterParameter1, @Nullable final Object filterParameter2) {
            return Objects.equals(filterParameter1, filterParameter2);
        }

        @GwtIncompatible
        @NonNull
        @Override
        String filterParameterToString(@Nullable final Object filterParameter) {
            return String.valueOf(filterParameter);
        }
    }

    private FilterParameterUtil() {}
}
