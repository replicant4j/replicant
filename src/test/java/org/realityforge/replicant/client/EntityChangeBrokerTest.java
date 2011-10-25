package org.realityforge.replicant.client;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityChangeBrokerTest
{

  public static final String ATTR_KEY = "MyKey";

  @Test
  public void ensureCanSendMessagesWhenNoListeners()
  {
    final EntityChangeBroker broker = new EntityChangeBrokerImpl();
    broker.entityRemoved( new A() );
    broker.attributeChanged( new A(), "MyKey", 42 );

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
  }

  @Test
  public void ensureMessagesAreNotDeliveredWhileDisabled()
  {
    final B entity = new B();

    final EntityChangeBroker broker = new EntityChangeBrokerImpl();
    final RecordingListener globalListener = new RecordingListener();

    broker.addChangeListener( globalListener );

    //Attr changed
    assertAttributeChangeEventCount( globalListener, 0 );

    broker.disable();

    broker.attributeChanged( entity, ATTR_KEY, 42 );

    assertHasNoRecordedEvents( globalListener );

    broker.enable();

    assertHasNoRecordedEvents( globalListener );
  }

  private void assertHasNoRecordedEvents( final RecordingListener listener )
  {
    assertTrue( listener.hasNoRecordedEvents() );
  }

  private void assertAttributeChangeEventCount( final RecordingListener listener, final int eventCount )
  {
    assertEquals( eventCount, listener.getAttributeChangedEvents().size() );
  }

  private static void assertAttributeChangedEvent( final EntityChangeEvent event,
                                                   final Object entity,
                                                   final String attrKey,
                                                   final int attributeValue )
  {
    assertEquals( entity, event.getObject() );
    assertEquals( attrKey, event.getName() );
    assertEquals( attributeValue, event.getValue() );
    assertEquals( EntityChangeType.ATTRIBUTE_CHANGED, event.getType() );
  }

  private void assertEntityRemovedEventCount( final RecordingListener listener, final int eventCount )
  {
    assertEquals( eventCount, listener.getEntityRemovedEvents().size() );
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
