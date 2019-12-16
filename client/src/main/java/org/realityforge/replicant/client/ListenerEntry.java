package org.realityforge.replicant.client;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * A simple container for representing listener in the broker.
 */
final class ListenerEntry
{
  @Nonnull
  private final EntityChangeListener _listener;
  private boolean _globalListener;
  @Nonnull
  private final Set<Class<?>> _interestedTypes = new HashSet<>();
  @Nonnull
  private final Set<Object> _interestedInstances = new HashSet<>();

  ListenerEntry( @Nonnull final EntityChangeListener listener )
  {
    _listener = Objects.requireNonNull( listener );
  }

  boolean isEmpty()
  {
    return !isGlobalListener() && 0 == interestedTypeSet().size() && 0 == interestedInstanceSet().size();
  }

  boolean isGlobalListener()
  {
    return _globalListener;
  }

  void setGlobalListener( final boolean globalListener )
  {
    _globalListener = globalListener;
  }

  @Nonnull
  EntityChangeListener getListener()
  {
    return _listener;
  }

  @Nonnull
  Set<Class<?>> interestedTypeSet()
  {
    return _interestedTypes;
  }

  @Nonnull
  Set<Object> interestedInstanceSet()
  {
    return _interestedInstances;
  }
}
