package org.realityforge.replicant.client.runtime;

import javax.annotation.Nonnull;
import org.realityforge.replicant.client.ChannelAddress;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class SubscriptionTest
{
  enum TestGraph
  {
    A
  }

  @Test
  public void basicSubscriptionOperation()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ChannelAddress descriptor = new ChannelAddress( TestGraph.A, null );

    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );

    assertEquals( subscription.getDescriptor(), descriptor );
    assertTrue( subscription.isActive() );
    assertFalse( subscription.hasBeenReleased() );
    assertEquals( subscription.getReferenceCount(), 0 );

    assertNull( subscription.getFilter() );

    // Update subscription works ....

    final Object filter = new Object();
    subscription.setFilter( filter );

    assertEquals( subscription.getFilter(), filter );

    answerDestroySubscription( areaOfInterestService, subscription );

    //Delete the subscription
    subscription.release();

    verify( areaOfInterestService, only() ).destroySubscription( subscription );

    assertFalse( subscription.isActive() );
    assertTrue( subscription.hasBeenReleased() );

    reset( areaOfInterestService );

    subscription.release();

    verify( areaOfInterestService, never() ).destroySubscription( subscription );
  }

  @Test
  public void subscriptionReferences()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ChannelAddress descriptor = new ChannelAddress( TestGraph.A, null );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );

    assertEquals( subscription.getReferenceCount(), 0 );

    final SubscriptionReference reference = subscription.createReference();

    assertEquals( subscription.getReferenceCount(), 1 );
    assertTrue( subscription.isActive() );

    // Create a second reference and let it be managed appropriately
    {
      final SubscriptionReference reference2 = subscription.createReference();
      assertEquals( subscription.getReferenceCount(), 2 );

      assertFalse( reference2.hasBeenReleased() );
      reference2.release();
      assertTrue( reference2.hasBeenReleased() );

      assertThrows( ReferenceReleasedException.class, reference2::getSubscription );

      // Subscription still has active reference so it should be alive
      assertEquals( subscription.getReferenceCount(), 1 );
      assertTrue( subscription.isActive() );
    }

    answerDestroySubscription( areaOfInterestService, subscription );

    assertFalse( reference.hasBeenReleased() );
    reference.release();
    assertTrue( reference.hasBeenReleased() );

    // Releasing the last reference removes the subscription
    verify( areaOfInterestService, only() ).destroySubscription( subscription );

    assertFalse( subscription.isActive() );
    assertEquals( subscription.getReferenceCount(), 0 );
  }

  @Test
  public void subscriptionReferencesReleasedDuringSubscriptionRelease()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ChannelAddress descriptor = new ChannelAddress( TestGraph.A, null );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );

    final SubscriptionReference reference = subscription.createReference();

    answerDestroySubscription( areaOfInterestService, subscription );

    //Delete the subscription
    subscription.release();

    verify( areaOfInterestService, only() ).destroySubscription( subscription );

    assertFalse( subscription.isActive() );
    assertTrue( reference.hasBeenReleased() );
  }

  @Test
  public void subscriptionRequirements()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ChannelAddress descriptor1 = new ChannelAddress( TestGraph.A, 1 );
    final ChannelAddress descriptor2 = new ChannelAddress( TestGraph.A, 2 );

    final Subscription subscription1 = new Subscription( areaOfInterestService, descriptor1 );
    final Subscription subscription2 = new Subscription( areaOfInterestService, descriptor2 );

    assertTrue( subscription1.isActive() );
    assertTrue( subscription2.isActive() );
    assertEquals( subscription1.getReferenceCount(), 0 );
    assertEquals( subscription2.getReferenceCount(), 0 );

    assertFalse( subscription1.isSubscriptionRequired( subscription2 ) );
    assertEquals( subscription1.getRequiredSubscriptions().size(), 0 );

    final SubscriptionReference reference = subscription1.requireSubscription( subscription2 );

    assertTrue( subscription1.isSubscriptionRequired( subscription2 ) );
    assertEquals( subscription1.getRequiredSubscriptions().size(), 1 );
    assertTrue( subscription1.getRequiredSubscriptions().contains( subscription2 ) );

    assertTrue( subscription1.isActive() );
    assertTrue( subscription2.isActive() );
    assertEquals( subscription1.getReferenceCount(), 0 );
    assertEquals( subscription2.getReferenceCount(), 1 );

    assertThrows( SubscriptionAlreadyRequiredException.class,
                  () -> subscription1.requireSubscription( subscription2 ) );

    answerDestroySubscription( areaOfInterestService, subscription1 );
    answerDestroySubscription( areaOfInterestService, subscription2 );

    //Delete the subscription
    subscription1.release();

    verify( areaOfInterestService ).destroySubscription( subscription1 );
    verify( areaOfInterestService ).destroySubscription( subscription2 );

    assertFalse( subscription1.isActive() );
    assertTrue( reference.hasBeenReleased() );
    assertFalse( subscription2.isActive() );
    assertTrue( reference.hasBeenReleased() );
    assertEquals( subscription1.getReferenceCount(), 0 );
    assertEquals( subscription2.getReferenceCount(), 0 );

    assertThrows( SubscriptionInactiveException.class, () -> subscription1.requireSubscription( subscription2 ) );
  }

  @Test
  public void releaseMultiple()
  {
    final Subscription subscription1 =
      new Subscription( mock( AreaOfInterestService.class ), new ChannelAddress( TestGraph.A ) );

    final SubscriptionReference reference1 = subscription1.createReference();
    final SubscriptionReference reference2 = subscription1.createReference();
    final SubscriptionReference reference3 = subscription1.createReference();

    subscription1.release();

    assertTrue( reference1.hasBeenReleased() );
    assertTrue( reference2.hasBeenReleased() );
    assertTrue( reference3.hasBeenReleased() );
  }

  private void answerDestroySubscription( @Nonnull final AreaOfInterestService areaOfInterestService,
                                          @Nonnull final Subscription subscription )
  {
    doAnswer( i ->
              {
                subscription.delete();
                return null;
              } ).
      when( areaOfInterestService ).destroySubscription( eq( subscription ) );
  }
}
