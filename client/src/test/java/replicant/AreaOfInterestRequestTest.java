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
    final Object filter = null;
    final AreaOfInterestAction action = AreaOfInterestAction.ADD;

    final AreaOfInterestRequest entry = new AreaOfInterestRequest( address, action, filter );

    assertEquals( entry.getAddress(), address );
    assertEquals( entry.getAction(), action );
    assertEquals( entry.getCacheKey(), "G:G1" );
    assertEquals( entry.toString(), "AreaOfInterestRequest[Action=ADD Address=G.G1]" );
    assertEquals( entry.getFilter(), filter );
    assertEquals( entry.match( action, address, filter ), true );
    assertEquals( entry.match( action, address, "OtherFilter" ), false );
    assertEquals( entry.match( AreaOfInterestAction.REMOVE, address, filter ), false );
    assertEquals( entry.match( action, new ChannelAddress( G.G2, ValueUtil.randomInt() ), filter ),
                  false );

    assertEquals( entry.isInProgress(), false );
    entry.markAsInProgress();
    assertEquals( entry.isInProgress(), true );
    entry.markAsComplete();
    assertEquals( entry.isInProgress(), false );
  }

  @Test
  public void construct_withNOnNullFIlterAndRemoveAction()
  {
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> new AreaOfInterestRequest( new ChannelAddress( G.G1 ),
                                                     AreaOfInterestAction.REMOVE,
                                                     "XXX" ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0027: AreaOfInterestRequest constructor passed a REMOVE request for address 'G.G1' with a non-null filter 'XXX'." );
  }

  @Test
  public void toString_WithFilter()
  {
    final AreaOfInterestRequest entry =
      new AreaOfInterestRequest( new ChannelAddress( G.G1 ), AreaOfInterestAction.UPDATE, "XXX" );

    assertEquals( entry.toString(), "AreaOfInterestRequest[Action=UPDATE Address=G.G1 Filter=XXX]" );
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
