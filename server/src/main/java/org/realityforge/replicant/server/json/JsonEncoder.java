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
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import org.realityforge.replicant.server.Change;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelAction;
import org.realityforge.replicant.server.ChannelAction.Action;
import org.realityforge.replicant.server.ChannelAddress;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.shared.SharedConstants;

/**
 * Utility class used when encoding EntityMessage into JSON payload.
 */
public final class JsonEncoder
{
  // Use constant to avoid slow filesystem access when serializing a message.
  private static final JsonGeneratorFactory FACTORY = Json.createGeneratorFactory( null );

  private JsonEncoder()
  {
  }

  /**
   * Encode the change set with the EntityMessages.
   *
   * @param requestId       the requestId that initiated the change. Only set if packet is destined for originating session.
   * @param etag            the associated etag.
   * @param changeSet       the changeSet being encoded.
   * @return the encoded change set.
   */
  @Nonnull
  public static String encodeChangeSet( @Nullable final Integer requestId,
                                        @Nullable final String etag,
                                        @Nonnull final ChangeSet changeSet )
  {
    final StringWriter writer = new StringWriter();
    final JsonGenerator generator = FACTORY.createGenerator( writer );
    final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );

    generator.writeStartObject();
    if ( null != requestId )
    {
      generator.write( TransportConstants.REQUEST_ID, requestId );
    }
    if ( null != etag )
    {
      generator.write( TransportConstants.ETAG, etag );
    }

    final List<ChannelAction> actions =
      changeSet.getChannelActions().stream().filter( c -> null == c.getFilter() ).collect( Collectors.toList() );
    if ( !actions.isEmpty() )
    {
      generator.writeStartArray( TransportConstants.CHANNEL_ACTIONS );
      actions.stream().map( JsonEncoder::toDescriptor ).forEach( generator::write );
      generator.writeEnd();
    }

    final List<ChannelAction> filteredActions =
      changeSet.getChannelActions().stream().filter( c -> null != c.getFilter() ).collect( Collectors.toList() );
    if ( !filteredActions.isEmpty() )
    {
      generator.writeStartArray( TransportConstants.FILTERED_CHANNEL_ACTIONS );
      filteredActions.forEach( a -> {
        generator.writeStartObject();
        generator.write( TransportConstants.CHANNEL, toDescriptor( a ) );
        generator.write( TransportConstants.CHANNEL_FILTER, a.getFilter() );
        generator.writeEnd();
      } );
      generator.writeEnd();
    }

    final Collection<Change> changes = changeSet.getChanges();
    if ( 0 != changes.size() )
    {
      generator.writeStartArray( TransportConstants.CHANGES );

      for ( final Change change : changes )
      {
        final EntityMessage entityMessage = change.getEntityMessage();

        generator.writeStartObject();
        generator.write( TransportConstants.ENTITY_ID, entityMessage.getTypeId() + "." + entityMessage.getId() );

        final Map<Integer, Integer> channels = change.getChannels();
        if ( channels.size() > 0 )
        {
          generator.writeStartArray( TransportConstants.CHANNELS );
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
          generator.writeStartObject( TransportConstants.DATA );
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
  private static String toDescriptor( final ChannelAction channelAction )
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
  public static String asString( @Nonnull final JsonObject message )
  {
    final StringWriter writer = new StringWriter();
    final JsonWriter jsonWriter = Json.createWriter( writer );
    jsonWriter.writeObject( message );
    jsonWriter.close();
    writer.flush();
    return writer.toString();
  }
}
