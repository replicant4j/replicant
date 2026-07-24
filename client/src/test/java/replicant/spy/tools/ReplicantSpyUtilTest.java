package replicant.spy.tools;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;

public class ReplicantSpyUtilTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        // Not a lot of ability to test actual listener code outside a browser
        assertFalse(ReplicantSpyUtil.isSpyEventLoggingEnabled());
        ReplicantSpyUtil.enableSpyEventLogging();
        assertTrue(ReplicantSpyUtil.isSpyEventLoggingEnabled());
        ReplicantSpyUtil.disableSpyEventLogging();
        assertFalse(ReplicantSpyUtil.isSpyEventLoggingEnabled());
    }
}
