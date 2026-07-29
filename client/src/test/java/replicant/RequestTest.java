package replicant;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class RequestTest extends AbstractReplicantTest {
    @Test
    public void construct() {
        final Connection connection = createConnection();
        final String name = ValueUtil.randomString();
        final RequestEntry entry = connection.newRequest(name, false, null);
        final Request request = new Request(connection, entry);

        assertEquals(request.getReplicantSessionId(), connection.getReplicantSessionId());
        assertEquals(request.getRequestId(), entry.getRequestId());
    }
}
