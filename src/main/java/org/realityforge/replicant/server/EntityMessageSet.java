package org.realityforge.replicant.server;

import java.util.Collection;
import java.util.LinkedHashMap;

public class EntityMessageSet
{
  private final LinkedHashMap<String, EntityMessage> _entities = new LinkedHashMap<>();

  public void merge( final EntityMessage message )
  {
    final String key = message.getTypeID() + "#" + message.getID();
    final EntityMessage existing = _entities.get( key );
    if( null != existing )
    {
      existing.merge( message );
    }
    else
    {
      _entities.put( key, message );
    }
  }

  public Collection<EntityMessage> getEntityMessages()
  {
    return _entities.values();
  }
}
