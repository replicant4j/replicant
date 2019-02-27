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
    final ChannelAddress address = new ChannelAddress( 1, 2 );
    final Object filter = null;
    final AreaOfInterestRequest.Type action = AreaOfInterestRequest.Type.ADD;

    final AreaOfInterestRequest entry = new AreaOfInterestRequest( address, action, filter );

    assertEquals( entry.getAddress(), address );
    assertEquals( entry.getType(), action );
    assertEquals( entry.toString(), "AreaOfInterestRequest[Type=ADD Address=1.2]" );
    assertEquals( entry.getFilter(), filter );
    assertTrue( entry.match( action, address, filter ) );
    assertFalse( entry.match( action, address, "OtherFilter" ) );
    assertFalse( entry.match( AreaOfInterestRequest.Type.REMOVE, address, filter ) );
    assertFalse( entry.match( action, new ChannelAddress( 1, 3, ValueUtil.randomInt() ), filter ) );

    assertFalse( entry.isInProgress() );
    entry.markAsInProgress();
    assertTrue( entry.isInProgress() );
    entry.markAsComplete();
    assertFalse( entry.isInProgress() );
  }

  @Test
  public void construct_withNOnNullFIlterAndRemoveAction()
  {
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> new AreaOfInterestRequest( new ChannelAddress( 1, 2 ),
                                                     AreaOfInterestRequest.Type.REMOVE,
                                                     "XXX" ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0027: AreaOfInterestRequest constructor passed a REMOVE request for address '1.2' with a non-null filter 'XXX'." );
  }

  @Test
  public void toString_WithFilter()
  {
    final AreaOfInterestRequest entry =
      new AreaOfInterestRequest( new ChannelAddress( 1, 2 ), AreaOfInterestRequest.Type.UPDATE, "XXX" );

    assertEquals( entry.toString(), "AreaOfInterestRequest[Type=UPDATE Address=1.2 Filter=XXX]" );
  }

  @Test
  public void toString_NamingDisabled()
  {
    ReplicantTestUtil.disableNames();
    final AreaOfInterestRequest entry =
      new AreaOfInterestRequest( new ChannelAddress( 1, 2 ), AreaOfInterestRequest.Type.UPDATE, "XXX" );

    assertEquals( entry.toString(),
                  "replicant.AreaOfInterestRequest@" + Integer.toHexString( System.identityHashCode( entry ) ) );
  }

  @Test
  public void removeActionIgnoredFilterDuringMatch()
  {
    final ChannelAddress address = new ChannelAddress( 1, 2 );
    final AreaOfInterestRequest.Type action = AreaOfInterestRequest.Type.REMOVE;

    final AreaOfInterestRequest entry = new AreaOfInterestRequest( address, action, null );

    assertTrue( entry.match( action, address, null ) );
    assertTrue( entry.match( action, address, "OtherFilter" ) );
    assertFalse( entry.match( AreaOfInterestRequest.Type.ADD, address, null ) );
  }
}
