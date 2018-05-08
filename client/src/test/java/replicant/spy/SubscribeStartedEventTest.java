package replicant.spy;

import arez.Arez;
import java.util.HashMap;
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
    final ChannelAddress address = new ChannelAddress( G.G1 );
    final SubscribeStartedEvent event = new SubscribeStartedEvent( G.class, address );

    assertEquals( event.getSystemType(), G.class );
    assertEquals( event.getAddress(), address );

    final HashMap<String, Object> data = new HashMap<>();
    Arez.context().safeAction( () -> event.toMap( data ) );

    assertEquals( data.get( "type" ), "DataLoader.SubscribeStarted" );
    assertEquals( data.get( "systemType" ), "G" );
    assertEquals( data.get( "channel.type" ), "G1" );
    assertEquals( data.get( "channel.id" ), address.getId() );
    assertEquals( data.size(), 4 );
  }

  enum G
  {
    G1
  }
}
