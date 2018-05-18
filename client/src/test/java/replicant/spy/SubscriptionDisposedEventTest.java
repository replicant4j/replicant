package replicant.spy;

import java.util.HashMap;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ChannelAddress;
import replicant.Replicant;
import replicant.Subscription;
import static org.testng.Assert.*;

public class SubscriptionDisposedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    // Pause scheduler so Autoruns don't auto-converge
    pauseScheduler();

    final String filter = ValueUtil.randomString();
    final Subscription subscription =
      safeAction( () -> Replicant.context().createSubscription( new ChannelAddress( 1, 2 ),
                                                                filter,
                                                                true ) );

    final SubscriptionDisposedEvent event = new SubscriptionDisposedEvent( subscription );

    assertEquals( event.getSubscription(), subscription );

    final HashMap<String, Object> data = new HashMap<>();
    safeAction( () -> event.toMap( data ) );

    assertEquals( data.get( "type" ), "Subscription.Disposed" );
    assertEquals( data.get( "channel.systemId" ), 1 );
    assertEquals( data.get( "channel.channelId" ), 2 );
    assertEquals( data.get( "channel.id" ), null );
    assertEquals( data.get( "channel.filter" ), filter );
    assertEquals( data.get( "explicitSubscription" ), true );
    assertEquals( data.size(), 6 );
  }
}
