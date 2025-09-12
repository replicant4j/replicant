package replicant.server.json;

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
import org.testng.annotations.Test;
import replicant.server.Change;
import replicant.server.ChangeSet;
import replicant.server.ChannelAction;
import replicant.server.ChannelAction.Action;
import replicant.server.ChannelAddress;
import replicant.server.EntityMessage;
import replicant.server.MessageTestUtil;
import replicant.shared.Messages;
import static org.testng.Assert.*;

/**
 * Utility class used when encoding EntityMessage into JSON payload.
 */
public final class JsonEncoderTest
{
  @Test
  public void encodeAllData()
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

    final int requestId = 1;
    final String response = "[17, 42]";
    final String etag = "#1";
    final JsonObject filter = Json.createBuilderFactory( null ).createObjectBuilder().add( "a", "b" ).build();

    final Change change = new Change( message );
    change.getChannels().put( 1, null );
    change.getChannels().put( 2, 42 );
    change.getChannels().put( 3, 73 );
    final ChangeSet cs = new ChangeSet();
    cs.merge( change );
    cs.mergeAction( new ChannelAction( new ChannelAddress( 45, 77 ), Action.UPDATE, filter ) );
    final String encoded = JsonEncoder.encodeChangeSet( requestId, response, etag, cs );
    final JsonObject changeSet = toJsonObject( encoded );

    assertNotNull( changeSet );

    assertEquals( changeSet.getInt( Messages.Common.REQUEST_ID ), requestId );
    final JsonArray jsonResponse = changeSet.getJsonArray( Messages.Update.RESPONSE );
    assertEquals( jsonResponse.size(), 2 );
    assertEquals( jsonResponse.getInt( 0 ), 17 );
    assertEquals( jsonResponse.getInt( 1 ), 42 );
    assertEquals( changeSet.getString( Messages.S2C_Common.ETAG ), etag );

    final JsonObject action = changeSet.getJsonArray( Messages.Update.FILTERED_CHANNEL_ACTIONS ).getJsonObject( 0 );
    assertEquals( action.getString( Messages.Common.CHANNEL ), "=45.77" );
    assertEquals( action.getJsonObject( Messages.Update.FILTER ).toString(), filter.toString() );

    final JsonObject object = changeSet.getJsonArray( Messages.Update.CHANGES ).getJsonObject( 0 );

    assertEquals( object.getString( Messages.Update.ENTITY_ID ), "42.17" );

    final JsonObject data = object.getJsonObject( Messages.Update.DATA );
    assertNotNull( data );
    assertEquals( data.getString( MessageTestUtil.ATTR_KEY1 ), "a1" );
    assertEquals( data.getString( MessageTestUtil.ATTR_KEY2 ), "a2" );
    assertTrue( data.getString( "key3" ).startsWith( "2001-07-05T05:08:56.000" ) );

    final JsonArray channels = object.getJsonArray( Messages.Update.CHANNELS );
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
  {
    final int id = 17;
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", null, null );

    final ChangeSet cs = new ChangeSet();
    cs.merge( new Change( message ) );
    final String encoded = JsonEncoder.encodeChangeSet( null, null, null, cs );
    final JsonObject changeSet = toJsonObject( encoded );

    assertNotNull( changeSet );

    final JsonObject object = changeSet.getJsonArray( Messages.Update.CHANGES ).getJsonObject( 0 );

    assertEquals( object.getString( Messages.Update.ENTITY_ID ), "42.17" );

    assertFalse( object.containsKey( Messages.Update.DATA ) );
  }

  private JsonObject toJsonObject( final String encoded )
  {
    return Json.createReader( new StringReader( encoded ) ).readObject();
  }

  @Test
  public void action_WithNoFilter()
  {
    final ChangeSet cs = new ChangeSet();
    cs.mergeAction( new ChannelAction( new ChannelAddress( 45, null ), Action.ADD, null ) );
    final JsonObject changeSet = toJsonObject( JsonEncoder.encodeChangeSet( null, null, null, cs ) );
    assertNotNull( changeSet );

    assertEquals( changeSet.getJsonArray( Messages.Update.CHANNEL_ACTIONS ).getString( 0 ), "+45" );
  }

  @Test
  public void channelAction_DELETE()
  {
    final ChangeSet cs = new ChangeSet();
    cs.mergeAction( new ChannelAction( new ChannelAddress( 45, null ), Action.DELETE, null ) );
    final JsonObject changeSet = toJsonObject( JsonEncoder.encodeChangeSet( null, null, null, cs ) );
    assertNotNull( changeSet );

    assertEquals( changeSet.getJsonArray( Messages.Update.CHANNEL_ACTIONS ).getString( 0 ), "!45" );
  }

  @Test
  public void encodeLong()
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

    final String encoded = JsonEncoder.encodeChangeSet( null, null, null, cs );

    final String value =
      toJsonObject( encoded ).
        getJsonArray( Messages.Update.CHANGES ).
        getJsonObject( 0 ).
        getJsonObject( Messages.Update.DATA ).
        getString( "X" );
    assertNotNull( value );
    assertEquals( value, "1392061102056" );
  }
}
