package replicant.spy;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ChannelAddress;
import replicant.Subscription;
import replicant.ValueUtil;
import static org.testng.Assert.*;

public class SubscriptionCreatedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    // Pause scheduler so Autoruns don't auto-converge
    pauseScheduler();

    final var filter = ValueUtil.randomString();
    final var subscription = createSubscription( new ChannelAddress( 1, 2 ), filter, true );

    final var event = new SubscriptionCreatedEvent( subscription );

    assertEquals( event.getSubscription(), subscription );

    final var data = new HashMap<String, Object>();
    safeAction( () -> event.toMap( data ) );

    assertEquals( data.get( "type" ), "Subscription.Created" );
    assertEquals( data.get( "channel.schemaId" ), 1 );
    assertEquals( data.get( "channel.channelId" ), 2 );
    assertNull( data.get( "channel.rootId" ) );
    assertEquals( data.get( "channel.filter" ), filter );
    assertEquals( data.get( "explicitSubscription" ), true );
    assertEquals( data.size(), 6 );
  }
}
