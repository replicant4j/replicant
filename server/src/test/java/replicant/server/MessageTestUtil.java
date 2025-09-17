package replicant.server;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.testng.Assert.*;

/**
 * A set of utility methods for testing EntityMessage infrastructure.
 */
public final class MessageTestUtil
{
  @Nonnull
  static final String ROUTING_KEY1 = "ROUTING_KEY1";
  @Nonnull
  static final String ROUTING_KEY2 = "ROUTING_KEY2";
  @Nonnull
  public static final String ATTR_KEY1 = "ATTR_KEY1";
  @Nonnull
  public static final String ATTR_KEY2 = "ATTR_KEY2";

  @Nonnull
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

  @Nonnull
  static EntityMessage createMessage( final int id,
                                      final int typeID,
                                      final long timestamp,
                                      @Nullable final ChannelLink link,
                                      @Nullable final String r1,
                                      @Nullable final String r2,
                                      @Nullable final String a1,
                                      @Nullable final String a2 )
  {
    final var routingKeys = new HashMap<String, Serializable>();
    if ( null != r1 )
    {
      routingKeys.put( ROUTING_KEY1, r1 );
    }
    if ( null != r2 )
    {
      routingKeys.put( ROUTING_KEY2, r2 );
    }

    final var attributeValues = ( null == a1 && null == a2 ) ? null : new HashMap<String, Serializable>();
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

  static void assertAttributeValue( @Nonnull final EntityMessage message,
                                    @Nonnull final String key,
                                    @Nullable final String value )
  {
    final var values = message.getAttributeValues();
    assertNotNull( values );
    assertEquals( values.get( key ), value );
  }

  static void assertRouteValue( @Nonnull final EntityMessage message,
                                @Nonnull final String key,
                                @Nullable final String value )
  {
    assertEquals( message.getRoutingKeys().get( key ), value );
  }
}
