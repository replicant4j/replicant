package replicant.server;

import java.util.Objects;
import javax.json.JsonObject;
import org.jspecify.annotations.Nullable;

public final class FilterParameterUtil {
    private FilterParameterUtil() {}

    public static boolean filterParametersEqual(
            @Nullable final JsonObject filterParameter1, @Nullable final JsonObject filterParameter2) {
        return Objects.equals(filterParameter1, filterParameter2);
    }
}
