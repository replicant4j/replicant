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
import javax.json.JsonValue;
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
    final var id = 17;
    final var typeID = 42;
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

    final var message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final Map<String, Serializable> values = message.getAttributeValues();
    assertNotNull( values );
    values.put( "key3", date );

    final var requestId = 1;
    final JsonValue response =
      Json.createArrayBuilder().add( 17 ).add( 42 ).build();

    final var etag = "#1";
    final var filter = Json.createBuilderFactory( null ).createObjectBuilder().add( "a", "b" ).build();

    final Change change = new Change( message );
    change.getChannels().add( new ChannelAddress( 1, null ) );
    change.getChannels().add( new ChannelAddress( 2, 42 ) );
    change.getChannels().add( new ChannelAddress( 3, 73 ) );
    final var cs = new ChangeSet();
    cs.merge( change );
    cs.mergeAction( new ChannelAction( new ChannelAddress( 45, 77 ), Action.UPDATE, filter ) );
    final var encoded = JsonEncoder.encodeChangeSet( requestId, response, etag, cs );
    final var changeSet = toJsonObject( encoded );

    assertNotNull( changeSet );

    assertEquals( changeSet.getInt( Messages.Common.REQUEST_ID ), requestId );
    final JsonArray jsonResponse = changeSet.getJsonArray( Messages.Update.RESPONSE );
    assertEquals( jsonResponse.size(), 2 );
    assertEquals( jsonResponse.getInt( 0 ), 17 );
    assertEquals( jsonResponse.getInt( 1 ), 42 );
    assertEquals( changeSet.getString( Messages.S2C_Common.ETAG ), etag );

    final var action = changeSet.getJsonArray( Messages.Update.FILTERED_CHANNEL_ACTIONS ).getJsonObject( 0 );
    assertEquals( action.getString( Messages.Common.CHANNEL ), "=45.77" );
    assertEquals( action.getJsonObject( Messages.Update.FILTER ).toString(), filter.toString() );

    final var object = changeSet.getJsonArray( Messages.Update.CHANGES ).getJsonObject( 0 );

    assertEquals( object.getString( Messages.Update.ENTITY_ID ), "42.17" );

    final var data = object.getJsonObject( Messages.Update.DATA );
    assertNotNull( data );
    assertEquals( data.getString( MessageTestUtil.ATTR_KEY1 ), "a1" );
    assertEquals( data.getString( MessageTestUtil.ATTR_KEY2 ), "a2" );
    assertTrue( data.getString( "key3" ).startsWith( "2001-07-05T05:08:56.000" ) );

    final JsonArray channels = object.getJsonArray( Messages.Update.CHANNELS );
    assertNotNull( channels );
    assertEquals( channels.size(), 3 );
    final var channel1 = channels.getString( 0 );
    assertNotNull( channel1 );
    final var channel2 = channels.getString( 1 );
    assertNotNull( channel2 );
    final var channel3 = channels.getString( 2 );
    assertNotNull( channel3 );

    assertEquals( channel1, "1" );
    assertEquals( channel2, "2.42" );
    assertEquals( channel3, "3.73" );
  }

  @Test
  public void encodeChangeSetFromEntityMessages_deleteMessage()
  {
    final var id = 17;
    final var typeID = 42;

    final var message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", null, null );

    final var cs = new ChangeSet();
    cs.merge( new Change( message ) );
    final var encoded = JsonEncoder.encodeChangeSet( null, null, null, cs );
    final var changeSet = toJsonObject( encoded );

    assertNotNull( changeSet );

    final var object = changeSet.getJsonArray( Messages.Update.CHANGES ).getJsonObject( 0 );

    assertEquals( object.getString( Messages.Update.ENTITY_ID ), "42.17" );

    assertFalse( object.containsKey( Messages.Update.DATA ) );
  }

  @Test
  public void encodeChangeSet_empty()
  {
    final var cs = new ChangeSet();
    final var changeSet = toJsonObject( JsonEncoder.encodeChangeSet( null, null, null, cs ) );

    assertNotNull( changeSet );
    assertEquals( changeSet.getString( Messages.Common.TYPE ), Messages.S2C_Type.UPDATE );
    assertFalse( changeSet.containsKey( Messages.Common.REQUEST_ID ) );
    assertFalse( changeSet.containsKey( Messages.Update.RESPONSE ) );
    assertFalse( changeSet.containsKey( Messages.S2C_Common.ETAG ) );
    assertFalse( changeSet.containsKey( Messages.Update.CHANNEL_ACTIONS ) );
    assertFalse( changeSet.containsKey( Messages.Update.FILTERED_CHANNEL_ACTIONS ) );
    assertFalse( changeSet.containsKey( Messages.Update.CHANGES ) );
  }

  private JsonObject toJsonObject( final String encoded )
  {
    return Json.createReader( new StringReader( encoded ) ).readObject();
  }

  @Test
  public void action_WithNoFilter()
  {
    final var cs = new ChangeSet();
    cs.mergeAction( new ChannelAction( new ChannelAddress( 45, null ), Action.ADD, null ) );
    final var changeSet = toJsonObject( JsonEncoder.encodeChangeSet( null, null, null, cs ) );
    assertNotNull( changeSet );

    assertEquals( changeSet.getJsonArray( Messages.Update.CHANNEL_ACTIONS ).getString( 0 ), "+45" );
  }

  @Test
  public void channelAction_DELETE()
  {
    final var cs = new ChangeSet();
    cs.mergeAction( new ChannelAction( new ChannelAddress( 45, null ), Action.DELETE, null ) );
    final var changeSet = toJsonObject( JsonEncoder.encodeChangeSet( null, null, null, cs ) );
    assertNotNull( changeSet );

    assertEquals( changeSet.getJsonArray( Messages.Update.CHANNEL_ACTIONS ).getString( 0 ), "!45" );
  }

  @Test
  public void mixedChannelActions()
  {
    final var cs = new ChangeSet();
    cs.mergeAction( new ChannelAction( new ChannelAddress( 1, null ), Action.ADD, null ) );
    cs.mergeAction( new ChannelAction( new ChannelAddress( 2, 5 ), Action.REMOVE, null ) );
    cs.mergeAction( new ChannelAction( new ChannelAddress( 3, 7, "inst" ), Action.UPDATE, null ) );

    final var filter = Json.createObjectBuilder().add( "a", "b" ).build();
    cs.mergeAction( new ChannelAction( new ChannelAddress( 4, 9 ), Action.ADD, filter ) );

    final var changeSet = toJsonObject( JsonEncoder.encodeChangeSet( null, null, null, cs ) );
    assertNotNull( changeSet );

    final JsonArray actions = changeSet.getJsonArray( Messages.Update.CHANNEL_ACTIONS );
    assertEquals( actions.size(), 3 );
    assertEquals( actions.getString( 0 ), "+1" );
    assertEquals( actions.getString( 1 ), "-2.5" );
    assertEquals( actions.getString( 2 ), "=3.7#inst" );

    final var filteredAction =
      changeSet.getJsonArray( Messages.Update.FILTERED_CHANNEL_ACTIONS ).getJsonObject( 0 );
    assertEquals( filteredAction.getString( Messages.Common.CHANNEL ), "+4.9" );
    assertEquals( filteredAction.getJsonObject( Messages.Update.FILTER ).toString(), filter.toString() );
  }

  @Test
  public void encodeChangeSet_dataTypes()
  {
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

    final var routingKeys = new HashMap<String, Serializable>();
    final var attributeData = new HashMap<String, Serializable>();
    attributeData.put( "s", "text" );
    attributeData.put( "i", 12 );
    attributeData.put( "f", 1.5f );
    attributeData.put( "b", true );
    attributeData.put( "d", date );
    attributeData.put( "n", null );

    final var message = new EntityMessage( 1, 2, 0, routingKeys, attributeData, null );
    final var cs = new ChangeSet();
    cs.merge( new Change( message ) );

    final var changeSet = toJsonObject( JsonEncoder.encodeChangeSet( null, null, null, cs ) );
    final var change = changeSet.getJsonArray( Messages.Update.CHANGES ).getJsonObject( 0 );
    final var data = change.getJsonObject( Messages.Update.DATA );

    assertEquals( data.getString( "s" ), "text" );
    assertEquals( data.getInt( "i" ), 12 );
    assertEquals( data.getJsonNumber( "f" ).doubleValue(), 1.5, 0.0001 );
    assertTrue( data.getBoolean( "b" ) );
    assertTrue( data.getString( "d" ).startsWith( "2001-07-05T05:08:56.000" ) );
    assertFalse( data.containsKey( "n" ) );
    assertFalse( change.containsKey( Messages.Update.CHANNELS ) );
  }

  @Test
  public void encodeChangeSet_channelDescriptors_includeFilterInstanceId()
  {
    final var routingKeys = new HashMap<String, Serializable>();
    final var attributeData = new HashMap<String, Serializable>();
    attributeData.put( "x", "y" );
    final var message = new EntityMessage( 1, 2, 0, routingKeys, attributeData, null );
    final Change change = new Change( message );
    change.getChannels().add( new ChannelAddress( 7, null, "fi" ) );
    change.getChannels().add( new ChannelAddress( 8, 3, "fi-2" ) );
    final var cs = new ChangeSet();
    cs.merge( change );

    final var changeSet = toJsonObject( JsonEncoder.encodeChangeSet( null, null, null, cs ) );
    final JsonArray channels =
      changeSet.getJsonArray( Messages.Update.CHANGES ).getJsonObject( 0 ).getJsonArray( Messages.Update.CHANNELS );

    assertEquals( channels.size(), 2 );
    assertEquals( channels.getString( 0 ), "7#fi" );
    assertEquals( channels.getString( 1 ), "8.3#fi-2" );
  }

  @Test
  public void encodeChangeSet_rejectsUnsupportedValues()
  {
    final var routingKeys = new HashMap<String, Serializable>();
    final var attributeData = new HashMap<String, Serializable>();
    attributeData.put( "bad", (byte) 1 );
    final var message = new EntityMessage( 1, 2, 0, routingKeys, attributeData, null );
    final var cs = new ChangeSet();
    cs.merge( new Change( message ) );

    final var exception =
      expectThrows( IllegalStateException.class, () -> JsonEncoder.encodeChangeSet( null, null, null, cs ) );
    assertTrue( exception.getMessage().startsWith( "Unable to encode:" ) );
  }

  @Test
  public void encodeLong()
  {
    final var id = ValueUtil.randomInt();
    final var typeID = 42;
    final var routingKeys = new HashMap<String, Serializable>();
    final var attributeData = new HashMap<String, Serializable>();
    attributeData.put( "X", 1392061102056L );
    final var message =
      new EntityMessage( id, typeID, 0, routingKeys, attributeData, null );
    final var cs = new ChangeSet();
    cs.merge( new Change( message ) );

    final var encoded = JsonEncoder.encodeChangeSet( null, null, null, cs );

    final var value =
      toJsonObject( encoded ).
        getJsonArray( Messages.Update.CHANGES ).
        getJsonObject( 0 ).
        getJsonObject( Messages.Update.DATA ).
        getString( "X" );
    assertNotNull( value );
    assertEquals( value, "1392061102056" );
  }

  @Test
  public void encodeUseCacheMessage()
  {
    final var address = new ChannelAddress( 1, 2, "inst" );
    final var message = toJsonObject( JsonEncoder.encodeUseCacheMessage( address, "e1", 7 ) );

    assertEquals( message.getString( Messages.Common.TYPE ), Messages.S2C_Type.USE_CACHE );
    assertEquals( message.getString( Messages.Common.CHANNEL ), "1.2#inst" );
    assertEquals( message.getString( Messages.S2C_Common.ETAG ), "e1" );
    assertEquals( message.getInt( Messages.Common.REQUEST_ID ), 7 );
  }

  @Test
  public void encodeUseCacheMessage_withoutRequestId()
  {
    final var address = new ChannelAddress( 1, 2 );
    final var message = toJsonObject( JsonEncoder.encodeUseCacheMessage( address, "e1", null ) );

    assertEquals( message.getString( Messages.Common.TYPE ), Messages.S2C_Type.USE_CACHE );
    assertEquals( message.getString( Messages.Common.CHANNEL ), "1.2" );
    assertEquals( message.getString( Messages.S2C_Common.ETAG ), "e1" );
    assertFalse( message.containsKey( Messages.Common.REQUEST_ID ) );
  }

  @Test
  public void encodeSessionCreatedMessage()
  {
    final var message = toJsonObject( JsonEncoder.encodeSessionCreatedMessage( "sid-1" ) );

    assertEquals( message.getString( Messages.Common.TYPE ), Messages.S2C_Type.SESSION_CREATED );
    assertEquals( message.getString( Messages.S2C_Common.SESSION_ID ), "sid-1" );
  }

  @Test
  public void encodeOkMessage()
  {
    final var message = toJsonObject( JsonEncoder.encodeOkMessage( 4 ) );

    assertEquals( message.getString( Messages.Common.TYPE ), Messages.S2C_Type.OK );
    assertEquals( message.getInt( Messages.Common.REQUEST_ID ), 4 );
  }

  @Test
  public void encodeMalformedMessageMessage()
  {
    final var message = toJsonObject( JsonEncoder.encodeMalformedMessageMessage( "bad" ) );

    assertEquals( message.getString( Messages.Common.TYPE ), Messages.S2C_Type.MALFORMED_MESSAGE );
    assertEquals( message.getString( Messages.S2C_Common.MESSAGE ), "bad" );
  }

  @Test
  public void encodeUnknownRequestType()
  {
    final var command = Json.createObjectBuilder().add( "t", "x" ).build();
    final var message = toJsonObject( JsonEncoder.encodeUnknownRequestType( command ) );

    assertEquals( message.getString( Messages.Common.TYPE ), Messages.S2C_Type.UNKNOWN_REQUEST_TYPE );
    assertEquals( message.getJsonObject( Messages.Common.COMMAND ).toString(), command.toString() );
  }

  @Test
  public void encodeErrorMessage()
  {
    final var message = toJsonObject( JsonEncoder.encodeErrorMessage( "oops" ) );

    assertEquals( message.getString( Messages.Common.TYPE ), Messages.S2C_Type.ERROR );
    assertEquals( message.getString( Messages.S2C_Common.MESSAGE ), "oops" );
  }
}
