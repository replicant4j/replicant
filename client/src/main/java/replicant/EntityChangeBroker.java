package replicant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.base.Js;
import replicant.DeferredListenerAction.ActionType;
import static org.realityforge.braincheck.Guards.*;

/**
 * A broker for emitting events when changes occurs.
 * This functionality is based on replicant version5 and earlier mechanisms for propagating entity changes and
 * should not be used for new code.
 */
public final class EntityChangeBroker
{
  @Nonnull
  private final EntityChangeEmitter _emitter = new EntityChangeEmitterImpl( this );
  @Nonnull
  private final ListenerEntry[] _emptyListenerSet = new ListenerEntry[ 0 ];
  @Nullable
  private EntityBrokerLock _lock;
  private boolean _sending;
  /**
   * List of listeners that we either have to add or removed after message delivery completes.
   */
  @Nonnull
  private final List<DeferredListenerAction> _deferredListenerActions = new LinkedList<>();
  /**
   * Backlog of events we still have to send.
   */
  @Nullable
  private List<EntityChangeEvent> _deferredEvents;
  @Nonnull
  private ListenerEntry[] _globalListeners = _emptyListenerSet;
  @Nonnull
  private final Map<EntityChangeListener, ListenerEntry> _listenerEntries = new HashMap<>();
  @Nonnull
  private final Map<Object, ListenerEntry[]> _objectListeners = new HashMap<>();
  @Nonnull
  private final Map<Class<?>, ListenerEntry[]> _classListeners = new HashMap<>();

  EntityChangeBroker()
  {
    Js.debugger();
    if ( !ClassMetaDataCheck.isClassMetadataEnabled() )
    {
      throw new IllegalStateException( "Attempting to compile replicant with replicant.enable_change_broker set to true but the compiler was passed -XdisableClassMetadata that strips the metadata required for this functionality" );
    }
  }

  @Nonnull
  EntityChangeEmitter getEmitter()
  {
    return _emitter;
  }

