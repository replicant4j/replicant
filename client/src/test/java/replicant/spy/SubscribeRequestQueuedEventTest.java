package replicant.spy;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ChannelAddress;
import replicant.ValueUtil;
import static org.testng.Assert.*;

public class SubscribeRequestQueuedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final var address = new ChannelAddress( 1, 2 );
    final var filter = ValueUtil.randomString();

    final var event = new SubscribeRequestQueuedEvent( address, filter );

    assertEquals( event.getAddress(), address );
    assertEquals( event.getFilter(), filter );

    final var data = new HashMap<String, Object>();
    event.toMap( data );

    assertEquals( data.get( "type" ), "Connector.SubscribeRequestQueued" );
    assertEquals( data.get( "channel.schemaId" ), 1 );
    assertEquals( data.get( "channel.channelId" ), 2 );
    assertNull( data.get( "channel.rootId" ) );
    assertEquals( data.get( "channel.filter" ), filter );
    assertEquals( data.size(), 5 );
  }
}
