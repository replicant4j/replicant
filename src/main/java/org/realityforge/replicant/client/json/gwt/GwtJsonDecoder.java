package org.realityforge.replicant.client.json.gwt;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
      toArray( changeSet.get( TransportConstants.CHANGES ), TransportConstants.CHANGES );
    final int size = array.size();
    final ArrayList<Linkable> updatedEntities = new ArrayList<Linkable>( size );
    final HashSet<Linkable> removedEntities = new HashSet<Linkable>( size );
    for ( int i = 0; i < size; i++ )
    {
      final JSONObject change = toObject( array.get( i ), TransportConstants.TYPE_ID + "[" + i + "]" );
      final int typeID = toInteger( change.get( TransportConstants.TYPE_ID ), TransportConstants.TYPE_ID );

      final Map<String, java.io.Serializable> data =
        change.containsKey( TransportConstants.DATA ) ?
        toMap( toObject( change.get( TransportConstants.DATA ), TransportConstants.DATA ) ) :
        null;

      final Object id = toValue( change.get( TransportConstants.ENTITY_ID ) );

      final Object entity = _changeMapper.applyChange( typeID, id, data );
      //Is the entity a update and is it linkable?
      if ( entity instanceof Linkable )
      {
        if ( null == data )
        {
          removedEntities.add( (Linkable) entity );
        }
        else
        {
          updatedEntities.add( (Linkable) entity );
        }
      }
    }
    for ( final Linkable entity : updatedEntities )
    {
      // In some circumstances a create and remove can appear in same change set so guard against this
      if ( !removedEntities.contains( entity ) )
      {
        entity.link();
      }
    }
    return toInteger( changeSet.get( TransportConstants.LAST_CHANGE_SET_ID ), TransportConstants.LAST_CHANGE_SET_ID );
  }

  private Serializable toValue( final JSONValue value )
  {
    if( null != value.isNumber() )
    {
      return (int)value.isNumber().doubleValue();
    }
    else if( null != value.isBoolean() )
    {
      return value.isBoolean().booleanValue();
    }
    else if( null != value.isString() )
    {
      return value.isString().stringValue();
    }
    else if( null != value.isNull() )
    {
      return null;
    }
    else
    {
      throw new IllegalStateException( "Unexpected value " + value );
    }
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
      result.put( key, toValue( value.get( key ) ) );
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
