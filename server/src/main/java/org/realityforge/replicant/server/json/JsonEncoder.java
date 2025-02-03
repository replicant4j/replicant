package org.realityforge.replicant.server.json;

import java.io.Serializable;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import org.realityforge.replicant.server.Change;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelAction;
import org.realityforge.replicant.server.ChannelAction.Action;
import org.realityforge.replicant.server.ChannelAddress;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.ee.JsonUtil;
import org.realityforge.replicant.shared.SharedConstants;

/**
 * Utility class used when encoding EntityMessage into JSON payload.
 */
public final class JsonEncoder
{
  @Nonnull
  static final String CHANNEL_FILTER = "filter";
  @Nonnull
  static final String CHANNELS = "channels";
  @Nonnull
  public static final String CHANNEL = "channel";
  @Nonnull
  static final String FILTERED_CHANNEL_ACTIONS = "fchannels";
  @Nonnull
  static final String CHANNEL_ACTIONS = "channels";
  @Nonnull
  static final String DATA = "data";
  @Nonnull
  static final String ENTITY_ID = "id";
  @Nonnull
  static final String CHANGES = "changes";
  @Nonnull
  public static final String ETAG = "etag";
  @Nonnull
  public static final String ETAGS = "etags";
  @Nonnull
  public static final String REQUEST_ID = "requestId";
  @Nonnull
  public static final String RESPONSE = "response";
  @Nonnull
  public static final String TYPE = "type";
  @Nonnull
  public static final String SESSION_ID = "sessionId";
  @Nonnull
  public static final String MESSAGE = "message";
  @Nonnull
  public static final String COMMAND = "command";
  @Nonnull
  public static final String PAYLOAD = "payload";

  /**
   * Types of Client to Server messages.
   */
  public static final class C2S_Type
  {
    @Nonnull
    public static final String AUTH = "auth";
    @Nonnull
    public static final String ETAGS = "etags";
    @Nonnull
    public static final String PING = "ping";
    @Nonnull
    public static final String SUB = "sub";
    @Nonnull
    public static final String UNSUB = "unsub";
    @Nonnull
    public static final String BULK_SUB = "bulk-sub";
    @Nonnull
    public static final String BULK_UNSUB = "bulk-unsub";
    @Nonnull
    public static final String EXEC = "exec";

    private C2S_Type()
    {
    }
  }

  /**
   * Types of Server to Client messages.
   */
  public static final class S2C_Type
  {
    @Nonnull
    public static final String UPDATE = "update";
    @Nonnull
    public static final String USE_CACHE = "use-cache";
    @Nonnull
    public static final String SESSION_CREATED = "session-created";
    @Nonnull
    public static final String OK = "ok";
    @Nonnull
    public static final String MALFORMED_MESSAGE = "malformed-message";
    @Nonnull
    public static final String UNKNOWN_REQUEST_TYPE = "unknown-request-type";
    @Nonnull
    public static final String ERROR = "error";

    private S2C_Type()
    {
    }
  }

  // Use constant to avoid slow filesystem access when serializing a message.
  @Nonnull
  private static final JsonGeneratorFactory FACTORY = Json.createGeneratorFactory( null );

  private JsonEncoder()
  {
  }

  /**
   * Encode the change set with the EntityMessages.
   *
   * @param requestId the requestId that initiated the change. Only set if packet is destined for originating session.
   * @param response  the response message if the packet is the result of a request that has a response,
   *                  and the request was initiated by the session.
   * @param etag      the associated etag.
   * @param changeSet the changeSet being encoded.
   * @return the encoded change set.
   */
  @Nonnull
  public static String encodeChangeSet( @Nullable final Integer requestId,
                                        @Nullable final String response,
                                        @Nullable final String etag,
                                        @Nonnull final ChangeSet changeSet )
  {
    final StringWriter writer = new StringWriter();
    final JsonGenerator generator = FACTORY.createGenerator( writer );
    final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );

    generator.writeStartObject();
    generator.write( TYPE, S2C_Type.UPDATE );
    if ( null != requestId )
    {
      generator.write( REQUEST_ID, requestId );
    }
    if ( null != response )
    {
      generator.write( RESPONSE, JsonUtil.toJsonValue( response ) );
    }
    if ( null != etag )
    {
      generator.write( ETAG, etag );
    }

    final List<ChannelAction> actions =
      changeSet.getChannelActions().stream().filter( c -> null == c.getFilter() ).toList();
    if ( !actions.isEmpty() )
    {
      generator.writeStartArray( CHANNEL_ACTIONS );
      actions.stream().map( JsonEncoder::toDescriptor ).forEach( generator::write );
      generator.writeEnd();
    }

    final List<ChannelAction> filteredActions =
      changeSet.getChannelActions().stream().filter( c -> null != c.getFilter() ).toList();
    if ( !filteredActions.isEmpty() )
    {
      generator.writeStartArray( FILTERED_CHANNEL_ACTIONS );
      filteredActions.forEach( a -> {
        generator.writeStartObject();
        generator.write( CHANNEL, toDescriptor( a ) );
        generator.write( CHANNEL_FILTER, a.getFilter() );
        generator.writeEnd();
      } );
      generator.writeEnd();
    }

