package replicant.server.transport;

import javax.websocket.Session;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.server.ChannelAddress;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantSessionTest
{
  @SuppressWarnings( "ConstantConditions" )
  @Test
  public void basicOperation()
  {
    final Session webSocketSession = mock( Session.class );
    final String sessionId = ValueUtil.randomString();
    when( webSocketSession.getId() ).thenReturn( sessionId );
    final ReplicantSession session = new ReplicantSession( webSocketSession );
    session.getLock().lock();

    assertEquals( session.getId(), sessionId );

    assertEquals( session.getSubscriptions().size(), 0 );

    final ChannelAddress cd1 = new ChannelAddress( 1, null );

    assertNull( session.findSubscriptionEntry( cd1 ) );
    assertFalse( session.isSubscriptionEntryPresent( cd1 ) );

    try
    {
      session.getSubscriptionEntry( cd1 );
      fail( "Expected to be unable to get non existent entry" );
    }
    catch ( final IllegalStateException ise )
    {
      assertEquals( ise.getMessage(), "Unable to locate subscription entry for 1" );
    }

    final SubscriptionEntry entry = session.createSubscriptionEntry( cd1 );

    assertEquals( entry.address(), cd1 );
    assertEquals( session.getSubscriptions().size(), 1 );
    assertEquals( session.findSubscriptionEntry( cd1 ), entry );
    assertEquals( session.getSubscriptionEntry( cd1 ), entry );
    assertTrue( session.getSubscriptions().containsKey( cd1 ) );
    assertTrue( session.getSubscriptions().containsValue( entry ) );

    try
    {
      session.getSubscriptions().remove( cd1 );
      fail( "Expected to be unable to delete subscription as it is a read-only set" );
    }
    catch ( final UnsupportedOperationException uoe )
    {
      //ignored
    }

    assertTrue( session.deleteSubscriptionEntry( entry ) );
    assertFalse( session.deleteSubscriptionEntry( entry ) );

    assertNull( session.findSubscriptionEntry( cd1 ) );
    assertFalse( session.isSubscriptionEntryPresent( cd1 ) );
    assertEquals( session.getSubscriptions().size(), 0 );
  }

  @Test
  public void cacheKeys()
  {
    final ReplicantSession session = new ReplicantSession( mock( Session.class ) );
    session.getLock().lock();

    final ChannelAddress cd1 = new ChannelAddress( 1, null );

    assertNull( session.getETag( cd1 ) );

    session.setETag( cd1, "X" );

    assertEquals( session.getETag( cd1 ), "X" );
  }
}
