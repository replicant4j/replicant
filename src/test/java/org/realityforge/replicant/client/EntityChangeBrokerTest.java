package org.realityforge.replicant.client;

import java.util.ArrayList;
import javax.annotation.Nonnull;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityChangeBrokerTest
{
  public static final String ATTR_KEY = "MyAttribute";
  public static final String REL_KEY = "MyRelationship";

  @Test
  public void raiseErrorOnEventHandlerError()
  {
    final EntityChangeBrokerImpl broker = new EntityChangeBrokerImpl();
    final Exception cause = new Exception();
    try
    {
      broker.logEventHandlingError( null, cause );
    }
    catch ( final IllegalStateException e )
    {
      assertEquals( e.getCause(), cause );
      assertEquals( e.getMessage(), "Error sending event to listener: null" );
    }

    broker.setRaiseErrorOnEventHandlerError( false );
    try
    {
      broker.logEventHandlingError( null, cause );
    }
    catch ( final IllegalStateException e )
    {
      fail();
    }
  }

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

    assertEntityAddedEventCount( globalListener, 0 );
    assertEntityAddedEventCount( typeListener, 0 );
    assertEntityAddedEventCount( parentTypeListener, 0 );
    assertEntityAddedEventCount( instanceListener, 0 );

    broker.entityAdded( entity );

    assertEntityAddedEventCount( globalListener, 1 );
    assertEntityAddedEvent( globalListener.getEntityAddedEvents().get( 0 ), entity );

    assertEntityAddedEventCount( typeListener, 1 );
    assertEntityAddedEvent( typeListener.getEntityAddedEvents().get( 0 ), entity );

    assertEntityAddedEventCount( parentTypeListener, 1 );
    assertEntityAddedEvent( parentTypeListener.getEntityAddedEvents().get( 0 ), entity );

    assertEntityAddedEventCount( subTypeListener, 0 );

    assertEntityAddedEventCount( differentTypeListener, 0 );

    assertEntityAddedEventCount( instanceListener, 1 );
    assertEntityAddedEvent( instanceListener.getEntityAddedEvents().get( 0 ), entity );

    globalListener.clear();
    typeListener.clear();
    parentTypeListener.clear();
    subTypeListener.clear();
    differentTypeListener.clear();
    instanceListener.clear();

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

    assertNoActiveTransaction( broker );

    {
      final EntityBrokerTransaction transaction = broker.pause( "TEST" );
      assertTransactionMatches( transaction, "TEST", false );
    }

    broker.attributeChanged( entity, ATTR_KEY, 42 );

    assertHasNoRecordedEvents( globalListener );

    //Allow the entities to flow
    broker.resume( "TEST" );

    assertNoActiveTransaction( broker );

    assertAttributeChangeEventCount( globalListener, 1 );
    assertAttributeChangedEvent( globalListener.getAttributeChangedEvents().get( 0 ), entity, ATTR_KEY, 42 );

    globalListener.clear();

    //Entity Removed
    assertEntityRemovedEventCount( globalListener, 0 );

    {
      final EntityBrokerTransaction transaction = broker.pause( "TEST" );
      assertTransactionMatches( transaction, "TEST", false );
    }


    broker.entityRemoved( entity );

    assertHasNoRecordedEvents( globalListener );

    broker.resume( "TEST" );

    assertNoActiveTransaction( broker );
    assertEntityRemovedEventCount( globalListener, 1 );
    assertEntityRemovedEvent( globalListener.getEntityRemovedEvents().get( 0 ), entity );

    globalListener.clear();

    //Related Added
    assertRelatedAddedEventCount( globalListener, 0 );

    {
      final EntityBrokerTransaction transaction = broker.pause( "TEST" );
      assertTransactionMatches( transaction, "TEST", false );
    }

    broker.relatedAdded( entity, REL_KEY, other );

    assertHasNoRecordedEvents( globalListener );

    broker.resume( "TEST" );

    assertNoActiveTransaction( broker );
    assertRelatedAddedEventCount( globalListener, 1 );
    assertRelatedAddedEvent( globalListener.getRelatedAddedEvents().get( 0 ), entity, REL_KEY, other );

    globalListener.clear();

    //Related Removed
    assertRelatedRemovedEventCount( globalListener, 0 );

    {
      final EntityBrokerTransaction transaction = broker.pause( "TEST" );
      assertTransactionMatches( transaction, "TEST", false );
    }

    broker.relatedRemoved( entity, REL_KEY, other );

    assertHasNoRecordedEvents( globalListener );

    broker.resume( "TEST" );

    assertNoActiveTransaction( broker );
    assertRelatedRemovedEventCount( globalListener, 1 );
    assertRelatedRemovedEvent( globalListener.getRelatedRemovedEvents().get( 0 ), entity, REL_KEY, other );
  }

  private void assertNoActiveTransaction( final EntityChangeBroker broker )
  {
    assertEquals( broker.getCurrentTransaction(), null );
    assertEquals( broker.isInTransaction(), false );
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

    assertNoActiveTransaction( broker );

    final EntityBrokerTransaction transaction = broker.disable( "TEST" );
    assertTransactionMatches( transaction, "TEST", true );

    assertActiveTransaction( broker, transaction );

    broker.attributeChanged( entity, ATTR_KEY, 42 );
    assertHasNoRecordedEvents( globalListener );

    broker.entityRemoved( entity );
    assertHasNoRecordedEvents( globalListener );

    broker.relatedAdded( entity, REL_KEY, other );
    assertHasNoRecordedEvents( globalListener );

    broker.relatedRemoved( entity, REL_KEY, other );
    assertHasNoRecordedEvents( globalListener );

    broker.enable( "TEST" );

    assertNoActiveTransaction( broker );
    assertHasNoRecordedEvents( globalListener );
  }

  private void assertTransactionMatches( final EntityBrokerTransaction transaction,
                                         final String key,
                                         final boolean disabled )
  {
    assertNotNull( transaction );
    assertEquals( transaction.getKey(), key );
    assertEquals( transaction.isDisableAction(), disabled );
    assertEquals( transaction.isPauseAction(), !disabled );
  }

  private void assertActiveTransaction( final EntityChangeBroker broker, final EntityBrokerTransaction transaction )
  {
    assertEquals( broker.getCurrentTransaction(), transaction );
    assertEquals( broker.isInTransaction(), true );
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

  @Test
  public void ensureAddsAndRemovesDuringSendingAreDeferred()
  {
    final B entity = new B();

    final EntityChangeBroker broker = new EntityChangeBrokerImpl();

    final RecordingListener globalListener = new RecordingListener();
    final RecordingListener classListener = new RecordingListener();
    final RecordingListener instanceListener = new RecordingListener();

    final RecordingListener addingListener = new RecordingListener()
    {
      public void entityRemoved( @Nonnull final EntityChangeEvent event )
      {
        broker.addChangeListener( globalListener );
        broker.addChangeListener( B.class, classListener );
        broker.addChangeListener( entity, instanceListener );
      }
    };

    final RecordingListener removingListener = new RecordingListener()
    {
      public void entityRemoved( @Nonnull final EntityChangeEvent event )
      {
        broker.removeChangeListener( globalListener );
        broker.removeChangeListener( B.class, classListener );
        broker.removeChangeListener( entity, instanceListener );
      }
    };

    final RecordingListener purgingListener = new RecordingListener()
    {
      public void entityRemoved( @Nonnull final EntityChangeEvent event )
      {
        broker.purgeChangeListener( globalListener );
      }
    };

    broker.addChangeListener( addingListener );

    broker.entityRemoved( entity );

    broker.removeChangeListener( addingListener );

    //None of the listeners should receive messages as the action is deferred until after send occurs
    assertEntityRemovedEventCount( globalListener, 0 );
    assertEntityRemovedEventCount( classListener, 0 );
    assertEntityRemovedEventCount( instanceListener, 0 );

    broker.entityRemoved( entity );

    // All should receive messages as they were all added
    assertEntityRemovedEventCount( globalListener, 1 );
    assertEntityRemovedEventCount( classListener, 1 );
    assertEntityRemovedEventCount( instanceListener, 1 );

    globalListener.clear();
    classListener.clear();
    instanceListener.clear();

    //Remove all the change listeners again
    broker.removeChangeListener( globalListener );
    broker.removeChangeListener( B.class, classListener );
    broker.removeChangeListener( entity, instanceListener );

    broker.entityRemoved( entity );

    assertEntityRemovedEventCount( globalListener, 0 );
    assertEntityRemovedEventCount( classListener, 0 );
    assertEntityRemovedEventCount( instanceListener, 0 );

    // if we add removing listener first to ensure that it is called first then we
    // add the other listeners. This proves they will not be removed until after message
    // has been sent
    broker.addChangeListener( removingListener );
    broker.addChangeListener( globalListener );
    broker.addChangeListener( B.class, classListener );
    broker.addChangeListener( entity, instanceListener );

    broker.entityRemoved( entity );

    // All should receive messages as they were still around during last send
    assertEntityRemovedEventCount( globalListener, 1 );
    assertEntityRemovedEventCount( classListener, 1 );
    assertEntityRemovedEventCount( instanceListener, 1 );

    globalListener.clear();
    classListener.clear();
    instanceListener.clear();

    broker.entityRemoved( entity );

    // None should receive messages as they were removed in last event send
    assertEntityRemovedEventCount( globalListener, 0 );
    assertEntityRemovedEventCount( classListener, 0 );
    assertEntityRemovedEventCount( instanceListener, 0 );

    // if we add purging listener first to ensure that it is called first then we
    // add the other listeners. This proves they will not be removed until after message
    // has been sent
    broker.addChangeListener( purgingListener );
    broker.addChangeListener( globalListener );
    broker.addChangeListener( B.class, classListener );
    broker.addChangeListener( entity, instanceListener );

    broker.entityRemoved( entity );

    // All should receive messages as they were still around during last send
    assertEntityRemovedEventCount( globalListener, 1 );
    assertEntityRemovedEventCount( classListener, 1 );
    assertEntityRemovedEventCount( instanceListener, 1 );

    globalListener.clear();
    classListener.clear();
    instanceListener.clear();

    broker.entityRemoved( entity );

    // None should receive messages as they were removed in last event send
    assertEntityRemovedEventCount( globalListener, 0 );
    assertEntityRemovedEventCount( classListener, 0 );
    assertEntityRemovedEventCount( instanceListener, 0 );
  }

  @Test
  public void ensureMessagesSentInsideListenerAreDeferredTillAfterSendCompletes()
  {
    final B entity1 = new B();
    final B entity2 = new B();

    final EntityChangeBroker broker = new EntityChangeBrokerImpl();

    broker.addChangeListener( new RecordingListener()
    {
      private boolean _sent;

      public void entityRemoved( @Nonnull final EntityChangeEvent event )
      {
        if ( !_sent )
        {
          broker.entityRemoved( entity2 );
          _sent = true;
        }
      }
    } );

    final RecordingListener globalListener = new RecordingListener();
    broker.addChangeListener( globalListener );

    broker.entityRemoved( entity1 );

    assertEntityRemovedEventCount( globalListener, 2 );
    assertEquals( entity1, globalListener.getEntityRemovedEvents().get( 0 ).getObject() );
    assertEquals( entity2, globalListener.getEntityRemovedEvents().get( 1 ).getObject() );
  }

  @Test
  public void purgeChangeListener()
  {
    final B entity = new B();
    final B other = new B();

    final EntityChangeBroker broker = new EntityChangeBrokerImpl();
    final RecordingListener listener = new RecordingListener();

    //Pre add purge should not cause any problems
    broker.purgeChangeListener( listener );
    assertEntityRemovedMessageNotSent( entity, broker, listener );

    // purge after add a global
    broker.addChangeListener( listener );
    assertEntityRemovedMessageSent( entity, broker, listener );
    broker.purgeChangeListener( listener );
    assertEntityRemovedMessageNotSent( entity, broker, listener );

    //Double purge does nothing
    broker.purgeChangeListener( listener );
    assertEntityRemovedMessageNotSent( entity, broker, listener );

    // purge after add a type listener
    broker.addChangeListener( B.class, listener );
    assertEntityRemovedMessageSent( entity, broker, listener );
    broker.purgeChangeListener( listener );
    assertEntityRemovedMessageNotSent( entity, broker, listener );

    // purge after add multiple type listener
    broker.addChangeListener( B.class, listener );
    broker.addChangeListener( C.class, listener );
    assertEntityRemovedMessageSent( entity, broker, listener );
    assertEntityRemovedMessageSent( new C(), broker, listener );
    broker.purgeChangeListener( listener );
    assertEntityRemovedMessageNotSent( entity, broker, listener );
    assertEntityRemovedMessageNotSent( new C(), broker, listener );

    // purge after add a instance listener
    broker.addChangeListener( entity, listener );
    assertEntityRemovedMessageSent( entity, broker, listener );
    broker.purgeChangeListener( listener );
    assertEntityRemovedMessageNotSent( entity, broker, listener );

    // purge multiple after add a instance listener
    broker.addChangeListener( entity, listener );
    broker.addChangeListener( other, listener );
    assertEntityRemovedMessageSent( entity, broker, listener );
    assertEntityRemovedMessageSent( other, broker, listener );
    broker.purgeChangeListener( listener );
    assertEntityRemovedMessageNotSent( entity, broker, listener );
    assertEntityRemovedMessageNotSent( other, broker, listener );
  }

  private void assertEntityRemovedMessageSent( final Object entity,
                                               final EntityChangeBroker broker,
                                               final RecordingListener listener )
  {
    broker.entityRemoved( entity );
    assertEntityRemovedEventCount( listener, 1 );
    listener.clear();
  }

  private void assertEntityRemovedMessageNotSent( final Object entity,
                                                  final EntityChangeBroker broker,
                                                  final RecordingListener listener )
  {
    broker.entityRemoved( entity );
    assertEntityRemovedEventCount( listener, 0 );
  }

  private void assertHasNoRecordedEvents( final RecordingListener listener )
  {
    assertTrue( listener.hasNoRecordedEvents() );
  }

  private void assertEntityAddedEventCount( final RecordingListener listener, final int eventCount )
  {
    final ArrayList<EntityChangeEvent> events = listener.getEntityAddedEvents();
    assertChangeCount( eventCount, events );
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

  private static void assertEntityAddedEvent( final EntityChangeEvent event, final Object entity )
  {
    assertEquals( entity, event.getObject() );
    assertNull( event.getName() );
    assertNull( event.getValue() );
    assertEquals( EntityChangeType.ENTITY_ADDED, event.getType() );
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
