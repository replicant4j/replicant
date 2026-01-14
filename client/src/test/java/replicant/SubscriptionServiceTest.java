package replicant;

import arez.Disposable;
import java.util.concurrent.atomic.AtomicInteger;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.spy.SubscriptionCreatedEvent;
import replicant.spy.SubscriptionDisposedEvent;
import static org.testng.Assert.*;

public class SubscriptionServiceTest
  extends AbstractReplicantTest
{
  @Test
  public void typeSubscriptions()
  {
    final ChannelAddress address1 = new ChannelAddress( 1, 0 );
    final ChannelAddress address2 = new ChannelAddress( 1, 1 );
    final ChannelAddress address3 = new ChannelAddress( 1, 2 );

    final SubscriptionService service = SubscriptionService.create( null );

    final AtomicInteger findSubscriptionAddress1CallCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( service ) )
      {
        // Access observable next line
        service.findSubscription( address1 );
      }

      findSubscriptionAddress1CallCount.incrementAndGet();
    } );

    final AtomicInteger findSubscriptionAddress2CallCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( service ) )
      {
        // Access observable next line
        service.findSubscription( address2 );
      }
      findSubscriptionAddress2CallCount.incrementAndGet();
    } );

    final AtomicInteger getInstanceSubscriptionsCallCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( service ) )
      {
        service.getInstanceSubscriptions();
      }
      getInstanceSubscriptionsCallCount.incrementAndGet();
    } );

    final AtomicInteger getTypeSubscriptionsCallCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( service ) )
      {
        service.getTypeSubscriptions();
      }
      getTypeSubscriptionsCallCount.incrementAndGet();
    } );

    assertEquals( findSubscriptionAddress1CallCount.get(), 1 );
    assertEquals( findSubscriptionAddress2CallCount.get(), 1 );
    assertEquals( getInstanceSubscriptionsCallCount.get(), 1 );
    assertEquals( getTypeSubscriptionsCallCount.get(), 1 );
    assertNull( service.findSubscription( address1 ) );
    assertNull( service.findSubscription( address2 ) );
    safeAction( () -> assertEquals( service.getTypeSubscriptions().size(), 0 ) );

    // Add subscription on address1
    {
      safeAction( () -> service.createSubscription( address1, null, true ) );

      assertEquals( findSubscriptionAddress1CallCount.get(), 2 );
      assertEquals( findSubscriptionAddress2CallCount.get(), 2 );
      assertEquals( getInstanceSubscriptionsCallCount.get(), 1 );
      assertEquals( getTypeSubscriptionsCallCount.get(), 2 );
      assertNotNull( service.findSubscription( address1 ) );
      assertNull( service.findSubscription( address2 ) );
      safeAction( () -> assertEquals( service.getTypeSubscriptions().size(), 1 ) );
    }

    // Add subscription on address2
    {
      safeAction( () -> service.createSubscription( address2, null, true ) );

      assertEquals( findSubscriptionAddress1CallCount.get(), 2 );
      assertEquals( findSubscriptionAddress2CallCount.get(), 3 );
      assertEquals( getInstanceSubscriptionsCallCount.get(), 1 );
      assertEquals( getTypeSubscriptionsCallCount.get(), 3 );
      assertNotNull( service.findSubscription( address1 ) );
      assertNotNull( service.findSubscription( address2 ) );
      safeAction( () -> assertEquals( service.getTypeSubscriptions().size(), 2 ) );
    }

    // Add subscription on address3
    {
      safeAction( () -> service.createSubscription( address3, null, true ) );

      assertEquals( findSubscriptionAddress1CallCount.get(), 2 );
      assertEquals( findSubscriptionAddress2CallCount.get(), 3 );
      assertEquals( getInstanceSubscriptionsCallCount.get(), 1 );
      assertEquals( getTypeSubscriptionsCallCount.get(), 4 );
      assertNotNull( service.findSubscription( address1 ) );
      assertNotNull( service.findSubscription( address2 ) );
      safeAction( () -> assertEquals( service.getTypeSubscriptions().size(), 3 ) );
    }

    // Dispose subscription on address3
    // Should only reschedule `getTypeSubscriptions()`
    {
      safeAction( () -> {
        final Subscription subscription = service.findSubscription( address3 );
        assertNotNull( subscription );
        Disposable.dispose( subscription );

        // Check that subscription count is updated from within the subscription
        // to ensure not possible that disposed is returned
        assertEquals( service.getTypeSubscriptions().size(), 2 );
        assertNull( service.findSubscription( address3 ) );
      } );

      assertEquals( findSubscriptionAddress1CallCount.get(), 2 );
      assertEquals( findSubscriptionAddress2CallCount.get(), 3 );
      assertEquals( getInstanceSubscriptionsCallCount.get(), 1 );
      assertEquals( getTypeSubscriptionsCallCount.get(), 5 );
      assertNotNull( service.findSubscription( address1 ) );
      assertNotNull( service.findSubscription( address2 ) );
      safeAction( () -> assertEquals( service.getTypeSubscriptions().size(), 2 ) );
    }

    // Dispose subscription on address2
    // Should reschedule `getTypeSubscriptions()` and findSubscription( address2 )
    {
      safeAction( () -> {
        final Subscription subscription = service.findSubscription( address2 );
        assertNotNull( subscription );
        Disposable.dispose( subscription );

        // Check that subscription count is updated from within the subscription
        // to ensure not possible that disposed is returned
        assertEquals( service.getTypeSubscriptions().size(), 1 );
        assertNull( service.findSubscription( address2 ) );
      } );

      assertEquals( findSubscriptionAddress1CallCount.get(), 2 );
      assertEquals( findSubscriptionAddress2CallCount.get(), 4 );
      assertEquals( getInstanceSubscriptionsCallCount.get(), 1 );
      assertEquals( getTypeSubscriptionsCallCount.get(), 6 );
      assertNotNull( service.findSubscription( address1 ) );
      assertNull( service.findSubscription( address2 ) );
      safeAction( () -> assertEquals( service.getTypeSubscriptions().size(), 1 ) );
    }

    // Dispose service
    {
      Disposable.dispose( service );

      assertEquals( findSubscriptionAddress1CallCount.get(), 3 );
      assertEquals( findSubscriptionAddress2CallCount.get(), 5 );
      assertEquals( getInstanceSubscriptionsCallCount.get(), 2 );
      assertEquals( getTypeSubscriptionsCallCount.get(), 7 );
    }
  }

  @Test
  public void typeSubscriptions_withFilterInstanceId()
  {
    final ChannelAddress address1 = new ChannelAddress( 1, 0, null, "fi1" );
    final ChannelAddress address2 = new ChannelAddress( 1, 0, null, "fi2" );

    final SubscriptionService service = SubscriptionService.create( null );

    safeAction( () -> {
      service.createSubscription( address1, null, true );
      service.createSubscription( address2, null, true );
    } );

    assertNotNull( service.findSubscription( address1 ) );
    assertNotNull( service.findSubscription( address2 ) );
    safeAction( () -> assertEquals( service.getTypeSubscriptions().size(), 2 ) );

    safeAction( () -> {
      final Subscription subscription = service.findSubscription( address1 );
      assertNotNull( subscription );
      Disposable.dispose( subscription );
    } );

    assertNull( service.findSubscription( address1 ) );
    assertNotNull( service.findSubscription( address2 ) );
    safeAction( () -> assertEquals( service.getTypeSubscriptions().size(), 1 ) );
  }

  @Test
  public void typeSubscriptions_emptyFilterInstanceId()
  {
    final ChannelAddress address1 = new ChannelAddress( 1, 0, null, "" );
    final ChannelAddress address2 = new ChannelAddress( 1, 0, null, null );

    final SubscriptionService service = SubscriptionService.create( null );

    safeAction( () -> {
      service.createSubscription( address1, null, true );
      service.createSubscription( address2, null, true );
    } );

    assertNotNull( service.findSubscription( address1 ) );
    assertNotNull( service.findSubscription( address2 ) );
    safeAction( () -> assertEquals( service.getTypeSubscriptions().size(), 2 ) );
  }

  @Test
  public void instanceSubscriptions()
  {
    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 0, 2 );
    final ChannelAddress address3 = new ChannelAddress( 1, 1, ValueUtil.randomInt() );

    final SubscriptionService service = SubscriptionService.create( null );

    final AtomicInteger findSubscriptionAddress1CallCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( service ) )
      {
        // Access observable next line
        service.findSubscription( address1 );
      }

      findSubscriptionAddress1CallCount.incrementAndGet();
    } );

    final AtomicInteger findSubscriptionAddress2CallCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( service ) )
      {
        // Access observable next line
        service.findSubscription( address2 );
      }
      findSubscriptionAddress2CallCount.incrementAndGet();
    } );

    final AtomicInteger getInstanceSubscriptionsCallCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( service ) )
      {
        service.getInstanceSubscriptions();
      }
      getInstanceSubscriptionsCallCount.incrementAndGet();
    } );

    final AtomicInteger getTypeSubscriptionsCallCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( service ) )
      {
        service.getTypeSubscriptions();
      }
      getTypeSubscriptionsCallCount.incrementAndGet();
    } );

    assertEquals( findSubscriptionAddress1CallCount.get(), 1 );
    assertEquals( findSubscriptionAddress2CallCount.get(), 1 );
    assertEquals( getInstanceSubscriptionsCallCount.get(), 1 );
    assertEquals( getTypeSubscriptionsCallCount.get(), 1 );
    assertNull( service.findSubscription( address1 ) );
    assertNull( service.findSubscription( address2 ) );
    safeAction( () -> assertEquals( service.getInstanceSubscriptions().size(), 0 ) );
    safeAction( () -> assertEquals( service.getInstanceSubscriptionIds( 1, 0 ).size(), 0 ) );
    safeAction( () -> assertEquals( service.getInstanceSubscriptionIds( 1, 1 ).size(), 0 ) );

    // Add subscription on address1
    {
      safeAction( () -> service.createSubscription( address1, null, true ) );

      assertEquals( findSubscriptionAddress1CallCount.get(), 2 );
      assertEquals( findSubscriptionAddress2CallCount.get(), 2 );
      assertEquals( getInstanceSubscriptionsCallCount.get(), 2 );
      assertEquals( getTypeSubscriptionsCallCount.get(), 1 );
      assertNotNull( service.findSubscription( address1 ) );
      assertNull( service.findSubscription( address2 ) );
      safeAction( () -> assertEquals( service.getInstanceSubscriptions().size(), 1 ) );
      safeAction( () -> assertEquals( service.getInstanceSubscriptionIds( 1, 0 ).size(), 1 ) );
      safeAction( () -> assertEquals( service.getInstanceSubscriptionIds( 1, 1 ).size(), 0 ) );
    }

    // Add subscription on address2
    {
      safeAction( () -> service.createSubscription( address2, null, true ) );

      assertEquals( findSubscriptionAddress1CallCount.get(), 2 );
      assertEquals( findSubscriptionAddress2CallCount.get(), 3 );
      assertEquals( getInstanceSubscriptionsCallCount.get(), 3 );
      assertEquals( getTypeSubscriptionsCallCount.get(), 1 );
      assertNotNull( service.findSubscription( address1 ) );
      assertNotNull( service.findSubscription( address2 ) );
      safeAction( () -> assertEquals( service.getInstanceSubscriptions().size(), 2 ) );
      safeAction( () -> assertEquals( service.getInstanceSubscriptionIds( 1, 0 ).size(), 2 ) );
      safeAction( () -> assertEquals( service.getInstanceSubscriptionIds( 1, 1 ).size(), 0 ) );
    }

    // Add subscription on address3
    {
      safeAction( () -> service.createSubscription( address3, null, true ) );

      assertEquals( findSubscriptionAddress1CallCount.get(), 2 );
      assertEquals( findSubscriptionAddress2CallCount.get(), 3 );
      assertEquals( getInstanceSubscriptionsCallCount.get(), 4 );
      assertEquals( getTypeSubscriptionsCallCount.get(), 1 );
      assertNotNull( service.findSubscription( address1 ) );
      assertNotNull( service.findSubscription( address2 ) );
      safeAction( () -> assertEquals( service.getInstanceSubscriptions().size(), 3 ) );
      safeAction( () -> assertEquals( service.getInstanceSubscriptionIds( 1, 0 ).size(), 2 ) );
      safeAction( () -> assertEquals( service.getInstanceSubscriptionIds( 1, 1 ).size(), 1 ) );
    }

    // Dispose subscription on address3
    // Should only reschedule `getInstanceSubscriptions()`
    {
      safeAction( () -> {
        final Subscription subscription = service.findSubscription( address3 );
        assertNotNull( subscription );
        Disposable.dispose( subscription );

        // Check that subscription count is updated from within the subscription
        // to ensure not possible that disposed is returned
        assertEquals( service.getInstanceSubscriptions().size(), 2 );
        assertNull( service.findSubscription( address3 ) );
        assertEquals( service.getInstanceSubscriptionIds( 1, 0 ).size(), 2 );
      } );

      assertEquals( findSubscriptionAddress1CallCount.get(), 2 );
      assertEquals( findSubscriptionAddress2CallCount.get(), 3 );
      assertEquals( getInstanceSubscriptionsCallCount.get(), 5 );
      assertEquals( getTypeSubscriptionsCallCount.get(), 1 );
      assertNotNull( service.findSubscription( address1 ) );
      assertNotNull( service.findSubscription( address2 ) );
      safeAction( () -> assertEquals( service.getInstanceSubscriptions().size(), 2 ) );
      safeAction( () -> assertEquals( service.getInstanceSubscriptionIds( 1, 0 ).size(), 2 ) );
      safeAction( () -> assertEquals( service.getInstanceSubscriptionIds( 1, 1 ).size(), 0 ) );
    }

    // Dispose subscription on address2
    // Should reschedule `getInstanceSubscriptions()` and findSubscription( address2 )
    {
      safeAction( () -> {
        final Subscription subscription = service.findSubscription( address2 );
        assertNotNull( subscription );
        Disposable.dispose( subscription );

        // Check that subscription count is updated from within the subscription
        // to ensure not possible that disposed is returned
        assertEquals( service.getInstanceSubscriptions().size(), 1 );
        assertNull( service.findSubscription( address2 ) );
        assertEquals( service.getInstanceSubscriptionIds( 1, 0 ).size(), 1 );
      } );

      assertEquals( findSubscriptionAddress1CallCount.get(), 2 );
      assertEquals( findSubscriptionAddress2CallCount.get(), 4 );
      assertEquals( getInstanceSubscriptionsCallCount.get(), 6 );
      assertEquals( getTypeSubscriptionsCallCount.get(), 1 );
      assertNotNull( service.findSubscription( address1 ) );
      assertNull( service.findSubscription( address2 ) );
      safeAction( () -> assertEquals( service.getInstanceSubscriptions().size(), 1 ) );
      safeAction( () -> assertEquals( service.getInstanceSubscriptionIds( 1, 0 ).size(), 1 ) );
      safeAction( () -> assertEquals( service.getInstanceSubscriptionIds( 1, 1 ).size(), 0 ) );
    }

    // Dispose service
    {
      Disposable.dispose( service );

      assertEquals( findSubscriptionAddress1CallCount.get(), 3 );
      assertEquals( findSubscriptionAddress2CallCount.get(), 5 );
      assertEquals( getInstanceSubscriptionsCallCount.get(), 7 );
      assertEquals( getTypeSubscriptionsCallCount.get(), 2 );
    }
  }

  @Test
  public void instanceSubscriptions_withFilterInstanceId()
  {
    final ChannelAddress address1 = new ChannelAddress( 1, 0, 7, "fi1" );
    final ChannelAddress address2 = new ChannelAddress( 1, 0, 7, "fi2" );
    final ChannelAddress address3 = new ChannelAddress( 1, 0, 8, "fi1" );

    final SubscriptionService service = SubscriptionService.create( null );

    safeAction( () -> {
      service.createSubscription( address1, null, true );
      service.createSubscription( address2, null, true );
      service.createSubscription( address3, null, true );
    } );

    assertNotNull( service.findSubscription( address1 ) );
    assertNotNull( service.findSubscription( address2 ) );
    assertNotNull( service.findSubscription( address3 ) );
    safeAction( () -> assertEquals( service.getInstanceSubscriptions().size(), 3 ) );
    safeAction( () -> assertEquals( service.getInstanceSubscriptionIds( 1, 0 ).size(), 2 ) );

    safeAction( () -> {
      final Subscription subscription = service.findSubscription( address2 );
      assertNotNull( subscription );
      Disposable.dispose( subscription );
    } );

    assertNull( service.findSubscription( address2 ) );
    assertNotNull( service.findSubscription( address1 ) );
    assertNotNull( service.findSubscription( address3 ) );
    safeAction( () -> assertEquals( service.getInstanceSubscriptions().size(), 2 ) );
    safeAction( () -> assertEquals( service.getInstanceSubscriptionIds( 1, 0 ).size(), 2 ) );
  }

  @Test
  public void createSubscription_instanceChannel_NoFilter_Explicit()
  {
    final ChannelAddress address = new ChannelAddress( 1, 0, 1 );

    final SubscriptionService service = SubscriptionService.create( null );

    // instance channel, no filter, explicit subscription
    safeAction( () -> {
      final Subscription subscription = service.createSubscription( address, null, true );
      assertEquals( subscription.address(), address );
      assertNull( subscription.getFilter() );
      assertTrue( subscription.isExplicitSubscription() );
    } );
  }

  @Test
  public void createSubscription_instanceChannel_Filter_NoExplicit()
  {
    final ChannelAddress address = new ChannelAddress( 1, 0, 2 );

    final SubscriptionService service = SubscriptionService.create( null );

    // instance channel, filter, not explicit subscription
    safeAction( () -> {
      final String filter = ValueUtil.randomString();
      final boolean explicitSubscription = false;
      final Subscription subscription = service.createSubscription( address, filter, explicitSubscription );
      assertEquals( subscription.address(), address );
      assertEquals( subscription.getFilter(), filter );
      assertEquals( subscription.isExplicitSubscription(), explicitSubscription );
    } );
  }

  @Test
  public void createSubscription_typeChannel_NoFilter_NoExplicit()
  {
    final ChannelAddress address = new ChannelAddress( 1, 1 );

    final SubscriptionService service = SubscriptionService.create( null );

    // type channel, no filter, no explicit subscription
    safeAction( () -> {
      final String filter = null;
      final boolean explicitSubscription = false;
      final Subscription subscription = service.createSubscription( address, filter, explicitSubscription );
      assertEquals( subscription.address(), address );
      assertEquals( subscription.getFilter(), filter );
      assertEquals( subscription.isExplicitSubscription(), explicitSubscription );
    } );
  }

  @Test
  public void createSubscription_typeChannel_Filter_Explicit()
  {
    final ChannelAddress address = new ChannelAddress( 1, 1 );

    final SubscriptionService service = SubscriptionService.create( null );

    // type channel, filter, explicit subscription
    safeAction( () -> {
      final String filter = ValueUtil.randomString();
      final boolean explicitSubscription = true;
      final Subscription subscription = service.createSubscription( address, filter, explicitSubscription );
      assertEquals( subscription.address(), address );
      assertEquals( subscription.getFilter(), filter );
      assertEquals( subscription.isExplicitSubscription(), explicitSubscription );
    } );
  }

  @Test
  public void createSubscription_generatesSpyEvent()
  {
    final ChannelAddress address = new ChannelAddress( 1, 1 );

    final SubscriptionService service = SubscriptionService.create( null );
    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Subscription subscription =
      safeAction( () -> service.createSubscription( address, ValueUtil.randomString(), true ) );

    handler.assertEventCount( 1 );

    final SubscriptionCreatedEvent event = handler.assertNextEvent( SubscriptionCreatedEvent.class );
    assertEquals( event.getSubscription(), subscription );
  }

  @Test
  public void disposeSubscription_generatesSpyEvent()
  {
    final ChannelAddress address = new ChannelAddress( 1, 1 );

    final SubscriptionService service = SubscriptionService.create( null );

    final Subscription subscription =
      safeAction( () -> service.createSubscription( address, ValueUtil.randomString(), true ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    Disposable.dispose( subscription );

    handler.assertEventCount( 1 );

    final SubscriptionDisposedEvent event = handler.assertNextEvent( SubscriptionDisposedEvent.class );
    assertEquals( event.getSubscription(), subscription );
  }

  @Test
  public void createSubscription_alreadyExists()
  {
    final ChannelAddress address = new ChannelAddress( 1, 0 );

    final SubscriptionService service = SubscriptionService.create( null );

    safeAction( () -> service.createSubscription( address, null, true ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> service.createSubscription( address, null, true ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0064: createSubscription invoked with address 1.0 but a subscription with that address already exists." );
  }

  @Test
  public void removeSubscription_typeSubscription_noExist()
  {
    final ChannelAddress address = new ChannelAddress( 1, 0 );

    final SubscriptionService service = SubscriptionService.create( null );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> service.unlinkSubscription( address ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0062: unlinkSubscription invoked with address 1.0 but no subscription with that address exists." );
  }

  @Test
  public void removeSubscription_typeSubscription_notDisposed()
  {
    final ChannelAddress address = new ChannelAddress( 1, 0 );

    final SubscriptionService service = SubscriptionService.create( null );

    safeAction( () -> service.createSubscription( address, null, true ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> service.unlinkSubscription( address ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0063: unlinkSubscription invoked with address 1.0 but subscription has not already been disposed." );
  }

  @Test
  public void removeSubscription_instanceSubscription_noExist()
  {
    final ChannelAddress address = new ChannelAddress( 1, 0, 1 );

    final SubscriptionService service = SubscriptionService.create( null );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> service.unlinkSubscription( address ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0060: unlinkSubscription invoked with address 1.0.1 but no subscription with that address exists." );
  }

  @Test
  public void removeSubscription_instanceSubscription_noExist_butSameChannelTypeExists()
  {
    final ChannelAddress address1 = new ChannelAddress( 1, 0, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 0, 2 );

    final SubscriptionService service = SubscriptionService.create( null );

    safeAction( () -> service.createSubscription( address2, null, true ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> service.unlinkSubscription( address1 ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0060: unlinkSubscription invoked with address 1.0.1 but no subscription with that address exists." );
  }

  @Test
  public void removeSubscription_instanceSubscription_notDisposed()
  {
    final ChannelAddress address = new ChannelAddress( 1, 0, 2 );

    final SubscriptionService service = SubscriptionService.create( null );

    safeAction( () -> service.createSubscription( address, null, true ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> service.unlinkSubscription( address ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0061: unlinkSubscription invoked with address 1.0.2 but subscription has not already been disposed." );
  }

  @Test
  public void createSubscriptionServicePassingContextWhenNoZones()
  {
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> SubscriptionService.create( Replicant.context() ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0037: ReplicantService passed a context but Replicant.areZonesEnabled() is false" );
  }

  @Test
  public void dispose_delinksFromEntity()
  {
    createConnector();

    final EntityService entityService = Replicant.context().getEntityService();
    final SubscriptionService subscriptionService = Replicant.context().getSubscriptionService();

    final Subscription subscription1 =
      safeAction( () -> subscriptionService.createSubscription( new ChannelAddress( 1, 0, 1 ), null, true ) );
    final Subscription subscription2 =
      safeAction( () -> subscriptionService.createSubscription( new ChannelAddress( 1, 0, 2 ), null, true ) );

    final Entity entity1 = safeAction( () -> entityService.findOrCreateEntity( "A/1", A.class, 1 ) );
    final Entity entity2 = safeAction( () -> entityService.findOrCreateEntity( "A/2", A.class, 2 ) );

    safeAction( () -> entity1.linkToSubscription( subscription1 ) );
    safeAction( () -> entity2.linkToSubscription( subscription1 ) );
    safeAction( () -> entity1.linkToSubscription( subscription2 ) );

    safeAction( () -> assertEquals( subscription1.findAllEntityTypes().size(), 1 ) );
    safeAction( () -> assertEquals( subscription2.findAllEntityTypes().size(), 1 ) );
    safeAction( () -> assertEquals( entity1.getSubscriptions().size(), 2 ) );

    assertFalse( Disposable.isDisposed( subscription1 ) );
    assertFalse( Disposable.isDisposed( subscription2 ) );
    assertFalse( Disposable.isDisposed( entity1 ) );
    assertFalse( Disposable.isDisposed( entity2 ) );

    Disposable.dispose( subscription1 );

    assertTrue( Disposable.isDisposed( subscription1 ) );
    assertFalse( Disposable.isDisposed( subscription2 ) );
    // entity2 is associated with subscription2 so it stays
    assertFalse( Disposable.isDisposed( entity1 ) );
    // entity2 had no other subscriptions so it went away
    assertTrue( Disposable.isDisposed( entity2 ) );
  }

  @Test
  public void disposeService_disposesFilterInstanceSubscriptions()
  {
    final SubscriptionService service = SubscriptionService.create( null );

    final Subscription typeSubscription =
      safeAction( () -> service.createSubscription( new ChannelAddress( 1, 0, null, "fi" ), null, true ) );
    final Subscription instanceSubscription =
      safeAction( () -> service.createSubscription( new ChannelAddress( 1, 0, 2, "fi" ), null, true ) );

    assertFalse( Disposable.isDisposed( typeSubscription ) );
    assertFalse( Disposable.isDisposed( instanceSubscription ) );

    Disposable.dispose( service );

    assertTrue( Disposable.isDisposed( typeSubscription ) );
    assertTrue( Disposable.isDisposed( instanceSubscription ) );
  }

  static class A
  {
  }
}
