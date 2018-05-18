package replicant;

import arez.Disposable;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantContextTest
  extends AbstractReplicantTest
{
  @Test
  public void schemas()
  {
    final ReplicantContext context = Replicant.context();
    safeAction( () -> assertEquals( context.getSchemas().size(), 0 ) );
    final SystemSchema schema1 =
      new SystemSchema( ValueUtil.randomInt(),
                        ValueUtil.randomString(),
                        new ChannelSchema[ 0 ],
                        new EntitySchema[ 0 ] );

    safeAction( () -> context.getSchemaService().registerSchema( schema1 ) );

    safeAction( () -> assertEquals( context.getSchemas().size(), 1 ) );
    safeAction( () -> assertEquals( context.getSchemas().contains( schema1 ), true ) );
  }

  @Test
  public void areasOfInterest()
  {
    // Pause scheduler so Autoruns don't auto-converge
    pauseScheduler();

    final ReplicantContext context = Replicant.context();
    final ChannelAddress address = new ChannelAddress( G.G1 );
    final String filter = ValueUtil.randomString();
    final String filter2 = ValueUtil.randomString();

    safeAction( () -> {
      assertEquals( context.getAreasOfInterest().size(), 0 );
      assertEquals( context.findAreaOfInterestByAddress( address ), null );

      final AreaOfInterest areaOfInterest = context.createOrUpdateAreaOfInterest( address, filter );

      assertEquals( areaOfInterest.getFilter(), filter );
      assertEquals( context.getAreasOfInterest().size(), 1 );
      assertEquals( context.findAreaOfInterestByAddress( address ), areaOfInterest );

      final AreaOfInterest areaOfInterest2 = context.createOrUpdateAreaOfInterest( address, filter2 );

      assertEquals( areaOfInterest2, areaOfInterest );
      assertEquals( areaOfInterest2.getFilter(), filter2 );
      assertEquals( context.getAreasOfInterest().size(), 1 );
      assertEquals( context.findAreaOfInterestByAddress( address ), areaOfInterest2 );
      assertEquals( context.findAreaOfInterestByAddress( address ), areaOfInterest2 );
    } );
  }

  @Test
  public void entities()
  {
    final ReplicantContext context = Replicant.context();

    safeAction( () -> {
      assertEquals( context.findAllEntityTypes().size(), 0 );
      assertEquals( context.findAllEntitiesByType( A.class ).size(), 0 );
      assertEquals( context.findAllEntitiesByType( B.class ).size(), 0 );
      assertEquals( context.findEntityByTypeAndId( A.class, 1 ), null );
      assertEquals( context.findEntityByTypeAndId( A.class, 2 ), null );
      assertEquals( context.findEntityByTypeAndId( B.class, 47 ), null );

      final Entity entity1 = context.findOrCreateEntity( A.class, 1 );

      assertEquals( entity1.getName(), "A/1" );
      assertEquals( context.findAllEntityTypes().size(), 1 );
      assertEquals( context.findAllEntitiesByType( A.class ).size(), 1 );
      assertEquals( context.findAllEntitiesByType( B.class ).size(), 0 );
      assertEquals( context.findEntityByTypeAndId( A.class, 1 ), entity1 );
      assertEquals( context.findEntityByTypeAndId( A.class, 2 ), null );
      assertEquals( context.findEntityByTypeAndId( B.class, 47 ), null );

      final Entity entity2 = context.findOrCreateEntity( "Super-dee-duper", A.class, 2 );

      assertEquals( entity2.getName(), "Super-dee-duper" );
      assertEquals( context.findAllEntityTypes().size(), 1 );
      assertEquals( context.findAllEntitiesByType( A.class ).size(), 2 );
      assertEquals( context.findAllEntitiesByType( B.class ).size(), 0 );
      assertEquals( context.findEntityByTypeAndId( A.class, 1 ), entity1 );
      assertEquals( context.findEntityByTypeAndId( A.class, 2 ), entity2 );
      assertEquals( context.findEntityByTypeAndId( B.class, 47 ), null );

      final Entity entity3 = context.findOrCreateEntity( B.class, 47 );

      assertEquals( context.findAllEntityTypes().size(), 2 );
      assertEquals( context.findAllEntitiesByType( A.class ).size(), 2 );
      assertEquals( context.findAllEntitiesByType( B.class ).size(), 1 );
      assertEquals( context.findEntityByTypeAndId( A.class, 1 ), entity1 );
      assertEquals( context.findEntityByTypeAndId( A.class, 2 ), entity2 );
      assertEquals( context.findEntityByTypeAndId( B.class, 47 ), entity3 );

      Disposable.dispose( entity1 );

      assertEquals( context.findAllEntityTypes().size(), 2 );
      assertEquals( context.findAllEntitiesByType( A.class ).size(), 1 );
      assertEquals( context.findAllEntitiesByType( B.class ).size(), 1 );
      assertEquals( context.findEntityByTypeAndId( A.class, 1 ), null );
      assertEquals( context.findEntityByTypeAndId( A.class, 2 ), entity2 );
      assertEquals( context.findEntityByTypeAndId( B.class, 47 ), entity3 );

      Disposable.dispose( entity2 );

      assertEquals( context.findAllEntityTypes().size(), 1 );
      assertEquals( context.findAllEntitiesByType( A.class ).size(), 0 );
      assertEquals( context.findAllEntitiesByType( B.class ).size(), 1 );
      assertEquals( context.findEntityByTypeAndId( A.class, 1 ), null );
      assertEquals( context.findEntityByTypeAndId( A.class, 2 ), null );
      assertEquals( context.findEntityByTypeAndId( B.class, 47 ), entity3 );

      Disposable.dispose( entity3 );

      assertEquals( context.findAllEntityTypes().size(), 0 );
      assertEquals( context.findAllEntitiesByType( A.class ).size(), 0 );
      assertEquals( context.findAllEntitiesByType( B.class ).size(), 0 );
      assertEquals( context.findEntityByTypeAndId( A.class, 1 ), null );
      assertEquals( context.findEntityByTypeAndId( A.class, 2 ), null );
      assertEquals( context.findEntityByTypeAndId( B.class, 47 ), null );
    } );
  }

  @Test
  public void subscriptions()
  {
    // Pause scheduler so Autoruns don't auto-converge
    pauseScheduler();

    final ReplicantContext context = Replicant.context();
    final ChannelAddress address1 = new ChannelAddress( G.G1 );
    final ChannelAddress address2 = new ChannelAddress( G.G2, 1 );
    final ChannelAddress address3 = new ChannelAddress( G.G2, 2 );
    final String filter1 = null;
    final String filter2 = ValueUtil.randomString();
    final String filter3 = ValueUtil.randomString();
    final boolean explicitSubscription1 = true;
    final boolean explicitSubscription2 = true;
    final boolean explicitSubscription3 = false;

    safeAction( () -> {
      assertEquals( context.getTypeSubscriptions().size(), 0 );
      assertEquals( context.getInstanceSubscriptions().size(), 0 );
      assertEquals( context.getInstanceSubscriptionIds( G.G2 ).size(), 0 );
      assertEquals( context.findSubscription( address1 ), null );
      assertEquals( context.findSubscription( address2 ), null );
      assertEquals( context.findSubscription( address3 ), null );

      final Subscription subscription1 = context.createSubscription( address1, filter1, explicitSubscription1 );

      assertEquals( subscription1.getAddress(), address1 );
      assertEquals( subscription1.getFilter(), filter1 );
      assertEquals( subscription1.isExplicitSubscription(), explicitSubscription1 );

      assertEquals( context.getTypeSubscriptions().size(), 1 );
      assertEquals( context.getInstanceSubscriptions().size(), 0 );
      assertEquals( context.getInstanceSubscriptionIds( G.G2 ).size(), 0 );
      assertEquals( context.findSubscription( address1 ), subscription1 );
      assertEquals( context.findSubscription( address2 ), null );
      assertEquals( context.findSubscription( address3 ), null );

      final Subscription subscription2 = context.createSubscription( address2, filter2, explicitSubscription2 );

      assertEquals( subscription2.getAddress(), address2 );
      assertEquals( subscription2.getFilter(), filter2 );
      assertEquals( subscription2.isExplicitSubscription(), explicitSubscription2 );

      assertEquals( context.getTypeSubscriptions().size(), 1 );
      assertEquals( context.getInstanceSubscriptions().size(), 1 );
      assertEquals( context.getInstanceSubscriptionIds( G.G2 ).size(), 1 );
      assertEquals( context.findSubscription( address1 ), subscription1 );
      assertEquals( context.findSubscription( address2 ), subscription2 );
      assertEquals( context.findSubscription( address3 ), null );

      final Subscription subscription3 = context.createSubscription( address3, filter3, explicitSubscription3 );

      assertEquals( subscription3.getAddress(), address3 );
      assertEquals( subscription3.getFilter(), filter3 );
      assertEquals( subscription3.isExplicitSubscription(), explicitSubscription3 );

      assertEquals( context.getTypeSubscriptions().size(), 1 );
      assertEquals( context.getInstanceSubscriptions().size(), 2 );
      assertEquals( context.getInstanceSubscriptionIds( G.G2 ).size(), 2 );
      assertEquals( context.findSubscription( address1 ), subscription1 );
      assertEquals( context.findSubscription( address2 ), subscription2 );
      assertEquals( context.findSubscription( address3 ), subscription3 );

      Disposable.dispose( subscription2 );
      Disposable.dispose( subscription3 );

      assertEquals( context.getTypeSubscriptions().size(), 1 );
      assertEquals( context.getInstanceSubscriptions().size(), 0 );
      assertEquals( context.getInstanceSubscriptionIds( G.G2 ).size(), 0 );
      assertEquals( context.findSubscription( address1 ), subscription1 );
      assertEquals( context.findSubscription( address2 ), null );
      assertEquals( context.findSubscription( address3 ), null );
    } );
  }

  @Test
  public void getSpy_whenSpiesDisabled()
    throws Exception
  {
    ReplicantTestUtil.disableSpies();

    final ReplicantContext context = new ReplicantContext();

    assertEquals( expectThrows( IllegalStateException.class, context::getSpy ).getMessage(),
                  "Replicant-0021: Attempting to get Spy but spies are not enabled." );
  }

  @Test
  public void getSpy()
    throws Exception
  {
    final ReplicantContext context = new ReplicantContext();

    assertFalse( context.willPropagateSpyEvents() );

    final Spy spy = context.getSpy();

    spy.addSpyEventHandler( new TestSpyEventHandler() );

    assertTrue( spy.willPropagateSpyEvents() );
    assertTrue( context.willPropagateSpyEvents() );

    ReplicantTestUtil.disableSpies();

    assertFalse( spy.willPropagateSpyEvents() );
    assertFalse( context.willPropagateSpyEvents() );
  }

  @Test
  public void preConvergeAction()
    throws Exception
  {
    safeAction( () -> {
      final SafeProcedure action = () -> {
      };
      assertEquals( Replicant.context().getPreConvergeAction(), null );
      Replicant.context().setPreConvergeAction( action );
      assertEquals( Replicant.context().getPreConvergeAction(), action );
    } );
  }

  @Test
  public void convergeCompleteAction()
    throws Exception
  {
    safeAction( () -> {
      final SafeProcedure action = () -> {
      };
      assertEquals( Replicant.context().getConvergeCompleteAction(), null );
      Replicant.context().setConvergeCompleteAction( action );
      assertEquals( Replicant.context().getConvergeCompleteAction(), action );
    } );
  }

  @Test
  public void active()
    throws Exception
  {
    final ReplicantContext context = Replicant.context();
    safeAction( () -> assertEquals( context.getState(), RuntimeState.CONNECTED ) );
    safeAction( () -> assertEquals( context.isActive(), true ) );
    context.deactivate();
    safeAction( () -> assertEquals( context.getState(), RuntimeState.DISCONNECTED ) );
    safeAction( () -> assertEquals( context.isActive(), false ) );
    context.activate();
    safeAction( () -> assertEquals( context.getState(), RuntimeState.CONNECTED ) );
    safeAction( () -> assertEquals( context.isActive(), true ) );
  }

  @Test
  public void setConnectorRequired()
    throws Exception
  {
    TestConnector.create( G.class );

    final ReplicantContext context = Replicant.context();

    assertTrue( context.getRuntime().getConnectorEntryBySystemType( G.class ).isRequired() );
    context.setConnectorRequired( G.class, false );
    assertFalse( context.getRuntime().getConnectorEntryBySystemType( G.class ).isRequired() );
  }

  @Test
  public void setCacheService()
    throws Exception
  {
    TestConnector.create( G.class );

    final ReplicantContext context = Replicant.context();
    final CacheService cacheService = mock( CacheService.class );

    assertEquals( context.getCacheService(), null );

    context.setCacheService( cacheService );

    assertEquals( context.getCacheService(), cacheService );

    context.setCacheService( null );

    assertEquals( context.getCacheService(), null );
  }

  enum G
  {
    G1, G2
  }

  static class A
  {
  }

  private static class B
  {
  }
}
