package org.realityforge.replicant.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * A basic EntityLocator implementation that allows explicit per-type registration
 */
public abstract class AbstractEntityLocator
  implements EntityLocator
{
  private final Map<Class<?>, Function<Object, ?>> _lookups = new HashMap<>();

  protected final <T> void registerLookup( @Nonnull final Class<T> type, Function<Object, T> function )
  {
    apiInvariant( () -> !_lookups.containsKey( type ),
                         () -> "Attempting to register lookup function for type " + type + " when a " +
                               "function already exists." );
    _lookups.put( type, function );
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings( "unchecked" )
  @Nullable
  @Override
  public <T> T findByID( @Nonnull final Class<T> type, @Nonnull final Object id )
  {
    final Function<Object, ?> function = _lookups.get( type );
    if ( null == function )
    {
      return null;
    }
    else
    {
      return (T) function.apply( id );
    }
  }
}
