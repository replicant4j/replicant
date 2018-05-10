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

public class AreaOfInterestDisposedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    // Pause scheduler so Autoruns don't auto-converge
    Arez.context().pauseScheduler();

    final String filter = ValueUtil.randomString();
    final AreaOfInterest areaOfInterest =
      Arez.context().safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( new ChannelAddress( G.G1 ),
                                                                                         filter ) );

    final AreaOfInterestDisposedEvent event = new AreaOfInterestDisposedEvent( areaOfInterest );

    assertEquals( event.getAreaOfInterest(), areaOfInterest );

    final HashMap<String, Object> data = new HashMap<>();
    Arez.context().safeAction( () -> event.toMap( data ) );

    assertEquals( data.get( "type" ), "AreaOfInterest.Disposed" );
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
