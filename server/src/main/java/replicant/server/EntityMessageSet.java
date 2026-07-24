package replicant.server;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import org.jspecify.annotations.NonNull;

public final class EntityMessageSet
{
  @NonNull
  private final LinkedHashMap<String, EntityMessage> _entities = new LinkedHashMap<>();

  public boolean containsEntityMessage( final int typeID, @NonNull final Serializable id )
  {
    return _entities.containsKey( toKey( typeID, id ) );
  }

  public void mergeAll( @NonNull final Collection<EntityMessage> messages )
  {
    mergeAll( messages, false );
  }

  public void mergeAll( @NonNull final Collection<EntityMessage> messages, final boolean copyOnMerge )
  {
    for ( final var message : messages )
    {
      merge( message, copyOnMerge );
    }
  }

  public void merge( @NonNull final EntityMessage message )
  {
    merge( message, false );
  }

  public void merge( @NonNull final EntityMessage message, final boolean copyOnMerge )
  {
    final var key = toKey( message.getTypeId(), message.getId() );
    final var existing = _entities.get( key );
    if ( null != existing )
    {
      existing.merge( message );
    }
    else
    {
      _entities.put( key, copyOnMerge ? message.duplicate() : message );
    }
  }

  @NonNull
  public Collection<EntityMessage> getEntityMessages()
  {
    return _entities.values();
  }

  @NonNull
  private String toKey( final int typeID, @NonNull final Serializable id )
  {
    return typeID + "#" + id;
  }
}
