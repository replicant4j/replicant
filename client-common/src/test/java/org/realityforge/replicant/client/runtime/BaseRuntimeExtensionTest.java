package org.realityforge.replicant.client.runtime;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.client.ChannelDescriptor;
import org.realityforge.replicant.client.EntityRepository;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class BaseRuntimeExtensionTest
{
  enum TestGraph
  {
    A
  }

  enum TestGraph2
  {
    B
  }

  static class TestRuntime
    implements BaseRuntimeExtension
  {
    private AreaOfInterestService _areaOfInterestService = mock( AreaOfInterestService.class );
    private ContextConverger _contextConverger = mock( ContextConverger.class );
    private EntityRepository _repository = mock( EntityRepository.class );

    @Nonnull
    @Override
    public AreaOfInterestService getAreaOfInterestService()
    {
      return _areaOfInterestService;
    }

    @Nonnull
    @Override
    public ContextConverger getContextConverger()
    {
      return _contextConverger;
    }

    @Nonnull
    @Override
    public EntityRepository getRepository()
    {
      return _repository;
    }
  }

  @Test
  public void subscribe()
  {
    final TestRuntime r = new TestRuntime();
    final AreaOfInterestService aoiService = r.getAreaOfInterestService();
    final Scope scope = new Scope( aoiService, ValueUtil.randomString() );
    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraph.A );
    final Subscription subscription = new Subscription( aoiService, descriptor );
    final SubscriptionReference reference = subscription.createReference();
    final String filter = ValueUtil.randomString();

    // No existing subscription
    {
      reset( aoiService );

      when( aoiService.findSubscription( descriptor ) ).thenReturn( null );
      when( aoiService.createSubscription( descriptor, filter ) ).thenReturn( subscription );
      when( aoiService.createSubscriptionReference( scope, descriptor ) ).thenReturn( reference );

      r.subscribe( scope, descriptor, filter );

      verify( aoiService ).createSubscription( descriptor, filter );
      verify( aoiService, never() ).updateSubscription( any(), eq( filter ) );
      verify( aoiService ).createSubscriptionReference( scope, descriptor );
    }

    //Existing subscription, same filter
    {
      subscription.setFilter( filter );
      reset( aoiService );

      when( aoiService.findSubscription( descriptor ) ).thenReturn( subscription );
      when( aoiService.createSubscriptionReference( scope, descriptor ) ).thenReturn( reference );

      r.subscribe( scope, descriptor, filter );

      verify( aoiService, never() ).createSubscription( descriptor, filter );
      verify( aoiService, never() ).updateSubscription( any(), eq( filter ) );
      verify( aoiService ).createSubscriptionReference( scope, descriptor );
    }

    //Existing subscription, different filter
    {
      subscription.setFilter( ValueUtil.randomString() );
      reset( aoiService );

      when( aoiService.findSubscription( descriptor ) ).thenReturn( subscription );
      when( aoiService.createSubscriptionReference( scope, descriptor ) ).thenReturn( reference );

      r.subscribe( scope, descriptor, filter );

      verify( aoiService, never() ).createSubscription( descriptor, filter );
      verify( aoiService ).updateSubscription( subscription, filter );
      verify( aoiService ).createSubscriptionReference( scope, descriptor );
    }


    //Existing subscription, same filter, subscription already required. Should return existing
    {
      subscription.setFilter( filter );
      reset( aoiService );

      when( aoiService.findSubscription( descriptor ) ).thenReturn( subscription );

      final SubscriptionReference subscriptionReference = scope.requireSubscription( subscription );

      final SubscriptionReference result = r.subscribe( scope, descriptor, filter );

      assertEquals( result, subscriptionReference );

      verify( aoiService, never() ).createSubscription( descriptor, filter );
      verify( aoiService, never() ).updateSubscription( subscription, filter );
      verify( aoiService, never() ).createSubscriptionReference( scope, descriptor );
    }
  }

  @Test
  public void instanceSubscriptionToValues()
  {
    final TestRuntime r = new TestRuntime();
    final AreaOfInterestService aoiService = r.getAreaOfInterestService();
    final ChannelDescriptor descriptor1 = new ChannelDescriptor( TestGraph.A, 1 );
    final ChannelDescriptor descriptor2 = new ChannelDescriptor( TestGraph.A, 2 );
    final Subscription subscription1 = new Subscription( aoiService, descriptor1 );
    final Subscription subscription2 = new Subscription( aoiService, descriptor2 );

    {
      reset( r.getRepository() );
      when( r.getRepository().findByID( String.class, 1 ) ).thenReturn( null );

      final List<Object> list =
        r.instanceSubscriptionToValues( subscription1, String.class, Stream::of ).
          collect( Collectors.toList() );
      assertEquals( list.size(), 0 );
    }

    {
      final String entity2 = ValueUtil.randomString();

      reset( r.getRepository() );
      when( r.getRepository().findByID( String.class, 2 ) ).thenReturn( entity2 );

      final List<Object> list =
        r.instanceSubscriptionToValues( subscription2, String.class, Stream::of ).
          collect( Collectors.toList() );
      assertEquals( list.size(), 1 );
      assertTrue( list.contains( entity2 ) );
    }
  }

  @Test
  public void doConvergeCrossDataSourceSubscriptions()
  {
    final TestRuntime r = new TestRuntime();
    final AreaOfInterestService aoiService = r.getAreaOfInterestService();
    final Scope scope = new Scope( aoiService, ValueUtil.randomString() );
    final ChannelDescriptor descriptor1 = new ChannelDescriptor( TestGraph.A, 1 );
    final ChannelDescriptor descriptor2 = new ChannelDescriptor( TestGraph.A, 2 );
    final ChannelDescriptor descriptor4 = new ChannelDescriptor( TestGraph2.B, 1 );
    final ChannelDescriptor descriptor5 = new ChannelDescriptor( TestGraph2.B, 2 );
    final ChannelDescriptor descriptor6 = new ChannelDescriptor( TestGraph2.B, 3 );

    final Subscription subscription1 = new Subscription( aoiService, descriptor1 );
    final Subscription subscription2 = new Subscription( aoiService, descriptor2 );
    final Subscription subscription4 = new Subscription( aoiService, descriptor4 );
    final Subscription subscription5 = new Subscription( aoiService, descriptor5 );
    final Subscription subscription6 = new Subscription( aoiService, descriptor6 );

    // Requires as yet uncreated subscription (subscription4)
    final SubscriptionReference reference1 = scope.requireSubscription( subscription1 );

    // Matches subscription5
    final SubscriptionReference reference2 = scope.requireSubscription( subscription2 );

    // next one should be retained
    final SubscriptionReference reference5 = scope.requireSubscription( subscription5 );

    // Next subscription should be released
    final SubscriptionReference reference6 = scope.requireSubscription( subscription6 );

    final String filter = ValueUtil.randomString();

    when( aoiService.createSubscription( descriptor4, filter ) ).thenReturn( subscription4 );
    when( aoiService.createSubscriptionReference( scope, descriptor1 ) ).thenReturn( reference1 );
    when( aoiService.createSubscriptionReference( scope, descriptor2 ) ).thenReturn( reference2 );
    when( aoiService.createSubscriptionReference( scope, descriptor4 ) ).thenReturn( subscription4.createReference() );
    when( aoiService.createSubscriptionReference( scope, descriptor5 ) ).thenReturn( reference5 );

    r.doConvergeCrossDataSourceSubscriptions( scope,
                                              TestGraph.A,
                                              TestGraph2.B,
                                              filter,
                                              s -> Stream.of( s.getDescriptor().getID() ) );

    assertEquals( scope.getRequiredSubscriptions().size(), 3 );
    assertTrue( scope.getRequiredSubscriptions().contains( subscription5 ) );
    assertTrue( scope.getRequiredSubscriptions().stream().anyMatch( s -> s.getDescriptor().equals( descriptor1 ) ) );
    assertTrue( scope.getRequiredSubscriptions().stream().anyMatch( s -> s.getDescriptor().equals( descriptor2 ) ) );
    assertTrue( scope.getRequiredSubscriptions().stream().anyMatch( s -> s.getDescriptor().equals( descriptor5 ) ) );
    assertFalse( scope.getRequiredSubscriptions().contains( subscription6 ) );

    //Will be called multiple times as we are dealing with mocks that do not callback to subscription to update state
    verify( aoiService, atLeast( 1 ) ).destroySubscription( subscription6 );
    verify( aoiService ).createSubscription( descriptor4, filter );
  }
}
