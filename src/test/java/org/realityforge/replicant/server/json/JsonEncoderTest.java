package org.realityforge.replicant.server.json;

import com.jayway.jsonpath.JsonPath;
import java.text.ParseException;
import java.util.ArrayList;
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

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, "r1", "r2", "a1", "a2" );

    final ArrayList<EntityMessage> messages = new ArrayList<EntityMessage>();
    messages.add( message );
    final int lastChangeSetID = 1;
    final JSONObject changeSet = JsonEncoder.encodeChangeSetFromEntityMessages( lastChangeSetID, messages );

    assertNotNull( changeSet );

    assertEquals( changeSet.getInt( TransportConstants.LAST_CHANGE_SET_ID ), lastChangeSetID );

    final JSONObject object = changeSet.getJSONArray( TransportConstants.CHANGES ).getJSONObject( 0 );

    assertEquals( object.getString( TransportConstants.ENTITY_ID ), id );
    assertEquals( object.getInt( TransportConstants.TYPE_ID ), typeID );

    final JSONObject data = object.getJSONObject( TransportConstants.DATA );
    assertNotNull( data );
    assertEquals( data.getString( MessageTestUtil.ATTR_KEY1 ), "a1" );
    assertEquals( data.getString( MessageTestUtil.ATTR_KEY2 ), "a2" );
  }

  @Test
  public void encodeChangeSetFromEncodedStrings_updateMessage()
    throws JSONException, ParseException
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, "r1", "r2", "a1", "a2" );

    final ArrayList<String> messages = new ArrayList<String>();
    messages.add( JsonEncoder.encodeEntityMessageAsString( message ) );
    final int lastChangeSetID = 1;
    final JSONObject changeSet = JsonEncoder.encodeChangeSetFromEncodedStrings( lastChangeSetID, messages );

    assertNotNull( changeSet );

    assertEquals( changeSet.getInt( TransportConstants.LAST_CHANGE_SET_ID ), lastChangeSetID );

    final JSONObject object = changeSet.getJSONArray( TransportConstants.CHANGES ).getJSONObject( 0 );

    assertEquals( object.getString( TransportConstants.ENTITY_ID ), id );
    assertEquals( object.getInt( TransportConstants.TYPE_ID ), typeID );

    final JSONObject data = object.getJSONObject( TransportConstants.DATA );
    assertNotNull( data );
    assertEquals( data.getString( MessageTestUtil.ATTR_KEY1 ), "a1" );
    assertEquals( data.getString( MessageTestUtil.ATTR_KEY2 ), "a2" );
  }


  @Test
  public void encodeEntityMessageAsString_updateMessage()
    throws JSONException, ParseException
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, "r1", "r2", "a1", "a2" );

    final String result = JsonEncoder.encodeEntityMessageAsString( message );
    assertNotNull( result );
    assertEquals( id, JsonPath.read( result, "$." + TransportConstants.ENTITY_ID ) );
    assertEquals( typeID, JsonPath.read( result, "$." + TransportConstants.TYPE_ID ) );
    assertEquals( "a1", JsonPath.read( result, "$." + TransportConstants.DATA + "." + MessageTestUtil.ATTR_KEY1 ) );
    assertEquals( "a2", JsonPath.read( result, "$." + TransportConstants.DATA + "." + MessageTestUtil.ATTR_KEY2 ) );
  }

  @Test
  public void encodeEntityMessageAsString_deleteMessage()
    throws JSONException, ParseException
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, "r1", "r2", null, null );

    final String result = JsonEncoder.encodeEntityMessageAsString( message );
    assertNotNull( result );
    assertEquals( id, JsonPath.read( result, "$." + TransportConstants.ENTITY_ID ) );
    assertEquals( typeID, JsonPath.read( result, "$." + TransportConstants.TYPE_ID ) );
    assertNull( JsonPath.read( result, "$." + TransportConstants.DATA ) );
  }

  @Test
  public void encodeEntityMessage_updateMessage()
    throws JSONException
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, "r1", "r2", "a1", "a2" );

    final JSONObject object = JsonEncoder.encodeEntityMessage( message );

    assertEquals( object.getString( TransportConstants.ENTITY_ID ), id );
    assertEquals( object.getInt( TransportConstants.TYPE_ID ), typeID );

    final JSONObject data = object.getJSONObject( TransportConstants.DATA );
    assertNotNull( data );
    assertEquals( data.getString( MessageTestUtil.ATTR_KEY1 ), "a1" );
    assertEquals( data.getString( MessageTestUtil.ATTR_KEY2 ), "a2" );
  }

  @Test
  public void encodeEntityMessage_deleteMessage()
    throws JSONException
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, "r1", "r2", null, null );

    final JSONObject object = JsonEncoder.encodeEntityMessage( message );

    assertEquals( object.getString( TransportConstants.ENTITY_ID ), id );
    assertEquals( object.getInt( TransportConstants.TYPE_ID ), typeID );

    assertFalse( object.has( TransportConstants.DATA ) );
  }
}
