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
    final var address = new ChannelAddress( 1, 2 );

    final var event = new UnsubscribeRequestQueuedEvent( address );

    assertEquals( event.getAddress(), address );

    final var data = new HashMap<String, Object>();
    event.toMap( data );

    assertEquals( data.get( "type" ), "Connector.UnsubscribeRequestQueued" );
    assertEquals( data.get( "channel.schemaId" ), 1 );
    assertEquals( data.get( "channel.channelId" ), 2 );
    assertNull( data.get( "channel.rootId" ) );
    assertEquals( data.size(), 4 );
  }
}
