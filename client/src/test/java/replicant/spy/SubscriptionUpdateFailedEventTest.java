package replicant.spy;

import arez.Arez;
import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ChannelAddress;
import static org.testng.Assert.*;

public class SubscriptionUpdateFailedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    // Pause scheduler so Autoruns don't auto-converge
    Arez.context().pauseScheduler();

    final ChannelAddress address = new ChannelAddress( G.G1 );
    final SubscriptionUpdateFailedEvent event =
      new SubscriptionUpdateFailedEvent( G.class, address, new Error( "Something Bad Happened" ) );

    assertEquals( event.getSystemType(), G.class );
    assertEquals( event.getAddress(), address );

    final HashMap<String, Object> data = new HashMap<>();
    Arez.context().safeAction( () -> event.toMap( data ) );

    assertEquals( data.get( "type" ), "Connector.SubscriptionUpdateFailed" );
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
