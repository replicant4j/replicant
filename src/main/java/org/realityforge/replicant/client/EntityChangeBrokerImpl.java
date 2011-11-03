package org.realityforge.replicant.client;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * A single threaded in-memory EntityChangeBroker implementation.
 */
@SuppressWarnings( { "JavaDoc" } )
public final class EntityChangeBrokerImpl
    implements EntityChangeBroker
{
  private static final Logger LOG = Logger.getLogger( EntityChangeBrokerImpl.class.getName() );

  private final EntityChangeListener[] _emptyListenerSet = new EntityChangeListener[ 0 ];

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

  private EntityChangeListener[] _globalListeners = _emptyListenerSet;
  private final Map<Object, EntityChangeListener[]> _objectListeners = new HashMap<Object, EntityChangeListener[]>();
  private final Map<Class, EntityChangeListener[]> _classListeners = new HashMap<Class, EntityChangeListener[]>();

  /**
   * {@inheritDoc}
   */
  @Override
  public final void addChangeListener( @Nonnull final EntityChangeListener listener )
  {
    if( isSending() )
    {
      _deferredListenerActions.add( new DeferredListenerAction( null, listener, false ) );
    }
    else
    {
      _globalListeners = doAddChangeListener( _globalListeners, listener );
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
      _deferredListenerActions.add( new DeferredListenerAction( clazz, listener, false ) );
    }
    else
    {
      addChangeListener( _classListeners, clazz, listener );
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
      _deferredListenerActions.add( new DeferredListenerAction( object, listener, false ) );
    }
    else
    {
      addChangeListener( _objectListeners, object, listener );
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
      _deferredListenerActions.add( new DeferredListenerAction( null, listener, true ) );
    }
    else
    {
      _globalListeners = doRemoveChangeListener( _globalListeners, listener );
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
      _deferredListenerActions.add( new DeferredListenerAction( object, listener, true ) );
    }
    else
    {
      removeChangeListener( _objectListeners, object, listener );
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
      _deferredListenerActions.add( new DeferredListenerAction( clazz, listener, true ) );
    }
    else
    {
      removeChangeListener( _classListeners, clazz, listener );
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
      throw new IllegalStateException( "Attempting to disabled already disabled broker" );
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
      throw new IllegalStateException( "Attempting to enable already enable broker" );
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
      else
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
    }
    _deferredListenerActions.clear();
  }

  /**
   * Send the event to each listener in the array of listeners.
   *
   * @param listeners the listeners.
   * @param event the event.
   */
  private void doSendEvent( final EntityChangeListener[] listeners, final EntityChangeEvent event )
  {
    for( final EntityChangeListener listener : listeners )
    {
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
    LOG.log( Level.SEVERE, "Error sending event to listener: " + listener, t );
  }

  /**
   * Retrieve the list of listeners for specified key otherwise return an empty array.
   *
   * @param map the map of listener lists.
   * @param key the key to access the map.
   * @param <T> the type of the key. (Either a Class or Object type)
   * @return the listeners.
   */
  private <T> EntityChangeListener[] getListeners( final Map<T, EntityChangeListener[]> map, final T key )
  {
    final EntityChangeListener[] listeners = map.get( key );
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
  private <T> void removeChangeListener( final Map<T, EntityChangeListener[]> map,
                                         final T key,
                                         final EntityChangeListener listener )
  {
    final EntityChangeListener[] listenersSet = getListeners( map, key );
    final EntityChangeListener[] listeners = doRemoveChangeListener( listenersSet, listener );
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
  private EntityChangeListener[] doRemoveChangeListener( final EntityChangeListener[] listeners,
                                                         final EntityChangeListener listener )
  {
    for( int i = 0; i < listeners.length; i++ )
    {
      if( listener == listeners[ i ] )
      {
        final EntityChangeListener[] results = new EntityChangeListener[ listeners.length - 1 ];
        System.arraycopy( listeners, 0, results, 0, i );
        if( i != listeners.length - 1 )
        {
          System.arraycopy( listeners, i + 1, results, i, listeners.length - i - 1 );
        }
        return results;
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
  private <T> void addChangeListener( final Map<T, EntityChangeListener[]> map,
                                      final T key,
                                      final EntityChangeListener listener )
  {
    final EntityChangeListener[] listenerSet = getListeners( map, key );
    final EntityChangeListener[] listeners = doAddChangeListener( listenerSet, listener );
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
  private EntityChangeListener[] doAddChangeListener( final EntityChangeListener[] listeners,
                                                      final EntityChangeListener listener )
  {
    for( final EntityChangeListener candidate : listeners )
    {
      if( listener == candidate )
      {
        return listeners;
      }
    }
    final EntityChangeListener[] results = new EntityChangeListener[ listeners.length + 1 ];
    System.arraycopy( listeners, 0, results, 0, listeners.length );
    results[ listeners.length ] = listener;
    return results;
  }
}
