package replicant.events;

import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ChannelAddress;
import static org.testng.Assert.*;

public class SubscribeStartedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final ChannelAddress address = new ChannelAddress( 1, 2 );
    final SubscribeStartedEvent event = new SubscribeStartedEvent( 23, address );

    assertEquals( event.getSchemaId(), 23 );
    assertEquals( event.getAddress(), address );
  }
}
