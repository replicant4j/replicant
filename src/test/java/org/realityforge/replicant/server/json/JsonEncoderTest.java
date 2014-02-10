package org.realityforge.replicant.server.json;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
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
  public void encodeChangeSetFromEntityMessages_updateMessage()
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

    final ArrayList<EntityMessage> messages = new ArrayList<>();
    messages.add( message );
    final int lastChangeSetID = 1;
    final String requestID = "j1";
    final String etag = "#1";
    final String encoded = JsonEncoder.encodeChangeSetFromEntityMessages( lastChangeSetID, requestID, etag, messages );
    final JSONObject changeSet = new JSONObject( encoded );

    assertNotNull( changeSet );

    assertEquals( changeSet.getInt( TransportConstants.LAST_CHANGE_SET_ID ), lastChangeSetID );
    assertEquals( changeSet.getString( TransportConstants.REQUEST_ID ), requestID );
    assertEquals( changeSet.getString( TransportConstants.ETAG ), etag );

    final JSONObject object = changeSet.getJSONArray( TransportConstants.CHANGES ).getJSONObject( 0 );

    assertEquals( object.getString( TransportConstants.ENTITY_ID ), id );
    assertEquals( object.getInt( TransportConstants.TYPE_ID ), typeID );

    final JSONObject data = object.getJSONObject( TransportConstants.DATA );
    assertNotNull( data );
    assertEquals( data.getString( MessageTestUtil.ATTR_KEY1 ), "a1" );
    assertEquals( data.getString( MessageTestUtil.ATTR_KEY2 ), "a2" );
    assertTrue( data.getString( "key3" ).startsWith( "2001-07-05T05:08:56.000" ) );
  }

  @Test
  public void encodeChangeSetFromEntityMessages_deleteMessage()
    throws JSONException, ParseException
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", null, null );

    final ArrayList<EntityMessage> messages = new ArrayList<>();
    messages.add( message );
    final int lastChangeSetID = 1;
    final String encoded = JsonEncoder.encodeChangeSetFromEntityMessages( lastChangeSetID, null, null, messages );
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
  public void encodeLong()
    throws JSONException, ParseException
  {
    final String id = "myID";
    final int typeID = 42;
    final HashMap<String, Serializable> routingKeys = new HashMap<>();
    final HashMap<String, Serializable> attributeData = new HashMap<>();
    attributeData.put( "X", 1392061102056L );
    final EntityMessage message = new EntityMessage( id, typeID, 0, routingKeys, attributeData );
    final ArrayList<EntityMessage> messages = new ArrayList<>();
    messages.add( message );

    final String encoded = JsonEncoder.encodeChangeSetFromEntityMessages( 0, null, null, messages );

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
