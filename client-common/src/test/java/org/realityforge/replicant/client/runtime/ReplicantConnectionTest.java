package org.realityforge.replicant.client.runtime;

import arez.Arez;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.client.AbstractReplicantTest;
import org.realityforge.replicant.client.EntityLocator;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantConnectionTest
  extends AbstractReplicantTest
  implements IHookable
{
  enum TestGraph
  {
    A
  }

  enum TestGraph2
  {
    B
  }

  @Override
  public void run( final IHookCallBack callBack, final ITestResult testResult )
  {
    Arez.context().safeAction( () -> callBack.runTestMethod( testResult ) );
  }

  static class TestRuntime
    implements ReplicantConnection
  {
    private AreaOfInterestService _areaOfInterestService = new Arez_AreaOfInterestService();
    private ContextConverger _contextConverger = mock( ContextConverger.class );
    private EntityLocator _entityLocator = mock( EntityLocator.class );
    private EntitySubscriptionManager _subscriptionManager = mock( EntitySubscriptionManager.class );
    private ReplicantClientSystem _replicantClientSystem = mock( ReplicantClientSystem.class );

    @Override
    public void connect()
    {
    }

    @Override
    public void disconnect()
    {
    }

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
    public EntityLocator getEntityLocator()
    {
      return _entityLocator;
    }

    @Nonnull
    @Override
    public EntitySubscriptionManager getSubscriptionManager()
    {
      return _subscriptionManager;
    }

    @Nonnull
    @Override
    public ReplicantClientSystem getReplicantClientSystem()
    {
      return _replicantClientSystem;
    }
  }

  @Test
  public void instanceSubscriptionToValues()
  {
    final TestRuntime r = new TestRuntime();

    {
      reset( r.getEntityLocator() );
      when( r.getEntityLocator().findByID( String.class, 1 ) ).thenReturn( null );

      final List<Object> list =
        r.instanceSubscriptionToValues( String.class, 1, Stream::of ).
          collect( Collectors.toList() );
      assertEquals( list.size(), 0 );
    }

    {
      final String entity2 = ValueUtil.randomString();

      reset( r.getEntityLocator() );
      when( r.getEntityLocator().findByID( String.class, 2 ) ).thenReturn( entity2 );

      final List<Object> list =
        r.instanceSubscriptionToValues( String.class, 2, Stream::of ).
          collect( Collectors.toList() );
      assertEquals( list.size(), 1 );
      assertTrue( list.contains( entity2 ) );
    }
  }
/*
  @Test
  public void doConvergeCrossDataSourceSubscriptions()
  {
    final String filter = ValueUtil.randomString();

    final TestRuntime r = new TestRuntime();
    final AreaOfInterestService aoiService = r.getAreaOfInterestService();
    final ChannelAddress descriptor1 = new ChannelAddress( TestGraph.A, ValueUtil.nextID() );
    final ChannelAddress descriptor2 = new ChannelAddress( TestGraph.A, ValueUtil.nextID() );
    final ChannelAddress descriptor3 = new ChannelAddress( TestGraph.A, ValueUtil.nextID() );
    final ChannelAddress descriptor4 = new ChannelAddress( TestGraph2.B, descriptor1.getId() );
    final ChannelAddress descriptor5 = new ChannelAddress( TestGraph2.B, descriptor2.getId() );
    final ChannelAddress descriptor6 = new ChannelAddress( TestGraph2.B, descriptor3.getId() );

    // These descriptors have differing filters so Q should be updated
    final ChannelAddress descriptorP = new ChannelAddress( TestGraph.A, ValueUtil.nextID() );
    final ChannelAddress descriptorQ = new ChannelAddress( TestGraph2.B, descriptorP.getId() );

    final Subscription subscription1 = new Subscription( aoiService, descriptor1 );
    final Subscription subscription2 = new Subscription( aoiService, descriptor2 );
    final Subscription subscription4 = new Subscription( aoiService, descriptor4 );
    final Subscription subscription5 = new Subscription( aoiService, descriptor5 );
    final Subscription subscription6 = new Subscription( aoiService, descriptor6 );

    final Subscription subscriptionP = new Subscription( aoiService, descriptorP );
    final Subscription subscriptionQ = new Subscription( aoiService, descriptorQ );

    subscription1.setFilter( filter );
    subscription2.setFilter( filter );
    subscription4.setFilter( filter );
    subscription5.setFilter( filter );
    subscription6.setFilter( filter );

    subscriptionP.setFilter( filter );
    subscriptionQ.setFilter( ValueUtil.randomString() );

    // Requires as yet uncreated subscription (subscription4)
    final SubscriptionReference reference1 = scope.requireSubscription( subscription1 );

    // Matches subscription5
    final SubscriptionReference reference2 = scope.requireSubscription( subscription2 );

    // next one should be retained
    final SubscriptionReference reference5 = scope.requireSubscription( subscription5 );

    // Next subscription should be released
    scope.requireSubscription( subscription6 );

    final SubscriptionReference referenceP = scope.requireSubscription( subscriptionP );
    final SubscriptionReference referenceQ = scope.requireSubscription( subscriptionQ );

    when( aoiService.findSubscription( descriptorQ ) ).thenReturn( subscriptionQ );
    when( aoiService.createSubscription( descriptor4, filter ) ).thenReturn( subscription4 );
    when( aoiService.createSubscriptionReference( descriptor1 ) ).thenReturn( reference1 );
    when( aoiService.createSubscriptionReference( descriptor2 ) ).thenReturn( reference2 );
    when( aoiService.createSubscriptionReference( descriptor4 ) ).thenReturn( subscription4.createReference() );
    when( aoiService.createSubscriptionReference( descriptor5 ) ).thenReturn( reference5 );
    when( aoiService.createSubscriptionReference( descriptorP ) ).thenReturn( referenceP );
    when( aoiService.createSubscriptionReference( descriptorQ ) ).thenReturn( referenceQ );

    r.doConvergeCrossDataSourceSubscriptions( scope, TestGraph.A, TestGraph2.B, filter, Stream::of );

    assertEquals( scope.getRequiredSubscriptions().size(), 5 );
    assertTrue( scope.getRequiredSubscriptions().contains( subscription5 ) );
    assertTrue( scope.getRequiredSubscriptions().stream().anyMatch( s -> s.getDescriptor().equals( descriptor1 ) ) );
    assertTrue( scope.getRequiredSubscriptions().stream().anyMatch( s -> s.getDescriptor().equals( descriptor2 ) ) );
    assertTrue( scope.getRequiredSubscriptions().stream().anyMatch( s -> s.getDescriptor().equals( descriptor5 ) ) );
    assertTrue( scope.getRequiredSubscriptions().stream().anyMatch( s -> s.getDescriptor().equals( descriptorP ) ) );
    assertTrue( scope.getRequiredSubscriptions().stream().anyMatch( s -> s.getDescriptor().equals( descriptorQ ) ) );
    assertFalse( scope.getRequiredSubscriptions().contains( subscription6 ) );

    verify( aoiService, atLeast( 1 ) ).updateSubscription( subscriptionQ, filter );

    //Will be called multiple times as we are dealing with mocks that do not callback to subscription to update state
    verify( aoiService, atLeast( 1 ) ).destroySubscription( subscription6 );
    verify( aoiService ).createSubscription( descriptor4, filter );
  }
  */
}
