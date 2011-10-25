package org.realityforge.replicant.server.json;

import javax.annotation.Nonnull;
import org.json.JSONException;
import org.json.JSONObject;
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
   * Encode the specified message as a json string.
   *
   * @param message the EntityMessage
   * @return the json string.
   */
  public static String encodeEntityMessageAsString( @Nonnull final EntityMessage message )
  {
    return encodeEntityMessage( message ).toString();
  }

  /**
   * Encode the specified message as a json object.
   *
   * @param message the EntityMessage
   * @return the JSONObject representation
   */
  public static JSONObject encodeEntityMessage( @Nonnull final EntityMessage message )
  {
    try
    {
      final JSONObject json = new JSONObject();
      json.put( TransportConstants.ENTITY_ID, message.getID() );
      json.put( TransportConstants.TYPE_ID, message.getTypeID() );
      if ( message.isUpdate() )
      {
        json.put( TransportConstants.DATA, new JSONObject( message.getAttributeValues() ) );
      }
      return json;
    }
    catch ( final JSONException je )
    {
      throw new IllegalStateException( je.getMessage(), je );
    }
  }
}
