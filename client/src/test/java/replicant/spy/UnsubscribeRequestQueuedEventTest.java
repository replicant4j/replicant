package replicant.spy;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ChannelAddress;
import static org.testng.Assert.*;

public class UnsubscribeRequestQueuedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final ChannelAddress address = new ChannelAddress( 1, 2 );

    final UnsubscribeRequestQueuedEvent event = new UnsubscribeRequestQueuedEvent( address );

    assertEquals( event.getAddress(), address );

    final HashMap<String, Object> data = new HashMap<>();
    event.toMap( data );

    assertEquals( data.get( "type" ), "Connector.UnsubscribeRequestQueued" );
    assertEquals( data.get( "channel.systemId" ), 1 );
    assertEquals( data.get( "channel.channelId" ), 2 );
    assertEquals( data.get( "channel.id" ), null );
    assertEquals( data.size(), 4 );
  }
}
