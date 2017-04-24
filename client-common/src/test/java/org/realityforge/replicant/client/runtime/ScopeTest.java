package org.realityforge.replicant.client.runtime;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.client.ChannelDescriptor;
import org.testng.annotations.Test;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ScopeTest
{
  enum TestGraph
  {
    A,
    B
  }

  @Test
  public void basicScopeOperation()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final String name = ValueUtil.randomString();

    final Scope scope = new Scope( areaOfInterestService, name );

    assertEquals( scope.isActive(), true );
    assertEquals( scope.hasBeenReleased(), false );
    assertEquals( scope.getReferenceCount(), 0 );
    assertEquals( scope.getSubscriptionReferenceCount(), 0 );

    answerDestroyScope( areaOfInterestService, scope );

    //Delete the scope
    scope.release();

    verify( areaOfInterestService, only() ).destroyScope( scope );

    assertEquals( scope.isActive(), false );
    assertEquals( scope.hasBeenReleased(), true );

    reset( areaOfInterestService );

    scope.release();

    verify( areaOfInterestService, never() ).destroyScope( scope );
  }

  @Test
  public void scopeReferences()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final Scope scope = new Scope( areaOfInterestService, ValueUtil.randomString() );

    assertEquals( scope.getReferenceCount(), 0 );
    assertEquals( scope.getSubscriptionReferenceCount(), 0 );

    final ScopeReference reference = scope.createReference();

    assertEquals( scope.getReferenceCount(), 1 );
    assertEquals( scope.getSubscriptionReferenceCount(), 0 );
    assertEquals( scope.isActive(), true );

    // Create a second reference and let it be managed appropriately
    {
      final ScopeReference reference2 = scope.createReference();
      assertEquals( scope.getReferenceCount(), 2 );

      assertEquals( reference2.hasBeenReleased(), false );
      reference2.release();
      assertEquals( reference2.hasBeenReleased(), true );

      assertThrows( ReferenceReleasedException.class, reference2::getScope );

      // Scope still has active reference so it should be alive
      assertEquals( scope.getReferenceCount(), 1 );
      assertEquals( scope.isActive(), true );
    }

    answerDestroyScope( areaOfInterestService, scope );

    assertEquals( reference.hasBeenReleased(), false );
    reference.release();
    assertEquals( reference.hasBeenReleased(), true );

    // Releasing the last reference removes the scope
    verify( areaOfInterestService, only() ).destroyScope( scope );

    assertEquals( scope.isActive(), false );
    assertEquals( scope.getReferenceCount(), 0 );
  }

  @Test
  public void scopeReferencesReleasedDuringScopeRelease()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final Scope scope = new Scope( areaOfInterestService, ValueUtil.randomString() );

    final ScopeReference reference = scope.createReference();

    answerDestroyScope( areaOfInterestService, scope );

    //Delete the scope
    scope.release();

    verify( areaOfInterestService, only() ).destroyScope( scope );

    assertEquals( scope.isActive(), false );
    assertEquals( reference.hasBeenReleased(), true );
  }

  @Test
  public void releasedReferencesIgnoredDuringScopeRelease()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final Scope scope = new Scope( areaOfInterestService, ValueUtil.randomString() );

    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraph.A, null );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );

    assertEquals( scope.getSubscriptionReferenceCount(), 0 );
    assertEquals( scope.getRequiredScopes().size(), 0 );
    assertEquals( scope.getRequiredSubscriptions().size(), 0 );
    final SubscriptionReference reference1 = scope.requireSubscription( subscription );
    assertEquals( scope.getRequiredScopes().size(), 0 );
    assertEquals( scope.getRequiredSubscriptions().size(), 1 );
    assertEquals( scope.getSubscriptionReferenceCount(), 1 );
    assertEquals( reference1.hasBeenReleased(), false );

    answerDestroySubscription( areaOfInterestService, subscription );

    reference1.release();

    verify( areaOfInterestService, only() ).destroySubscription( subscription );

    assertEquals( scope.getSubscriptionReferenceCount(), 1 );
    assertEquals( reference1.hasBeenReleased(), true );

    assertEquals( scope.isActive(), true );
    assertEquals( scope.hasBeenReleased(), false );

    reset( areaOfInterestService );
    answerDestroyScope( areaOfInterestService, scope );

    //Delete the scope
    scope.release();

    verify( areaOfInterestService, only() ).destroyScope( scope );

    assertEquals( scope.isActive(), false );
    assertEquals( scope.hasBeenReleased(), true );
  }

  @Test
  public void getRequiredSubscriptionsByGraph()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final Scope scope = new Scope( areaOfInterestService, ValueUtil.randomString() );

    final ChannelDescriptor descriptor1 = new ChannelDescriptor( TestGraph.A, 1 );
    final ChannelDescriptor descriptor2 = new ChannelDescriptor( TestGraph.A, 2 );
    final ChannelDescriptor descriptor3 = new ChannelDescriptor( TestGraph.B, 3 );
    final ChannelDescriptor descriptor4 = new ChannelDescriptor( TestGraph.B, 4 );
    final ChannelDescriptor descriptor5 = new ChannelDescriptor( TestGraph.B, 5 );
    final Subscription subscription1 = new Subscription( areaOfInterestService, descriptor1 );
    final Subscription subscription2 = new Subscription( areaOfInterestService, descriptor2 );
    final Subscription subscription3 = new Subscription( areaOfInterestService, descriptor3 );
    final Subscription subscription4 = new Subscription( areaOfInterestService, descriptor4 );
    final Subscription subscription5 = new Subscription( areaOfInterestService, descriptor5 );

    assertEquals( scope.getSubscriptionReferenceCount(), 0 );

    assertEquals( scope.getRequiredSubscriptionsByGraph( TestGraph.A ).size(), 0 );
    assertEquals( scope.getRequiredSubscriptionsByGraph( TestGraph.B ).size(), 0 );

    scope.requireSubscription( subscription1 );
    scope.requireSubscription( subscription2 );

    assertEquals( scope.getSubscriptionReferenceCount(), 2 );

    assertEquals( scope.getRequiredSubscriptionsByGraph( TestGraph.A ).size(), 2 );
    assertEquals( scope.getRequiredSubscriptionsByGraph( TestGraph.B ).size(), 0 );

    scope.requireSubscription( subscription3 );
    scope.requireSubscription( subscription4 );
    scope.requireSubscription( subscription5 );

    assertEquals( scope.getSubscriptionReferenceCount(), 5 );

    assertEquals( scope.getRequiredSubscriptionsByGraph( TestGraph.A ).size(), 2 );
    final List<Subscription> graphBSubscriptions = scope.getRequiredSubscriptionsByGraph( TestGraph.B );
    assertEquals( graphBSubscriptions.size(), 3 );

    final List<ChannelDescriptor> descriptors =
      graphBSubscriptions.stream().map( Subscription::getDescriptor ).collect( Collectors.toList() );

    assertTrue( descriptors.contains( descriptor3 ) );
    assertTrue( descriptors.contains( descriptor4 ) );
    assertTrue( descriptors.contains( descriptor5 ) );
  }

  @Test
  public void purgeReleasedSubscriptionReferences()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final Scope scope = new Scope( areaOfInterestService, ValueUtil.randomString() );

    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraph.A, null );
    final Subscription subscription = new Subscription( areaOfInterestService, descriptor );

    assertEquals( scope.getSubscriptionReferenceCount(), 0 );
    final SubscriptionReference reference1 = scope.requireSubscription( subscription );
    assertEquals( scope.getSubscriptionReferenceCount(), 1 );

    answerDestroySubscription( areaOfInterestService, subscription );
    reference1.release();
    verify( areaOfInterestService, only() ).destroySubscription( subscription );
    assertEquals( scope.getSubscriptionReferenceCount(), 1 );
    assertEquals( reference1.hasBeenReleased(), true );

    scope.purgeReleasedSubscriptionReferences();

    assertEquals( scope.getSubscriptionReferenceCount(), 0 );
  }

  @Test
  public void scopeRequirements()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final String name1 = ValueUtil.randomString();
    final String name2 = ValueUtil.randomString();

    final Scope scope1 = new Scope( areaOfInterestService, name1 );
    final Scope scope2 = new Scope( areaOfInterestService, name2 );

    assertEquals( scope1.isActive(), true );
    assertEquals( scope2.isActive(), true );
    assertEquals( scope1.getReferenceCount(), 0 );
    assertEquals( scope2.getReferenceCount(), 0 );

    assertEquals( scope1.isScopeRequired( scope2 ), false );

    assertEquals( scope1.getRequiredScopes().size(), 0 );
    assertEquals( scope1.getRequiredSubscriptions().size(), 0 );
    final ScopeReference reference = scope1.requireScope( scope2 );
    assertEquals( scope1.getRequiredScopes().size(), 1 );
    assertEquals( scope1.getRequiredSubscriptions().size(), 0 );

    assertEquals( scope1.isScopeRequired( scope2 ), true );

    assertEquals( scope1.isActive(), true );
    assertEquals( scope2.isActive(), true );
    assertEquals( scope1.getReferenceCount(), 0 );
    assertEquals( scope2.getReferenceCount(), 1 );

    assertThrows( ScopeAlreadyRequiredException.class, () -> scope1.requireScope( scope2 ) );

    answerDestroyScope( areaOfInterestService, scope1 );
    answerDestroyScope( areaOfInterestService, scope2 );

    //Delete the scope
    scope1.release();

    verify( areaOfInterestService ).destroyScope( scope1 );
    verify( areaOfInterestService ).destroyScope( scope2 );

    assertEquals( scope1.isActive(), false );
    assertEquals( reference.hasBeenReleased(), true );
    assertEquals( scope2.isActive(), false );
    assertEquals( reference.hasBeenReleased(), true );
    assertEquals( scope1.getReferenceCount(), 0 );
    assertEquals( scope2.getReferenceCount(), 0 );

    assertThrows( ScopeInactiveException.class, () -> scope1.requireScope( scope2 ) );
  }

  @Test
  public void subscriptionRequirements()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );
    final String name = ValueUtil.randomString();

    final Scope scope = new Scope( areaOfInterestService, name );
    final Subscription subscription =
      new Subscription( areaOfInterestService, new ChannelDescriptor( TestGraph.A, null ) );

    assertEquals( scope.isActive(), true );
    assertEquals( subscription.isActive(), true );
    assertEquals( scope.getReferenceCount(), 0 );
    assertEquals( subscription.getReferenceCount(), 0 );

    assertEquals( scope.isSubscriptionRequired( subscription ), false );

    final SubscriptionReference reference = scope.requireSubscription( subscription );

    assertEquals( scope.isSubscriptionRequired( subscription ), true );

    assertEquals( scope.isActive(), true );
    assertEquals( subscription.isActive(), true );
    assertEquals( scope.getReferenceCount(), 0 );
    assertEquals( subscription.getReferenceCount(), 1 );

    assertThrows( SubscriptionAlreadyRequiredException.class, () -> scope.requireSubscription( subscription ) );

    answerDestroyScope( areaOfInterestService, scope );
    answerDestroySubscription( areaOfInterestService, subscription );

    //Delete the scope
    scope.release();

    verify( areaOfInterestService ).destroyScope( scope );

    assertEquals( scope.isActive(), false );
    assertEquals( reference.hasBeenReleased(), true );
    assertEquals( subscription.isActive(), false );
    assertEquals( reference.hasBeenReleased(), true );
    assertEquals( scope.getReferenceCount(), 0 );
    assertEquals( subscription.getReferenceCount(), 0 );

    assertThrows( ScopeInactiveException.class, () -> scope.requireSubscription( subscription ) );
  }

  @Test
  public void releaseMultiple()
  {
    final AreaOfInterestService areaOfInterestService = mock( AreaOfInterestService.class );

    final Scope scope = new Scope( areaOfInterestService, ValueUtil.randomString() );

    final ScopeReference reference1 = scope.createReference();
    final ScopeReference reference2 = scope.createReference();
    final ScopeReference reference3 = scope.createReference();

    scope.release();

    assertEquals( reference1.hasBeenReleased(), true );
    assertEquals( reference2.hasBeenReleased(), true );
    assertEquals( reference3.hasBeenReleased(), true );
  }

  private void answerDestroyScope( @Nonnull final AreaOfInterestService areaOfInterestService,
                                   @Nonnull final Scope scope )
  {
    doAnswer( i ->
              {
                scope.delete();
                return null;
              } ).
      when( areaOfInterestService ).destroyScope( eq( scope ) );
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
