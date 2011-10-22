package org.realityforge.replicant.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

@SuppressWarnings( { "JavaDoc" } )
public final class EntityChangeBrokerImpl
    implements EntityChangeBroker
{
  private static final Logger LOG = Logger.getLogger( EntityChangeBrokerImpl.class.getName() );

  private final EntityChangeListener[] _emptyListenerSet = new EntityChangeListener[ 0 ];

  /**
   * Count of users who have asked this object to suspend event transmission.
   */
  private int _count;

  /**
   * Backlog of events we still have to send
   */
  private List<EntityChangeEvent> _deferredEvents;

  private EntityChangeListener[] _globalListeners = _emptyListenerSet;
  private final Map<Object, EntityChangeListener[]> _objectListeners = new HashMap<Object, EntityChangeListener[]>();
  private final Map<Object, EntityChangeListener[]> _classListeners = new HashMap<Object, EntityChangeListener[]>();

  /**
   * {@inheritDoc}
   */
  @Override
  public final void addChangeListener( @Nonnull final EntityChangeListener listener )
  {
    _globalListeners = doAddChangeListener( _globalListeners, listener );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void addChangeListener( @Nonnull final Class clazz,
                                       @Nonnull final EntityChangeListener listener )
  {
    addChangeListener( _classListeners, clazz, listener );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void addChangeListener( @Nonnull final Object object, @Nonnull final EntityChangeListener listener )
  {
    addChangeListener( _objectListeners, object, listener );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void removeChangeListener( @Nonnull final EntityChangeListener listener )
  {
    _globalListeners = doRemoveAttributeChangeListener( _globalListeners, listener );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void removeChangeListener( @Nonnull final Object object, @Nonnull final EntityChangeListener listener )
  {
    removeChangeListener( _objectListeners, object, listener );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void removeChangeListener( @Nonnull final Class clazz,
                                          @Nonnull final EntityChangeListener listener )
  {
    removeChangeListener( _classListeners, clazz, listener );
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
  public void entityRemoved( @Nonnull final Object entity )
  {
    sendEvent( new EntityChangeEvent( EntityChangeType.ENTITY_REMOVED, entity, null, null ) );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void activate()
  {
    _count--;
    if( isActive() )
    {

      EntityChangeEvent[] deferredEvents = null;
      if( null != _deferredEvents )
      {
        deferredEvents = _deferredEvents.toArray( new EntityChangeEvent[ _deferredEvents.size() ] );
        _deferredEvents = null;
      }

      if( deferredEvents != null )
      {
        for( final EntityChangeEvent event : deferredEvents )
        {
          doSendEvent( event );
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void deactivate()
  {
    _count++;
  }

  private boolean isActive()
  {
    return _count == 0;
  }

  private void sendEvent( final EntityChangeEvent event )
  {
    if( isActive() )
    {
      doSendEvent( event );
    }
    else
    {
      if( null == _deferredEvents )
      {
        _deferredEvents = new ArrayList<EntityChangeEvent>();
      }
      _deferredEvents.add( event );
    }
  }

  private void doSendEvent( final EntityChangeEvent event )
  {
    if( LOG.isLoggable( Level.FINE ) )
    {
      LOG.fine( "Sending event " + event );
    }

    final Object object = event.getObject();

    // Cache all listeners
    final ArrayList<EntityChangeListener[]> classListenersCopy = new ArrayList<EntityChangeListener[]>();
    final EntityChangeListener[] listenersCopy = copyListeners( _globalListeners );
    final EntityChangeListener[] objectListenersCopy = copyListeners( getListeners( _objectListeners, object ) );

    Class clazz = object.getClass();
    while( clazz != Object.class )
    {
      classListenersCopy.add( copyListeners( getListeners( _classListeners, clazz ) ) );
      clazz = clazz.getSuperclass();
    }

    doSendEvent( listenersCopy, event );
    doSendEvent( objectListenersCopy, event );

    clazz = object.getClass();
    int i = 0;
    while( clazz != Object.class )
    {
      doSendEvent( classListenersCopy.get( i ), event );
      i++;
      clazz = clazz.getSuperclass();
    }
  }

  private void doSendEvent( final EntityChangeListener[] listenersCopy,
                            final EntityChangeEvent event )
  {
    for( final EntityChangeListener listener : listenersCopy )
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
        LOG.log( Level.SEVERE, "Error sending event to listener: " + listener, t );
      }
    }
  }

  private EntityChangeListener[] copyListeners( final EntityChangeListener[] listeners )
  {
    final EntityChangeListener[] listenersCopy = new EntityChangeListener[ listeners.length ];
    System.arraycopy( listeners, 0, listenersCopy, 0, listeners.length );
    return listenersCopy;
  }

  private EntityChangeListener[] getListeners( final Map<Object, EntityChangeListener[]> map, final Object object )
  {
    final EntityChangeListener[] listeners = map.get( object );
    if( null == listeners )
    {
      return _emptyListenerSet;
    }
    else
    {
      return listeners;
    }
  }

  private EntityChangeListener[] doAddChangeListener( final EntityChangeListener[] listeners,
                                                      final EntityChangeListener listener )
  {
    final ArrayList<EntityChangeListener> list = new ArrayList<EntityChangeListener>( listeners.length + 1 );
    list.addAll( Arrays.asList( listeners ) );
    if( !list.contains( listener ) )
    {
      list.add( listener );
    }
    return list.toArray( new EntityChangeListener[ list.size() ] );
  }

  private EntityChangeListener[] doRemoveAttributeChangeListener( final EntityChangeListener[] listeners,
                                                                  final EntityChangeListener listener )
  {
    final ArrayList<EntityChangeListener> list = new ArrayList<EntityChangeListener>( listeners.length );
    list.addAll( Arrays.asList( listeners ) );
    list.remove( listener );
    return list.toArray( new EntityChangeListener[ list.size() ] );
  }

  private void removeChangeListener( final Map<Object, EntityChangeListener[]> map,
                                     final Object object,
                                     final EntityChangeListener listener )
  {
    final EntityChangeListener[] listenersSet = getListeners( map, object );
    final EntityChangeListener[] listeners = doRemoveAttributeChangeListener( listenersSet, listener );
    if( 0 == listeners.length )
    {
      map.remove( object );
    }
    else
    {
      map.put( object, listeners );
    }
  }

  private void addChangeListener( final Map<Object, EntityChangeListener[]> map,
                                  final Object object,
                                  final EntityChangeListener listener )
  {
    final EntityChangeListener[] listenerSet = getListeners( map, object );
    final EntityChangeListener[] listeners = doAddChangeListener( listenerSet, listener );
    map.put( object, listeners );
  }
}
