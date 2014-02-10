package org.realityforge.replicant.server.json;

import java.io.Serializable;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.shared.json.TransportConstants;

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
   * @param requestID       the requestID that initiated the change. Only set if packet is destined for originating session.
   * @param etag            the associated etag.
   * @param messages        the messages encoded as EntityMessage objects.
   * @return the encoded change set.
   */
  @Nonnull
  public static String encodeChangeSetFromEntityMessages( final int lastChangeSetID,
                                                          @Nullable final String requestID,
                                                          @Nullable final String etag,
                                                          @Nonnull final Collection<EntityMessage> messages )
  {
    final StringWriter writer = new StringWriter();
    final JsonGenerator generator = FACTORY.createGenerator( writer );
    final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );

    generator.
      writeStartObject().
      write( TransportConstants.LAST_CHANGE_SET_ID, lastChangeSetID );
    if ( null == requestID )
    {
      generator.writeNull( TransportConstants.REQUEST_ID );
    }
    else
    {
      generator.write( TransportConstants.REQUEST_ID, requestID );
    }
    if ( null == etag )
    {
      generator.writeNull( TransportConstants.ETAG );
    }
    else
    {
      generator.write( TransportConstants.ETAG, etag );
    }

    generator.writeStartArray( TransportConstants.CHANGES );

    for ( final EntityMessage message : messages )
    {
      generator.writeStartObject();
      writeField( generator, TransportConstants.ENTITY_ID, message.getID(), dateFormat );
      generator.write( TransportConstants.TYPE_ID, message.getTypeID() );

      if ( message.isUpdate() )
      {
        generator.writeStartObject( TransportConstants.DATA );
        final Map<String, Serializable> values = message.getAttributeValues();
        assert null != values;
        for ( final Entry<String, Serializable> entry : values.entrySet() )
        {
          writeField( generator, entry.getKey(), entry.getValue(), dateFormat );
        }
        generator.writeEnd();
      }
      generator.writeEnd();
    }
    generator.
      writeEnd().
      writeEnd().
      close();
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
      generator.write( key, ( (Integer) serializable ).intValue() );
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
}
