package replicant.server.json;

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
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import replicant.server.Change;
import replicant.server.ChangeSet;
import replicant.server.ChannelAction;
import replicant.server.ChannelAction.Action;
import replicant.server.ChannelAddress;
import replicant.server.EntityMessage;
import replicant.shared.Messages;

/**
 * Utility class used when encoding EntityMessage into JSON payload.
 */
public final class JsonEncoder
{
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
                                        @Nullable final JsonValue response,
                                        @Nullable final String etag,
                                        @Nonnull final ChangeSet changeSet )
  {
    final StringWriter writer = new StringWriter();
    final JsonGenerator generator = FACTORY.createGenerator( writer );
    final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );

    generator.writeStartObject();
    generator.write( Messages.Common.TYPE, Messages.S2C_Type.UPDATE );
    if ( null != requestId )
    {
      generator.write( Messages.Common.REQUEST_ID, requestId );
    }
    if ( null != response )
    {
      generator.write( Messages.Update.RESPONSE, response );
    }
    if ( null != etag )
    {
      generator.write( Messages.S2C_Common.ETAG, etag );
    }

    final List<ChannelAction> actions =
      changeSet.getChannelActions().stream().filter( c -> null == c.filter() ).toList();
    if ( !actions.isEmpty() )
    {
      generator.writeStartArray( Messages.Update.CHANNEL_ACTIONS );
      actions.stream().map( JsonEncoder::toDescriptor ).forEach( generator::write );
      generator.writeEnd();
    }

    final List<ChannelAction> filteredActions =
      changeSet.getChannelActions().stream().filter( c -> null != c.filter() ).toList();
    if ( !filteredActions.isEmpty() )
    {
      generator.writeStartArray( Messages.Update.FILTERED_CHANNEL_ACTIONS );
      filteredActions.forEach( a -> {
        generator.writeStartObject();
        generator.write( Messages.Common.CHANNEL, toDescriptor( a ) );
        generator.write( Messages.Update.FILTER, a.filter() );
        generator.writeEnd();
      } );
      generator.writeEnd();
    }

    final Collection<Change> changes = changeSet.getChanges();
    if ( !changes.isEmpty() )
    {
      generator.writeStartArray( Messages.Update.CHANGES );

      for ( final Change change : changes )
      {
        final EntityMessage entityMessage = change.getEntityMessage();

        generator.writeStartObject();
        generator.write( Messages.Update.ENTITY_ID, entityMessage.getTypeId() + "." + entityMessage.getId() );

        final var channels = change.getChannels();
        if ( !channels.isEmpty() )
        {
          generator.writeStartArray( Messages.Update.CHANNELS );
          for ( final ChannelAddress address : channels )
          {
            generator.write( address.toString() );
          }
          generator.writeEnd();
        }

        if ( entityMessage.isUpdate() )
        {
          generator.writeStartObject( Messages.Update.DATA );
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
  public static String toDescriptor( @Nonnull final ChannelAction channelAction )
  {
    final Action action = channelAction.action();
    final char actionValue =
      Action.ADD == action ? Messages.Update.CHANNEL_ACTION_ADD :
      Action.REMOVE == action ? Messages.Update.CHANNEL_ACTION_REMOVE :
      Action.UPDATE == action ? Messages.Update.CHANNEL_ACTION_UPDATE :
      Messages.Update.CHANNEL_ACTION_DELETE;
    return String.valueOf( actionValue ) + channelAction.address();
  }

  @SuppressWarnings( "StatementWithEmptyBody" )
  private static void writeField( @Nonnull final JsonGenerator generator,
                                  @Nonnull final String key,
                                  @Nullable final Serializable serializable,
                                  @Nonnull final SimpleDateFormat dateFormat )
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
      // No need to write anything as the client code will treat missing field as null
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
        .add( Messages.Common.TYPE, Messages.S2C_Type.USE_CACHE )
        .add( Messages.Common.CHANNEL, address.toString() )
        .add( Messages.S2C_Common.ETAG, eTag );
    if ( null != requestId )
    {
      response.add( Messages.Common.REQUEST_ID, requestId );
    }
    return asString( response.build() );
  }

  @Nonnull
  public static String encodeSessionCreatedMessage( @Nonnull final String sessionId )
  {
    return asString( Json
                       .createObjectBuilder()
                       .add( Messages.Common.TYPE, Messages.S2C_Type.SESSION_CREATED )
                       .add( Messages.S2C_Common.SESSION_ID, sessionId )
                       .build() );
  }

  @Nonnull
  public static String encodeOkMessage( final int requestId )
  {
    return asString( Json
                       .createObjectBuilder()
                       .add( Messages.Common.TYPE, Messages.S2C_Type.OK )
                       .add( Messages.Common.REQUEST_ID, requestId )
                       .build() );
  }

  @Nonnull
  public static String encodeMalformedMessageMessage( @Nonnull final String message )
  {
    return asString( Json
                       .createObjectBuilder()
                       .add( Messages.Common.TYPE, Messages.S2C_Type.MALFORMED_MESSAGE )
                       .add( Messages.S2C_Common.MESSAGE, message )
                       .build() );
  }

  @Nonnull
  public static String encodeUnknownRequestType( @Nonnull final JsonObject command )
  {
    return asString( Json
                       .createObjectBuilder()
                       .add( Messages.Common.TYPE, Messages.S2C_Type.UNKNOWN_REQUEST_TYPE )
                       .add( Messages.Common.COMMAND, command )
                       .build() );
  }

  @Nonnull
  public static String encodeErrorMessage( @Nonnull final String message )
  {
    return asString( Json
                       .createObjectBuilder()
                       .add( Messages.Common.TYPE, Messages.S2C_Type.ERROR )
                       .add( Messages.S2C_Common.MESSAGE, message )
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
