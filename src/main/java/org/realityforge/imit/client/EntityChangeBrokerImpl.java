package org.realityforge.imit.client;

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
  private final Map<Object, Map<Object, EntityChangeListener[]>> _featureListeners = new HashMap<Object, Map<Object, EntityChangeListener[]>>();
  private final Map<Object, EntityChangeListener[]> _classListeners = new HashMap<Object, EntityChangeListener[]>();
  private final Map<Object, Map<Object, EntityChangeListener[]>> _classFeatureListeners = new HashMap<Object, Map<Object, EntityChangeListener[]>>();

  @Override
  public final void addChangeListener( @Nonnull final EntityChangeListener listener )
  {
    _globalListeners = doAddChangeListener( getGlobalListeners(), listener );
  }

  @Override
  public final void addChangeListener( @Nonnull final Class clazz, @Nonnull final EntityChangeListener listener )
  {
    addChangeListener( _classListeners, clazz, listener );
  }

  @Override
  public final void addChangeListener( @Nonnull final Object object, @Nonnull final EntityChangeListener listener )
  {
    addChangeListener( _objectListeners, object, listener );
  }

  @Override
  public final void addAttributeChangeListener( @Nonnull final Object object,
                                                @Nonnull final String feature,
                                                @Nonnull final EntityChangeListener listener )
  {
    addChangeListener( _featureListeners, object, feature, listener );
  }

  @Override
  public final void addAttributeChangeListener( @Nonnull final Class clazz,
                                                @Nonnull final String feature,
                                                @Nonnull final EntityChangeListener listener )
  {
    addChangeListener( _classFeatureListeners, clazz, feature, listener );
  }

  @Override
  public final void removeChangeListener( @Nonnull final EntityChangeListener listener )
  {
    _globalListeners = doRemoveAttributeChangeListener( getGlobalListeners(), listener );
  }

  @Override
  public final void removeChangeListener( @Nonnull final Object object, @Nonnull final EntityChangeListener listener )
  {
    removeChangeListener( _objectListeners, object, listener );
  }

  @Override
  public final void removeChangeListener( @Nonnull final Class clazz, @Nonnull final EntityChangeListener listener )
  {
    removeChangeListener( _classListeners, clazz, listener );
  }

  @Override
  public final void removeAttributeChangeListener( @Nonnull final Class clazz,
                                                   @Nonnull final String name,
                                                   @Nonnull final EntityChangeListener listener )
  {
    removeChangeListener( _classFeatureListeners, clazz, name, listener );
  }

  @Override
  public final void removeAttributeChangeListener( @Nonnull final Object object,
                                                   @Nonnull final String name,
                                                   @Nonnull final EntityChangeListener listener )
  {
    removeChangeListener( _featureListeners, object, name, listener );
  }

  @Override
  public void attributeChanged( @Nonnull final Object entity, @Nonnull final String name, @Nonnull final Object value )
  {
    sendEvent( new EntityChangeEvent( EntityChangeType.ATTRIBUTE_CHANGED, entity, name, value ) );
  }

  @Override
  public void entityRemoved( @Nonnull final Object entity )
  {
    sendEvent( new EntityChangeEvent( EntityChangeType.ENTITY_REMOVED, entity ) );
  }

  private boolean isActive()
  {
    return _count == 0;
  }

  @Override
  public final void activate()
  {
    _count--;
    if ( isActive() )
    {

      EntityChangeEvent[] deferredEvents = null;
      if ( null != _deferredEvents )
      {
        deferredEvents = _deferredEvents.toArray( new EntityChangeEvent[ _deferredEvents.size() ] );
        _deferredEvents = null;
      }

      if ( deferredEvents != null )
      {
        for ( final EntityChangeEvent event : deferredEvents )
        {
          doSendEvent( event );
        }
      }
    }
  }

  @Override
  public final void deactivate()
  {
    _count++;
  }

  private void sendEvent( final EntityChangeEvent event )
  {
    if ( isActive() )
    {
      doSendEvent( event );
    }
    else
    {
      if ( null == _deferredEvents )
      {
        _deferredEvents = new ArrayList<EntityChangeEvent>();
      }
      _deferredEvents.add( event );
    }
  }

  private void doSendEvent( final EntityChangeEvent event )
  {
    if ( LOG.isLoggable( Level.FINE ) )
    {
      LOG.fine( "Sending event " + event );
    }

    final Object object = event.getObject();
    final EntityChangeType type = event.getType();

    String name = event.getName();

    if ( type == EntityChangeType.RELATED_ADDED || type == EntityChangeType.RELATED_REMOVED )
    {
      final Object other = event.getValue();
      name = other.getClass().getName() + name;
    }

    Class clazz;

    // Cache all listeners
    final ArrayList<EntityChangeListener[]> classListenersCopy = new ArrayList<EntityChangeListener[]>();
    final ArrayList<EntityChangeListener[]> classFeatureListenersCopy = new ArrayList<EntityChangeListener[]>();
    final EntityChangeListener[] listenersCopy = copyListeners( getGlobalListeners() );
    final EntityChangeListener[] objectListenersCopy = copyListeners( getListeners( _objectListeners, object ) );
    final EntityChangeListener[] featureListenersCopy =
      copyListeners( getListeners( _featureListeners, object, name ) );
    clazz = object.getClass();
    while ( clazz != Object.class )
    {
      classListenersCopy.add( copyListeners( getListeners( _classListeners, clazz ) ) );
      classFeatureListenersCopy.add( copyListeners( getListeners( _classFeatureListeners, clazz, name ) ) );
      clazz = clazz.getSuperclass();
    }

    doSendEvent( listenersCopy, event );
    doSendEvent( objectListenersCopy, event );
    doSendEvent( featureListenersCopy, event );

    clazz = object.getClass();
    int i = 0;
    while ( clazz != Object.class )
    {
      doSendEvent( classListenersCopy.get( i ), event );
      doSendEvent( classFeatureListenersCopy.get( i ), event );
      i++;
      clazz = clazz.getSuperclass();
    }
  }

  private void doSendEvent( final EntityChangeListener[] listenersCopy,
                            final EntityChangeEvent event )
  {
    for ( final EntityChangeListener listener : listenersCopy )
    {
      final EntityChangeType type = event.getType();
      try
      {
        switch ( type )
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
      catch ( final Throwable t )
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
    if ( null == listeners )
    {
      return _emptyListenerSet;
    }
    else
    {
      return listeners;
    }
  }

  private EntityChangeListener[] getListeners( final Map<Object, Map<Object, EntityChangeListener[]>> masterMap,
                                               final Object masterKey,
                                               final String feature )
  {
    final Map<Object, EntityChangeListener[]> map = masterMap.get( masterKey );
    if ( null == map )
    {
      return _emptyListenerSet;
    }
    else
    {
      final EntityChangeListener[] listeners = map.get( feature );
      if ( null == listeners )
      {
        return _emptyListenerSet;
      }
      else
      {
        return listeners;
      }
    }
  }

  private EntityChangeListener[] getGlobalListeners()
  {
    return _globalListeners;
  }

  private EntityChangeListener[] doAddChangeListener( final EntityChangeListener[] listeners,
                                                      final EntityChangeListener listener )
  {
    final List<EntityChangeListener> list = new ArrayList<EntityChangeListener>();
    list.addAll( Arrays.asList( listeners ) );
    if ( !list.contains( listener ) )
    {
      list.add( listener );
    }
    return list.toArray( new EntityChangeListener[ list.size() ] );
  }

  private EntityChangeListener[] doRemoveAttributeChangeListener( final EntityChangeListener[] listeners,
                                                                  final EntityChangeListener listener )
  {
    final List<EntityChangeListener> list = new ArrayList<EntityChangeListener>();
    list.addAll( Arrays.asList( listeners ) );
    list.remove( listener );
    return list.toArray( new EntityChangeListener[ list.size() ] );
  }

  private void removeChangeListener( final Map<Object, EntityChangeListener[]> map,
                                     final Object object,
                                     final EntityChangeListener listener )
  {
    final EntityChangeListener[] listenersSet = getListeners( map, object );
    final EntityChangeListener[] listeners = doRemoveAttributeChangeListener( listenersSet,
                                                                              listener );
    if ( 0 == listeners.length )
    {
      map.remove( object );
    }
    else
    {
      map.put( object, listeners );
    }
  }

  private void removeChangeListener( final Map<Object, Map<Object, EntityChangeListener[]>> masterMap,
                                     final Object object,
                                     final String feature,
                                     final EntityChangeListener listener )
  {
    final EntityChangeListener[] listenersSet = getListeners( masterMap, object, feature );
    final EntityChangeListener[] listeners = doRemoveAttributeChangeListener( listenersSet, listener );
    final Map<Object, EntityChangeListener[]> objectMap = masterMap.get( object );
    if ( null != objectMap )
    {
      if ( 0 == listeners.length )
      {
        objectMap.remove( object );
      }
      else
      {
        objectMap.put( object, listeners );
      }
    }
  }

  private void addChangeListener( final Map<Object, Map<Object, EntityChangeListener[]>> masterMap,
                                  final Object object,
                                  final String feature,
                                  final EntityChangeListener listener )
  {
    final EntityChangeListener[] listenerSet = getListeners( masterMap, object, feature );
    final EntityChangeListener[] listeners = doAddChangeListener( listenerSet, listener );
    Map<Object, EntityChangeListener[]> objectMap = masterMap.get( object );
    if ( null == objectMap )
    {
      objectMap = new HashMap<Object, EntityChangeListener[]>();
      masterMap.put( object, objectMap );
    }
    objectMap.put( feature, listeners );
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
