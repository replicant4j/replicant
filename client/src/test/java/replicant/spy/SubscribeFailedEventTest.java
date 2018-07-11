package replicant.spy;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ChannelAddress;
import static org.testng.Assert.*;

public class SubscribeFailedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final ChannelAddress address = new ChannelAddress( 1,2 );
    final SubscribeFailedEvent event =
      new SubscribeFailedEvent( 23, "Rose", address, new Error( "Something Bad Happened" ) );

    assertEquals( event.getSchemaId(), 23 );
    assertEquals( event.getSchemaName(), "Rose" );
    assertEquals( event.getAddress(), address );

    final HashMap<String, Object> data = new HashMap<>();
    event.toMap( data );

    assertEquals( data.get( "type" ), "Connector.SubscribeFailed" );
    assertEquals( data.get( "schema.id" ), 23 );
    assertEquals( data.get( "schema.name" ), "Rose" );
    assertEquals( data.get( "channel.systemId" ), 1 );
    assertEquals( data.get( "channel.channelId" ), 2 );
    assertEquals( data.get( "channel.id" ), address.getId() );
    assertEquals( data.get( "message" ), "Something Bad Happened" );
    assertEquals( data.size(), 7 );
  }
}
