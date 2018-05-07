package replicant.spy;

import arez.Arez;
import java.util.HashMap;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.AreaOfInterest;
import replicant.ChannelAddress;
import replicant.Replicant;
import static org.testng.Assert.*;

public class AreaOfInterestFilterUpdatedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final String filter = ValueUtil.randomString();
    final AreaOfInterest areaOfInterest =
      Arez.context().safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( new ChannelAddress( G.G1 ),
                                                                                         filter ) );

    final AreaOfInterestFilterUpdatedEvent event = new AreaOfInterestFilterUpdatedEvent( areaOfInterest );

    assertEquals( event.getAreaOfInterest(), areaOfInterest );

    final HashMap<String, Object> data = new HashMap<>();
    Arez.context().safeAction( () -> event.toMap( data ) );

    assertEquals( data.get( "type" ), "AreaOfInterest.Updated" );
    assertEquals( data.get( "channel.type" ), "G1" );
    assertEquals( data.get( "channel.id" ), null );
    assertEquals( data.get( "channel.filter" ), filter );
    assertEquals( data.size(), 4 );
  }

  enum G
  {
    G1
  }
}
