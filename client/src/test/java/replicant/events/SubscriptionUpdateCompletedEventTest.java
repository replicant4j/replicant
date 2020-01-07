package replicant.events;

import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ChannelAddress;
import static org.testng.Assert.*;

public class SubscriptionUpdateCompletedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    // Pause scheduler so Autoruns don't auto-converge
    pauseScheduler();

    final ChannelAddress address = new ChannelAddress( 1, 2 );
    final SubscriptionUpdateCompletedEvent event = new SubscriptionUpdateCompletedEvent( 23, "Rose", address );

    assertEquals( event.getSchemaId(), 23 );
    assertEquals( event.getSchemaName(), "Rose" );
    assertEquals( event.getAddress(), address );
  }
}
