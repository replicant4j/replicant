package org.realityforge.replicant.client;

import java.util.ArrayList;

class RecordingListener
    implements EntityChangeListener
{
  private final ArrayList<EntityChangeEvent> _entityRemovedEvents = new ArrayList<EntityChangeEvent>();
  private final ArrayList<EntityChangeEvent> _attributeChangedEvents = new ArrayList<EntityChangeEvent>();
  private final ArrayList<EntityChangeEvent> _relatedAddedEvents = new ArrayList<EntityChangeEvent>();
  private final ArrayList<EntityChangeEvent> _relatedRemovedEvents = new ArrayList<EntityChangeEvent>();

  public boolean hasRecordedEvents()
  {
    return !hasNoRecordedEvents();
  }

  private boolean hasNoRecordedEvents()
  {
    return 0 == _entityRemovedEvents.size() &&
           0 == _attributeChangedEvents.size() &&
           0 == _relatedAddedEvents.size() &&
           0 == _relatedRemovedEvents.size();
  }

  public void clear()
  {
    _entityRemovedEvents.clear();
    _attributeChangedEvents.clear();
    _relatedAddedEvents.clear();
    _relatedRemovedEvents.clear();
  }

  public void entityRemoved( final EntityChangeEvent event )
  {
    _entityRemovedEvents.add( event );
  }

  public void attributeChanged( final EntityChangeEvent event )
  {
    _attributeChangedEvents.add( event );
  }

  public void relatedAdded( final EntityChangeEvent event )
  {
    _relatedAddedEvents.add( event );
  }

  public void relatedRemoved( final EntityChangeEvent event )
  {
    _relatedRemovedEvents.add( event );
  }

  public ArrayList<EntityChangeEvent> getEntityRemovedEvents()
  {
    return _entityRemovedEvents;
  }

  public ArrayList<EntityChangeEvent> getAttributeChangedEvents()
  {
    return _attributeChangedEvents;
  }

  public ArrayList<EntityChangeEvent> getRelatedAddedEvents()
  {
    return _relatedAddedEvents;
  }

  public ArrayList<EntityChangeEvent> getRelatedRemovedEvents()
  {
    return _relatedRemovedEvents;
  }
}
