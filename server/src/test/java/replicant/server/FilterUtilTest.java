package replicant.server;

import static org.testng.Assert.*;

import javax.json.Json;
import org.testng.annotations.Test;

public class FilterUtilTest {
    @Test
    public void filtersEqual_handlesNullFilters() {
        final var filter = Json.createObjectBuilder().add("name", "alpha").build();

        assertTrue(FilterUtil.filtersEqual(null, null));
        assertFalse(FilterUtil.filtersEqual(filter, null));
        assertFalse(FilterUtil.filtersEqual(null, filter));
    }

    @Test
    public void filtersEqual_matchesEquivalentJsonObjects() {
        final var filter1 = Json.createObjectBuilder()
                .add("name", "alpha")
                .add("enabled", true)
                .build();
        final var filter2 = Json.createObjectBuilder()
                .add("enabled", true)
                .add("name", "alpha")
                .build();

        assertTrue(FilterUtil.filtersEqual(filter1, filter2));
    }

    @Test
    public void filtersEqual_rejectsDifferentJsonObjects() {
        final var filter1 = Json.createObjectBuilder().add("name", "alpha").build();
        final var filter2 = Json.createObjectBuilder().add("name", "beta").build();

        assertFalse(FilterUtil.filtersEqual(filter1, filter2));
    }
}
