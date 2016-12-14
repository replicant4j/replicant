package org.realityforge.replicant.server.transport;

import javax.json.Json;
import javax.json.JsonObject;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.server.ChannelDescriptor;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class SubscriptionEntryTest
{
  @Test
  public void basicFlow()
  {
    final ChannelDescriptor cd1 = new ChannelDescriptor( ValueUtil.randomInt(), ValueUtil.randomString() );
    final ChannelDescriptor cd2 = new ChannelDescriptor( ValueUtil.randomInt(), ValueUtil.randomString() );
    final ChannelDescriptor cd3 = new ChannelDescriptor( ValueUtil.randomInt(), ValueUtil.randomString() );
    final ChannelDescriptor cd4 = new ChannelDescriptor( ValueUtil.randomInt(), ValueUtil.randomString() );
    final ChannelDescriptor cd5 = new ChannelDescriptor( ValueUtil.randomInt(), ValueUtil.randomString() );

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
    assertEquals( entry.deregisterOutwardSubscriptions( cd2 ), new ChannelDescriptor[ 0 ] );
    assertEquals( entry.deregisterInwardSubscriptions( cd2 ), new ChannelDescriptor[ 0 ] );

    // Register incoming channels
    assertEquals( entry.registerInwardSubscriptions( cd2, cd3, cd4 ), new ChannelDescriptor[]{ cd2, cd3, cd4 } );
    assertEquals( entry.canUnsubscribe(), false );
    assertEquals( entry.getInwardSubscriptions().size(), 3 );
    assertEquals( entry.getInwardSubscriptions().contains( cd2 ), true );
    assertEquals( entry.getInwardSubscriptions().contains( cd3 ), true );
    assertEquals( entry.getInwardSubscriptions().contains( cd4 ), true );
    assertEquals( entry.getInwardSubscriptions().contains( cd5 ), false );
    assertEquals( entry.getOutwardSubscriptions().size(), 0 );

    assertEquals( entry.registerInwardSubscriptions( cd2, cd3, cd4 ), new ChannelDescriptor[ 0 ] );

    //Deregister some of those incoming
    assertEquals( entry.deregisterInwardSubscriptions( cd2, cd3 ), new ChannelDescriptor[]{ cd2, cd3 } );
    assertEquals( entry.canUnsubscribe(), false );
    assertEquals( entry.getInwardSubscriptions().size(), 1 );
    assertEquals( entry.getInwardSubscriptions().contains( cd2 ), false );
    assertEquals( entry.getInwardSubscriptions().contains( cd3 ), false );
    assertEquals( entry.getInwardSubscriptions().contains( cd4 ), true );
    assertEquals( entry.getInwardSubscriptions().contains( cd5 ), false );
    assertEquals( entry.getOutwardSubscriptions().size(), 0 );

    //Deregister the remaining
    assertEquals( entry.deregisterInwardSubscriptions( cd2, cd3, cd4 ), new ChannelDescriptor[]{ cd4 } );
    assertEquals( entry.canUnsubscribe(), true );
    assertEquals( entry.getInwardSubscriptions().size(), 0 );
    assertEquals( entry.getOutwardSubscriptions().size(), 0 );

    //Register outgoing channels
    assertEquals( entry.registerOutwardSubscriptions( cd2, cd3, cd3, cd4 ), new ChannelDescriptor[]{ cd2, cd3, cd4 } );
    assertEquals( entry.canUnsubscribe(), true );
    assertEquals( entry.getInwardSubscriptions().size(), 0 );
    assertEquals( entry.getOutwardSubscriptions().size(), 3 );
    assertEquals( entry.getOutwardSubscriptions().contains( cd2 ), true );
    assertEquals( entry.getOutwardSubscriptions().contains( cd3 ), true );
    assertEquals( entry.getOutwardSubscriptions().contains( cd4 ), true );
    assertEquals( entry.getOutwardSubscriptions().contains( cd5 ), false );

    assertEquals( entry.registerOutwardSubscriptions( cd2, cd3, cd3, cd4 ), new ChannelDescriptor[ 0 ] );

    //Deregister some outgoing
    assertEquals( entry.deregisterOutwardSubscriptions( cd2, cd3, cd3 ), new ChannelDescriptor[]{ cd2, cd3 } );
    assertEquals( entry.canUnsubscribe(), true );
    assertEquals( entry.getInwardSubscriptions().size(), 0 );
    assertEquals( entry.getOutwardSubscriptions().size(), 1 );
    assertEquals( entry.getOutwardSubscriptions().contains( cd2 ), false );
    assertEquals( entry.getOutwardSubscriptions().contains( cd3 ), false );
    assertEquals( entry.getOutwardSubscriptions().contains( cd4 ), true );
    assertEquals( entry.getOutwardSubscriptions().contains( cd5 ), false );
  }
}
