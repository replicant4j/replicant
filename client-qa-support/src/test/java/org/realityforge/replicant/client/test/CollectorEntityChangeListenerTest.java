package org.realityforge.replicant.client.test;

import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityChangeBrokerImpl;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class CollectorEntityChangeListenerTest
{
  @Test
  public void basicOperation()
  {
    final EntityChangeBroker broker = new EntityChangeBrokerImpl();
    final CollectorEntityChangeListener l = new CollectorEntityChangeListener();
    broker.addChangeListener( l );

    final Object entity = new Object();

    assertEvents( l, 0, 0, 0, 0, 0 );

    broker.entityAdded( entity );
    assertEvents( l, 1, 0, 0, 0, 0 );

    broker.entityRemoved( entity );
    assertEvents( l, 1, 1, 0, 0, 0 );

    broker.attributeChanged( entity, "X", "v2" );
    assertEvents( l, 1, 1, 1, 0, 0 );

    broker.relatedAdded( entity, "Rel", entity );
    assertEvents( l, 1, 1, 1, 1, 0 );

    broker.relatedRemoved( entity, "Rel", entity );
    assertEvents( l, 1, 1, 1, 1, 1 );
  }

  protected void assertEvents( final CollectorEntityChangeListener l,
                               final int entityAdded,
                               final int entityRemoved,
                               final int attributeChanged, final int relatedAdded, final int relatedRemoved )
  {
    assertEquals( l.getEntityAddedEvents().size(), entityAdded );
    assertEquals( l.getEntityRemovedEvents().size(), entityRemoved );
    assertEquals( l.getAttributeChangedEvents().size(), attributeChanged );
    assertEquals( l.getRelatedAddedEvents().size(), relatedAdded );
    assertEquals( l.getRelatedRemovedEvents().size(), relatedRemoved );
  }
}
