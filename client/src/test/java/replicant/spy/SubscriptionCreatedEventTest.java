package replicant.spy;

import arez.Arez;
import java.util.HashMap;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ChannelAddress;
import replicant.Replicant;
import replicant.Subscription;
import static org.testng.Assert.*;

public class SubscriptionCreatedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final String filter = ValueUtil.randomString();
    final Subscription subscription =
      Arez.context().safeAction( () -> Replicant.context().createSubscription( new ChannelAddress( G.G1 ),
                                                                               filter,
                                                                               true ) );

    final SubscriptionCreatedEvent event = new SubscriptionCreatedEvent( subscription );

    assertEquals( event.getSubscription(), subscription );

    final HashMap<String, Object> data = new HashMap<>();
    Arez.context().safeAction( () -> event.toMap( data ) );

    assertEquals( data.get( "type" ), "Subscription.Created" );
    assertEquals( data.get( "channel.type" ), "G1" );
    assertEquals( data.get( "channel.id" ), null );
    assertEquals( data.get( "channel.filter" ), filter );
    assertEquals( data.get( "explicitSubscription" ), true );
    assertEquals( data.size(), 5 );
  }

  enum G
  {
    G1
  }
}
