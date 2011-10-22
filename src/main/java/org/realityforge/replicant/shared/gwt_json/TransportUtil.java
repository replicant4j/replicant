package org.realityforge.replicant.shared.gwt_json;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Constants used in building up the JSON response to gwt client.
 */
public final class TransportUtil
{
  public static final String ID = "id";
  public static final String LAST_CHANGE_SET_ID = "last_id";
  public static final String CHANGE_SET = "changeset";
  public static final String CHANGES = "changes";
  public static final String ENTITY_ID = ID;
  public static final String TYPE_ID = "type";
  public static final String DATA = "data";

  @Nonnull
  public static JSONObject toObject( final JSONValue value, final String message )
  {
    final JSONObject object = value.isObject();
    if ( null == object )
    {
      throw new IllegalStateException( message + " is not an object" );
    }
    return object;
  }

  @Nonnull
  public static Map<String, Serializable> toMap( final JSONObject value )
  {
    final HashMap<String, Serializable> result = new HashMap<String, Serializable>();
    for ( final String key : value.keySet() )
    {
      final JSONValue attributeValue = value.get( key );
      if ( null != attributeValue.isString() )
      {
        result.put( key, attributeValue.isString().stringValue() );
      }
      else if ( null != attributeValue.isNumber() )
      {
        result.put( key, (int) attributeValue.isNumber().doubleValue() );
      }
      else if ( null != attributeValue.isBoolean() )
      {
        result.put( key, attributeValue.isBoolean().booleanValue() );
      }
      else if ( null != attributeValue.isNull() )
      {
        result.put( key, null );
      }
      else
      {
        throw new IllegalStateException( "Unexpected value for " + key );
      }
    }
    return result;
  }

  @Nonnull
  public static JSONArray toArray( final JSONValue value, final String message )
  {
    final JSONArray result = value.isArray();
    if ( null == result )
    {
      throw new IllegalStateException( message + " is not an array" );
    }
    return result;
  }

  public static int toInteger( final JSONValue value, final String message )
  {
    final JSONNumber number = value.isNumber();
    if ( null == number )
    {
      throw new IllegalStateException( message + " is not a number" );
    }
    return (int) number.doubleValue();
  }

  public static double toDouble( final JSONValue value, final String message )
  {
    final JSONNumber number = value.isNumber();
    if ( null == number )
    {
      throw new IllegalStateException( message + " is not a number" );
    }
    return number.doubleValue();
  }

  @Nonnull
  public static String toString( final JSONValue value, final String message )
  {
    final JSONString result = value.isString();
    if ( null == result )
    {
      throw new IllegalStateException( message + " is not a string" );
    }
    return result.stringValue();
  }
}
