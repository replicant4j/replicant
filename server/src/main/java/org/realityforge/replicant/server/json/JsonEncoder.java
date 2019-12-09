package org.realityforge.replicant.server.json;

import java.io.Serializable;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import org.realityforge.replicant.server.Change;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelAction;
import org.realityforge.replicant.server.ChannelAction.Action;
import org.realityforge.replicant.server.EntityMessage;

/**
 * Utility class used when encoding EntityMessage into JSON payload.
 */
public final class JsonEncoder
{
  // Use constant to avoid slow filesystem access when serializing a message.
  public static final JsonGeneratorFactory FACTORY = Json.createGeneratorFactory( null );

  private JsonEncoder()
  {
  }

  /**
   * Encode the change set with the EntityMessages.
   *
   * @param lastChangeSetID the last change set ID.
   * @param requestId       the requestId that initiated the change. Only set if packet is destined for originating session.
   * @param etag            the associated etag.
   * @param changeSet       the changeSet being encoded.
   * @return the encoded change set.
   */
  @Nonnull
  public static String encodeChangeSet( final int lastChangeSetID,
                                        @Nullable final Integer requestId,
                                        @Nullable final String etag,
                                        @Nonnull final ChangeSet changeSet )
  {
    final StringWriter writer = new StringWriter();
    final JsonGenerator generator = FACTORY.createGenerator( writer );
    final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );

    generator.
      writeStartObject().
      write( TransportConstants.LAST_CHANGE_SET_ID, lastChangeSetID );
    if ( null == requestId )
    {
      generator.writeNull( TransportConstants.REQUEST_ID );
    }
    else
    {
      generator.write( TransportConstants.REQUEST_ID, requestId );
    }
    if ( null == etag )
    {
      generator.writeNull( TransportConstants.ETAG );
    }
    else
    {
      generator.write( TransportConstants.ETAG, etag );
    }

    final LinkedList<ChannelAction> actions = changeSet.getChannelActions();
    if ( 0 != actions.size() )
    {
      generator.writeStartArray( TransportConstants.CHANNEL_ACTIONS );
      for ( final ChannelAction action : actions )
      {
        generator.writeStartObject();
        generator.write( TransportConstants.CHANNEL_ID, action.getAddress().getChannelId() );
        writeSubChannel( generator, action.getAddress().getSubChannelId() );
        final String actionValue =
          action.getAction() == Action.ADD ? TransportConstants.ACTION_ADD :
          action.getAction() == Action.REMOVE ? TransportConstants.ACTION_REMOVE :
          TransportConstants.ACTION_UPDATE;
        generator.write( TransportConstants.ACTION, actionValue );
        final JsonObject filter = action.getFilter();
        if ( null != filter )
        {
          generator.write( TransportConstants.CHANNEL_FILTER, filter );
        }
        generator.writeEnd();
      }
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
        writeField( generator, TransportConstants.ENTITY_ID, entityMessage.getId(), dateFormat );
        generator.write( TransportConstants.TYPE, entityMessage.getTypeId() );

        final Map<Integer, Integer> channels = change.getChannels();
        if ( channels.size() > 0 )
        {
          generator.writeStartArray( TransportConstants.CHANNELS );
          for ( final Entry<Integer, Integer> entry : channels.entrySet() )
          {
            generator.writeStartObject();
            generator.write( TransportConstants.CHANNEL_ID, entry.getKey() );
            writeSubChannel( generator, entry.getValue() );
            generator.writeEnd();
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

  private static void writeSubChannel( final JsonGenerator generator,
                                       final Serializable serializable )
  {
    if ( serializable instanceof String )
    {
      generator.write( TransportConstants.SUBCHANNEL_ID, (String) serializable );
    }
    else if ( serializable instanceof Integer )
    {

      final Integer value = (Integer) serializable;
      if ( 0 != value )
      {
        generator.write( TransportConstants.SUBCHANNEL_ID, value );
      }
    }
    else if ( null != serializable )
    {
      throw new IllegalStateException( "Unable to encode: " + serializable );
    }
  }
}
