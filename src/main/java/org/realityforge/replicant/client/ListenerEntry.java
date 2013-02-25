package org.realityforge.replicant.client;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * A simple container for representing listener in the broker.
 */
public final class ListenerEntry
  implements Cloneable
{
  private final EntityChangeListener _listener;
  private boolean _globalListener;
  private final HashSet<Class> _interestedTypes = new HashSet<Class>();
  private final HashSet<Object> _interestedInstances = new HashSet<Object>();

  public ListenerEntry( final EntityChangeListener listener )
  {
    _listener = listener;
  }

  boolean isEmpty()
  {
    return !isGlobalListener() && 0 == interestedTypeSet().size() && 0 == interestedInstanceSet().size();
  }

  public boolean isGlobalListener()
  {
    return _globalListener;
  }

  void setGlobalListener( final boolean globalListener )
  {
    _globalListener = globalListener;
  }

  @Nonnull
  public EntityChangeListener getListener()
  {
    return _listener;
  }

  @Nonnull
  public Set<Class> getInterestedTypeSet()
  {
    return Collections.unmodifiableSet( interestedTypeSet() );
  }

  @Nonnull
  HashSet<Class> interestedTypeSet()
  {
    return _interestedTypes;
  }

  @Nonnull
  public Set<Object> getInterestedInstanceSet()
  {
    return Collections.unmodifiableSet( _interestedInstances );
  }

  @Nonnull
  HashSet<Object> interestedInstanceSet()
  {
    return _interestedInstances;
  }

  @SuppressWarnings( "CloneDoesntCallSuperClone" )
  @Override
  public ListenerEntry clone()
  {
    final ListenerEntry entry = new ListenerEntry(_listener );
    entry.setGlobalListener( isGlobalListener() );
    for ( final Class type : _interestedTypes )
    {
      entry.interestedTypeSet().add( type );
    }
    for ( final Object instance : _interestedInstances )
    {
      entry.interestedInstanceSet().add( instance );
    }
    return entry;
  }
}
