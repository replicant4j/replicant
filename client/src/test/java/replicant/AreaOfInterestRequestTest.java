package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class AreaOfInterestRequestTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final ChannelAddress address = new ChannelAddress( G.G1 );
    final Object filterParameter = null;
    final AreaOfInterestAction action = AreaOfInterestAction.ADD;

    final AreaOfInterestRequest entry = new AreaOfInterestRequest( address, action, filterParameter );

    assertEquals( entry.getAddress(), address );
    assertEquals( entry.getAction(), action );
    assertEquals( entry.getCacheKey(), "G:G1" );
    assertEquals( entry.toString(), "AOI[Channel=G.G1]" );
    assertEquals( entry.getFilter(), filterParameter );
    assertEquals( entry.match( action, address, filterParameter ), true );
    assertEquals( entry.match( action, address, "OtherFilter" ), false );
    assertEquals( entry.match( AreaOfInterestAction.REMOVE, address, filterParameter ), false );
    assertEquals( entry.match( action, new ChannelAddress( G.G2, ValueUtil.randomInt() ), filterParameter ),
                  false );

    assertEquals( entry.isInProgress(), false );
    entry.markAsInProgress();
    assertEquals( entry.isInProgress(), true );
    entry.markAsComplete();
    assertEquals( entry.isInProgress(), false );
  }

  @Test
  public void toString_WithFilter()
  {
    final AreaOfInterestRequest entry =
      new AreaOfInterestRequest( new ChannelAddress( G.G1 ), AreaOfInterestAction.UPDATE, "XXX" );

    assertEquals( entry.toString(), "AOI[Channel=G.G1,filter=XXX]" );
  }

  @Test
  public void toString_NamingDisabled()
  {
    ReplicantTestUtil.disableNames();
    final AreaOfInterestRequest entry =
      new AreaOfInterestRequest( new ChannelAddress( G.G1 ), AreaOfInterestAction.UPDATE, "XXX" );

    assertEquals( entry.toString(),
                  "replicant.AreaOfInterestRequest@" + Integer.toHexString( System.identityHashCode( entry ) ) );
  }

  @Test
  public void removeActionIgnoredFilterDuringMatch()
  {
    final ChannelAddress address = new ChannelAddress( G.G1 );
    final AreaOfInterestAction action = AreaOfInterestAction.REMOVE;

    final AreaOfInterestRequest entry = new AreaOfInterestRequest( address, action, null );

    assertEquals( entry.match( action, address, null ), true );
    assertEquals( entry.match( action, address, "OtherFilter" ), true );
    assertEquals( entry.match( AreaOfInterestAction.ADD, address, null ), false );
  }

  public enum G
  {
    G1, G2
  }
}
