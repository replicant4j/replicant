package replicant.events;

import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ChannelAddress;
import static org.testng.Assert.*;

public class SubscriptionUpdateStartedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    // Pause scheduler so Autoruns don't auto-converge
    pauseScheduler();

    final ChannelAddress address = new ChannelAddress( 1, 2 );
    final SubscriptionUpdateStartedEvent event = new SubscriptionUpdateStartedEvent( 23, address );

    assertEquals( event.getSchemaId(), 23 );
    assertEquals( event.getAddress(), address );
  }
}
