package replicant.server.transport;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import replicant.server.Change;
import replicant.server.ChangeSet;
import replicant.server.ChannelAction;
import replicant.server.ChannelAddress;
import replicant.server.ChannelLink;
import replicant.server.EntityMessage;
import replicant.server.ServerConstants;
import replicant.server.ee.RegistryUtil;
import replicant.server.runtime.EntityMessageCacheUtil;
import replicant.server.runtime.TransactionSynchronizationRegistryUtil;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantSessionManagerImplTest
{
  @BeforeMethod
  public void setup()
  {
    RegistryUtil.bind();
  }

  @AfterMethod
  public void clearContext()
  {
    RegistryUtil.unbind();
  }

  @Test
  public void sendChangeMessage_invalidAuthorizationDoesNotProcessPacket()
    throws Exception
  {
    final var authorization = mock( ReplicantSessionAuthorization.class );
    when( authorization.runIfValid( any() ) ).thenReturn( false );
    final var session = new ReplicantSession( mock( Session.class ), authorization );
    final var manager = new ReplicantSessionManagerImpl();
    final var packet = new Packet( false, null, null, null, List.of(), new ChangeSet() );

    assertFalse( manager.sendChangeMessage( session, packet ) );

    verify( authorization ).runIfValid( any() );
  }

  @Test
  public void sendChangeMessage_staticInstancedLinkFollow_usesTargetInstanceId()
  {
    final var sourceChannel =
      new ChannelMetaData( 0,
                           "Source",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var targetChannel =
      new ChannelMetaData( 1,
                           "Target",
                           1,
                           ChannelMetaData.FilterType.STATIC_INSTANCED,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", sourceChannel, targetChannel );

    final var context = new TestSessionContext( schema );
    final var manager = new ReplicantSessionManagerImpl();
    setField( manager, "_context", context );
    setField( manager, "_broker", mock( ReplicantMessageBroker.class ) );
    setField( manager, "_registry", TransactionSynchronizationRegistryUtil.lookup() );

    final var webSocketSession = mock( Session.class );
    final var remote = mock( RemoteEndpoint.Basic.class );
    when( webSocketSession.getId() ).thenReturn( "session-1" );
    when( webSocketSession.isOpen() ).thenReturn( true );
    when( webSocketSession.getBasicRemote() ).thenReturn( remote );

    final var session = new ReplicantSession( webSocketSession );

    final var sourceAddress = ChannelAddress.of( 0 );
    final var targetAddress = ChannelAddress.of( 1, 7, "fi-7" );
    final var link = new ChannelLink( sourceAddress, targetAddress, null, true );

    final var routingKeys = new HashMap<String, Serializable>();
    routingKeys.put( "Source", "present" );
    final var attributes = new HashMap<String, Serializable>();
    attributes.put( "ID", 1 );
    final var message = new EntityMessage( 1, 1, 0L, routingKeys, attributes, Set.of( link ) );

    final var newFilter = Json.createObjectBuilder().add( "k", "v" ).build();
    final var packet = new Packet( false, null, null, null, List.of( message ), new ChangeSet() );

    session.getLock().lock();
    try
    {
      final var sourceEntry = session.createSubscriptionEntry( sourceAddress );
      final var originalFilter = Json.createObjectBuilder().add( "old", "value" ).build();
      sourceEntry.setFilter( originalFilter );

      manager.sendChangeMessage( session, packet );

      assertEquals( context.getPreSendChangeMessages(), List.of( packet ) );
      final var targetEntry = session.findSubscriptionEntry( targetAddress );
      assertNotNull( targetEntry );
      assertEquals( targetEntry.getFilter(), newFilter );
      assertTrue( sourceEntry.getOutwardSubscriptions().contains( targetAddress ) );
      assertTrue( targetEntry.getInwardSubscriptions().contains( sourceAddress ) );
    }
    finally
    {
      session.getLock().unlock();
    }

    final var collectCalls = context.getBulkCollectCalls();
    assertEquals( collectCalls.size(), 1 );
    final var call = collectCalls.get( 0 );
    assertEquals( call.addresses(), List.of( targetAddress ) );
    assertEquals( call.filter(), newFilter );
    assertFalse( call.isExplicitSubscribe() );
  }

  @Test
  public void sendChangeMessage_deleteRemovesOnlyDeletedEntityOwnershipForSharedTarget()
  {
    final var sourceChannel =
      new ChannelMetaData( 0,
                           "Source",
                           1,
                           ChannelMetaData.FilterType.NONE,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var targetChannel =
      new ChannelMetaData( 1,
                           "Target",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", sourceChannel, targetChannel );
    final var context = createManagerContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );

    final var webSocketSession = mock( Session.class );
    final var remote = mock( RemoteEndpoint.Basic.class );
    when( webSocketSession.getId() ).thenReturn( "session-1" );
    when( webSocketSession.isOpen() ).thenReturn( true );
    when( webSocketSession.getBasicRemote() ).thenReturn( remote );
    final var session = new ReplicantSession( webSocketSession );

    final var sourceAddress = ChannelAddress.of( 0, 10 );
    final var targetAddress = ChannelAddress.of( 1 );
    final var link = new ChannelLink( sourceAddress, targetAddress );
    final var routingKeys = new HashMap<String, Serializable>();
    routingKeys.put( "Source", new ArrayList<>( List.of( 10 ) ) );
    final var attributesA = new HashMap<String, Serializable>();
    final var attributesB = new HashMap<String, Serializable>();
    attributesA.put( "ID", 100 );
    attributesB.put( "ID", 101 );

    final var updateA = new EntityMessage( 100, 2, 0L, routingKeys, attributesA, Set.of( link ) );
    final var updateB = new EntityMessage( 101, 2, 0L, routingKeys, attributesB, Set.of( link ) );
    final var deleteA = new EntityMessage( 100, 2, 1L, routingKeys, null, null );

    session.getLock().lock();
    try
    {
      final var sourceEntry = session.createSubscriptionEntry( sourceAddress );
      sourceEntry.setExplicitlySubscribed( true );

      manager.sendChangeMessage( session,
                                 new Packet( false, null, null, null, List.of( updateA, updateB ), new ChangeSet() ) );

      assertNotNull( session.findSubscriptionEntry( targetAddress ) );
      assertEquals( sourceEntry.getOwnedOutwardSubscriptions( LinkOwner.entity( 2, 100 ) ), Set.of( targetAddress ) );
      assertEquals( sourceEntry.getOwnedOutwardSubscriptions( LinkOwner.entity( 2, 101 ) ), Set.of( targetAddress ) );

      manager.sendChangeMessage( session, new Packet( false, null, null, null, List.of( deleteA ), new ChangeSet() ) );

      final var targetEntry = session.findSubscriptionEntry( targetAddress );
      assertNotNull( targetEntry );
      assertTrue( sourceEntry.getOutwardSubscriptions().contains( targetAddress ) );
      assertTrue( targetEntry.getInwardSubscriptions().contains( sourceAddress ) );
      assertTrue( sourceEntry.getOwnedOutwardSubscriptions( LinkOwner.entity( 2, 100 ) ).isEmpty() );
      assertEquals( sourceEntry.getOwnedOutwardSubscriptions( LinkOwner.entity( 2, 101 ) ), Set.of( targetAddress ) );
    }
    finally
    {
      session.getLock().unlock();
    }
  }

  @Test
  public void sendChangeMessage_sameTargetReplacementFromPacketMessage_preservesWithoutTargetReload()
  {
    final var sourceChannel =
      new ChannelMetaData( 0,
                           "Source",
                           1,
                           ChannelMetaData.FilterType.NONE,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var targetChannel =
      new ChannelMetaData( 1,
                           "Target",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", sourceChannel, targetChannel );
    final var context = createManagerContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );
    final var session = createOpenSession();

    final var sourceAddress = ChannelAddress.of( 0, 10 );
    final var targetAddress = ChannelAddress.of( 1 );
    final var oldOwner = LinkOwner.entity( 2, 100 );
    final var newOwner = LinkOwner.entity( 2, 101 );
    final var link = new ChannelLink( sourceAddress, targetAddress );
    final var updateNew = new EntityMessage( 101,
                                             2,
                                             0L,
                                             instanceRouting( "Source", 10 ),
                                             attributes( 101 ),
                                             Set.of( link ) );
    final var deleteOld = new EntityMessage( 100,
                                             2,
                                             1L,
                                             instanceRouting( "Source", 10 ),
                                             null,
                                             null );

    session.getLock().lock();
    try
    {
      final var sourceEntry = session.createSubscriptionEntry( sourceAddress );
      sourceEntry.setExplicitlySubscribed( true );
      session.createSubscriptionEntry( targetAddress );
      session.recordEntityScopedGraphLink( sourceAddress, targetAddress, 2, 100 );

      manager.sendChangeMessage( session,
                                 new Packet( false,
                                             null,
                                             null,
                                             null,
                                             List.of( deleteOld, updateNew ),
                                             new ChangeSet() ) );

      final var targetEntry = session.findSubscriptionEntry( targetAddress );
      assertNotNull( targetEntry );
      assertTrue( sourceEntry.getOwnedOutwardSubscriptions( oldOwner ).isEmpty() );
      assertEquals( sourceEntry.getOwnedOutwardSubscriptions( newOwner ), Set.of( targetAddress ) );
      assertTrue( sourceEntry.getOutwardSubscriptions().contains( targetAddress ) );
      assertTrue( targetEntry.getInwardSubscriptions().contains( sourceAddress ) );
    }
    finally
    {
      session.getLock().unlock();
    }

    assertTrue( context.getBulkCollectCalls().isEmpty() );
  }

  @Test
  public void sendChangeMessage_sameTargetReplacementFromChangeSet_preservesWithoutTargetReload()
  {
    final var sourceChannel =
      new ChannelMetaData( 0,
                           "Source",
                           1,
                           ChannelMetaData.FilterType.NONE,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var targetChannel =
      new ChannelMetaData( 1,
                           "Target",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", sourceChannel, targetChannel );
    final var context = createManagerContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );
    final var session = createOpenSession();

    final var sourceAddress = ChannelAddress.of( 0, 10 );
    final var targetAddress = ChannelAddress.of( 1 );
    final var oldOwner = LinkOwner.entity( 2, 100 );
    final var newOwner = LinkOwner.entity( 2, 101 );
    final var link = new ChannelLink( sourceAddress, targetAddress );
    final var updateNew = new EntityMessage( 101,
                                             2,
                                             0L,
                                             instanceRouting( "Source", 10 ),
                                             attributes( 101 ),
                                             Set.of( link ) );
    final var deleteOld = new EntityMessage( 100,
                                             2,
                                             1L,
                                             instanceRouting( "Source", 10 ),
                                             null,
                                             null );
    final var changeSet = new ChangeSet();
    changeSet.merge( new Change( updateNew, sourceAddress ) );

    session.getLock().lock();
    try
    {
      final var sourceEntry = session.createSubscriptionEntry( sourceAddress );
      sourceEntry.setExplicitlySubscribed( true );
      session.createSubscriptionEntry( targetAddress );
      session.recordEntityScopedGraphLink( sourceAddress, targetAddress, 2, 100 );

      manager.sendChangeMessage( session,
                                 new Packet( false, null, null, null, List.of( deleteOld ), changeSet ) );

      final var targetEntry = session.findSubscriptionEntry( targetAddress );
      assertNotNull( targetEntry );
      assertTrue( sourceEntry.getOwnedOutwardSubscriptions( oldOwner ).isEmpty() );
      assertEquals( sourceEntry.getOwnedOutwardSubscriptions( newOwner ), Set.of( targetAddress ) );
      assertTrue( targetEntry.getInwardSubscriptions().contains( sourceAddress ) );
    }
    finally
    {
      session.getLock().unlock();
    }

    assertTrue( context.getBulkCollectCalls().isEmpty() );
  }

  @Test
  public void sendChangeMessage_newTargetReplacement_isCollectedByNormalExpansion()
  {
    final var sourceChannel =
      new ChannelMetaData( 0,
                           "Source",
                           1,
                           ChannelMetaData.FilterType.NONE,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var targetChannel =
      new ChannelMetaData( 1,
                           "Target",
                           3,
                           ChannelMetaData.FilterType.NONE,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", sourceChannel, targetChannel );
    final var context = createManagerContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );
    final var session = createOpenSession();

    final var sourceAddress = ChannelAddress.of( 0, 10 );
    final var oldTargetAddress = ChannelAddress.of( 1, 20 );
    final var newTargetAddress = ChannelAddress.of( 1, 21 );
    final var newOwner = LinkOwner.entity( 2, 101 );
    final var link = new ChannelLink( sourceAddress, newTargetAddress );
    final var updateNew = new EntityMessage( 101,
                                             2,
                                             0L,
                                             instanceRouting( "Source", 10 ),
                                             attributes( 101 ),
                                             Set.of( link ) );
    final var deleteOld = new EntityMessage( 100,
                                             2,
                                             1L,
                                             instanceRouting( "Source", 10 ),
                                             null,
                                             null );

    session.getLock().lock();
    try
    {
      final var sourceEntry = session.createSubscriptionEntry( sourceAddress );
      sourceEntry.setExplicitlySubscribed( true );
      session.createSubscriptionEntry( oldTargetAddress );
      session.recordEntityScopedGraphLink( sourceAddress, oldTargetAddress, 2, 100 );

      manager.sendChangeMessage( session,
                                 new Packet( false,
                                             null,
                                             null,
                                             null,
                                             List.of( deleteOld, updateNew ),
                                             new ChangeSet() ) );

      assertNull( session.findSubscriptionEntry( oldTargetAddress ) );
      final var newTargetEntry = session.findSubscriptionEntry( newTargetAddress );
      assertNotNull( newTargetEntry );
      assertEquals( sourceEntry.getOwnedOutwardSubscriptions( newOwner ), Set.of( newTargetAddress ) );
      assertTrue( newTargetEntry.getInwardSubscriptions().contains( sourceAddress ) );
    }
    finally
    {
      session.getLock().unlock();
    }

    final var collectCalls = context.getBulkCollectCalls();
    assertEquals( collectCalls.size(), 1 );
    assertEquals( collectCalls.get( 0 ).addresses(), List.of( newTargetAddress ) );
    assertNull( collectCalls.get( 0 ).filter() );
    assertFalse( collectCalls.get( 0 ).isExplicitSubscribe() );
  }

  @Test
  public void sendChangeMessage_filterMismatchReplacement_isCollectedWithNewFilterByNormalExpansion()
  {
    final var sourceChannel =
      new ChannelMetaData( 0,
                           "Source",
                           1,
                           ChannelMetaData.FilterType.NONE,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var targetChannel =
      new ChannelMetaData( 1,
                           "Target",
                           null,
                           ChannelMetaData.FilterType.DYNAMIC,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", sourceChannel, targetChannel );
    final var context = createManagerContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );
    final var session = createOpenSession();

    final var sourceAddress = ChannelAddress.of( 0, 10 );
    final var targetAddress = ChannelAddress.of( 1 );
    final var oldFilter = Json.createObjectBuilder().add( "filter", "old" ).build();
    final var newFilter = Json.createObjectBuilder().add( "filter", "new" ).build();
    final var newOwner = LinkOwner.entity( 2, 101 );
    final var link = new ChannelLink( sourceAddress, targetAddress, newFilter );
    final var updateNew = new EntityMessage( 101,
                                             2,
                                             0L,
                                             instanceRouting( "Source", 10 ),
                                             attributes( 101 ),
                                             Set.of( link ) );
    final var deleteOld = new EntityMessage( 100,
                                             2,
                                             1L,
                                             instanceRouting( "Source", 10 ),
                                             null,
                                             null );

    session.getLock().lock();
    try
    {
      final var sourceEntry = session.createSubscriptionEntry( sourceAddress );
      sourceEntry.setExplicitlySubscribed( true );
      final var targetEntry = session.createSubscriptionEntry( targetAddress );
      targetEntry.setFilter( oldFilter );
      session.recordEntityScopedGraphLink( sourceAddress, targetAddress, 2, 100 );

      manager.sendChangeMessage( session,
                                 new Packet( false,
                                             null,
                                             null,
                                             null,
                                             List.of( deleteOld, updateNew ),
                                             new ChangeSet() ) );

      final var reloadedTargetEntry = session.findSubscriptionEntry( targetAddress );
      assertNotNull( reloadedTargetEntry );
      assertEquals( reloadedTargetEntry.getFilter(), newFilter );
      assertEquals( sourceEntry.getOwnedOutwardSubscriptions( newOwner ), Set.of( targetAddress ) );
    }
    finally
    {
      session.getLock().unlock();
    }

    final var collectCalls = context.getBulkCollectCalls();
    assertEquals( collectCalls.size(), 1 );
    assertEquals( collectCalls.get( 0 ).addresses(), List.of( targetAddress ) );
    assertEquals( collectCalls.get( 0 ).filter(), newFilter );
    assertFalse( collectCalls.get( 0 ).isExplicitSubscribe() );
  }

  @Test
  public void sendChangeMessage_filteredOutSourceRoute_isNotPreserved()
  {
    final var sourceChannel =
      new ChannelMetaData( 0,
                           "Source",
                           1,
                           ChannelMetaData.FilterType.STATIC_INSTANCED,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var targetChannel =
      new ChannelMetaData( 1,
                           "Target",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", sourceChannel, targetChannel );
    final var context = createManagerContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );
    final var session = createOpenSession();

    final var includedSourceAddress = ChannelAddress.of( 0, 10, "included" );
    final var excludedSourceAddress = ChannelAddress.of( 0, 10, "excluded" );
    final var targetAddress = ChannelAddress.of( 1 );
    final var oldOwner = LinkOwner.entity( 2, 100 );
    final var newOwner = LinkOwner.entity( 2, 101 );
    final var link = new ChannelLink( ChannelAddress.partial( 0, 10 ), targetAddress, null, true );
    final var updateNew = new EntityMessage( 101,
                                             2,
                                             0L,
                                             instanceRouting( "Source", 10 ),
                                             attributes( 101 ),
                                             Set.of( link ) );
    final var deleteOld = new EntityMessage( 100,
                                             2,
                                             1L,
                                             instanceRouting( "Source", 10 ),
                                             null,
                                             null );
    context.excludeFilterEntityMessageAddress( excludedSourceAddress );

    session.getLock().lock();
    try
    {
      final var includedSourceEntry = session.createSubscriptionEntry( includedSourceAddress );
      final var excludedSourceEntry = session.createSubscriptionEntry( excludedSourceAddress );
      includedSourceEntry.setExplicitlySubscribed( true );
      excludedSourceEntry.setExplicitlySubscribed( true );
      session.createSubscriptionEntry( targetAddress );
      session.recordEntityScopedGraphLink( includedSourceAddress, targetAddress, 2, 100 );
      session.recordEntityScopedGraphLink( excludedSourceAddress, targetAddress, 2, 100 );

      manager.sendChangeMessage( session,
                                 new Packet( false,
                                             null,
                                             null,
                                             null,
                                             List.of( deleteOld, updateNew ),
                                             new ChangeSet() ) );

      assertTrue( includedSourceEntry.getOwnedOutwardSubscriptions( oldOwner ).isEmpty() );
      assertEquals( includedSourceEntry.getOwnedOutwardSubscriptions( newOwner ), Set.of( targetAddress ) );
      assertEquals( excludedSourceEntry.getOwnedOutwardSubscriptions( oldOwner ), Set.of( targetAddress ) );
      assertTrue( excludedSourceEntry.getOwnedOutwardSubscriptions( newOwner ).isEmpty() );
    }
    finally
    {
      session.getLock().unlock();
    }

    assertTrue( context.getBulkCollectCalls().isEmpty() );
  }

  @Test
  public void sendChangeMessage_shouldFollowLinkFalse_isNotPreserved()
  {
    final var sourceChannel =
      new ChannelMetaData( 0,
                           "Source",
                           1,
                           ChannelMetaData.FilterType.NONE,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var targetChannel =
      new ChannelMetaData( 1,
                           "Target",
                           null,
                           ChannelMetaData.FilterType.DYNAMIC,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", sourceChannel, targetChannel );
    final var context = createManagerContext( schema );
    context.setShouldFollowLink( false );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );
    final var session = createOpenSession();

    final var sourceAddress = ChannelAddress.of( 0, 10 );
    final var targetAddress = ChannelAddress.of( 1 );
    final var targetFilter = Json.createObjectBuilder().add( "filter", "current" ).build();
    final var newOwner = LinkOwner.entity( 2, 101 );
    final var link = new ChannelLink( sourceAddress, targetAddress, targetFilter );
    final var updateNew = new EntityMessage( 101,
                                             2,
                                             0L,
                                             instanceRouting( "Source", 10 ),
                                             attributes( 101 ),
                                             Set.of( link ) );
    final var deleteOld = new EntityMessage( 100,
                                             2,
                                             1L,
                                             instanceRouting( "Source", 10 ),
                                             null,
                                             null );

    session.getLock().lock();
    try
    {
      final var sourceEntry = session.createSubscriptionEntry( sourceAddress );
      sourceEntry.setExplicitlySubscribed( true );
      final var targetEntry = session.createSubscriptionEntry( targetAddress );
      targetEntry.setFilter( targetFilter );
      session.recordEntityScopedGraphLink( sourceAddress, targetAddress, 2, 100 );

      manager.sendChangeMessage( session,
                                 new Packet( false,
                                             null,
                                             null,
                                             null,
                                             List.of( deleteOld, updateNew ),
                                             new ChangeSet() ) );

      assertNull( session.findSubscriptionEntry( targetAddress ) );
      assertTrue( sourceEntry.getOwnedOutwardSubscriptions( newOwner ).isEmpty() );
    }
    finally
    {
      session.getLock().unlock();
    }

    assertTrue( context.getBulkCollectCalls().isEmpty() );
  }

  @Test
  public void sendChangeMessage_sourceRootDeleteWinsOverPreservation()
  {
    final var sourceChannel =
      new ChannelMetaData( 0,
                           "Source",
                           1,
                           ChannelMetaData.FilterType.NONE,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var targetChannel =
      new ChannelMetaData( 1,
                           "Target",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", sourceChannel, targetChannel );
    final var context = createManagerContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );
    final var session = createOpenSession();

    final var sourceAddress = ChannelAddress.of( 0, 10 );
    final var targetAddress = ChannelAddress.of( 1 );
    final var link = new ChannelLink( sourceAddress, targetAddress );
    final var updateNew = new EntityMessage( 101,
                                             2,
                                             0L,
                                             instanceRouting( "Source", 10 ),
                                             attributes( 101 ),
                                             Set.of( link ) );
    final var deleteSourceRoot = new EntityMessage( 10,
                                                    1,
                                                    1L,
                                                    instanceRouting( "Source", 10 ),
                                                    null,
                                                    null );

    session.getLock().lock();
    try
    {
      final var sourceEntry = session.createSubscriptionEntry( sourceAddress );
      sourceEntry.setExplicitlySubscribed( true );
      session.createSubscriptionEntry( targetAddress );
      session.recordEntityScopedGraphLink( sourceAddress, targetAddress, 1, 10 );

      manager.sendChangeMessage( session,
                                 new Packet( false,
                                             null,
                                             null,
                                             null,
                                             List.of( deleteSourceRoot, updateNew ),
                                             new ChangeSet() ) );

      assertNull( session.findSubscriptionEntry( sourceAddress ) );
      assertNull( session.findSubscriptionEntry( targetAddress ) );
    }
    finally
    {
      session.getLock().unlock();
    }

    assertTrue( context.getBulkCollectCalls().isEmpty() );
  }

  @Test
  public void sendChangeMessage_targetRootDeleteWinsOverPreservation()
  {
    final var sourceChannel =
      new ChannelMetaData( 0,
                           "Source",
                           1,
                           ChannelMetaData.FilterType.STATIC_INSTANCED,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var targetChannel =
      new ChannelMetaData( 1,
                           "Target",
                           3,
                           ChannelMetaData.FilterType.NONE,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", sourceChannel, targetChannel );
    final var context = createManagerContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );
    final var session = createOpenSession();

    final var sourceAddress = ChannelAddress.of( 0, 10, "fi-source" );
    final var targetAddress = ChannelAddress.of( 1, 20 );
    final var newOwner = LinkOwner.entity( 2, 101 );
    final var link = new ChannelLink( sourceAddress, targetAddress );
    final var updateNew = new EntityMessage( 101,
                                             2,
                                             0L,
                                             instanceRouting( "Source", 10 ),
                                             attributes( 101 ),
                                             Set.of( link ) );
    final var deleteOld = new EntityMessage( 100,
                                             2,
                                             1L,
                                             instanceRouting( "Source", 10 ),
                                             null,
                                             null );
    final var deleteTargetRoot = new EntityMessage( 20,
                                                    3,
                                                    1L,
                                                    instanceRouting( "Target", 20 ),
                                                    null,
                                                    null );
    session.getLock().lock();
    try
    {
      final var sourceEntry = session.createSubscriptionEntry( sourceAddress );
      sourceEntry.setExplicitlySubscribed( true );
      session.createSubscriptionEntry( targetAddress );
      session.recordEntityScopedGraphLink( sourceAddress, targetAddress, 2, 100 );

      manager.sendChangeMessage( session,
                                 new Packet( false,
                                             null,
                                             null,
                                             null,
                                             List.of( deleteOld, deleteTargetRoot, updateNew ),
                                             new ChangeSet() ) );

      assertNull( session.findSubscriptionEntry( targetAddress ) );
      assertTrue( sourceEntry.getOwnedOutwardSubscriptions( newOwner ).isEmpty() );
    }
    finally
    {
      session.getLock().unlock();
    }

    assertTrue( context.getBulkCollectCalls().isEmpty() );
  }

  @Test
  public void invalidateSession_removesAndClosesExistingSession()
    throws Exception
  {
    final var schema =
      new SchemaMetaData( "Test",
                          new ChannelMetaData( 0,
                                               "Source",
                                               null,
                                               ChannelMetaData.FilterType.NONE,
                                               ChannelMetaData.CacheType.NONE,
                                               true ) );
    final var context = new TestSessionContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );

    final var webSocketSession = mock( Session.class );
    when( webSocketSession.getId() ).thenReturn( "session-1" );
    when( webSocketSession.isOpen() ).thenReturn( true );

    final var session = manager.createSession( webSocketSession, mock( ReplicantSessionAuthorization.class ) );
    assertNotNull( manager.getSession( "session-1" ) );

    manager.invalidateSession( session );

    assertNull( manager.getSession( "session-1" ) );
    verify( webSocketSession ).close();
  }

  @Test
  public void invalidateSession_ignoresUnknownSession()
    throws Exception
  {
    final var schema =
      new SchemaMetaData( "Test",
                          new ChannelMetaData( 0,
                                               "Source",
                                               null,
                                               ChannelMetaData.FilterType.NONE,
                                               ChannelMetaData.CacheType.NONE,
                                               true ) );
    final var context = new TestSessionContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );

    final var webSocketSession = mock( Session.class );
    when( webSocketSession.getId() ).thenReturn( "unknown-session" );

    final var session = new ReplicantSession( webSocketSession );
    manager.invalidateSession( session );

    verify( webSocketSession, never() ).close();
  }

  @Test
  public void unsubscribe_removesSubscriptionsViaSessionLogic()
  {
    final var channel =
      new ChannelMetaData( 0,
                           "Channel",
                           1,
                           ChannelMetaData.FilterType.NONE,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", channel );
    final var context = new TestSessionContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );

    final var webSocketSession = mock( Session.class );
    when( webSocketSession.getId() ).thenReturn( "session-1" );
    when( webSocketSession.isOpen() ).thenReturn( true );
    final var session = new ReplicantSession( webSocketSession );

    final var address1 = ChannelAddress.of( 0, 1 );
    final var address2 = ChannelAddress.of( 0, 2 );
    final var address3 = ChannelAddress.of( 0, 3 );

    session.getLock().lock();
    try
    {
      final var entry1 = session.createSubscriptionEntry( address1 );
      final var entry2 = session.createSubscriptionEntry( address2 );
      entry1.setExplicitlySubscribed( true );
      entry2.setExplicitlySubscribed( true );
    }
    finally
    {
      session.getLock().unlock();
    }

    final var registry = TransactionSynchronizationRegistryUtil.lookup();
    registry.putResource( ServerConstants.REPLICATION_INVOCATION_KEY, null );

    manager.unsubscribe( session, 99, List.of( address1, address2, address3 ) );

    session.getLock().lock();
    try
    {
      assertNull( session.findSubscriptionEntry( address1 ) );
      assertNull( session.findSubscriptionEntry( address2 ) );
    }
    finally
    {
      session.getLock().unlock();
    }

    final var sessionChanges = EntityMessageCacheUtil.lookupSessionChanges();
    assertNotNull( sessionChanges );
    assertEquals( sessionChanges.getChannelActions().size(), 2 );
    assertEquals( sessionChanges.getChannelActions().get( 0 ).action(), ChannelAction.Action.REMOVE );
    assertEquals( sessionChanges.getChannelActions().get( 1 ).action(), ChannelAction.Action.REMOVE );
  }

  @Test
  public void sendChangeMessage_deleteRootUnsubscribesRootAndDownstream()
  {
    final var sourceChannel =
      new ChannelMetaData( 0,
                           "Source",
                           1,
                           ChannelMetaData.FilterType.NONE,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var targetChannel =
      new ChannelMetaData( 1,
                           "Target",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", sourceChannel, targetChannel );
    final var context = new TestSessionContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );

    final var webSocketSession = mock( Session.class );
    final var remote = mock( RemoteEndpoint.Basic.class );
    when( webSocketSession.getId() ).thenReturn( "session-1" );
    when( webSocketSession.isOpen() ).thenReturn( true );
    when( webSocketSession.getBasicRemote() ).thenReturn( remote );
    final var session = new ReplicantSession( webSocketSession );

    final var sourceAddress = ChannelAddress.of( 0, 10 );
    final var targetAddress = ChannelAddress.of( 1 );

    final var routingKeys = new HashMap<String, Serializable>();
    routingKeys.put( "Source", new ArrayList<>( List.of( 10 ) ) );
    final var deleteMessage = new EntityMessage( 10,
                                                 1,
                                                 0,
                                                 routingKeys,
                                                 null,
                                                 null );
    final var changeSet = new ChangeSet();
    final var packet = new Packet( false, null, null, null, List.of( deleteMessage ), changeSet );

    session.getLock().lock();
    try
    {
      final var sourceEntry = session.createSubscriptionEntry( sourceAddress );
      sourceEntry.setExplicitlySubscribed( true );
      session.createSubscriptionEntry( targetAddress );
      session.recordGraphScopedGraphLink( sourceAddress, targetAddress );

      manager.sendChangeMessage( session, packet );

      assertNull( session.findSubscriptionEntry( sourceAddress ) );
      assertNull( session.findSubscriptionEntry( targetAddress ) );
    }
    finally
    {
      session.getLock().unlock();
    }

    assertEquals( changeSet.getChannelActions().size(), 2 );
    final var actionByAddress =
      changeSet
        .getChannelActions()
        .stream()
        .collect( java.util.stream.Collectors.toMap( ChannelAction::address, ChannelAction::action ) );
    assertEquals( actionByAddress.get( sourceAddress ), ChannelAction.Action.DELETE );
    assertEquals( actionByAddress.get( targetAddress ), ChannelAction.Action.REMOVE );
  }

  @Test
  public void sendChangeMessage_deleteWithInstancedSubscriptions_unsubscribesConcreteTargetsWithoutMessageLinks()
  {
    final var sourceChannel =
      new ChannelMetaData( 0,
                           "Source",
                           1,
                           ChannelMetaData.FilterType.STATIC_INSTANCED,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var targetChannel =
      new ChannelMetaData( 1,
                           "Target",
                           null,
                           ChannelMetaData.FilterType.STATIC_INSTANCED,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", sourceChannel, targetChannel );
    final var context = new TestSessionContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );

    final var webSocketSession = mock( Session.class );
    final var remote = mock( RemoteEndpoint.Basic.class );
    when( webSocketSession.getId() ).thenReturn( "session-1" );
    when( webSocketSession.isOpen() ).thenReturn( true );
    when( webSocketSession.getBasicRemote() ).thenReturn( remote );
    final var session = new ReplicantSession( webSocketSession );

    final var sourceAddressA = ChannelAddress.of( 0, 10, "fi-a" );
    final var sourceAddressB = ChannelAddress.of( 0, 10, "fi-b" );
    final var targetAddressA = ChannelAddress.of( 1, null, "fi-a" );
    final var targetAddressB = ChannelAddress.of( 1, null, "fi-b" );
    final var routingKeys = new HashMap<String, Serializable>();
    routingKeys.put( "Source", new ArrayList<>( List.of( 10 ) ) );
    final var deleteMessage = new EntityMessage( 10,
                                                 1,
                                                 0,
                                                 routingKeys,
                                                 null,
                                                 null );
    final var changeSet = new ChangeSet();
    final var packet = new Packet( false, null, null, null, List.of( deleteMessage ), changeSet );

    session.getLock().lock();
    try
    {
      final var sourceEntryA = session.createSubscriptionEntry( sourceAddressA );
      final var sourceEntryB = session.createSubscriptionEntry( sourceAddressB );
      sourceEntryA.setExplicitlySubscribed( true );
      sourceEntryB.setExplicitlySubscribed( true );
      session.createSubscriptionEntry( targetAddressA );
      session.createSubscriptionEntry( targetAddressB );
      session.recordGraphScopedGraphLink( sourceAddressA, targetAddressA );
      session.recordGraphScopedGraphLink( sourceAddressB, targetAddressB );

      manager.sendChangeMessage( session, packet );

      assertNull( session.findSubscriptionEntry( sourceAddressA ) );
      assertNull( session.findSubscriptionEntry( sourceAddressB ) );
      assertNull( session.findSubscriptionEntry( targetAddressA ) );
      assertNull( session.findSubscriptionEntry( targetAddressB ) );
    }
    finally
    {
      session.getLock().unlock();
    }
  }

  @Test
  public void tryGetCacheEntry_rejectsPartialAddresses()
    throws Exception
  {
    final var channel =
      new ChannelMetaData( 0,
                           "Source",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           ChannelMetaData.CacheType.INTERNAL,
                           true );
    final var schema = new SchemaMetaData( "Test", channel );
    final var context = new TestSessionContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );

    final var method =
      ReplicantSessionManagerImpl.class.getDeclaredMethod( "tryGetCacheEntry", ChannelAddress.class );
    method.setAccessible( true );

    final var exception =
      expectThrows( InvocationTargetException.class,
                    () -> method.invoke( manager, ChannelAddress.partial( 0 ) ) );
    assertTrue( exception.getCause() instanceof AssertionError );
  }

  @Nonnull
  private ReplicantSessionManagerImpl createManager( @Nonnull final TestSessionContext context,
                                                     @Nonnull final ReplicantMessageBroker broker )
  {
    final var manager = new ReplicantSessionManagerImpl();
    setField( manager, "_context", context );
    setField( manager, "_broker", broker );
    setField( manager, "_registry", TransactionSynchronizationRegistryUtil.lookup() );
    return manager;
  }

  @Nonnull
  private TestSessionContext createManagerContext( @Nonnull final SchemaMetaData schema )
  {
    return new TestSessionContext( schema );
  }

  private void setField( @Nonnull final Object target, @Nonnull final String name, @Nullable final Object value )
  {
    try
    {
      final var field = ReplicantSessionManagerImpl.class.getDeclaredField( name );
      field.setAccessible( true );
      field.set( target, value );
    }
    catch ( final Exception e )
    {
      throw new AssertionError( e );
    }
  }

  @Nonnull
  private ReplicantSession createOpenSession()
  {
    final var webSocketSession = mock( Session.class );
    final var remote = mock( RemoteEndpoint.Basic.class );
    when( webSocketSession.getId() ).thenReturn( "session-1" );
    when( webSocketSession.isOpen() ).thenReturn( true );
    when( webSocketSession.getBasicRemote() ).thenReturn( remote );
    return new ReplicantSession( webSocketSession );
  }

  @Nonnull
  private HashMap<String, Serializable> instanceRouting( @Nonnull final String channelName, final int rootId )
  {
    final var routingKeys = new HashMap<String, Serializable>();
    routingKeys.put( channelName, new ArrayList<>( List.of( rootId ) ) );
    return routingKeys;
  }

  @Nonnull
  private HashMap<String, Serializable> attributes( final int id )
  {
    final var attributes = new HashMap<String, Serializable>();
    attributes.put( "ID", id );
    return attributes;
  }

  private static final class TestSessionContext
    implements ReplicantSessionContext
  {
    @Nonnull
    private final SchemaMetaData _schema;
    @Nonnull
    private final List<BulkCollectCall> _bulkCollectCalls = new ArrayList<>();
    @Nonnull
    private final List<Packet> _preSendChangeMessages = new ArrayList<>();
    @Nonnull
    private final Set<ChannelAddress> _excludedFilterEntityMessageAddresses = new HashSet<>();
    private boolean _shouldFollowLink = true;

    private TestSessionContext( @Nonnull final SchemaMetaData schema )
    {
      _schema = schema;
    }

    @Nonnull
    @Override
    public SchemaMetaData getSchemaMetaData()
    {
      return _schema;
    }

    @Override
    public boolean isAuthorized( @Nonnull final ReplicantSession session )
    {
      return true;
    }

    @Override
    public void preSubscribe( @Nonnull final ReplicantSession session,
                              @Nonnull final ChannelAddress address,
                              @Nullable final JsonObject filter )
    {
    }

    @Override
    public void preSendChangeMessage( @Nonnull final ReplicantSession session, @Nonnull final Packet packet )
    {
      _preSendChangeMessages.add( packet );
    }

    @Nonnull
    @Override
    public JsonObject deriveTargetFilter( @Nonnull final EntityMessage entityMessage,
                                          @Nonnull final ChannelAddress source,
                                          @Nullable final JsonObject sourceFilter,
                                          @Nonnull final ChannelAddress target )
    {
      return Json.createObjectBuilder().add( "k", "v" ).build();
    }

    @Nonnull
    @Override
    public String deriveTargetFilterInstanceId( @Nonnull final EntityMessage entityMessage,
                                                @Nonnull final ChannelAddress source,
                                                @Nullable final JsonObject sourceFilter,
                                                @Nonnull final ChannelAddress target,
                                                @Nullable final JsonObject targetFilter )
    {
      final var sourceInstanceId = source.filterInstanceId();
      return null == sourceInstanceId ? "fi-7" : sourceInstanceId;
    }

    @Override
    public boolean flushOpenEntityManager()
    {
      return false;
    }

    @Override
    public void execCommand( @Nonnull final ReplicantSession session,
                             @Nonnull final String command,
                             final int requestId,
                             @Nullable final JsonObject payload )
    {
    }

    @Override
    public void collectChannelData( @Nullable final ReplicantSession session,
                                    @Nonnull final List<ChannelAddress> addresses,
                                    @Nullable final JsonObject filter,
                                    @Nonnull final ChangeSet changeSet,
                                    final boolean isExplicitSubscribe )
    {
      _bulkCollectCalls.add( new BulkCollectCall( addresses, filter, isExplicitSubscribe ) );
      if ( null != session )
      {
        for ( final var address : addresses )
        {
          final var existing = session.findSubscriptionEntry( address );
          final var entry = null == existing ? session.createSubscriptionEntry( address ) : existing;
          entry.setFilter( filter );
          changeSet.mergeAction( address,
                                 null == existing ? ChannelAction.Action.ADD : ChannelAction.Action.UPDATE,
                                 filter );
        }
      }
    }

    @Override
    public void collectChannelDataForFilterChange( @Nonnull final ReplicantSession session,
                                                   @Nonnull final List<ChannelAddress> addresses,
                                                   @Nullable final JsonObject originalFilter,
                                                   @Nullable final JsonObject newFilter,
                                                   @Nonnull final ChangeSet changeSet )
    {
    }

    @Nullable
    @Override
    public EntityMessage filterEntityMessage( @Nonnull final ReplicantSession session,
                                              @Nonnull final ChannelAddress address,
                                              @Nonnull final EntityMessage message )
    {
      if ( _excludedFilterEntityMessageAddresses.contains( address ) )
      {
        return null;
      }
      return message;
    }

    @Override
    public boolean shouldFollowLink( @Nonnull final ChannelAddress source,
                                     @Nullable final JsonObject sourceFilter,
                                     @Nonnull final ChannelAddress target,
                                     @Nullable final JsonObject targetFilter )
    {
      return _shouldFollowLink;
    }

    @Nonnull
    List<BulkCollectCall> getBulkCollectCalls()
    {
      return _bulkCollectCalls;
    }

    @Nonnull
    List<Packet> getPreSendChangeMessages()
    {
      return _preSendChangeMessages;
    }

    void excludeFilterEntityMessageAddress( @Nonnull final ChannelAddress address )
    {
      _excludedFilterEntityMessageAddresses.add( address );
    }

    void setShouldFollowLink( final boolean shouldFollowLink )
    {
      _shouldFollowLink = shouldFollowLink;
    }
  }

  private record BulkCollectCall(@Nonnull List<ChannelAddress> addresses,
                                 @Nullable JsonObject filter,
                                 boolean isExplicitSubscribe)
  {
  }

  private record DeriveTargetFilterInstanceIdCall(@Nonnull EntityMessage entityMessage,
                                                  @Nonnull ChannelAddress source,
                                                  @Nullable JsonObject sourceFilter,
                                                  @Nonnull ChannelAddress target,
                                                  @Nullable JsonObject targetFilter)
  {
  }

}
