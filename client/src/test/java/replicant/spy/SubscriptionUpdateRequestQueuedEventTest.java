package replicant.spy;

import java.util.HashMap;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ChannelAddress;
import static org.testng.Assert.*;

public class SubscriptionUpdateRequestQueuedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final ChannelAddress address = new ChannelAddress( 1, 2 );
    final String filter = ValueUtil.randomString();

    final SubscriptionUpdateRequestQueuedEvent event = new SubscriptionUpdateRequestQueuedEvent( address, filter );

    assertEquals( event.getAddress(), address );
    assertEquals( event.getFilter(), filter );

    final HashMap<String, Object> data = new HashMap<>();
    event.toMap( data );

    assertEquals( data.get( "type" ), "Connector.SubscriptionUpdateRequestQueued" );
    assertEquals( data.get( "channel.systemId" ), 1 );
    assertEquals( data.get( "channel.channelId" ), 2 );
    assertEquals( data.get( "channel.id" ), null );
    assertEquals( data.get( "channel.filter" ), filter );
    assertEquals( data.size(), 5 );
  }
}
