package org.realityforge.replicant.server.transport;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.server.ChannelDescriptor;
import org.realityforge.replicant.server.TestSession;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ReplicantSessionTest
{
  @Test
  public void basicOperation()
  {
    final String sessionID = ValueUtil.randomString();
    final ReplicantSession session = new TestSession( sessionID );

    assertEquals( session.getSessionID(), sessionID );
    assertEquals( session.getQueue().size(), 0 );

    assertEquals( session.getSubscriptions().size(), 0 );

    final ChannelDescriptor cd1 = new ChannelDescriptor( 1, null );

    assertNull( session.findSubscriptionEntry( cd1 ) );
    assertFalse( session.isSubscriptionEntryPresent( cd1 ) );

    try
    {
      session.getSubscriptionEntry( cd1 );
      fail( "Expected to be unable to get non existent entry" );
    }
    catch ( final IllegalStateException ise )
    {
      assertEquals( ise.getMessage(), "Unable to locate subscription entry for #1#" );
    }

    final SubscriptionEntry entry = session.createSubscriptionEntry( cd1 );

    assertEquals( entry.getDescriptor(), cd1 );
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
}
