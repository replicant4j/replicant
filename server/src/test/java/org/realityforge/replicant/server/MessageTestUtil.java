package org.realityforge.replicant.server;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.annotation.Nullable;
import static org.testng.Assert.*;

/**
 * A set of utility methods for testing EntityMessage infrastructure.
 */
public final class MessageTestUtil
{
  public static final String ROUTING_KEY1 = "ROUTING_KEY1";
  public static final String ROUTING_KEY2 = "ROUTING_KEY2";
  public static final String ATTR_KEY1 = "ATTR_KEY1";
  public static final String ATTR_KEY2 = "ATTR_KEY2";

  public static EntityMessage createMessage( final int id,
                                             final int typeID,
                                             final long timestamp,
                                             @Nullable final String r1,
                                             @Nullable final String r2,
                                             @Nullable final String a1,
                                             @Nullable final String a2 )
  {
    return createMessage( id, typeID, timestamp, null, r1, r2, a1, a2 );
  }

  public static EntityMessage createMessage( final int id,
                                             final int typeID,
                                             final long timestamp,
                                             @Nullable final ChannelLink link,
                                             @Nullable final String r1,
                                             @Nullable final String r2,
                                             @Nullable final String a1,
                                             @Nullable final String a2 )
  {
    final HashMap<String, Serializable> routingKeys = new HashMap<>();
    if ( null != r1 )
    {
      routingKeys.put( ROUTING_KEY1, r1 );
    }
    if ( null != r2 )
    {
      routingKeys.put( ROUTING_KEY2, r2 );
    }

    final HashMap<String, Serializable> attributeValues =
      ( null == a1 && null == a2 ) ? null : new HashMap<>();
    if ( null != a1 )
    {
      attributeValues.put( ATTR_KEY1, a1 );
    }
    if ( null != a2 )
    {
      attributeValues.put( ATTR_KEY2, a2 );
    }

    final HashSet<ChannelLink> links;
    if ( null != link )
    {
      links = new HashSet<>();
      links.add( link );
    }
    else
    {
      links = null;
    }

    return new EntityMessage( id, typeID, timestamp, routingKeys, attributeValues, links );
  }

  public static void assertAttributeValue( final EntityMessage message,
                                           final String key,
                                           final String value )
  {
    final Map<String, Serializable> values = message.getAttributeValues();
    assertNotNull( values );
    assertEquals( values.get( key ), value );
  }

  public static void assertRouteValue( final EntityMessage message,
                                       final String key,
                                       final String value )
  {
    assertEquals( message.getRoutingKeys().get( key ), value );
  }
}
