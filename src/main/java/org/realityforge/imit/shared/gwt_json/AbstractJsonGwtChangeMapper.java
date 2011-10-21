package org.realityforge.imit.shared.gwt_json;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import org.realityforge.imit.client.Linkable;

public abstract class AbstractJsonGwtChangeMapper
  implements JsonGwtChangeMapper
{
  @Override
  public final int apply( final com.google.gwt.json.client.JSONValue value )
  {
    final JSONObject changeSet = TransportUtil.toObject( value, TransportUtil.CHANGE_SET );
    final com.google.gwt.json.client.JSONArray array =
      TransportUtil.toArray( changeSet.get( TransportUtil.CHANGES ),
                             TransportUtil.CHANGES );
    final int size = array.size();
    final ArrayList<Linkable> entitiesToLink = new ArrayList<Linkable>( size );
    for ( int i = 0; i < size; i++ )
    {
      final JSONObject change = TransportUtil.toObject( array.get( i ), TransportUtil.TYPE_ID + "[" + i + "]" );
      final int typeID = TransportUtil.toInteger( change.get( TransportUtil.TYPE_ID ), TransportUtil.TYPE_ID );

      final Map<String, java.io.Serializable> data =
        change.containsKey( TransportUtil.DATA ) ?
        TransportUtil.toMap( TransportUtil.toObject( change.get( TransportUtil.DATA ), TransportUtil.DATA ) ) :
        null;

      final JSONValue idValue = change.get( TransportUtil.ENTITY_ID );
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

      final Object entity = applyChange( typeID, id, data );
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
    return TransportUtil.toInteger( changeSet.get( TransportUtil.LAST_CHANGE_SET_ID ), TransportUtil.LAST_CHANGE_SET_ID );
  }

  protected abstract Object applyChange( int typeID, Object id, final Map<String, Serializable> data );
}
