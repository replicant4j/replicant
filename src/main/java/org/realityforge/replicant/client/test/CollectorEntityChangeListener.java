package org.realityforge.replicant.client.test;

import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.EntityChangeEvent;
import org.realityforge.replicant.client.EntityChangeListener;

/**
 * A simple EntityChangeListener implementation that stores all the events emitted.
 */
public class CollectorEntityChangeListener
  implements EntityChangeListener
{
  private final LinkedList<EntityChangeEvent> _entityAddedEvents = new LinkedList<>();
  private final LinkedList<EntityChangeEvent> _entityRemovedEvents = new LinkedList<>();
  private final LinkedList<EntityChangeEvent> _attributeChangedEvents = new LinkedList<>();
  private final LinkedList<EntityChangeEvent> _relatedAddedEvents = new LinkedList<>();
  private final LinkedList<EntityChangeEvent> _relatedRemovedEvents = new LinkedList<>();

  @Override
  public void entityAdded( @Nonnull final EntityChangeEvent event )
  {
    _entityAddedEvents.add( event );
  }

  @Override
  public void entityRemoved( @Nonnull final EntityChangeEvent event )
  {
    _entityRemovedEvents.add( event );
  }

  @Override
  public void attributeChanged( @Nonnull final EntityChangeEvent event )
  {
    _attributeChangedEvents.add( event );
  }

  @Override
  public void relatedAdded( @Nonnull final EntityChangeEvent event )
  {
    _relatedAddedEvents.add( event );
  }

  @Override
  public void relatedRemoved( @Nonnull final EntityChangeEvent event )
  {
    _relatedRemovedEvents.add( event );
  }

  @Nonnull
  public LinkedList<EntityChangeEvent> getEntityAddedEvents()
  {
    return _entityAddedEvents;
  }

  @Nonnull
  public List<EntityChangeEvent> getEntityRemovedEvents()
  {
    return _entityRemovedEvents;
  }

  @Nonnull
  public List<EntityChangeEvent> getAttributeChangedEvents()
  {
    return _attributeChangedEvents;
  }

  @Nonnull
  public List<EntityChangeEvent> getRelatedAddedEvents()
  {
    return _relatedAddedEvents;
  }

  @Nonnull
  public List<EntityChangeEvent> getRelatedRemovedEvents()
  {
    return _relatedRemovedEvents;
  }
}
