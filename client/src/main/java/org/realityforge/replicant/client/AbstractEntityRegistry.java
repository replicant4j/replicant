package org.realityforge.replicant.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.annotation.Nonnull;
import replicant.Replicant;
import static org.realityforge.braincheck.Guards.*;

/**
 * A basic EntityRegistry implementation that allows explicit per-type registration
 */
@SuppressWarnings( "unchecked" )
public abstract class AbstractEntityRegistry
  implements EntityRegistry
{
  private final Map<Class<?>, BiConsumer<Object, ?>> _registerFunctions = new HashMap<>();

  protected final <T> void bind( @Nonnull final Class<T> type, @Nonnull final BiConsumer<Object, ?> registerFunction )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> !_registerFunctions.containsKey( type ),
                    () -> "Replicant-0018: Attempting to bind register function for type " + type + " when a " +
                          "function already exists for type." );
    }
    _registerFunctions.put( type, registerFunction );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> void registerEntity( @Nonnull final Class<T> type, @Nonnull final Object id, @Nonnull final T entity )
  {
    final BiConsumer<Object, T> function = (BiConsumer<Object, T>) _registerFunctions.get( type );
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> null != function,
                 () -> "Replicant-0075: Attempting to register entity of type " + type + " with id '" + id +
                       "' but no register function exists for type." );
    }
    assert null != function;
    function.accept( id, entity );
  }
}
