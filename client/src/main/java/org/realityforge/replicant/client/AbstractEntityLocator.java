package org.realityforge.replicant.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.Replicant;
import static org.realityforge.braincheck.Guards.*;

/**
 * A basic EntityLocator implementation that allows explicit per-type registration
 */
@SuppressWarnings( "unchecked" )
public abstract class AbstractEntityLocator
  implements EntityLocator
{
  private final Map<Class<?>, Function<Object, ?>> _findByIdFunctions = new HashMap<>();

  protected final <T> void registerLookup( @Nonnull final Class<T> type,
                                           @Nonnull final Function<Object, T> findByIdFunction )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> !_findByIdFunctions.containsKey( type ),
                    () -> "Replicant-0086: Attempting to register findById function for type " + type + " when a " +
                          "function already exists." );
    }
    _findByIdFunctions.put( type, findByIdFunction );
  }

  /**
   * {@inheritDoc}
   */
  @Nullable
  @Override
  public final <T> T findById( @Nonnull final Class<T> type, @Nonnull final Object id )
  {
    final Function<Object, ?> function = _findByIdFunctions.get( type );
    if ( null != function )
    {
      return (T) function.apply( id );
    }
    else
    {
      return null;
    }
  }
}
