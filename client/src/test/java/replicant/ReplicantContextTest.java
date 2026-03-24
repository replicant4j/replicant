package replicant;

import arez.Disposable;
import java.util.List;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantContextTest
  extends AbstractReplicantTest
{
  @Test
  public void schemas()
  {
    final var schemaId = 22;

    final var context = Replicant.context();
    assertEquals( context.getSchemas().size(), 0 );
    assertNull( context.findSchemaById( schemaId ) );

    final var exception =
      expectThrows( IllegalStateException.class, () -> safeAction( () -> context.getSchemaById( schemaId ) ) );
    assertEquals( exception.getMessage(), "Replicant-0059: Unable to locate SystemSchema with id 22" );

    final var schema1 =
      new SystemSchema( schemaId,
                        ValueUtil.randomString(),
                        new ChannelSchema[ 0 ],
                        new EntitySchema[ 0 ] );

    context.getSchemaService().registerSchema( schema1 );

    assertEquals( context.getSchemas().size(), 1 );
    assertTrue( context.getSchemas().contains( schema1 ) );

    assertEquals( context.findSchemaById( schemaId ), schema1 );
    assertEquals( context.getSchemaById( schemaId ), schema1 );
  }

  @Test
  public void areasOfInterest()
  {
    // Pause scheduler so Autoruns don't auto-converge
    pauseScheduler();

    final var context = Replicant.context();
    final var address = new ChannelAddress( 1, 0 );
    final var filter = ValueUtil.randomString();
    final var filter2 = ValueUtil.randomString();

    safeAction( () -> {
      assertEquals( context.getAreasOfInterest().size(), 0 );
      assertNull( context.findAreaOfInterestByAddress( address ) );

      final var areaOfInterest = context.createOrUpdateAreaOfInterest( address, filter );

      assertEquals( areaOfInterest.getFilter(), filter );
      assertEquals( context.getAreasOfInterest().size(), 1 );
      assertEquals( context.findAreaOfInterestByAddress( address ), areaOfInterest );

      final var areaOfInterest2 = context.createOrUpdateAreaOfInterest( address, filter2 );

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
    final var context = Replicant.context();

    safeAction( () -> assertEquals( context.findAllEntityTypes().size(), 0 ) );
    safeAction( () -> assertEquals( context.findAllEntitiesByType( A.class ).size(), 0 ) );
    safeAction( () -> assertEquals( context.findAllEntitiesByType( B.class ).size(), 0 ) );
    safeAction( () -> assertNull( context.findEntityByTypeAndId( A.class, 1 ) ) );
    safeAction( () -> assertNull( context.findEntityByTypeAndId( A.class, 2 ) ) );
    safeAction( () -> assertNull( context.findEntityByTypeAndId( B.class, 47 ) ) );

    final var entity1 = findOrCreateEntity( A.class, 1 );

    assertEquals( entity1.getName(), "A/1" );
    safeAction( () -> assertEquals( context.findAllEntityTypes().size(), 1 ) );
    safeAction( () -> assertEquals( context.findAllEntitiesByType( A.class ).size(), 1 ) );
    safeAction( () -> assertEquals( context.findAllEntitiesByType( B.class ).size(), 0 ) );
    safeAction( () -> assertEquals( context.findEntityByTypeAndId( A.class, 1 ), entity1 ) );
    safeAction( () -> assertNull( context.findEntityByTypeAndId( A.class, 2 ) ) );
    safeAction( () -> assertNull( context.findEntityByTypeAndId( B.class, 47 ) ) );

    final var entity2 =
      safeAction( () -> context.getEntityService().findOrCreateEntity( "Super-dee-duper", A.class, 2 ) );

    assertEquals( entity2.getName(), "Super-dee-duper" );
    safeAction( () -> assertEquals( context.findAllEntityTypes().size(), 1 ) );
    safeAction( () -> assertEquals( context.findAllEntitiesByType( A.class ).size(), 2 ) );
    safeAction( () -> assertEquals( context.findAllEntitiesByType( B.class ).size(), 0 ) );
    safeAction( () -> assertEquals( context.findEntityByTypeAndId( A.class, 1 ), entity1 ) );
    safeAction( () -> assertEquals( context.findEntityByTypeAndId( A.class, 2 ), entity2 ) );
    safeAction( () -> assertNull( context.findEntityByTypeAndId( B.class, 47 ) ) );

    final var entity3 = findOrCreateEntity( B.class, 47 );

    safeAction( () -> assertEquals( context.findAllEntityTypes().size(), 2 ) );
    safeAction( () -> assertEquals( context.findAllEntitiesByType( A.class ).size(), 2 ) );
    safeAction( () -> assertEquals( context.findAllEntitiesByType( B.class ).size(), 1 ) );
    safeAction( () -> assertEquals( context.findEntityByTypeAndId( A.class, 1 ), entity1 ) );
    safeAction( () -> assertEquals( context.findEntityByTypeAndId( A.class, 2 ), entity2 ) );
    safeAction( () -> assertEquals( context.findEntityByTypeAndId( B.class, 47 ), entity3 ) );

    Disposable.dispose( entity1 );

    safeAction( () -> assertEquals( context.findAllEntityTypes().size(), 2 ) );
    safeAction( () -> assertEquals( context.findAllEntitiesByType( A.class ).size(), 1 ) );
    safeAction( () -> assertEquals( context.findAllEntitiesByType( B.class ).size(), 1 ) );
    safeAction( () -> assertNull( context.findEntityByTypeAndId( A.class, 1 ) ) );
    safeAction( () -> assertEquals( context.findEntityByTypeAndId( A.class, 2 ), entity2 ) );
    safeAction( () -> assertEquals( context.findEntityByTypeAndId( B.class, 47 ), entity3 ) );

    Disposable.dispose( entity2 );

    safeAction( () -> assertEquals( context.findAllEntityTypes().size(), 1 ) );
    safeAction( () -> assertEquals( context.findAllEntitiesByType( A.class ).size(), 0 ) );
    safeAction( () -> assertEquals( context.findAllEntitiesByType( B.class ).size(), 1 ) );
    safeAction( () -> assertNull( context.findEntityByTypeAndId( A.class, 1 ) ) );
    safeAction( () -> assertNull( context.findEntityByTypeAndId( A.class, 2 ) ) );
    safeAction( () -> assertEquals( context.findEntityByTypeAndId( B.class, 47 ), entity3 ) );

    Disposable.dispose( entity3 );

    safeAction( () -> assertEquals( context.findAllEntityTypes().size(), 0 ) );
    safeAction( () -> assertEquals( context.findAllEntitiesByType( A.class ).size(), 0 ) );
    safeAction( () -> assertEquals( context.findAllEntitiesByType( B.class ).size(), 0 ) );
    safeAction( () -> assertNull( context.findEntityByTypeAndId( A.class, 1 ) ) );
    safeAction( () -> assertNull( context.findEntityByTypeAndId( A.class, 2 ) ) );
    safeAction( () -> assertNull( context.findEntityByTypeAndId( B.class, 47 ) ) );
  }

  @Test
  public void subscriptions()
  {
    // Pause scheduler so Autoruns don't auto-converge
    pauseScheduler();

    final var context = Replicant.context();
    final var address1 = new ChannelAddress( 1, 0 );
    final var address2 = new ChannelAddress( 1, 1, 1 );
    final var address3 = new ChannelAddress( 1, 1, 2 );
    final var filter1 = (String) null;
    final var filter2 = ValueUtil.randomString();
    final var filter3 = ValueUtil.randomString();
    final var explicitSubscription1 = true;
    final var explicitSubscription2 = true;
    final var explicitSubscription3 = false;

    safeAction( () -> {
      assertEquals( context.getTypeSubscriptions().size(), 0 );
      assertEquals( context.getInstanceSubscriptions().size(), 0 );
      assertEquals( context.getInstanceSubscriptionIds( 1, 1 ).size(), 0 );
      assertNull( context.findSubscription( address1 ) );
      assertNull( context.findSubscription( address2 ) );
      assertNull( context.findSubscription( address3 ) );

      final var subscription1 = createSubscription( address1, filter1, explicitSubscription1 );

      assertEquals( subscription1.address(), address1 );
      assertEquals( subscription1.getFilter(), filter1 );
      assertEquals( subscription1.isExplicitSubscription(), explicitSubscription1 );

      assertEquals( context.getTypeSubscriptions().size(), 1 );
      assertEquals( context.getInstanceSubscriptions().size(), 0 );
      assertEquals( context.getInstanceSubscriptionIds( 1, 1 ).size(), 0 );
      assertEquals( context.findSubscription( address1 ), subscription1 );
      assertNull( context.findSubscription( address2 ) );
      assertNull( context.findSubscription( address3 ) );

      final var subscription2 = createSubscription( address2, filter2, explicitSubscription2 );

      assertEquals( subscription2.address(), address2 );
      assertEquals( subscription2.getFilter(), filter2 );
      assertEquals( subscription2.isExplicitSubscription(), explicitSubscription2 );

      assertEquals( context.getTypeSubscriptions().size(), 1 );
      assertEquals( context.getInstanceSubscriptions().size(), 1 );
      assertEquals( context.getInstanceSubscriptionIds( 1, 1 ).size(), 1 );
      assertEquals( context.findSubscription( address1 ), subscription1 );
      assertEquals( context.findSubscription( address2 ), subscription2 );
      assertNull( context.findSubscription( address3 ) );

      final var subscription3 = createSubscription( address3, filter3, explicitSubscription3 );

      assertEquals( subscription3.address(), address3 );
      assertEquals( subscription3.getFilter(), filter3 );
      assertEquals( subscription3.isExplicitSubscription(), explicitSubscription3 );

      assertEquals( context.getTypeSubscriptions().size(), 1 );
      assertEquals( context.getInstanceSubscriptions().size(), 2 );
      assertEquals( context.getInstanceSubscriptionIds( 1, 1 ).size(), 2 );
      assertEquals( context.findSubscription( address1 ), subscription1 );
      assertEquals( context.findSubscription( address2 ), subscription2 );
      assertEquals( context.findSubscription( address3 ), subscription3 );

      Disposable.dispose( subscription2 );
      Disposable.dispose( subscription3 );

      assertEquals( context.getTypeSubscriptions().size(), 1 );
      assertEquals( context.getInstanceSubscriptions().size(), 0 );
      assertEquals( context.getInstanceSubscriptionIds( 1, 1 ).size(), 0 );
      assertEquals( context.findSubscription( address1 ), subscription1 );
      assertNull( context.findSubscription( address2 ) );
      assertNull( context.findSubscription( address3 ) );
    } );
  }

  @Test
  public void getSpy_whenSpiesDisabled()
  {
    ReplicantTestUtil.disableSpies();
    ReplicantTestUtil.resetState();

    assertEquals( expectThrows( IllegalStateException.class, Replicant.context()::getSpy ).getMessage(),
                  "Replicant-0021: Attempting to get Spy but spies are not enabled." );
  }

  @Test
  public void getSpy()
  {
    final var context = Replicant.context();

    assertFalse( context.willPropagateSpyEvents() );

    final var spy = context.getSpy();

    spy.addSpyEventHandler( new TestSpyEventHandler() );

    assertTrue( spy.willPropagateSpyEvents() );
    assertTrue( context.willPropagateSpyEvents() );

    ReplicantTestUtil.disableSpies();

    assertFalse( spy.willPropagateSpyEvents() );
    assertFalse( context.willPropagateSpyEvents() );
  }

  @Test
  public void preConvergeAction()
  {
    safeAction( () -> {
      final var action = (SafeProcedure) () -> {
      };
      assertNull( Replicant.context().getPreConvergeAction() );
      Replicant.context().setPreConvergeAction( action );
      assertEquals( Replicant.context().getPreConvergeAction(), action );
    } );
  }

  @Test
  public void convergeCompleteAction()
  {
    safeAction( () -> {
      final var action = (SafeProcedure) () -> {
      };
      assertNull( Replicant.context().getConvergeCompleteAction() );
      Replicant.context().setConvergeCompleteAction( action );
      assertEquals( Replicant.context().getConvergeCompleteAction(), action );
    } );
  }

  @Test
  public void active()
  {
    final var context = Replicant.context();
    assertEquals( context.getState(), RuntimeState.CONNECTED );
    safeAction( () -> assertTrue( context.isActive() ) );
    context.deactivate();
    assertEquals( context.getState(), RuntimeState.DISCONNECTED );
    safeAction( () -> assertFalse( context.isActive() ) );
    context.activate();
    assertEquals( context.getState(), RuntimeState.CONNECTED );
    safeAction( () -> assertTrue( context.isActive() ) );
  }

  @Test
  public void setConnectorRequired()
  {
    final var schema = newSchema();

    createConnector( schema );

    final var context = Replicant.context();

    final var schemaId = schema.getId();
    assertTrue( context.getRuntime().getConnectorEntryBySchemaId( schemaId ).isRequired() );
    context.setConnectorRequired( schemaId, false );
    assertFalse( context.getRuntime().getConnectorEntryBySchemaId( schemaId ).isRequired() );
  }

  @SuppressWarnings( "ConstantValue" )
  @Test
  public void setCacheService()
  {
    createConnector();

    final var context = Replicant.context();
    final var cacheService = mock( CacheService.class );

    assertNull( context.getCacheService() );

    context.setCacheService( cacheService );

    assertEquals( context.getCacheService(), cacheService );

    context.setCacheService( null );

    assertNull( context.getCacheService() );
  }

  @Test
  public void exec()
  {
    final var connector = createConnector();
    connector.pauseMessageScheduler();
    final var connection = newConnection( connector );

    final var command = ValueUtil.randomString();
    final var payload = new Object();

    Replicant.context().exec( connector.getSchema().getId(), command, payload, null );

    final var requests = connection.getPendingExecRequests();
    assertEquals( requests.size(), 1 );
    final var request = requests.get( 0 );
    assertEquals( request.getCommand(), command );
    assertEquals( request.getPayload(), payload );
  }

  @Test
  public void findConnectionId()
  {
    final var connector = createConnector();
    final var schemaId = connector.getSchema().getId();

    assertNull( Replicant.context().findConnectionId( schemaId ) );

    final var connection = newConnection( connector );

    assertEquals( Replicant.context().findConnectionId( schemaId ), connection.getConnectionId() );
  }

  @Test
  public void registerConnector()
  {
    safeAction( () -> assertEquals( Replicant.context().getRuntime().getConnectors().size(), 0 ) );
    assertEquals( Replicant.context().getSchemas().size(), 0 );

    final var disposable =
      Replicant.context().registerConnector( newSchema(), mock( Transport.class ) );

    safeAction( () -> assertEquals( Replicant.context().getRuntime().getConnectors().size(), 1 ) );
    assertEquals( Replicant.context().getSchemas().size(), 1 );

    disposable.dispose();

    safeAction( () -> assertEquals( Replicant.context().getRuntime().getConnectors().size(), 0 ) );
    assertEquals( Replicant.context().getSchemas().size(), 0 );
  }

  static class A
  {
  }

  private static class B
  {
  }
}
