package replicant;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import replicant.messages.SubscriptionChangeMessage;

public class SubscriptionChangeMessageTest extends AbstractReplicantTest {
    @Test
    public void construct() {
        final SubscriptionChangeMessage action = SubscriptionChangeMessage.create("+1.2", null);

        assertEquals(action.getSubscriptionAction(), "+1.2");
        assertNull(action.getFilter());
    }

    @Test
    public void construct_withoutSubdatasetId() {
        final Object filter = ValueUtil.randomString();
        final SubscriptionChangeMessage action = SubscriptionChangeMessage.create("-1", filter);

        assertEquals(action.getSubscriptionAction(), "-1");
        assertEquals(action.getFilter(), filter);
    }
}
