package org.realityforge.replicant.server.json;

import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.json.JSONArray;
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
   * Encode the change set with the EntityMessages.
   *
   * @param lastChangeSetID the last change set ID.
   * @param messages        the messages encoded as EntityMessage objects.
   * @return the encoded change set.
   */
  @Nonnull
  public static JSONObject encodeChangeSetFromEntityMessages( final int lastChangeSetID,
                                                              @Nonnull final Collection<EntityMessage> messages )
  {
    final ArrayList<JSONObject> jsonMessages = new ArrayList<JSONObject>();
    for ( final EntityMessage message : messages )
    {
      jsonMessages.add( JsonEncoder.encodeEntityMessage( message ) );
    }
    return encodeChangeSet( lastChangeSetID, jsonMessages );
  }

  /**
   * Encode the change set with the EntityMessages.
   *
   * @param lastChangeSetID the last change set ID.
   * @param messages        the messages encoded as EntityMessage objects.
   * @return the encoded change set.
   */
  @Nonnull
  public static JSONObject encodeChangeSetFromEncodedStrings( final int lastChangeSetID,
                                                              @Nonnull final Collection<String> messages )
  {
    try
    {
      final ArrayList<JSONObject> jsonMessages = new ArrayList<JSONObject>();
      for ( final String message : messages )
      {
        jsonMessages.add( new JSONObject( message ) );
      }
      return encodeChangeSet( lastChangeSetID, jsonMessages );
    }
    catch ( final JSONException je )
    {
      throw new IllegalStateException( je.getMessage(), je );
    }
  }

  /**
   * Encode the change set with the structured messages.
   *
   * @param lastChangeSetID the last change set ID.
   * @param messages        the messages encoded as json objects.
   * @return the encoded change set.
   */
  private static JSONObject encodeChangeSet( final int lastChangeSetID, final ArrayList<JSONObject> messages )
  {
    try
    {
      final JSONObject changeSet = new JSONObject();
      changeSet.put( TransportConstants.LAST_CHANGE_SET_ID, lastChangeSetID );
      final JSONArray changes = new JSONArray();
      changeSet.put( TransportConstants.CHANGES, changes );
      for ( final JSONObject jsonObject : messages )
      {
        changes.put( jsonObject );
      }
      return changeSet;
    }
    catch ( final JSONException je )
    {
      throw new IllegalStateException( je.getMessage(), je );
    }
  }

  /**
   * Encode the specified message as a json string.
   *
   * @param message the EntityMessage
   * @return the json string.
   */
  @Nonnull
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
  @Nonnull
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
