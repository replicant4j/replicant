package replicant.server.ee;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import replicant.server.runtime.ReplicantRequestContextHolder;

public class ReplicantRequestContextHolderTest {
    @Test
    public void basicWorkflow() throws InterruptedException {
        final var key = "X";
        final var value = "1";

        ReplicantRequestContextHolder.clean();
        assertNull(ReplicantRequestContextHolder.get(key));
        ReplicantRequestContextHolder.put(key, value);
        final var v2 = ReplicantRequestContextHolder.get(key);
        assertEquals(v2, value);

        final var result = new Object[1];

        final var thread = new Thread(() -> result[0] = ReplicantRequestContextHolder.get(key));
        thread.start();
        thread.join();

        assertNull(result[0]);

        ReplicantRequestContextHolder.put(key, null);
        assertNull(ReplicantRequestContextHolder.get(key));

        ReplicantRequestContextHolder.put(key, value);
        assertNotNull(ReplicantRequestContextHolder.get(key));

        ReplicantRequestContextHolder.clean();

        assertNull(ReplicantRequestContextHolder.get(key));
    }
}
