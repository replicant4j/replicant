package replicant.spy;

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
    pauseScheduler();

    final ChannelAddress address = new ChannelAddress( 1, 2 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterest areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, filter ) );

    final AreaOfInterestDisposedEvent event = new AreaOfInterestDisposedEvent( areaOfInterest );

    assertEquals( event.getAreaOfInterest(), areaOfInterest );

    final HashMap<String, Object> data = new HashMap<>();
    safeAction( () -> event.toMap( data ) );

    assertEquals( data.get( "type" ), "AreaOfInterest.Disposed" );
    assertEquals( data.get( "channel.schemaId" ), 1 );
    assertEquals( data.get( "channel.channelId" ), 2 );
    assertNull( data.get( "channel.rootId" ) );
    assertEquals( data.get( "channel.filter" ), filter );
    assertEquals( data.size(), 5 );
  }
}
