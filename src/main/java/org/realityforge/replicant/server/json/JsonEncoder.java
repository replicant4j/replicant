package org.realityforge.replicant.server.json;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import org.glassfish.json.JsonGeneratorFactoryImpl;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.shared.json.TransportConstants;

/**
 * Utility class used when encoding EntityMessage into JSON payload.
 */
public final class JsonEncoder
{
  private JsonEncoder()
  {
  }

  /**
   * Encode the change set with the EntityMessages.
   *
   * @param lastChangeSetID the last change set ID.
   * @param messages        the messages encoded as EntityMessage objects.
   * @return the encoded change set.
   */
  @Nonnull
  public static String encodeChangeSetFromEntityMessages( final int lastChangeSetID,
                                                          @Nonnull final Collection<EntityMessage> messages )
  {
    final JsonGeneratorFactory factory = new JsonGeneratorFactoryImpl();
    final StringWriter writer = new StringWriter();
    final JsonGenerator generator = factory.createGenerator( writer );

    generator.writeStartObject().
      write( TransportConstants.LAST_CHANGE_SET_ID, lastChangeSetID ).
      writeStartArray( TransportConstants.CHANGES );

    for ( final EntityMessage message : messages )
    {
      generator.writeStartObject();
      writeField( generator, TransportConstants.ENTITY_ID, message.getID() );
      generator.write( TransportConstants.TYPE_ID, message.getTypeID() );

      if ( message.isUpdate() )
      {
        generator.writeStartObject( TransportConstants.DATA );
        final Map<String, Serializable> values = message.getAttributeValues();
        assert null != values;
        for ( final Entry<String, Serializable> entry : values.entrySet() )
        {
          writeField( generator, entry.getKey(), entry.getValue() );
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

  private static void writeField( final JsonGenerator generator, final String key, final Serializable serializable )
  {
    if ( serializable instanceof String )
    {
      generator.write( key, (String) serializable );
    }
    else if ( serializable instanceof Integer )
    {
      generator.write( key, ( (Integer) serializable ).intValue() );
    }
    else if ( null == serializable )
    {
      generator.writeNull( key );
    }
    else if ( serializable instanceof Float )
    {
      generator.write( key, (Float) serializable );
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
