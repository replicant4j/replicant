package org.realityforge.replicant.server.transport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.server.ChannelAddress;
import org.testng.annotations.Test;
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

    final SubscriptionEntry entry = new SubscriptionEntry( cd1 );

    assertEquals( entry.getDescriptor(), cd1 );
    assertEquals( entry.isExplicitlySubscribed(), false );
    assertEquals( entry.getInwardSubscriptions().size(), 0 );
    assertEquals( entry.getOutwardSubscriptions().size(), 0 );
    assertEquals( entry.canUnsubscribe(), true );
    assertEquals( entry.getFilter(), null );

    entry.setExplicitlySubscribed( true );
    assertEquals( entry.isExplicitlySubscribed(), true );
    assertEquals( entry.canUnsubscribe(), false );
    entry.setExplicitlySubscribed( false );
    assertEquals( entry.isExplicitlySubscribed(), false );
    assertEquals( entry.canUnsubscribe(), true );

    final JsonObject filter = Json.createObjectBuilder().build();

    entry.setFilter( filter );
    assertEquals( entry.getFilter(), filter );
    entry.setFilter( null );
    assertEquals( entry.getFilter(), null );


    // Deregister when there is none subscribed
    assertEquals( entry.deregisterOutwardSubscriptions( cd2 ), new ChannelAddress[ 0 ] );
    assertEquals( entry.deregisterInwardSubscriptions( cd2 ), new ChannelAddress[ 0 ] );

    // Register incoming channels
    assertEquals( entry.registerInwardSubscriptions( cd2, cd3, cd4 ), new ChannelAddress[]{ cd2, cd3, cd4 } );
    assertEquals( entry.canUnsubscribe(), false );
    assertEquals( entry.getInwardSubscriptions().size(), 3 );
    assertEquals( entry.getInwardSubscriptions().contains( cd2 ), true );
    assertEquals( entry.getInwardSubscriptions().contains( cd3 ), true );
    assertEquals( entry.getInwardSubscriptions().contains( cd4 ), true );
    assertEquals( entry.getInwardSubscriptions().contains( cd5 ), false );
    assertEquals( entry.getOutwardSubscriptions().size(), 0 );

    assertEquals( entry.registerInwardSubscriptions( cd2, cd3, cd4 ), new ChannelAddress[ 0 ] );

    //Deregister some of those incoming
    assertEquals( entry.deregisterInwardSubscriptions( cd2, cd3 ), new ChannelAddress[]{ cd2, cd3 } );
    assertEquals( entry.canUnsubscribe(), false );
    assertEquals( entry.getInwardSubscriptions().size(), 1 );
    assertEquals( entry.getInwardSubscriptions().contains( cd2 ), false );
    assertEquals( entry.getInwardSubscriptions().contains( cd3 ), false );
    assertEquals( entry.getInwardSubscriptions().contains( cd4 ), true );
    assertEquals( entry.getInwardSubscriptions().contains( cd5 ), false );
    assertEquals( entry.getOutwardSubscriptions().size(), 0 );

    //Deregister the remaining
    assertEquals( entry.deregisterInwardSubscriptions( cd2, cd3, cd4 ), new ChannelAddress[]{ cd4 } );
    assertEquals( entry.canUnsubscribe(), true );
    assertEquals( entry.getInwardSubscriptions().size(), 0 );
    assertEquals( entry.getOutwardSubscriptions().size(), 0 );

    //Register outgoing channels
    assertEquals( entry.registerOutwardSubscriptions( cd2, cd3, cd3, cd4 ), new ChannelAddress[]{ cd2, cd3, cd4 } );
    assertEquals( entry.canUnsubscribe(), true );
    assertEquals( entry.getInwardSubscriptions().size(), 0 );
    assertEquals( entry.getOutwardSubscriptions().size(), 3 );
    assertEquals( entry.getOutwardSubscriptions().contains( cd2 ), true );
    assertEquals( entry.getOutwardSubscriptions().contains( cd3 ), true );
    assertEquals( entry.getOutwardSubscriptions().contains( cd4 ), true );
    assertEquals( entry.getOutwardSubscriptions().contains( cd5 ), false );

    assertEquals( entry.registerOutwardSubscriptions( cd2, cd3, cd3, cd4 ), new ChannelAddress[ 0 ] );

    //Deregister some outgoing
    assertEquals( entry.deregisterOutwardSubscriptions( cd2, cd3, cd3 ), new ChannelAddress[]{ cd2, cd3 } );
    assertEquals( entry.canUnsubscribe(), true );
    assertEquals( entry.getInwardSubscriptions().size(), 0 );
    assertEquals( entry.getOutwardSubscriptions().size(), 1 );
    assertEquals( entry.getOutwardSubscriptions().contains( cd2 ), false );
    assertEquals( entry.getOutwardSubscriptions().contains( cd3 ), false );
    assertEquals( entry.getOutwardSubscriptions().contains( cd4 ), true );
    assertEquals( entry.getOutwardSubscriptions().contains( cd5 ), false );
  }

  @Test
  public void sorting()
  {
    final ChannelAddress cd1 = new ChannelAddress( 1, 77 );
    final ChannelAddress cd3 = new ChannelAddress( 1, 78 );
    final ChannelAddress cd4 = new ChannelAddress( 2, null );
    final ChannelAddress cd5 = new ChannelAddress( 3, null );

    final SubscriptionEntry entry1 = new SubscriptionEntry( cd1 );
    final SubscriptionEntry entry3 = new SubscriptionEntry( cd3 );
    final SubscriptionEntry entry4 = new SubscriptionEntry( cd4 );
    final SubscriptionEntry entry5 = new SubscriptionEntry( cd5 );

    final List<SubscriptionEntry> list = new ArrayList<>( Arrays.asList( entry5, entry4, entry3, entry1 ) );

    Collections.sort( list );

    final SubscriptionEntry[] expected = { entry1, entry3, entry4, entry5 };
    assertEquals( list.toArray( new SubscriptionEntry[ 0 ] ), expected );
  }
}
