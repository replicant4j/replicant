package org.realityforge.replicant.server.transport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.Session;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.server.ChannelAddress;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class SubscriptionEntryTest
{
  @Test
  public void basicFlow()
  {
    final ChannelAddress cd1 = new ChannelAddress( ValueUtil.randomInt(), ValueUtil.randomInt() );
    final ChannelAddress cd2 = new ChannelAddress( ValueUtil.randomInt(), ValueUtil.randomInt() );
    final ChannelAddress cd3 = new ChannelAddress( ValueUtil.randomInt(), ValueUtil.randomInt() );
    final ChannelAddress cd4 = new ChannelAddress( ValueUtil.randomInt(), ValueUtil.randomInt() );
    final ChannelAddress cd5 = new ChannelAddress( ValueUtil.randomInt(), ValueUtil.randomInt() );

    final SubscriptionEntry entry = new SubscriptionEntry( newSession(), cd1 );

    assertEquals( entry.getAddress(), cd1 );
    assertFalse( entry.isExplicitlySubscribed() );
    assertEquals( entry.getInwardSubscriptions().size(), 0 );
    assertEquals( entry.getOutwardSubscriptions().size(), 0 );
    assertTrue( entry.canUnsubscribe() );
    assertNull( entry.getFilter() );

    entry.setExplicitlySubscribed( true );
    assertTrue( entry.isExplicitlySubscribed() );
    assertFalse( entry.canUnsubscribe() );
    entry.setExplicitlySubscribed( false );
    assertFalse( entry.isExplicitlySubscribed() );
    assertTrue( entry.canUnsubscribe() );

    final JsonObject filter = Json.createObjectBuilder().build();

    entry.setFilter( filter );
    assertEquals( entry.getFilter(), filter );
    entry.setFilter( null );
    assertNull( entry.getFilter() );

    // Deregister when there is none subscribed
    assertEquals( entry.deregisterOutwardSubscriptions( cd2 ), new ChannelAddress[ 0 ] );
    assertEquals( entry.deregisterInwardSubscriptions( cd2 ), new ChannelAddress[ 0 ] );

    // Register incoming channels
    assertEquals( entry.registerInwardSubscriptions( cd2, cd3, cd4 ), new ChannelAddress[]{ cd2, cd3, cd4 } );
    assertFalse( entry.canUnsubscribe() );
    assertEquals( entry.getInwardSubscriptions().size(), 3 );
    assertTrue( entry.getInwardSubscriptions().contains( cd2 ) );
    assertTrue( entry.getInwardSubscriptions().contains( cd3 ) );
    assertTrue( entry.getInwardSubscriptions().contains( cd4 ) );
    assertFalse( entry.getInwardSubscriptions().contains( cd5 ) );
    assertEquals( entry.getOutwardSubscriptions().size(), 0 );

    assertEquals( entry.registerInwardSubscriptions( cd2, cd3, cd4 ), new ChannelAddress[ 0 ] );

    //Deregister some of those incoming
    assertEquals( entry.deregisterInwardSubscriptions( cd2, cd3 ), new ChannelAddress[]{ cd2, cd3 } );
    assertFalse( entry.canUnsubscribe() );
    assertEquals( entry.getInwardSubscriptions().size(), 1 );
    assertFalse( entry.getInwardSubscriptions().contains( cd2 ) );
    assertFalse( entry.getInwardSubscriptions().contains( cd3 ) );
    assertTrue( entry.getInwardSubscriptions().contains( cd4 ) );
    assertFalse( entry.getInwardSubscriptions().contains( cd5 ) );
    assertEquals( entry.getOutwardSubscriptions().size(), 0 );

    //Deregister the remaining
    assertEquals( entry.deregisterInwardSubscriptions( cd2, cd3, cd4 ), new ChannelAddress[]{ cd4 } );
    assertTrue( entry.canUnsubscribe() );
    assertEquals( entry.getInwardSubscriptions().size(), 0 );
    assertEquals( entry.getOutwardSubscriptions().size(), 0 );

    //Register outgoing channels
    assertEquals( entry.registerOutwardSubscriptions( cd2, cd3, cd3, cd4 ), new ChannelAddress[]{ cd2, cd3, cd4 } );
    assertTrue( entry.canUnsubscribe() );
    assertEquals( entry.getInwardSubscriptions().size(), 0 );
    assertEquals( entry.getOutwardSubscriptions().size(), 3 );
    assertTrue( entry.getOutwardSubscriptions().contains( cd2 ) );
    assertTrue( entry.getOutwardSubscriptions().contains( cd3 ) );
    assertTrue( entry.getOutwardSubscriptions().contains( cd4 ) );
    assertFalse( entry.getOutwardSubscriptions().contains( cd5 ) );

    assertEquals( entry.registerOutwardSubscriptions( cd2, cd3, cd3, cd4 ), new ChannelAddress[ 0 ] );

    //Deregister some outgoing
    assertEquals( entry.deregisterOutwardSubscriptions( cd2, cd3, cd3 ), new ChannelAddress[]{ cd2, cd3 } );
    assertTrue( entry.canUnsubscribe() );
    assertEquals( entry.getInwardSubscriptions().size(), 0 );
    assertEquals( entry.getOutwardSubscriptions().size(), 1 );
    assertFalse( entry.getOutwardSubscriptions().contains( cd2 ) );
    assertFalse( entry.getOutwardSubscriptions().contains( cd3 ) );
    assertTrue( entry.getOutwardSubscriptions().contains( cd4 ) );
    assertFalse( entry.getOutwardSubscriptions().contains( cd5 ) );
  }

  @Test
  public void sorting()
  {
    final ChannelAddress cd1 = new ChannelAddress( 1, 42 );
    final ChannelAddress cd3 = new ChannelAddress( 1, 43 );
    final ChannelAddress cd4 = new ChannelAddress( 2, null );
    final ChannelAddress cd5 = new ChannelAddress( 3, null );

    final ReplicantSession session = newSession();
    final SubscriptionEntry entry1 = new SubscriptionEntry( session, cd1 );
    final SubscriptionEntry entry3 = new SubscriptionEntry( session, cd3 );
    final SubscriptionEntry entry4 = new SubscriptionEntry( session, cd4 );
    final SubscriptionEntry entry5 = new SubscriptionEntry( session, cd5 );

    final List<SubscriptionEntry> list = new ArrayList<>( Arrays.asList( entry5, entry4, entry3, entry1 ) );

    Collections.sort( list );

    final SubscriptionEntry[] expected = { entry1, entry3, entry4, entry5 };
    assertEquals( list.toArray( new SubscriptionEntry[ 0 ] ), expected );
  }

  @Nonnull
  private ReplicantSession newSession()
  {
    return new ReplicantSession( mock( Session.class ) );
  }
}
