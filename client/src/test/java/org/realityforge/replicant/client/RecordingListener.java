package org.realityforge.replicant.client;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

class RecordingListener
  implements EntityChangeListener
{
  @Nonnull
  private final List<EntityChangeEvent> _entityAddedEvents = new ArrayList<>();
  @Nonnull
  private final List<EntityChangeEvent> _entityRemovedEvents = new ArrayList<>();
  @Nonnull
  private final List<EntityChangeEvent> _attributeChangedEvents = new ArrayList<>();
  @Nonnull
  private final List<EntityChangeEvent> _relatedAddedEvents = new ArrayList<>();
  @Nonnull
  private final List<EntityChangeEvent> _relatedRemovedEvents = new ArrayList<>();

  boolean hasNoRecordedEvents()
  {
    return 0 == _entityAddedEvents.size() &&
           0 == _entityRemovedEvents.size() &&
           0 == _attributeChangedEvents.size() &&
           0 == _relatedAddedEvents.size() &&
           0 == _relatedRemovedEvents.size();
  }

  void clear()
  {
    _entityAddedEvents.clear();
    _entityRemovedEvents.clear();
    _attributeChangedEvents.clear();
    _relatedAddedEvents.clear();
    _relatedRemovedEvents.clear();
  }

  @Override
  public void entityAdded( @Nonnull final EntityChangeEvent event )
  {
    _entityAddedEvents.add( event );
  }

  public void entityRemoved( @Nonnull final EntityChangeEvent event )
  {
    _entityRemovedEvents.add( event );
  }

  public void attributeChanged( @Nonnull final EntityChangeEvent event )
  {
    _attributeChangedEvents.add( event );
  }

  public void relatedAdded( @Nonnull final EntityChangeEvent event )
  {
    _relatedAddedEvents.add( event );
  }

  public void relatedRemoved( @Nonnull final EntityChangeEvent event )
  {
    _relatedRemovedEvents.add( event );
  }

  @Nonnull
  List<EntityChangeEvent> getEntityAddedEvents()
  {
    return _entityAddedEvents;
  }

  @Nonnull
  List<EntityChangeEvent> getEntityRemovedEvents()
  {
    return _entityRemovedEvents;
  }

  @Nonnull
  List<EntityChangeEvent> getAttributeChangedEvents()
  {
    return _attributeChangedEvents;
  }

  @Nonnull
  List<EntityChangeEvent> getRelatedAddedEvents()
  {
    return _relatedAddedEvents;
  }

  @Nonnull
  List<EntityChangeEvent> getRelatedRemovedEvents()
  {
    return _relatedRemovedEvents;
  }
}
