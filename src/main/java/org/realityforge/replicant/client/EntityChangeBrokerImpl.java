package org.realityforge.replicant.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.DeferredListenerAction.ActionType;

/**
 * A single threaded in-memory EntityChangeBroker implementation.
 */
@SuppressWarnings( { "JavaDoc" } )
public class EntityChangeBrokerImpl
    implements EntityChangeBroker
{
  private static final Logger LOG = Logger.getLogger( EntityChangeBrokerImpl.class.getName() );

  private final ListenerEntry[] _emptyListenerSet = new ListenerEntry[ 0 ];

  private boolean _disabled;
  private boolean _paused;
  private boolean _sending;

  /**
   * List of listeners that we either have to add or removed after message delivery completes.
   */
  private final LinkedList<DeferredListenerAction> _deferredListenerActions = new LinkedList<DeferredListenerAction>();

  /**
   * Backlog of events we still have to send.
   */
  private LinkedList<EntityChangeEvent> _deferredEvents;

  private ListenerEntry[] _globalListeners = _emptyListenerSet;
  private final HashMap<EntityChangeListener, ListenerEntry> _listenerEntries = new HashMap<EntityChangeListener, ListenerEntry>();
  private final Map<Object, ListenerEntry[]> _objectListeners = new HashMap<Object, ListenerEntry[]>();
  private final Map<Class, ListenerEntry[]> _classListeners = new HashMap<Class, ListenerEntry[]>();

  private boolean _raiseErrorOnEventHandlerError = true;

  public final boolean shouldRaiseErrorOnEventHandlerError()
  {
    return _raiseErrorOnEventHandlerError;
  }

  public void setRaiseErrorOnEventHandlerError( final boolean raiseErrorOnEventHandlerError )
  {
    _raiseErrorOnEventHandlerError = raiseErrorOnEventHandlerError;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void addChangeListener( @Nonnull final EntityChangeListener listener )
  {
    if( isSending() )
    {
      _deferredListenerActions.add( new DeferredListenerAction( null, listener, ActionType.ADD ) );
    }
    else
    {
      final ListenerEntry entry = getEntryForListener( listener );
      _globalListeners = doAddChangeListener( _globalListeners, entry );
      entry.setGlobalListener( true );
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void addChangeListener( @Nonnull final Class clazz,
                                       @Nonnull final EntityChangeListener listener )
  {
    if( isSending() )
    {
      _deferredListenerActions.add( new DeferredListenerAction( clazz, listener, ActionType.ADD ) );
    }
    else
    {
      final ListenerEntry entry = getEntryForListener( listener );
      addChangeListener( _classListeners, clazz, entry );
      entry.interestedTypeSet().add( clazz );
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void addChangeListener( @Nonnull final Object object, @Nonnull final EntityChangeListener listener )
  {
    if( isSending() )
    {
      _deferredListenerActions.add( new DeferredListenerAction( object, listener, ActionType.ADD ) );
    }
    else
    {
      final ListenerEntry entry = getEntryForListener( listener );
      addChangeListener( _objectListeners, object, entry );
      entry.interestedInstanceSet().add( object );
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void removeChangeListener( @Nonnull final EntityChangeListener listener )
  {
    if( isSending() )
    {
      _deferredListenerActions.add( new DeferredListenerAction( null, listener, ActionType.REMOVE ) );
    }
    else
    {
      final ListenerEntry entry = findEntryForListener( listener );
      if ( null != entry )
      {
        _globalListeners = doRemoveChangeListener( _globalListeners, entry );
        removeEntryIfEmpty( entry );
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void removeChangeListener( @Nonnull final Object object, @Nonnull final EntityChangeListener listener )
  {
    if( isSending() )
    {
      _deferredListenerActions.add( new DeferredListenerAction( object, listener, ActionType.REMOVE ) );
    }
    else
    {
      final ListenerEntry entry = findEntryForListener( listener );
      if ( null != entry )
      {
        removeChangeListener( _objectListeners, object, entry );
        entry.interestedInstanceSet().remove( object );
        removeEntryIfEmpty( entry );
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void removeChangeListener( @Nonnull final Class clazz,
                                          @Nonnull final EntityChangeListener listener )
  {
    if( isSending() )
    {
      _deferredListenerActions.add( new DeferredListenerAction( clazz, listener, ActionType.REMOVE ) );
    }
    else
    {
      final ListenerEntry entry = findEntryForListener( listener );
      if ( null != entry )
      {
        removeChangeListener( _classListeners, clazz, entry );
        entry.interestedTypeSet().remove( clazz );
        removeEntryIfEmpty( entry );
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void purgeChangeListener( @Nonnull final EntityChangeListener listener )
  {
    if( isSending() )
    {
      _deferredListenerActions.add( new DeferredListenerAction( null, listener, ActionType.PURGE ) );
    }
    else
    {
      final ListenerEntry entry = findEntryForListener( listener );
      if ( null != entry )
      {
        if ( entry.isGlobalListener() )
        {
          removeChangeListener( listener );
        }
        final HashSet<Class> types = entry.interestedTypeSet();
        final HashSet<Class> typesToRemove = types.size() > 1 ? new HashSet<Class>( types ) : types;
        for ( final Class type : typesToRemove )
        {
          removeChangeListener( type, listener );
        }
        final HashSet<Object> instances = entry.interestedInstanceSet();
        final HashSet<Object> instancesToRemove = instances.size() > 1 ? new HashSet<Object>( instances ) : instances;
        for ( final Object instance : instancesToRemove )
        {
          removeChangeListener( instance, listener );
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void attributeChanged( @Nonnull final Object entity, @Nonnull final String name, @Nonnull final Object value )
  {
    sendEvent( new EntityChangeEvent( EntityChangeType.ATTRIBUTE_CHANGED, entity, name, value ) );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void relatedAdded( @Nonnull final Object entity, @Nonnull final String name, @Nonnull final Object other )
  {
    sendEvent( new EntityChangeEvent( EntityChangeType.RELATED_ADDED, entity, name, other ) );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void relatedRemoved( @Nonnull final Object entity, @Nonnull final String name, @Nonnull final Object other )
  {
    sendEvent( new EntityChangeEvent( EntityChangeType.RELATED_REMOVED, entity, name, other ) );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void entityRemoved( @Nonnull final Object entity )
  {
    sendEvent( new EntityChangeEvent( EntityChangeType.ENTITY_REMOVED, entity, null, null ) );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void resume()
  {
    if( !_paused )
    {
      throw new IllegalStateException( "Attempting to resume already resumed broker" );
    }
    _paused = false;
    deliverDeferredEvents();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void pause()
  {
    if( _paused )
    {
      throw new IllegalStateException( "Attempting to pause already paused broker" );
    }
    _paused = true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isPaused()
  {
    return _paused;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void disable()
  {
    if( _disabled )
    {
      throw new IllegalStateException( "Attempting to disable already disabled broker" );
    }
    _disabled = true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void enable()
  {
    if( !_disabled )
    {
      throw new IllegalStateException( "Attempting to enable already enabled broker" );
    }
    _disabled = false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEnabled()
  {
    return !_disabled;
  }

  /**
   * @return true if in the middle of delivering a change message.
   */
  private boolean isSending()
  {
    return _sending;
  }

  /**
   * Attempt to deliver an event.
   * If the broker is disabled then discard the event.
   * If the broker is paused then defer sending until it is resumed.
   *
   * @param event the event.
   */
  private void sendEvent( final EntityChangeEvent event )
  {
    if( isEnabled() )
    {
      if( isPaused() || isSending() )
      {
        if( null == _deferredEvents )
        {
          _deferredEvents = new LinkedList<EntityChangeEvent>();
        }
        _deferredEvents.add( event );
      }
      else
      {
        doSendEvent( event );
      }
    }
  }

  /**
   * Deliver the events that were deferred due to pause or attempted delivery whilst already sending a message.
   */
  private void deliverDeferredEvents()
  {
    LinkedList<EntityChangeEvent> deferredEvents = _deferredEvents;
    _deferredEvents = null;
    if( null != deferredEvents )
    {
      for( final EntityChangeEvent event : deferredEvents )
      {
        doSendEvent( event );
      }
    }
  }

  /**
   * Perform the sending of message to all interested listeners.
   * After message delivery it will register any deferred listeners and deliver ay deferred events.
   *
   * @param event the event to deliver.
   */
  private void doSendEvent( final EntityChangeEvent event )
  {
    logEventSend( event );
    _sending = true;
    final Object object = event.getObject();

    doSendEvent( _globalListeners, event );
    doSendEvent( getListeners( _objectListeners, object ), event );

    Class clazz = object.getClass();
    while( clazz != Object.class )
    {
      doSendEvent( getListeners( _classListeners, clazz ), event );
      clazz = clazz.getSuperclass();
    }
    _sending = false;
    applyDeferredListenerActions();
    deliverDeferredEvents();
  }

  /**
   * Method invoked to log the sending of an event.
   * Subclasses can override to control the logging.
   *
   * @param event the event to log.
   */
  protected void logEventSend( final EntityChangeEvent event )
  {
    if( LOG.isLoggable( Level.FINE ) )
    {
      LOG.fine( "Sending event " + event );
    }
  }

  /**
   * Apply all the deferred listener actions that occurred during event delivery.
   */
  private void applyDeferredListenerActions()
  {
    for( final DeferredListenerAction action : _deferredListenerActions )
    {
      final Object key = action.getKey();
      final EntityChangeListener listener = action.getListener();
      if( action.isRemove() )
      {
        if( null == key )
        {
          removeChangeListener( listener );
        }
        else if( key instanceof Class )
        {
          removeChangeListener( (Class) key, listener );
        }
        else
        {
          removeChangeListener( key, listener );
        }
      }
      else if( action.isAdd() )
      {
        if( null == key )
        {
          addChangeListener( listener );
        }
        else if( key instanceof Class )
        {
          addChangeListener( (Class) key, listener );
        }
        else
        {
          addChangeListener( key, listener );
        }
      }
      else
      {
        assert action.isPurge();
        purgeChangeListener( listener );
      }
    }
    _deferredListenerActions.clear();
  }

  /**
   * Send the event to each listener in the array of listeners.
   *
   * @param listeners the listeners.
   * @param event the event.
   */
  private void doSendEvent( final ListenerEntry[] listeners, final EntityChangeEvent event )
  {
    for( final ListenerEntry entry : listeners )
    {
      final EntityChangeListener listener = entry.getListener();
      final EntityChangeType type = event.getType();
      try
      {
        switch( type )
        {
          case ATTRIBUTE_CHANGED:
            listener.attributeChanged( event );
            break;
          case RELATED_ADDED:
            listener.relatedAdded( event );
            break;
          case RELATED_REMOVED:
            listener.relatedRemoved( event );
            break;
          case ENTITY_REMOVED:
            listener.entityRemoved( event );
            break;
          default:
            throw new IllegalStateException( "Unknown type: " + type );
        }
      }
      catch( final Throwable t )
      {
        logEventHandlingError( listener, t );
      }
    }
  }

  /**
   * Method for handling errors that arise during event handling.
   * Can be overridden by sub-classes.
   *
   * @param listener the listener that generated the exception.
   * @param t the exception.
   */
  protected void logEventHandlingError( final EntityChangeListener listener, final Throwable t )
  {
    final String message = "Error sending event to listener: " + listener;
    LOG.log( Level.SEVERE, message, t );
    if ( shouldRaiseErrorOnEventHandlerError() )
    {
      throw new IllegalStateException( message, t );
    }
  }

  /**
   * Retrieve the list of listeners for specified key otherwise return an empty array.
   *
   * @param map the map of listener lists.
   * @param key the key to access the map.
   * @param <T> the type of the key. (Either a Class or Object type)
   * @return the listeners.
   */
  private <T> ListenerEntry[] getListeners( final Map<T, ListenerEntry[]> map, final T key )
  {
    final ListenerEntry[] listeners = map.get( key );
    if( null == listeners )
    {
      return _emptyListenerSet;
    }
    else
    {
      return listeners;
    }
  }

  /**
   * Remove the listener from the list of listeners accessed by specified key.
   *
   * @param map the map of listener lists.
   * @param key the key to access the map.
   * @param listener the listener to remove.
   * @param <T> the type of the key. (Either a Class or Object type)
   */
  private <T> void removeChangeListener( final Map<T, ListenerEntry[]> map,
                                         final T key,
                                         final ListenerEntry listener )
  {
    final ListenerEntry[] listenersSet = getListeners( map, key );
    final ListenerEntry[] listeners = doRemoveChangeListener( listenersSet, listener );
    if( 0 == listeners.length )
    {
      map.remove( key );
    }
    else
    {
      map.put( key, listeners );
    }
  }

  /**
   * Remove the specified listener from the array and return the new array.
   * If the listener is not present in the array the original will be returned.
   *
   * @param listeners the array of listeners.
   * @param listener the listener to remove.
   * @return the new listener array sans the specified listener.
   */
  private ListenerEntry[] doRemoveChangeListener( final ListenerEntry[] listeners, final ListenerEntry listener )
  {
    for( int i = 0; i < listeners.length; i++ )
    {
      if( listener == listeners[ i ] )
      {
        if ( 1 == listeners.length )
        {
          return _emptyListenerSet;
        }
        else
        {
          final ListenerEntry[] results = new ListenerEntry[ listeners.length - 1 ];
          System.arraycopy( listeners, 0, results, 0, i );
          if ( i != listeners.length - 1 )
          {
            System.arraycopy( listeners, i + 1, results, i, listeners.length - i - 1 );
          }
          return results;
        }
      }
    }
    return listeners;
  }

  /**
   * Add the listener to the list of listeners accessed by specified key.
   *
   * @param map the map of listener lists.
   * @param key the key to access the map.
   * @param listener the listener to add.
   * @param <T> the type of the key. (Either a Class or Object type)
   */
  private <T> void addChangeListener( final Map<T, ListenerEntry[]> map,
                                      final T key,
                                      final ListenerEntry listener )
  {
    final ListenerEntry[] listenerSet = getListeners( map, key );
    final ListenerEntry[] listeners = doAddChangeListener( listenerSet, listener );
    map.put( key, listeners );
  }

  /**
   * Add the specified listener to the array and return the new array.
   * If the listener is already present in the array the original array will be returned.
   *
   * @param listeners the array of listeners.
   * @param listener the listener to add.
   * @return the new listener array with the specified listener added.
   */
  private ListenerEntry[] doAddChangeListener( final ListenerEntry[] listeners, final ListenerEntry listener )
  {
    for ( final ListenerEntry candidate : listeners )
    {
      if( listener == candidate )
      {
        return listeners;
      }
    }
    final ListenerEntry[] results = new ListenerEntry[ listeners.length + 1 ];
    System.arraycopy( listeners, 0, results, 0, listeners.length );
    results[ listeners.length ] = listener;
    return results;
  }

  /**
   * Return the listener entry for specified listener and create one if they do not exist.
   *
   * @param listener the listener.
   * @return the associated entry or newly created entry.
   */
  @Nonnull
  private ListenerEntry getEntryForListener( @Nonnull final EntityChangeListener listener )
  {
    ListenerEntry entry = _listenerEntries.get( listener );
    if ( null == entry )
    {
      entry = new ListenerEntry( listener );
      _listenerEntries.put( listener, entry );
    }
    return entry;
  }

  /**
   * Return the listener entry for specified listener if it exists.
   *
   * @param listener the listener.
   * @return the associated entry or null.
   */
  @Nullable
  public ListenerEntry findEntryForListener( @Nonnull final EntityChangeListener listener )
  {
    return _listenerEntries.get( listener );
  }

  /**
   * Remove specified entry if it no longer references any listeners.
   *
   * @param entry the entry.
   */
  private void removeEntryIfEmpty( final ListenerEntry entry )
  {
    if( entry.isEmpty() )
    {
      _listenerEntries.remove( entry.getListener() );
    }
  }
}