  /**
   * Add a listener that receives all change messages.
   *
   * @param listener the EntityChangeListener
   */
  public void addChangeListener( @Nonnull final EntityChangeListener listener )
  {
    if ( isSending() )
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
   * Add a listener that receives all change messages for models of a particular type and all sub-types.
   *
   * @param clazz    the type to subscribe to.
   * @param listener the EntityChangeListener
   */
  public void addChangeListener( @Nonnull final Class<?> clazz,
                                 @Nonnull final EntityChangeListener listener )
  {
    if ( isSending() )
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
   * Add a listener that receives all change messages for a particular entity.
   *
   * @param entity   the entity to subscribe to.
   * @param listener the EntityChangeListener
   */
  public void addChangeListener( @Nonnull final Object entity, @Nonnull final EntityChangeListener listener )
  {
    if ( isSending() )
    {
      _deferredListenerActions.add( new DeferredListenerAction( entity, listener, ActionType.ADD ) );
    }
    else
    {
      final ListenerEntry entry = getEntryForListener( listener );
      addChangeListener( _objectListeners, entity, entry );
      entry.interestedInstanceSet().add( entity );
    }
  }

  /**
   * Remove a listener that receives all change messages.
   *
   * @param listener the EntityChangeListener
   */
  public void removeChangeListener( @Nonnull final EntityChangeListener listener )
  {
    if ( isSending() )
    {
      _deferredListenerActions.add( new DeferredListenerAction( null, listener, ActionType.REMOVE ) );
    }
    else
    {
      final ListenerEntry entry = findEntryForListener( listener );
      if ( null != entry )
      {
        _globalListeners = doRemoveChangeListener( _globalListeners, entry );
        entry.setGlobalListener( false );
        removeEntryIfEmpty( entry );
      }
    }
  }

  /**
   * Remove a listener that receives all change messages for a particular entity.
   *
   * @param entity   the entity to subscribe to.
   * @param listener the EntityChangeListener
   */
  public void removeChangeListener( @Nonnull final Object entity, @Nonnull final EntityChangeListener listener )
  {
    if ( isSending() )
    {
      _deferredListenerActions.add( new DeferredListenerAction( entity, listener, ActionType.REMOVE ) );
    }
    else
    {
      final ListenerEntry entry = findEntryForListener( listener );
      if ( null != entry )
      {
        doRemoveObjectChangeListener( entity, entry );
      }
    }
  }

  /**
   * Remove the object change listener associated with specified entry and entity.
   */
  private void doRemoveObjectChangeListener( @Nonnull final Object entity, @Nonnull final ListenerEntry entry )
  {
    removeChangeListener( _objectListeners, entity, entry );
    entry.interestedInstanceSet().remove( entity );
    removeEntryIfEmpty( entry );
  }

  public void removeAllChangeListeners( @Nonnull final Object object )
  {
    if ( isSending() )
    {
      _deferredListenerActions.add( new DeferredListenerAction( object, null, ActionType.REMOVE ) );
    }
    else
    {
      final ListenerEntry[] listenerEntries = _objectListeners.get( object );
      if ( null != listenerEntries )
      {
        for ( final ListenerEntry entry : listenerEntries )
        {
          doRemoveObjectChangeListener( object, entry );
        }
      }
    }
  }

  /**
   * Remove a listener that receives all change messages for models of a particular type and all sub-types.
   *
   * @param clazz    the type to subscribe to.
   * @param listener the EntityChangeListener
   */
  public void removeChangeListener( @Nonnull final Class<?> clazz,
                                    @Nonnull final EntityChangeListener listener )
  {
    if ( isSending() )
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
   * Remove listener from listening to any changes.
   *
   * @param listener the EntityChangeListener
   */
  public void purgeChangeListener( @Nonnull final EntityChangeListener listener )
  {
    if ( isSending() )
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
        final Set<Class<?>> types = entry.interestedTypeSet();
        final Set<Class<?>> typesToRemove = types.size() > 1 ? new HashSet<>( types ) : types;
        for ( final Class<?> type : typesToRemove )
        {
          removeChangeListener( type, listener );
        }
        final Set<Object> instances = entry.interestedInstanceSet();
        final Set<Object> instancesToRemove = instances.size() > 1 ? new HashSet<>( instances ) : instances;
        for ( final Object instance : instancesToRemove )
        {
          removeChangeListener( instance, listener );
        }
      }
    }
  }

  /**
   * Resume the broker.
   *
   * <p>Any changes that have been delivered since pause has been invoked will be delivered on resume.</p>
   */
  void resume()
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> null != _lock,
                    () -> "Replicant-0110: EntityChangeBroker.resume invoked but no lock is present." );
      assert null != _lock;
      apiInvariant( () -> _lock.isPauseAction(),
                    () -> "Replicant-0111: EntityChangeBroker.resume invoked but lock is of the incorrect type." );
    }
    _lock = null;
    deliverDeferredEvents();
  }

  /**
   * Pause the broker.
   *
   * <p>Changes sent to the broker while it is paused will be cached and transmitted when it is resumed.</p>
   *
   * @return the transaction created by action.
   */
  @Nonnull
  public EntityBrokerLock pause()
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> null == _lock,
                    () -> "Replicant-0112: EntityChangeBroker.pause invoked but lock is present." );
    }
    _lock = new EntityBrokerLock( this, false );
    return _lock;
  }

  /**
   * @return true if the broker is paused.
   */
  public boolean isPaused()
  {
    return null != _lock && _lock.isPauseAction();
  }

  /**
   * Disable the transmission of changes to listeners.
   *
   * <p>Changes sent to the broker while it is disabled will be discarded.</p>
   *
   * @return the transaction created by action.
   */
  @Nonnull
  EntityBrokerLock disable()
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> null == _lock,
                    () -> "Replicant-0112: EntityChangeBroker.disable invoked but lock is present." );
    }
    _lock = new EntityBrokerLock( this, true );
    return _lock;
  }

  /**
   * Re-enable the transmission of changes to listeners after a disable.
   */
  void enable()
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> null != _lock,
                    () -> "Replicant-0110: EntityChangeBroker.enable invoked but no lock is present." );
      assert null != _lock;
      apiInvariant( () -> _lock.isDisableAction(),
                    () -> "Replicant-0111: EntityChangeBroker.enable invoked but lock is of the incorrect type." );
    }
    _lock = null;
  }

  /**
   * @return true if the broker is enabled.
   */
  public boolean isEnabled()
  {
    return null == _lock || !_lock.isDisableAction();
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
  void sendEvent( @Nonnull final EntityChangeEvent event )
  {
    if ( isEnabled() )
    {
      if ( isPaused() || isSending() )
      {
        if ( null == _deferredEvents )
        {
          _deferredEvents = new LinkedList<>();
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
    List<EntityChangeEvent> deferredEvents = _deferredEvents;
    _deferredEvents = null;
    if ( null != deferredEvents )
    {
      for ( final EntityChangeEvent event : deferredEvents )
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
  private void doSendEvent( @Nonnull final EntityChangeEvent event )
  {
    _sending = true;
    final Object object = event.getObject();

    doSendEvent( _globalListeners, event );
    doSendEvent( getListeners( _objectListeners, object ), event );

    Class<?> clazz = object.getClass();
    while ( null != clazz && clazz != Object.class )
    {
      doSendEvent( getListeners( _classListeners, clazz ), event );

      // getSuperclass() will return NULL when -XdisableClassMetadata is passed to the compiler
      // When we use Arez entities, the actual class of the entities are something like com.biz.Arez_MyEntity
      // but the application code will add listeners for the public type com.biz.MyEntity as
      // com.biz.Arez_MyEntity is generated code and package access. Thus the EntityChangeBroker
      // will fail to send messages when compiled with -XdisableClassMetadata for these entities.
      clazz = clazz.getSuperclass();
    }
    _sending = false;
    applyDeferredListenerActions();
    deliverDeferredEvents();
  }

  /**
   * Apply all the deferred listener actions that occurred during event delivery.
   */
  private void applyDeferredListenerActions()
  {
    for ( final DeferredListenerAction action : _deferredListenerActions )
    {
      final Object key = action.getKey();
      final EntityChangeListener listener = action.getListener();
      if ( null == listener )
      {
        assert action.isRemove();
        assert null != key;
        removeAllChangeListeners( key );
      }
      else if ( action.isRemove() )
      {
        if ( null == key )
        {
          removeChangeListener( listener );
        }
        else if ( key instanceof Class )
        {
          removeChangeListener( (Class<?>) key, listener );
        }
        else
        {
          removeChangeListener( key, listener );
        }
      }
      else if ( action.isAdd() )
      {
        if ( null == key )
        {
          addChangeListener( listener );
        }
        else if ( key instanceof Class )
        {
          addChangeListener( (Class<?>) key, listener );
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
   * @param event     the event.
   */
  private void doSendEvent( @Nonnull final ListenerEntry[] listeners, @Nonnull final EntityChangeEvent event )
  {
    for ( final ListenerEntry entry : listeners )
    {
      final EntityChangeListener listener = entry.getListener();
      final EntityChangeEvent.Type type = event.getType();
      try
      {
        if ( EntityChangeEvent.Type.ATTRIBUTE_CHANGED == type )
        {
          listener.attributeChanged( event );
        }
        else if ( EntityChangeEvent.Type.RELATED_ADDED == type )
        {
          listener.relatedAdded( event );
        }
        else if ( EntityChangeEvent.Type.RELATED_REMOVED == type )
        {
          listener.relatedRemoved( event );
        }
        else if ( EntityChangeEvent.Type.ENTITY_ADDED == type )
        {
          listener.entityAdded( event );
        }
        else
        {
          assert EntityChangeEvent.Type.ENTITY_REMOVED == type;
          listener.entityRemoved( event );
        }
      }
      catch ( final Throwable t )
      {
        if ( Replicant.shouldCheckInvariants() )
        {
          fail( () -> "Replicant-0010: Error sending event to listener: " + listener + " Error: " + t );
        }
        else
        {
          ReplicantLogger.log( "Replicant-0010: Error sending event to listener: " + listener, t );
        }
      }
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
    if ( null == listeners )
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
   * @param map      the map of listener lists.
   * @param key      the key to access the map.
   * @param listener the listener to remove.
   * @param <T>      the type of the key. (Either a Class or Object type)
   */
  private <T> void removeChangeListener( @Nonnull final Map<T, ListenerEntry[]> map,
                                         @Nonnull final T key,
                                         @Nonnull final ListenerEntry listener )
  {
    final ListenerEntry[] listenersSet = getListeners( map, key );
    final ListenerEntry[] listeners = doRemoveChangeListener( listenersSet, listener );
    if ( 0 == listeners.length )
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
   * @param listener  the listener to remove.
   * @return the new listener array sans the specified listener.
   */
  private ListenerEntry[] doRemoveChangeListener( final ListenerEntry[] listeners, final ListenerEntry listener )
  {
    for ( int i = 0; i < listeners.length; i++ )
    {
      if ( listener == listeners[ i ] )
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
   * @param map      the map of listener lists.
   * @param key      the key to access the map.
   * @param listener the listener to add.
   * @param <T>      the type of the key. (Either a Class or Object type)
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
   * @param listener  the listener to add.
   * @return the new listener array with the specified listener added.
   */
  private ListenerEntry[] doAddChangeListener( final ListenerEntry[] listeners, final ListenerEntry listener )
  {
    for ( final ListenerEntry candidate : listeners )
    {
      if ( listener == candidate )
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
    return _listenerEntries.computeIfAbsent( listener, k -> new ListenerEntry( listener ) );
  }

  /**
   * Return the listener entry for specified listener if it exists.
   *
   * @param listener the listener.
   * @return the associated entry or null.
   */
  @Nullable
  private ListenerEntry findEntryForListener( @Nonnull final EntityChangeListener listener )
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
    if ( entry.isEmpty() )
    {
      _listenerEntries.remove( entry.getListener() );
    }
  }
}
