package org.realityforge.replicant.client;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * A basic EntityLocator implementation that allows explicit per-type registration
 */
public class AggregateEntityLocator
  implements EntityLocator
{
  private final ArrayList<EntityLocator> _entityLocators = new ArrayList<>();

  protected final <T> void registerEntityLocator( @Nonnull final EntityLocator entityLocator )
  {
    apiInvariant( () -> !_entityLocators.contains( entityLocator ),
                  () -> "Attempting to register entityLocator " + entityLocator + " when already present." );
    _entityLocators.add( entityLocator );
  }

  /**
   * {@inheritDoc}
   */
  @Nullable
  @Override
  public final <T> T findByID( @Nonnull final Class<T> type, @Nonnull final Object id )
  {
    for ( final EntityLocator entityLocator : _entityLocators )
    {
      final T entity = entityLocator.findByID( type, id );
      if ( null != entity )
      {
        return entity;
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public <T> List<T> findAll( @Nonnull final Class<T> type )
  {
    final ArrayList<T> results = new ArrayList<>();
    for ( final EntityLocator entityLocator : _entityLocators )
    {
      results.addAll( entityLocator.findAll( type ) );
    }
    return results;
  }
}
