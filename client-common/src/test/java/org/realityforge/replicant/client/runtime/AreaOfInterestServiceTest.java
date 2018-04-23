package org.realityforge.replicant.client.runtime;

import arez.Arez;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.client.AbstractReplicantTest;
import org.realityforge.replicant.client.ChannelAddress;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@SuppressWarnings( "Duplicates" )
public class AreaOfInterestServiceTest
  extends AbstractReplicantTest
  implements IHookable
{
  enum TestGraph
  {
    A, B
  }

  @Override
  public void run( final IHookCallBack callBack, final ITestResult testResult )
  {
    Arez.context().safeAction( () -> callBack.runTestMethod( testResult ) );
  }

  @Test
  public void basicSubscriptionManagement()
  {
    final AreaOfInterestService service = new Arez_AreaOfInterestService();

    final AreaOfInterestListener listener = mock( AreaOfInterestListener.class );
    service.addAreaOfInterestListener( listener );

    final ChannelAddress descriptor1 = new ChannelAddress( TestGraph.A, null );
    final ChannelAddress descriptor2 = new ChannelAddress( TestGraph.B, 1 );
    final ChannelAddress descriptor3 = new ChannelAddress( TestGraph.B, 2 );

    final SubscriptionReference reference1 = service.findOrCreateSubscription( descriptor1, null ).createReference();

    final Subscription subscription1 = reference1.getSubscription();
    assertNotNull( subscription1 );

    assertEquals( subscription1.getDescriptor(), descriptor1 );
    assertEquals( subscription1.getReferenceCount(), 1 );
    assertEquals( subscription1.isActive(), true );

    verify( listener ).subscriptionCreated( subscription1 );

    assertEquals( service.getSubscriptionsChannels().size(), 1 );
    assertEquals( service.getSubscriptionsChannels().stream().anyMatch( n -> n.equals( descriptor1 ) ), true );
    assertEquals( service.getSubscriptionsChannels().stream().anyMatch( n -> n.equals( descriptor2 ) ), false );
    assertEquals( service.getSubscriptionsChannels().stream().anyMatch( n -> n.equals( descriptor3 ) ), false );

    final Object newFilter = new Object();
    service.updateSubscription( subscription1, newFilter );

    assertEquals( subscription1.getFilter(), newFilter );

    verify( listener ).subscriptionUpdated( subscription1 );

    // Verify a second reference to channel in different scope is fine
    {
      final SubscriptionReference reference1c = service.findOrCreateSubscription( descriptor1, null ).createReference();

      assertEquals( reference1c.getSubscription(), subscription1 );
      assertEquals( subscription1.getReferenceCount(), 2 );
      assertEquals( service.getSubscriptionsChannels().size(), 1 );

      assertFalse( reference1c.hasBeenReleased() );
      reference1c.release();
      assertTrue( reference1c.hasBeenReleased() );

      assertEquals( subscription1.getReferenceCount(), 1 );
    }

    assertFalse( reference1.hasBeenReleased() );
    reference1.release();
    assertTrue( reference1.hasBeenReleased() );

    assertEquals( subscription1.isActive(), false );

    assertEquals( service.getSubscriptionsChannels().size(), 0 );

    verify( listener ).subscriptionDeleted( subscription1 );
  }

  @Test
  public void createSubscription()
  {
    final AreaOfInterestService service = new Arez_AreaOfInterestService();

    final AreaOfInterestListener listener = mock( AreaOfInterestListener.class );
    service.addAreaOfInterestListener( listener );

    final ChannelAddress descriptor1 = new ChannelAddress( TestGraph.A );
    final ChannelAddress descriptor2 = new ChannelAddress( TestGraph.B );

    final String filer1 = "Filer1";
    final String filer2 = null;

    final Subscription subscription1 = service.createSubscription( descriptor1, filer1 );

    assertEquals( subscription1.getDescriptor(), descriptor1 );
    assertEquals( subscription1.getFilter(), filer1 );
    assertEquals( subscription1.getReferenceCount(), 0 );

    verify( listener ).subscriptionCreated( subscription1 );

    final Subscription subscription2 = service.createSubscription( descriptor2, filer2 );

    assertEquals( subscription2.getDescriptor(), descriptor2 );
    assertEquals( subscription2.getFilter(), filer2 );
    assertEquals( subscription2.getReferenceCount(), 0 );

    verify( listener ).subscriptionCreated( subscription2 );
  }

  @Test
  public void findOrCreateSubscription()
  {
    final ChannelAddress channel = new ChannelAddress( ReplicantConnectionTest.TestGraph.A );
    final String filter1 = ValueUtil.randomString();
    final String filter2 = ValueUtil.randomString();

    final AreaOfInterestService service = new Arez_AreaOfInterestService();

    final AreaOfInterestListener listener = mock( AreaOfInterestListener.class );
    service.addAreaOfInterestListener( listener );

    // No existing subscription
    final Subscription subscription1 = service.findOrCreateSubscription( channel, filter1 );
    assertEquals( subscription1.getDescriptor(), channel );
    assertEquals( subscription1.getFilter(), filter1 );
    assertEquals( service.findSubscription( channel ), subscription1 );
    assertEquals( service.getSubscriptions().size(), 1 );

    verify( listener ).subscriptionCreated( subscription1 );
    verify( listener, never() ).subscriptionUpdated( subscription1 );

    reset( listener );

    //Existing subscription, same filter
    final Subscription subscription2 = service.findOrCreateSubscription( channel, filter1 );
    assertEquals( subscription2.getDescriptor(), channel );
    assertEquals( subscription2.getFilter(), filter1 );
    assertEquals( subscription1, subscription2 );
    assertEquals( service.findSubscription( channel ), subscription2 );
    assertEquals( service.getSubscriptions().size(), 1 );

    verify( listener, never() ).subscriptionCreated( subscription1 );
    verify( listener, never() ).subscriptionUpdated( subscription1 );

    reset( listener );

    //Existing subscription, different filter
    final Subscription subscription3 = service.findOrCreateSubscription( channel, filter2 );
    assertEquals( subscription3.getDescriptor(), channel );
    assertEquals( subscription3.getFilter(), filter2 );
    assertEquals( subscription1, subscription3 );
    assertEquals( service.findSubscription( channel ), subscription3 );
    assertEquals( service.getSubscriptions().size(), 1 );

    verify( listener, never() ).subscriptionCreated( subscription1 );
    verify( listener ).subscriptionUpdated( subscription1 );
  }
}
