package replicant.spy;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ChannelAddress;
import static org.testng.Assert.*;

public class SubscriptionUpdateStartedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    // Pause scheduler so Autoruns don't auto-converge
    pauseScheduler();

    final ChannelAddress address = new ChannelAddress( G.G1 );
    final SubscriptionUpdateStartedEvent event = new SubscriptionUpdateStartedEvent( 23, "Rose", address );

    assertEquals( event.getSchemaId(), 23 );
    assertEquals( event.getSchemaName(), "Rose" );
    assertEquals( event.getAddress(), address );

    final HashMap<String, Object> data = new HashMap<>();
    safeAction( () -> event.toMap( data ) );

    assertEquals( data.get( "type" ), "Connector.SubscriptionUpdateStarted" );
    assertEquals( data.get( "schema.id" ), 23 );
    assertEquals( data.get( "schema.name" ), "Rose" );
    assertEquals( data.get( "channel.type" ), "G1" );
    assertEquals( data.get( "channel.id" ), address.getId() );
    assertEquals( data.size(), 5 );
  }

  enum G
  {
    G1
  }
}
