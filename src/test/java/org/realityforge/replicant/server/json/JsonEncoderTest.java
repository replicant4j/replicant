package org.realityforge.replicant.server.json;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.realityforge.replicant.server.Change;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelAction;
import org.realityforge.replicant.server.ChannelAction.Action;
import org.realityforge.replicant.server.ChannelDescriptor;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.MessageTestUtil;
import org.realityforge.replicant.shared.json.TransportConstants;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Utility class used when encoding EntityMessage into JSON payload.
 */
public final class JsonEncoderTest
{
  @Test
  public void encodeAllData()
    throws JSONException, ParseException
  {
    final String id = "myID";
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
    final String requestID = "j1";
    final String etag = "#1";
    final JsonObject filter = Json.createBuilderFactory( null ).createObjectBuilder().add( "a", "b" ).build();

    final Change change = new Change( message );
    change.getChannels().put( 1, 0 );
    change.getChannels().put( 2, 42 );
    change.getChannels().put( 3, "Blah" );
    final ChangeSet cs = new ChangeSet();
    cs.merge( change );
    cs.addAction( new ChannelAction( new ChannelDescriptor( 45, "X" ), Action.UPDATE, filter ) );
    final String encoded = JsonEncoder.encodeChangeSet( lastChangeSetID, requestID, etag, cs );
    final JSONObject changeSet = new JSONObject( encoded );

    assertNotNull( changeSet );

    assertEquals( changeSet.getInt( TransportConstants.LAST_CHANGE_SET_ID ), lastChangeSetID );
    assertEquals( changeSet.getString( TransportConstants.REQUEST_ID ), requestID );
    assertEquals( changeSet.getString( TransportConstants.ETAG ), etag );

    final JSONObject action = changeSet.getJSONArray( TransportConstants.CHANNEL_ACTIONS ).getJSONObject( 0 );
    assertEquals( action.getInt( TransportConstants.CHANNEL_ID ), 45 );
    assertEquals( action.getString( TransportConstants.SUBCHANNEL_ID ), "X" );
    assertEquals( action.getString( TransportConstants.ACTION ), "update" );
    assertEquals( action.getJSONObject( TransportConstants.CHANNEL_FILTER ).toString(), filter.toString() );

    final JSONObject object = changeSet.getJSONArray( TransportConstants.CHANGES ).getJSONObject( 0 );

    assertEquals( object.getString( TransportConstants.ENTITY_ID ), id );
    assertEquals( object.getInt( TransportConstants.TYPE_ID ), typeID );

    final JSONObject data = object.getJSONObject( TransportConstants.DATA );
    assertNotNull( data );
    assertEquals( data.getString( MessageTestUtil.ATTR_KEY1 ), "a1" );
    assertEquals( data.getString( MessageTestUtil.ATTR_KEY2 ), "a2" );
    assertTrue( data.getString( "key3" ).startsWith( "2001-07-05T05:08:56.000" ) );

    final JSONArray channels = object.getJSONArray( TransportConstants.CHANNELS );
    assertNotNull( channels );
    assertEquals( channels.length(), 3 );
    final JSONObject channel1 = channels.getJSONObject( 0 );
    assertNotNull( channel1 );
    final JSONObject channel2 = channels.getJSONObject( 1 );
    assertNotNull( channel2 );
    final JSONObject channel3 = channels.getJSONObject( 2 );
    assertNotNull( channel3 );

    assertEquals( channel1.getInt( TransportConstants.CHANNEL_ID ), 1 );
    assertEquals( channel2.getInt( TransportConstants.CHANNEL_ID ), 2 );
    assertEquals( channel3.getInt( TransportConstants.CHANNEL_ID ), 3 );

    assertNull( channel1.opt( TransportConstants.SUBCHANNEL_ID ) );
    assertEquals( channel2.getInt( TransportConstants.SUBCHANNEL_ID ), 42 );
    assertEquals( channel3.getString( TransportConstants.SUBCHANNEL_ID ), "Blah" );
  }

  @Test
  public void encodeChangeSetFromEntityMessages_deleteMessage()
    throws JSONException, ParseException
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", null, null );

    final int lastChangeSetID = 1;
    final ChangeSet cs = new ChangeSet();
    cs.merge( new Change( message ) );
    final String encoded = JsonEncoder.encodeChangeSet( lastChangeSetID, null, null, cs );
    final JSONObject changeSet = new JSONObject( encoded );

    assertNotNull( changeSet );

    assertEquals( changeSet.getInt( TransportConstants.LAST_CHANGE_SET_ID ), lastChangeSetID );

    final JSONObject object = changeSet.getJSONArray( TransportConstants.CHANGES ).getJSONObject( 0 );

    assertEquals( object.getString( TransportConstants.ENTITY_ID ), id );
    assertEquals( object.getInt( TransportConstants.TYPE_ID ), typeID );

    final JSONObject data = object.optJSONObject( TransportConstants.DATA );
    assertNull( data );
  }

@Test
  public void action_WithNull()
    throws JSONException, ParseException
  {
    final ChangeSet cs = new ChangeSet();
    cs.addAction( new ChannelAction( new ChannelDescriptor( 45, null ), Action.ADD, null ) );
    final JSONObject changeSet = new JSONObject( JsonEncoder.encodeChangeSet( 1, null, null, cs ) );
    assertNotNull( changeSet );

    final JSONObject action = changeSet.getJSONArray( TransportConstants.CHANNEL_ACTIONS ).getJSONObject( 0 );
    assertEquals( action.getInt( TransportConstants.CHANNEL_ID ), 45 );
    assertNull( action.opt( TransportConstants.SUBCHANNEL_ID ) );
    assertEquals( action.getString( TransportConstants.ACTION ), "add" );
    assertNull( action.optJSONObject( TransportConstants.CHANNEL_FILTER ) );
  }

  @Test
  public void encodeLong()
    throws JSONException, ParseException
  {
    final String id = "myID";
    final int typeID = 42;
    final HashMap<String, Serializable> routingKeys = new HashMap<>();
    final HashMap<String, Serializable> attributeData = new HashMap<>();
    attributeData.put( "X", 1392061102056L );
    final EntityMessage message = new EntityMessage( id, typeID, 0, routingKeys, attributeData, null );
    final ChangeSet cs = new ChangeSet();
    cs.merge( new Change( message ) );

    final String encoded = JsonEncoder.encodeChangeSet( 0, null, null, cs );

    final String value =
      new JSONObject( encoded ).
        getJSONArray( TransportConstants.CHANGES ).
        getJSONObject( 0 ).
        getJSONObject( TransportConstants.DATA ).
        getString( "X" );
    assertNotNull( value );
    assertEquals( value, "1392061102056" );
  }
}
