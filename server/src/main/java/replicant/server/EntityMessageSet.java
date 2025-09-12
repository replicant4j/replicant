package replicant.server;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import javax.annotation.Nonnull;

public final class EntityMessageSet
{
  @Nonnull
  private final LinkedHashMap<String, EntityMessage> _entities = new LinkedHashMap<>();

  public boolean containsEntityMessage( final int typeID, @Nonnull final Serializable id )
  {
    return _entities.containsKey( toKey( typeID, id ) );
  }

  public void mergeAll( @Nonnull final Collection<EntityMessage> messages )
  {
    mergeAll( messages, false );
  }

  public void mergeAll( @Nonnull final Collection<EntityMessage> messages, final boolean copyOnMerge )
  {
    for ( final EntityMessage message : messages )
    {
      merge( message, copyOnMerge );
    }
  }

  public void merge( @Nonnull final EntityMessage message )
  {
    merge( message, false );
  }

  public void merge( @Nonnull final EntityMessage message, final boolean copyOnMerge )
  {
    final String key = toKey( message.getTypeId(), message.getId() );
    final EntityMessage existing = _entities.get( key );
    if ( null != existing )
    {
      existing.merge( message );
    }
    else
    {
      _entities.put( key, copyOnMerge ? message.duplicate() : message );
    }
  }

  @Nonnull
  public Collection<EntityMessage> getEntityMessages()
  {
    return _entities.values();
  }

  @Nonnull
  private String toKey( final int typeID, @Nonnull final Serializable id )
  {
    return typeID + "#" + id;
  }
}
