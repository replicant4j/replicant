package org.realityforge.replicant.client;

import java.util.ArrayList;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityChangeBrokerTest
{
  public static final String ATTR_KEY = "MyAttribute";
  public static final String REL_KEY = "MyRelationship";

  @Test
  public void ensureCanSendMessagesWhenNoListeners()
  {
    final EntityChangeBroker broker = new EntityChangeBrokerImpl();
    broker.entityRemoved( new A() );
    broker.attributeChanged( new A(), ATTR_KEY, 42 );
    broker.relatedAdded( new A(), REL_KEY, new B() );
    broker.relatedRemoved( new A(), REL_KEY, new B() );

    // We are really just ensuring that the above methods do no raise an exception
    assertTrue( true );
  }

  @Test
  public void ensureSubscribedListenersReceiveAppropriateMessages()
  {
    final B entity = new B();

    final EntityChangeBroker broker = new EntityChangeBrokerImpl();
    final RecordingListener globalListener = new RecordingListener();
    final RecordingListener typeListener = new RecordingListener();
    final RecordingListener parentTypeListener = new RecordingListener();
    final RecordingListener subTypeListener = new RecordingListener();
    final RecordingListener differentTypeListener = new RecordingListener();
    final RecordingListener instanceListener = new RecordingListener();

    broker.addChangeListener( globalListener );
    broker.addChangeListener( entity.getClass(), typeListener );
    broker.addChangeListener( entity.getClass().getSuperclass(), parentTypeListener );
    broker.addChangeListener( SubB.class, subTypeListener );
    broker.addChangeListener( C.class, differentTypeListener );
    broker.addChangeListener( entity, instanceListener );

    //Attr changed
    assertAttributeChangeEventCount( globalListener, 0 );
    assertAttributeChangeEventCount( typeListener, 0 );
    assertAttributeChangeEventCount( parentTypeListener, 0 );
    assertAttributeChangeEventCount( instanceListener, 0 );

    broker.attributeChanged( entity, ATTR_KEY, 42 );

    assertAttributeChangeEventCount( globalListener, 1 );
    assertAttributeChangedEvent( globalListener.getAttributeChangedEvents().get( 0 ), entity, ATTR_KEY, 42 );

    assertAttributeChangeEventCount( typeListener, 1 );
    assertAttributeChangedEvent( typeListener.getAttributeChangedEvents().get( 0 ), entity, ATTR_KEY, 42 );

    assertAttributeChangeEventCount( parentTypeListener, 1 );
    assertAttributeChangedEvent( parentTypeListener.getAttributeChangedEvents().get( 0 ), entity, ATTR_KEY, 42 );

    assertHasNoRecordedEvents( subTypeListener );

    assertHasNoRecordedEvents( differentTypeListener );

    assertAttributeChangeEventCount( instanceListener, 1 );
    assertAttributeChangedEvent( instanceListener.getAttributeChangedEvents().get( 0 ), entity, ATTR_KEY, 42 );

    globalListener.clear();
    typeListener.clear();
    parentTypeListener.clear();
    subTypeListener.clear();
    differentTypeListener.clear();
    instanceListener.clear();

    //Entity Removed
    assertEntityRemovedEventCount( globalListener, 0 );
    assertEntityRemovedEventCount( typeListener, 0 );
    assertEntityRemovedEventCount( parentTypeListener, 0 );
    assertEntityRemovedEventCount( instanceListener, 0 );

    broker.entityRemoved( entity );

    assertEntityRemovedEventCount( globalListener, 1 );
    assertEntityRemovedEvent( globalListener.getEntityRemovedEvents().get( 0 ), entity );

    assertEntityRemovedEventCount( typeListener, 1 );
    assertEntityRemovedEvent( typeListener.getEntityRemovedEvents().get( 0 ), entity );

    assertEntityRemovedEventCount( parentTypeListener, 1 );
    assertEntityRemovedEvent( parentTypeListener.getEntityRemovedEvents().get( 0 ), entity );

    assertHasNoRecordedEvents( subTypeListener );

    assertHasNoRecordedEvents( differentTypeListener );

    assertEntityRemovedEventCount( instanceListener, 1 );
    assertEntityRemovedEvent( instanceListener.getEntityRemovedEvents().get( 0 ), entity );

    broker.removeChangeListener( globalListener );
    broker.removeChangeListener( entity.getClass(), typeListener );
    broker.removeChangeListener( entity.getClass().getSuperclass(), parentTypeListener );
    broker.removeChangeListener( SubB.class, subTypeListener );
    broker.removeChangeListener( C.class, differentTypeListener );
    broker.removeChangeListener( entity, instanceListener );

    globalListener.clear();
    typeListener.clear();
    parentTypeListener.clear();
    subTypeListener.clear();
    differentTypeListener.clear();
    instanceListener.clear();

    broker.attributeChanged( entity, ATTR_KEY, 43 );

    assertHasNoRecordedEvents( globalListener );
    assertHasNoRecordedEvents( typeListener );
    assertHasNoRecordedEvents( parentTypeListener );
    assertHasNoRecordedEvents( subTypeListener );
    assertHasNoRecordedEvents( differentTypeListener );
    assertHasNoRecordedEvents( instanceListener );

    broker.entityRemoved( entity );

    assertHasNoRecordedEvents( globalListener );
    assertHasNoRecordedEvents( typeListener );
    assertHasNoRecordedEvents( parentTypeListener );
    assertHasNoRecordedEvents( subTypeListener );
    assertHasNoRecordedEvents( differentTypeListener );
    assertHasNoRecordedEvents( instanceListener );
  }

  @Test
  public void ensureDeferredMessagesAreDelivered()
  {
    final B entity = new B();
    final B other = new B();

    final EntityChangeBroker broker = new EntityChangeBrokerImpl();
    final RecordingListener globalListener = new RecordingListener();

    broker.addChangeListener( globalListener );

    //Attr changed
    assertAttributeChangeEventCount( globalListener, 0 );

    broker.pause();

    broker.attributeChanged( entity, ATTR_KEY, 42 );

    assertHasNoRecordedEvents( globalListener );

    //Allow the entities to flow
    broker.resume();

    assertAttributeChangeEventCount( globalListener, 1 );
    assertAttributeChangedEvent( globalListener.getAttributeChangedEvents().get( 0 ), entity, ATTR_KEY, 42 );

    globalListener.clear();

    //Entity Removed
    assertEntityRemovedEventCount( globalListener, 0 );

    broker.pause();

    broker.entityRemoved( entity );

    assertHasNoRecordedEvents( globalListener );

    broker.resume();

    assertEntityRemovedEventCount( globalListener, 1 );
    assertEntityRemovedEvent( globalListener.getEntityRemovedEvents().get( 0 ), entity );

    globalListener.clear();

    //Related Added
    assertRelatedAddedEventCount( globalListener, 0 );

    broker.pause();

    broker.relatedAdded( entity, REL_KEY, other );

    assertHasNoRecordedEvents( globalListener );

    broker.resume();

    assertRelatedAddedEventCount( globalListener, 1 );
    assertRelatedAddedEvent( globalListener.getRelatedAddedEvents().get( 0 ), entity, REL_KEY, other );

    globalListener.clear();

    //Related Removed
    assertRelatedRemovedEventCount( globalListener, 0 );

    broker.pause();

    broker.relatedRemoved( entity, REL_KEY, other );

    assertHasNoRecordedEvents( globalListener );

    broker.resume();

    assertRelatedRemovedEventCount( globalListener, 1 );
    assertRelatedRemovedEvent( globalListener.getRelatedRemovedEvents().get( 0 ), entity, REL_KEY, other );

  }

  @Test
  public void ensureMessagesAreNotDeliveredWhileDisabled()
  {
    final B entity = new B();
    final B other = new B();

    final EntityChangeBroker broker = new EntityChangeBrokerImpl();
    final RecordingListener globalListener = new RecordingListener();

    broker.addChangeListener( globalListener );

    //Attr changed
    assertAttributeChangeEventCount( globalListener, 0 );

    broker.disable();

    broker.attributeChanged( entity, ATTR_KEY, 42 );
    assertHasNoRecordedEvents( globalListener );

    broker.entityRemoved( entity );
    assertHasNoRecordedEvents( globalListener );

    broker.relatedAdded( entity, REL_KEY, other );
    assertHasNoRecordedEvents( globalListener );

    broker.relatedRemoved( entity, REL_KEY, other );
    assertHasNoRecordedEvents( globalListener );

    broker.enable();

    assertHasNoRecordedEvents( globalListener );
  }

  @Test
  public void ensureDuplicateAddsAndRemovesAreIgnored()
  {
    final B entity = new B();

    final EntityChangeBroker broker = new EntityChangeBrokerImpl();
    final RecordingListener listener = new RecordingListener();

    // Add the same listener twice
    broker.addChangeListener( listener );
    broker.addChangeListener( listener );

    broker.entityRemoved( entity );
    assertEntityRemovedEventCount( listener, 1 );

    listener.clear();

    //One remove is enough to clear it out
    broker.removeChangeListener( listener );

    broker.entityRemoved( entity );
    assertEntityRemovedEventCount( listener, 0 );

    // duplicate remove does not cause an error
    broker.removeChangeListener( listener );


    // Add the same listener twice
    broker.addChangeListener( entity, listener );
    broker.addChangeListener( entity, listener );

    broker.entityRemoved( entity );
    assertEntityRemovedEventCount( listener, 1 );

    listener.clear();

    //One remove is enough to clear it out
    broker.removeChangeListener( entity, listener );

    broker.entityRemoved( entity );
    assertEntityRemovedEventCount( listener, 0 );

    // duplicate remove does not cause an error
    broker.removeChangeListener( entity, listener );

    // Add the same listener twice
    broker.addChangeListener( B.class, listener );
    broker.addChangeListener( B.class, listener );

    broker.entityRemoved( entity );
    assertEntityRemovedEventCount( listener, 1 );

    listener.clear();

    //One remove is enough to clear it out
    broker.removeChangeListener( B.class, listener );

    broker.entityRemoved( entity );
    assertEntityRemovedEventCount( listener, 0 );

    // duplicate remove does not cause an error
    broker.removeChangeListener( B.class, listener );
  }

  @Test
  public void ensureMultipleListenersOfSameTypeAllReceiveMessages()
  {
    final B entity = new B();

    final EntityChangeBroker broker = new EntityChangeBrokerImpl();
    final RecordingListener listener1 = new RecordingListener();
    final RecordingListener listener2 = new RecordingListener();
    final RecordingListener listener3 = new RecordingListener();
    final RecordingListener listener4 = new RecordingListener();
    final RecordingListener listener5 = new RecordingListener();

    broker.addChangeListener( listener1 );
    broker.addChangeListener( listener2 );
    broker.addChangeListener( listener3 );
    broker.addChangeListener( listener4 );
    broker.addChangeListener( listener5 );

    broker.entityRemoved( entity );
    assertEntityRemovedEventCount( listener1, 1 );
    assertEntityRemovedEventCount( listener2, 1 );
    assertEntityRemovedEventCount( listener3, 1 );
    assertEntityRemovedEventCount( listener4, 1 );
    assertEntityRemovedEventCount( listener5, 1 );

    listener1.clear();
    listener2.clear();
    listener3.clear();
    listener4.clear();
    listener5.clear();

    broker.removeChangeListener( listener3 );

    broker.entityRemoved( entity );
    assertEntityRemovedEventCount( listener1, 1 );
    assertEntityRemovedEventCount( listener2, 1 );
    assertEntityRemovedEventCount( listener3, 0 );
    assertEntityRemovedEventCount( listener4, 1 );
    assertEntityRemovedEventCount( listener5, 1 );

    listener1.clear();
    listener2.clear();
    listener3.clear();
    listener4.clear();
    listener5.clear();

    broker.removeChangeListener( listener5 );

    broker.entityRemoved( entity );
    assertEntityRemovedEventCount( listener1, 1 );
    assertEntityRemovedEventCount( listener2, 1 );
    assertEntityRemovedEventCount( listener3, 0 );
    assertEntityRemovedEventCount( listener4, 1 );
    assertEntityRemovedEventCount( listener5, 0 );

    listener1.clear();
    listener2.clear();
    listener3.clear();
    listener4.clear();
    listener5.clear();

    broker.removeChangeListener( listener1 );

    broker.entityRemoved( entity );
    assertEntityRemovedEventCount( listener1, 0 );
    assertEntityRemovedEventCount( listener2, 1 );
    assertEntityRemovedEventCount( listener3, 0 );
    assertEntityRemovedEventCount( listener4, 1 );
    assertEntityRemovedEventCount( listener5, 0 );

    listener1.clear();
    listener2.clear();
    listener3.clear();
    listener4.clear();
    listener5.clear();
  }

  private void assertHasNoRecordedEvents( final RecordingListener listener )
  {
    assertTrue( listener.hasNoRecordedEvents() );
  }

  private void assertAttributeChangeEventCount( final RecordingListener listener, final int eventCount )
  {
    final ArrayList<EntityChangeEvent> events = listener.getAttributeChangedEvents();
    assertChangeCount( eventCount, events );
  }

  private void assertChangeCount( final int eventCount, final ArrayList<EntityChangeEvent> events )
  {
    assertEquals( eventCount, events.size() );
  }

  private static void assertAttributeChangedEvent( final EntityChangeEvent event,
                                                   final Object entity,
                                                   final String attrKey,
                                                   final int attributeValue )
  {
    assertEvent( event, EntityChangeType.ATTRIBUTE_CHANGED, entity, attrKey, attributeValue );
  }

  private static void assertRelatedRemovedEvent( final EntityChangeEvent event,
                                                 final Object entity,
                                                 final String relKey,
                                                 final Object other )
  {
    assertEvent( event, EntityChangeType.RELATED_REMOVED, entity, relKey, other );
  }

  private void assertRelatedRemovedEventCount( final RecordingListener listener, final int eventCount )
  {
    assertChangeCount( eventCount, listener.getRelatedRemovedEvents() );
  }


  private static void assertRelatedAddedEvent( final EntityChangeEvent event,
                                               final Object entity,
                                               final String relKey,
                                               final Object other )
  {
    assertEvent( event, EntityChangeType.RELATED_ADDED, entity, relKey, other );
  }

  private void assertRelatedAddedEventCount( final RecordingListener listener, final int eventCount )
  {
    assertChangeCount( eventCount, listener.getRelatedAddedEvents() );
  }

  private static void assertEvent( final EntityChangeEvent event,
                                   final EntityChangeType type,
                                   final Object entity,
                                   final String name, final Object value )
  {
    assertEquals( entity, event.getObject() );
    assertEquals( name, event.getName() );
    assertEquals( value, event.getValue() );
    assertEquals( type, event.getType() );
  }

  private void assertEntityRemovedEventCount( final RecordingListener listener, final int eventCount )
  {
    assertChangeCount( eventCount, listener.getEntityRemovedEvents() );
  }

  private static void assertEntityRemovedEvent( final EntityChangeEvent event,
                                                final Object entity )
  {
    assertEquals( entity, event.getObject() );
    assertNull( event.getName() );
    assertNull( event.getValue() );
    assertEquals( EntityChangeType.ENTITY_REMOVED, event.getType() );
  }


  public static class A
  {
  }

  public static class B
      extends A
  {
  }

  public static class SubB
      extends B
  {
  }

  public static class C
  {
  }
}
