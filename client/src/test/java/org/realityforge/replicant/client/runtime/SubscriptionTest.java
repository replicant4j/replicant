package org.realityforge.replicant.client.runtime;

import javax.annotation.Nonnull;
import org.realityforge.replicant.client.ChannelDescriptor;
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
    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraph.A, null );

    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );

    assertEquals( subscription.getDescriptor(), descriptor );
    assertEquals( subscription.isActive(), true );
    assertEquals( subscription.hasBeenReleased(), false );
    assertEquals( subscription.getReferenceCount(), 0 );

    assertEquals( subscription.getFilter(), null );

    // Update subscription works ....

    final Object filter = new Object();
    subscription.setFilter( filter );

    assertEquals( subscription.getFilter(), filter );

    answerDestroySubscription( areaOfInterestService, subscription );

    //Delete the subscription
    subscription.release();

    verify( areaOfInterestService, only() ).destroySubscription( subscription );

    assertEquals( subscription.isActive(), false );
    assertEquals( subscription.hasBeenReleased(), true );

    reset( areaOfInterestService );

    subscription.release();

    verify( areaOfInterestService, never() ).destroySubscription( subscription );
  }

  @Test
  public void subscriptionReferences()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraph.A, null );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );

    assertEquals( subscription.getReferenceCount(), 0 );

    final SubscriptionReference reference = subscription.createReference();

    assertEquals( subscription.getReferenceCount(), 1 );
    assertEquals( subscription.isActive(), true );

    // Create a second reference and let it be managed appropriately
    {
      final SubscriptionReference reference2 = subscription.createReference();
      assertEquals( subscription.getReferenceCount(), 2 );

      assertEquals( reference2.hasBeenReleased(), false );
      reference2.release();
      assertEquals( reference2.hasBeenReleased(), true );

      assertThrows( ReferenceReleasedException.class, reference2::getSubscription );

      // Subscription still has active reference so it should be alive
      assertEquals( subscription.getReferenceCount(), 1 );
      assertEquals( subscription.isActive(), true );
    }

    answerDestroySubscription( areaOfInterestService, subscription );

    assertEquals( reference.hasBeenReleased(), false );
    reference.release();
    assertEquals( reference.hasBeenReleased(), true );

    // Releasing the last reference removes the subscription
    verify( areaOfInterestService, only() ).destroySubscription( subscription );

    assertEquals( subscription.isActive(), false );
    assertEquals( subscription.getReferenceCount(), 0 );
  }

  @Test
  public void subscriptionReferencesReleasedDuringSubscriptionRelease()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraph.A, null );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );

    final SubscriptionReference reference = subscription.createReference();

    answerDestroySubscription( areaOfInterestService, subscription );

    //Delete the subscription
    subscription.release();

    verify( areaOfInterestService, only() ).destroySubscription( subscription );

    assertEquals( subscription.isActive(), false );
    assertEquals( reference.hasBeenReleased(), true );
  }

  @Test
  public void subscriptionRequirements()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final ChannelDescriptor descriptor1 = new ChannelDescriptor( TestGraph.A, 1 );
    final ChannelDescriptor descriptor2 = new ChannelDescriptor( TestGraph.A, 2 );

    final Subscription subscription1 = new Subscription( areaOfInterestService, descriptor1 );
    final Subscription subscription2 = new Subscription( areaOfInterestService, descriptor2 );

    assertEquals( subscription1.isActive(), true );
    assertEquals( subscription2.isActive(), true );
    assertEquals( subscription1.getReferenceCount(), 0 );
    assertEquals( subscription2.getReferenceCount(), 0 );

    assertEquals( subscription1.isSubscriptionRequired( subscription2 ), false );
    assertEquals( subscription1.getRequiredSubscriptions().size(), 0 );

    final SubscriptionReference reference = subscription1.requireSubscription( subscription2 );

    assertEquals( subscription1.isSubscriptionRequired( subscription2 ), true );
    assertEquals( subscription1.getRequiredSubscriptions().size(), 1 );
    assertEquals( subscription1.getRequiredSubscriptions().contains( subscription2 ), true );

    assertEquals( subscription1.isActive(), true );
    assertEquals( subscription2.isActive(), true );
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

    assertEquals( subscription1.isActive(), false );
    assertEquals( reference.hasBeenReleased(), true );
    assertEquals( subscription2.isActive(), false );
    assertEquals( reference.hasBeenReleased(), true );
    assertEquals( subscription1.getReferenceCount(), 0 );
    assertEquals( subscription2.getReferenceCount(), 0 );

    assertThrows( SubscriptionInactiveException.class, () -> subscription1.requireSubscription( subscription2 ) );
  }

  @Test
  public void releaseMultiple()
  {
    final Subscription subscription1 =
      new Subscription( mock( AreaOfInterestService.class ), new ChannelDescriptor( TestGraph.A ) );

    final SubscriptionReference reference1 = subscription1.createReference();
    final SubscriptionReference reference2 = subscription1.createReference();
    final SubscriptionReference reference3 = subscription1.createReference();

    subscription1.release();

    assertEquals( reference1.hasBeenReleased(), true );
    assertEquals( reference2.hasBeenReleased(), true );
    assertEquals( reference3.hasBeenReleased(), true );
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
