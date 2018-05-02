package org.realityforge.replicant.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * A basic EntityLocator implementation that allows explicit per-type registration
 */
@SuppressWarnings( "unchecked" )
public abstract class AbstractEntityLocator
  implements EntityLocator
{
  private final Map<Class<?>, Function<Object, ?>> _findByIdFunctions = new HashMap<>();
  private final Map<Class<?>, Supplier<List<?>>> _findAllFunctions = new HashMap<>();

  protected final <T> void registerLookup( @Nonnull final Class<T> type,
                                           @Nonnull final Function<Object, T> findByIdFunction,
                                           @Nonnull final Supplier<List<T>> findAllFunction )
  {
    apiInvariant( () -> !_findByIdFunctions.containsKey( type ),
                  () -> "Attempting to register findById function for type " + type + " when a " +
                        "function already exists." );
    apiInvariant( () -> !_findAllFunctions.containsKey( type ),
                  () -> "Attempting to register findAll function for type " + type + " when a " +
                        "function already exists." );
    _findByIdFunctions.put( type, findByIdFunction );
    _findAllFunctions.put( type, (Supplier<List<?>>) (Supplier) findAllFunction );
  }

  /**
   * {@inheritDoc}
   */
  @Nullable
  @Override
  public final <T> T findByID( @Nonnull final Class<T> type, @Nonnull final Object id )
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

  @Nonnull
  @Override
  public <T> List<T> findAll( @Nonnull final Class<T> type )
  {
    final Supplier<List<?>> function = _findAllFunctions.get( type );
    if ( null != function )
    {
      return (List<T>) function.get();
    }
    else
    {
      return Collections.emptyList();
    }
  }
}
