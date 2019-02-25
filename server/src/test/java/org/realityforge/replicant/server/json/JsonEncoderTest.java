package org.realityforge.replicant.server.json;

import java.io.Serializable;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.server.Change;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelAction;
import org.realityforge.replicant.server.ChannelAction.Action;
import org.realityforge.replicant.server.ChannelAddress;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.MessageTestUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Utility class used when encoding EntityMessage into JSON payload.
 */
public final class JsonEncoderTest
{
  @Test
  public void encodeAllData()
    throws Exception
  {
    final int id = 17;
    final int typeID = 42;
    final Calendar calendar = Calendar.getInstance();
    calendar.set( Calendar.YEAR, 2001 );
    calendar.set( Calendar.MONTH, Calendar.JULY );
    calendar.set( Calendar.DAY_OF_MONTH, 5 );
    calendar.set( Calendar.AM_PM, Calendar.AM );
    calendar.set( Calendar.HOUR_OF_DAY, 5 );
    calendar.set( Calendar.MINUTE, 8 );
    calendar.set( Calendar.SECOND, 56 );
    calendar.set( Calendar.MILLISECOND, 0 );

    final Date date = calendar.getTime();

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final Map<String, Serializable> values = message.getAttributeValues();
    assertNotNull( values );
    values.put( "key3", date );

    final int lastChangeSetID = 1;
    final int requestId = 1;
    final String etag = "#1";
    final JsonObject filter = Json.createBuilderFactory( null ).createObjectBuilder().add( "a", "b" ).build();

    final Change change = new Change( message );
    change.getChannels().put( 1, null );
    change.getChannels().put( 2, 42 );
    change.getChannels().put( 3, 73 );
    final ChangeSet cs = new ChangeSet();
    cs.merge( change );
    cs.mergeAction( new ChannelAction( new ChannelAddress( 45, 77 ), Action.UPDATE, filter ) );
    final String encoded = JsonEncoder.encodeChangeSet( lastChangeSetID, requestId, etag, cs );
    final JsonObject changeSet = toJsonObject( encoded );

    assertNotNull( changeSet );

    assertEquals( changeSet.getInt( TransportConstants.LAST_CHANGE_SET_ID ), lastChangeSetID );
    assertEquals( changeSet.getInt( TransportConstants.REQUEST_ID ), requestId );
    assertEquals( changeSet.getString( TransportConstants.ETAG ), etag );

    final JsonObject action = changeSet.getJsonArray( TransportConstants.CHANNEL_ACTIONS ).getJsonObject( 0 );
    assertEquals( action.getInt( TransportConstants.CHANNEL_ID ), 45 );
    assertEquals( action.getInt( TransportConstants.SUBCHANNEL_ID ), 77 );
    assertEquals( action.getString( TransportConstants.ACTION ), "update" );
    assertEquals( action.getJsonObject( TransportConstants.CHANNEL_FILTER ).toString(), filter.toString() );

    final JsonObject object = changeSet.getJsonArray( TransportConstants.CHANGES ).getJsonObject( 0 );

    assertEquals( object.getString( TransportConstants.ENTITY_ID ), "42.17" );

    final JsonObject data = object.getJsonObject( TransportConstants.DATA );
    assertNotNull( data );
    assertEquals( data.getString( MessageTestUtil.ATTR_KEY1 ), "a1" );
    assertEquals( data.getString( MessageTestUtil.ATTR_KEY2 ), "a2" );
    assertTrue( data.getString( "key3" ).startsWith( "2001-07-05T05:08:56.000" ) );

    final JsonArray channels = object.getJsonArray( TransportConstants.CHANNELS );
    assertNotNull( channels );
    assertEquals( channels.size(), 3 );
    final String channel1 = channels.getString( 0 );
    assertNotNull( channel1 );
    final String channel2 = channels.getString( 1 );
    assertNotNull( channel2 );
    final String channel3 = channels.getString( 2 );
    assertNotNull( channel3 );

    assertEquals( channel1, "1" );
    assertEquals( channel2, "2.42" );
    assertEquals( channel3, "3.73" );
  }

  @Test
  public void encodeChangeSetFromEntityMessages_deleteMessage()
    throws Exception
  {
    final int id = 17;
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", null, null );

    final int lastChangeSetID = 1;
    final ChangeSet cs = new ChangeSet();
    cs.merge( new Change( message ) );
    final String encoded = JsonEncoder.encodeChangeSet( lastChangeSetID, null, null, cs );
    final JsonObject changeSet = toJsonObject( encoded );

    assertNotNull( changeSet );

    assertEquals( changeSet.getInt( TransportConstants.LAST_CHANGE_SET_ID ), lastChangeSetID );

    final JsonObject object = changeSet.getJsonArray( TransportConstants.CHANGES ).getJsonObject( 0 );

    assertEquals( object.getString( TransportConstants.ENTITY_ID ), "42.17" );

    assertFalse( object.containsKey( TransportConstants.DATA ) );
  }

  private JsonObject toJsonObject( final String encoded )
  {
    return Json.createReader( new StringReader( encoded ) ).readObject();
  }

  @Test
  public void action_WithNull()
    throws Exception
  {
    final ChangeSet cs = new ChangeSet();
    cs.mergeAction( new ChannelAction( new ChannelAddress( 45, null ), Action.ADD, null ) );
    final JsonObject changeSet = toJsonObject( JsonEncoder.encodeChangeSet( 1, null, null, cs ) );
    assertNotNull( changeSet );

    final JsonObject action = changeSet.getJsonArray( TransportConstants.CHANNEL_ACTIONS ).getJsonObject( 0 );
    assertEquals( action.getInt( TransportConstants.CHANNEL_ID ), 45 );
    assertFalse( action.containsKey( TransportConstants.SUBCHANNEL_ID ) );
    assertEquals( action.getString( TransportConstants.ACTION ), "add" );
    assertFalse( action.containsKey( TransportConstants.CHANNEL_FILTER ) );
  }

  @Test
  public void encodeLong()
    throws Exception
  {
    final int id = ValueUtil.randomInt();
    final int typeID = 42;
    final HashMap<String, Serializable> routingKeys = new HashMap<>();
    final HashMap<String, Serializable> attributeData = new HashMap<>();
    attributeData.put( "X", 1392061102056L );
    final EntityMessage message =
      new EntityMessage( id, typeID, 0, routingKeys, attributeData, null );
    final ChangeSet cs = new ChangeSet();
    cs.merge( new Change( message ) );

    final String encoded = JsonEncoder.encodeChangeSet( 0, null, null, cs );

    final String value =
      toJsonObject( encoded ).
        getJsonArray( TransportConstants.CHANGES ).
        getJsonObject( 0 ).
        getJsonObject( TransportConstants.DATA ).
        getString( "X" );
    assertNotNull( value );
    assertEquals( value, "1392061102056" );
  }
}
