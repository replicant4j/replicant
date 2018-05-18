package replicant.spy;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ChannelAddress;
import static org.testng.Assert.*;

public class UnsubscribeFailedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final ChannelAddress address = new ChannelAddress( G.G1 );
    final UnsubscribeFailedEvent event =
      new UnsubscribeFailedEvent( G.class, address, new Error( "Something Bad Happened" ) );

    assertEquals( event.getSystemType(), G.class );
    assertEquals( event.getAddress(), address );

    final HashMap<String, Object> data = new HashMap<>();
    safeAction( () -> event.toMap( data ) );

    assertEquals( data.get( "type" ), "Connector.UnsubscribeFailed" );
    assertEquals( data.get( "systemType" ), "G" );
    assertEquals( data.get( "channel.type" ), "G1" );
    assertEquals( data.get( "channel.id" ), address.getId() );
    assertEquals( data.get( "message" ), "Something Bad Happened" );
    assertEquals( data.size(), 5 );
  }

  enum G
  {
    G1
  }
}
