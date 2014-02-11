package org.realityforge.replicant.server;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class EntityMessageSet
{
  private final LinkedHashMap<String, EntityMessage> _entities = new LinkedHashMap<String, EntityMessage>();

  public void mergeAll( final Collection<EntityMessage> messages )
  {
    mergeAll( messages, false );
  }

  public void mergeAll( final Collection<EntityMessage> messages, final boolean copyOnMerge )
  {
    for ( final EntityMessage message : messages )
    {
      merge( message, copyOnMerge );
    }
  }

  public void merge( final EntityMessage message )
  {
    merge( message, false );
  }

  public void merge( final EntityMessage message, final boolean copyOnMerge )
  {
    final String key = message.getTypeID() + "#" + message.getID();
    final EntityMessage existing = _entities.get( key );
    if ( null != existing )
    {
      existing.merge( message );
    }
    else
    {
      final EntityMessage messageToInsert;
      if ( copyOnMerge )
      {
        messageToInsert = new EntityMessage( message.getID(),
                                             message.getTypeID(),
                                             message.getTimestamp(),
                                             new HashMap<String, Serializable>(),
                                             new HashMap<String, Serializable>() );
        messageToInsert.merge( message );
      }
      else
      {
        messageToInsert = message;
      }
      _entities.put( key, messageToInsert );
    }
  }

  public Collection<EntityMessage> getEntityMessages()
  {
    return _entities.values();
  }
}
