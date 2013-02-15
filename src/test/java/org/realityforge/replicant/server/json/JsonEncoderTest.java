package org.realityforge.replicant.server.json;

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

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );

    final ArrayList<EntityMessage> messages = new ArrayList<EntityMessage>();
    messages.add( message );
    final int lastChangeSetID = 1;
    final String encoded = JsonEncoder.encodeChangeSetFromEntityMessages( lastChangeSetID, messages );
    final JSONObject changeSet = new JSONObject( encoded );

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
  public void encodeChangeSetFromEntityMessages_deleteMessage()
    throws JSONException, ParseException
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", null, null );

    final ArrayList<EntityMessage> messages = new ArrayList<EntityMessage>();
    messages.add( message );
    final int lastChangeSetID = 1;
    final String encoded = JsonEncoder.encodeChangeSetFromEntityMessages( lastChangeSetID, messages );
    final JSONObject changeSet = new JSONObject( encoded );

    assertNotNull( changeSet );

    assertEquals( changeSet.getInt( TransportConstants.LAST_CHANGE_SET_ID ), lastChangeSetID );

    final JSONObject object = changeSet.getJSONArray( TransportConstants.CHANGES ).getJSONObject( 0 );

    assertEquals( object.getString( TransportConstants.ENTITY_ID ), id );
    assertEquals( object.getInt( TransportConstants.TYPE_ID ), typeID );

    final JSONObject data = object.optJSONObject( TransportConstants.DATA );
    assertNull( data );
  }
}
