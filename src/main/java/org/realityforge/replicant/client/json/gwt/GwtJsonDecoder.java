package org.realityforge.replicant.client.json.gwt;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.realityforge.replicant.client.ChangeMapper;
import org.realityforge.replicant.client.Linkable;
import org.realityforge.replicant.shared.json.TransportConstants;

public class GwtJsonDecoder
{
  private final ChangeMapper _changeMapper;

  @Inject
  protected GwtJsonDecoder( @Nonnull final ChangeMapper changeMapper )
  {
    _changeMapper = changeMapper;
  }

  public final int apply( final com.google.gwt.json.client.JSONValue value )
  {
    final JSONObject changeSet = toObject( value, "changeset" );
    final com.google.gwt.json.client.JSONArray array =
      toArray( changeSet.get( TransportConstants.CHANGES ),
               TransportConstants.CHANGES );
    final int size = array.size();
    final ArrayList<Linkable> entitiesToLink = new ArrayList<Linkable>( size );
    for ( int i = 0; i < size; i++ )
    {
      final JSONObject change = toObject( array.get( i ), TransportConstants.TYPE_ID + "[" + i + "]" );
      final int typeID = toInteger( change.get( TransportConstants.TYPE_ID ), TransportConstants.TYPE_ID );

      final Map<String, java.io.Serializable> data =
        change.containsKey( TransportConstants.DATA ) ?
        toMap( toObject( change.get( TransportConstants.DATA ), TransportConstants.DATA ) ) :
        null;

      final JSONValue idValue = change.get( TransportConstants.ENTITY_ID );
      final Object id;
      if( null != idValue.isNumber() )
      {
        id = (int)idValue.isNumber().doubleValue();
      }
      else if( null != idValue.isBoolean() )
      {
        id = idValue.isBoolean().booleanValue();
      }
      else if( null != idValue.isString() )
      {
        id = idValue.isString().stringValue();
      }
      else
      {
        throw new IllegalStateException( "Unexpected id type " + idValue );
      }

      final Object entity = _changeMapper.applyChange( typeID, id, data );
      //Is the entity a update and is it linkable?
      if ( null != data && entity instanceof Linkable )
      {
        entitiesToLink.add( (Linkable) entity );
      }
    }
    for ( final Linkable linkable : entitiesToLink )
    {
      linkable.link();
    }
    return toInteger( changeSet.get( TransportConstants.LAST_CHANGE_SET_ID ), TransportConstants.LAST_CHANGE_SET_ID );
  }

  @Nonnull
  private JSONObject toObject( final JSONValue value, final String message )
  {
    final JSONObject object = value.isObject();
    if ( null == object )
    {
      throw new IllegalStateException( message + " is not an object" );
    }
    return object;
  }

  @Nonnull
  private Map<String, Serializable> toMap( final JSONObject value )
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
  private JSONArray toArray( final JSONValue value, final String message )
  {
    final JSONArray result = value.isArray();
    if ( null == result )
    {
      throw new IllegalStateException( message + " is not an array" );
    }
    return result;
  }

  private int toInteger( final JSONValue value, final String message )
  {
    final JSONNumber number = value.isNumber();
    if ( null == number )
    {
      throw new IllegalStateException( message + " is not a number" );
    }
    return (int) number.doubleValue();
  }
}
