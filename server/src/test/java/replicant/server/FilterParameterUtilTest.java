package replicant.server;

import static org.testng.Assert.*;

import javax.json.Json;
import org.testng.annotations.Test;

public class FilterParameterUtilTest {
    @Test
    public void filterParametersEqual_handlesNullFilterParameters() {
        final var filterParameter =
                Json.createObjectBuilder().add("name", "alpha").build();

        assertTrue(FilterParameterUtil.filterParametersEqual(null, null));
        assertFalse(FilterParameterUtil.filterParametersEqual(filterParameter, null));
        assertFalse(FilterParameterUtil.filterParametersEqual(null, filterParameter));
    }

    @Test
    public void filterParametersEqual_matchesEquivalentJsonObjects() {
        final var filterParameter1 = Json.createObjectBuilder()
                .add("name", "alpha")
                .add("enabled", true)
                .build();
        final var filterParameter2 = Json.createObjectBuilder()
                .add("enabled", true)
                .add("name", "alpha")
                .build();

        assertTrue(FilterParameterUtil.filterParametersEqual(filterParameter1, filterParameter2));
    }

    @Test
    public void filterParametersEqual_rejectsDifferentJsonObjects() {
        final var filterParameter1 =
                Json.createObjectBuilder().add("name", "alpha").build();
        final var filterParameter2 =
                Json.createObjectBuilder().add("name", "beta").build();

        assertFalse(FilterParameterUtil.filterParametersEqual(filterParameter1, filterParameter2));
    }
}