    final Collection<Change> changes = changeSet.getChanges();
    if ( !changes.isEmpty() )
    {
      generator.writeStartArray( CHANGES );

      for ( final Change change : changes )
      {
        final EntityMessage entityMessage = change.getEntityMessage();

        generator.writeStartObject();
        generator.write( ENTITY_ID, entityMessage.getTypeId() + "." + entityMessage.getId() );

        final Map<Integer, Integer> channels = change.getChannels();
        if ( !channels.isEmpty() )
        {
          generator.writeStartArray( CHANNELS );
          for ( final Entry<Integer, Integer> entry : channels.entrySet() )
          {
            final Integer cid = entry.getKey();
            final Integer scid = entry.getValue();
            generator.write( cid + ( null == scid ? "" : "." + scid ) );
          }
          generator.writeEnd();
        }

        if ( entityMessage.isUpdate() )
        {
          generator.writeStartObject( DATA );
          final Map<String, Serializable> values = entityMessage.getAttributeValues();
          assert null != values;
          for ( final Entry<String, Serializable> entry : values.entrySet() )
          {
            writeField( generator, entry.getKey(), entry.getValue(), dateFormat );
          }
          generator.writeEnd();
        }
        generator.writeEnd();
      }
      generator.writeEnd();
    }
    generator.writeEnd();
    generator.close();
    return writer.toString();
  }

  @Nonnull
  private static String toDescriptor( @Nonnull final ChannelAction channelAction )
  {
    final Action action = channelAction.getAction();
    final char actionValue =
      Action.ADD == action ? SharedConstants.CHANNEL_ACTION_ADD :
      Action.REMOVE == action ? SharedConstants.CHANNEL_ACTION_REMOVE :
      Action.UPDATE == action ? SharedConstants.CHANNEL_ACTION_UPDATE :
      SharedConstants.CHANNEL_ACTION_DELETE;

    final ChannelAddress address = channelAction.getAddress();

    final Integer scid = address.getSubChannelId();
    return String.valueOf( actionValue ) + address.getChannelId() + ( null == scid ? "" : "." + scid );
  }

  private static void writeField( final JsonGenerator generator,
                                  final String key,
                                  final Serializable serializable,
                                  final SimpleDateFormat dateFormat )
  {
    if ( serializable instanceof String )
    {
      generator.write( key, (String) serializable );
    }
    else if ( serializable instanceof Integer )
    {
      generator.write( key, (Integer) serializable );
    }
    else if ( serializable instanceof Long )
    {
      generator.write( key, new BigDecimal( (Long) serializable ).toString() );
    }
    else if ( null == serializable )
    {
      generator.writeNull( key );
    }
    else if ( serializable instanceof Float )
    {
      generator.write( key, (Float) serializable );
    }
    else if ( serializable instanceof Date )
    {
      generator.write( key, dateFormat.format( (Date) serializable ) );
    }
    else if ( serializable instanceof Boolean )
    {
      generator.write( key, (Boolean) serializable );
    }
    else
    {
      throw new IllegalStateException( "Unable to encode: " + serializable );
    }
  }

  @Nonnull
  public static String encodeUseCacheMessage( @Nonnull final ChannelAddress address,
                                              @Nonnull final String eTag,
                                              @Nullable final Integer requestId )
  {
    final JsonObjectBuilder response =
      Json
        .createObjectBuilder()
        .add( TYPE, S2C_Type.USE_CACHE )
        .add( CHANNEL, address.toString() )
        .add( ETAG, eTag );
    if ( null != requestId )
    {
      response.add( REQUEST_ID, requestId );
    }
    return asString( response.build() );
  }

  @Nonnull
  public static String encodeSessionCreatedMessage( @Nonnull final String sessionId )
  {
    return asString( Json
                       .createObjectBuilder()
                       .add( TYPE, S2C_Type.SESSION_CREATED )
                       .add( SESSION_ID, sessionId )
                       .build() );
  }

  @Nonnull
  public static String encodeOkMessage( final int requestId )
  {
    return asString( Json
                       .createObjectBuilder()
                       .add( TYPE, S2C_Type.OK )
                       .add( REQUEST_ID, requestId )
                       .build() );
  }

  @Nonnull
  public static String encodeMalformedMessageMessage( @Nonnull final String message )
  {
    return asString( Json
                       .createObjectBuilder()
                       .add( TYPE, S2C_Type.MALFORMED_MESSAGE )
                       .add( MESSAGE, message )
                       .build() );
  }

  @Nonnull
  public static String encodeUnknownRequestType( @Nonnull final JsonObject command )
  {
    return asString( Json
                       .createObjectBuilder()
                       .add( TYPE, S2C_Type.UNKNOWN_REQUEST_TYPE )
                       .add( COMMAND, command )
                       .build() );
  }

  @Nonnull
  public static String encodeErrorMessage( @Nonnull final String message )
  {
    return asString( Json
                       .createObjectBuilder()
                       .add( TYPE, S2C_Type.ERROR )
                       .add( MESSAGE, message )
                       .build() );
  }

  @Nonnull
  private static String asString( @Nonnull final JsonObject message )
  {
    final StringWriter writer = new StringWriter();
    final JsonWriter jsonWriter = Json.createWriter( writer );
    jsonWriter.writeObject( message );
    jsonWriter.close();
    writer.flush();
    return writer.toString();
  }
}
