package org.realityforge.replicant.client;

import java.util.HashSet;

/**
 * A simple container for representing listener in the broker.
 */
final class ListenerEntry
{
  private final EntityChangeListener _listener;
  private boolean _globalListener;
  private final HashSet<Class> _interestedTypes = new HashSet<Class>();
  private final HashSet<Object> _interestedInstances = new HashSet<Object>();

  ListenerEntry( final EntityChangeListener listener )
  {
    _listener = listener;
  }

  boolean isEmpty()
  {
    return !isGlobalListener() && 0 == getInterestedTypes().size() && 0 == getInterestedInstances().size();
  }

  boolean isGlobalListener()
  {
    return _globalListener;
  }

  void setGlobalListener( final boolean globalListener )
  {
    _globalListener = globalListener;
  }

  EntityChangeListener getListener()
  {
    return _listener;
  }

  HashSet<Class> getInterestedTypes()
  {
    return _interestedTypes;
  }

  HashSet<Object> getInterestedInstances()
  {
    return _interestedInstances;
  }
}
