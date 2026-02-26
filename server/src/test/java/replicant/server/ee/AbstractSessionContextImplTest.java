package replicant.server.ee;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonObject;
import javax.persistence.EntityManager;
import javax.websocket.Session;
import org.realityforge.guiceyloops.server.TestInitialContextFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import replicant.server.ChangeSet;
import replicant.server.ChannelAddress;
import replicant.server.EntityMessage;
import replicant.server.transport.ChannelMetaData;
import replicant.server.transport.ReplicantSession;
import replicant.server.transport.SchemaMetaData;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@SuppressWarnings( { "resource", "SqlNoDataSourceInspection" } )
public class AbstractSessionContextImplTest
{
  @BeforeMethod
  public void setup()
  {
    RegistryUtil.bind();
  }

  @AfterMethod
  public void teardown()
  {
    TestInitialContextFactory.reset();
  }

  @Test
  public void deriveTargetFilter_throwsWhenNotOverridden()
  {
    final var context = newContext( mock( EntityManager.class ) );
    final var source = new ChannelAddress( 1, 2 );
    final var target = new ChannelAddress( 3, 4 );
    final var message = new EntityMessage( 1, 2, 0, new HashMap<>(), null, null );

    final var exception =
      expectThrows( IllegalStateException.class,
                    () -> context.deriveTargetFilter( message, source, "filter", target ) );

    assertEquals( exception.getMessage(),
                  "deriveTargetFilter called for link from " + source + " to " + target +
                  " with source filter filter in the context of the entity message " + message +
                  " but no such graph link exists or the target graph has no filter parameter" );
  }

  @Test
  public void bulkCollectDataForSubscribe_delegates()
  {
    final var em = mock( EntityManager.class );
    final var context = newContext( em );
    final var session = newSession();
    final var addresses = List.of( new ChannelAddress( 1, 2 ), new ChannelAddress( 3, 4 ) );
    final var filter = Map.of( "k", "v" );
    final var changeSet = new ChangeSet();

    context.bulkCollectDataForSubscribe( session, addresses, filter, changeSet, true );

    assertEquals( context.getBulkCollectCalls().size(), 1 );
    final var call = context.getBulkCollectCalls().get( 0 );
    assertEquals( call.session(), session );
    assertEquals( call.addresses(), addresses );
    assertEquals( call.filter(), filter );
    assertEquals( call.changeSet(), changeSet );
    assertTrue( call.explicitSubscribe() );
  }

  @Test
  public void convertToEntityMessage_delegatesWithInitialLoadFalse()
  {
    final var context = newContext( mock( EntityManager.class ) );
    final var entity = new Object();
    final var message = new EntityMessage( 11, 7, 0, new HashMap<>(), Map.of( "k", "v" ) );
    context.registerMessageForObject( entity, message );

    assertEquals( context.convertToEntityMessage( entity, true ), message );
    assertEquals( context.getConvertCalls().size(), 1 );

    final var call = context.getConvertCalls().get( 0 );
    assertEquals( call.object(), entity );
    assertTrue( call.isUpdate() );
    assertFalse( call.isInitialLoad() );
  }

  @Test
  public void connection_usesEntityManagerUnwrap()
  {
    final var em = mock( EntityManager.class );
    final var connection = mock( Connection.class );
    when( em.unwrap( Connection.class ) ).thenReturn( connection );
    final var context = newContext( em );

    assertEquals( context.connection(), connection );
    verify( em ).unwrap( Connection.class );
  }

  @Test
  public void generateTempIdTable_buildsSql()
  {
    final var context = newContext( mock( EntityManager.class ) );
    final var addresses = List.of( new ChannelAddress( 1, 11 ),
                                   new ChannelAddress( 1, 12 ),
                                   new ChannelAddress( 1, 13 ) );

    final var sql = context.generateTempIdTable( addresses );

    assertEquals( sql,
                  """
                    DECLARE @Ids TABLE ( Id INTEGER NOT NULL );
                    INSERT INTO @Ids VALUES (11),(12),(13)
                    """ );
  }

  @Test
  public void generateTempIdAndFilterIdTable_buildsSql()
  {
    final var context = newContext( mock( EntityManager.class ) );
    final var addresses = List.of( new ChannelAddress( 1, 11, "fi-1" ),
                                   new ChannelAddress( 1, 12, "fi-2" ),
                                   new ChannelAddress( 1, 13, "fi-3" ) );

    final var sql = context.generateTempIdAndFilterIdTable( addresses );

    assertEquals( sql,
                  """
                    DECLARE @IdAndFilterIds TABLE ( Id INTEGER NOT NULL, FilterInstanceId VARCHAR(255) NOT NULL );
                    INSERT INTO @IdAndFilterIds VALUES (11,'fi-1'),(12,'fi-2'),(13,'fi-3')
                    """ );
  }

  @Test
  public void chunked_groupsValues()
  {
    final var context = newContext( mock( EntityManager.class ) );
    final var values = List.of( 1, 2, 3, 4, 5 );

    final List<List<Integer>> chunks = context.chunked( values.stream(), 2 ).toList();

    assertEquals( chunks.size(), 3 );
    assertEquals( chunks.get( 0 ), List.of( 1, 2 ) );
    assertEquals( chunks.get( 1 ), List.of( 3, 4 ) );
    assertEquals( chunks.get( 2 ), List.of( 5 ) );
  }

  @Test
  public void addInstanceRootRouterKey_appendsIds()
  {
    final var context = newContext( mock( EntityManager.class ) );
    final var routingKeys = new HashMap<String, Serializable>();

    context.addInstanceRootRouterKey( routingKeys, "R", 1 );
    context.addInstanceRootRouterKey( routingKeys, "R", 2 );

    @SuppressWarnings( "unchecked" )
    final var ids = (List<Integer>) routingKeys.get( "R" );
    assertEquals( ids, List.of( 1, 2 ) );
  }

  @Test
  public void decodeAttributes_populatesMap()
    throws Exception
  {
    final var context = newContext( mock( EntityManager.class ) );
    final var resultSet = mock( ResultSet.class );
    final var attributes = new HashMap<String, Serializable>();

    when( resultSet.getInt( "I" ) ).thenReturn( 12 );
    when( resultSet.getObject( "NI" ) ).thenReturn( 13 );
    when( resultSet.getTimestamp( "TS" ) ).thenReturn( new Timestamp( 1000L ) );
    when( resultSet.getTimestamp( "NTS" ) ).thenReturn( new Timestamp( 2000L ) );
    when( resultSet.getDate( "D" ) ).thenReturn( new java.sql.Date( 0L ) );
    when( resultSet.getDate( "ND" ) ).thenReturn( new java.sql.Date( 0L ) );
    when( resultSet.getString( "S" ) ).thenReturn( "value" );
    when( resultSet.getString( "NS" ) ).thenReturn( "nvalue" );
    when( resultSet.getBoolean( "B" ) ).thenReturn( true );
    when( resultSet.getObject( "NB" ) ).thenReturn( Boolean.FALSE );

    assertEquals( context.decodeIntAttribute( resultSet, attributes, "I", "I" ), 12 );
    assertEquals( context.decodeNullableIntAttribute( resultSet, attributes, "NI", "NI" ), Integer.valueOf( 13 ) );
    context.decodeTimestampAttribute( resultSet, attributes, "TS", "TS" );
    context.decodeNullableTimestampAttribute( resultSet, attributes, "NTS", "NTS" );
    context.decodeDateAttribute( resultSet, attributes, "D", "D" );
    context.decodeNullableDateAttribute( resultSet, attributes, "ND", "ND" );
    context.decodeStringAttribute( resultSet, attributes, "S", "S" );
    context.decodeNullableStringAttribute( resultSet, attributes, "NS", "NS" );
    context.decodeBooleanAttribute( resultSet, attributes, "B", "B" );
    context.decodeNullableBooleanAttribute( resultSet, attributes, "NB", "NB" );

    assertEquals( attributes.get( "I" ), 12 );
    assertEquals( attributes.get( "NI" ), 13 );
    assertEquals( attributes.get( "TS" ), 1000L );
    assertEquals( attributes.get( "NTS" ), 2000L );
    assertEquals( attributes.get( "D" ), context.toDateString( new java.sql.Date( 0L ) ) );
    assertEquals( attributes.get( "ND" ), context.toDateString( new java.sql.Date( 0L ) ) );
    assertEquals( attributes.get( "S" ), "value" );
    assertEquals( attributes.get( "NS" ), "nvalue" );
    assertEquals( attributes.get( "B" ), true );
    assertEquals( attributes.get( "NB" ), false );
  }

  @Test
  public void decodeNullableAttributes_skipNulls()
    throws Exception
  {
    final var context = newContext( mock( EntityManager.class ) );
    final var resultSet = mock( ResultSet.class );
    final var attributes = new HashMap<String, Serializable>();

    when( resultSet.getObject( "NI" ) ).thenReturn( null );
    when( resultSet.getTimestamp( "NTS" ) ).thenReturn( null );
    when( resultSet.getDate( "ND" ) ).thenReturn( null );
    when( resultSet.getString( "NS" ) ).thenReturn( null );
    when( resultSet.getObject( "NB" ) ).thenReturn( null );

    assertNull( context.decodeNullableIntAttribute( resultSet, attributes, "NI", "NI" ) );
    context.decodeNullableTimestampAttribute( resultSet, attributes, "NTS", "NTS" );
    context.decodeNullableDateAttribute( resultSet, attributes, "ND", "ND" );
    context.decodeNullableStringAttribute( resultSet, attributes, "NS", "NS" );
    context.decodeNullableBooleanAttribute( resultSet, attributes, "NB", "NB" );

    assertTrue( attributes.isEmpty() );
  }

  @Test
  public void toDateString_convertsDate()
  {
    final var context = newContext( mock( EntityManager.class ) );
    final var value = new Date( 10_000L );
    final var expected =
      new Date( value.getTime() )
        .toInstant()
        .atZone( ZoneId.systemDefault() )
        .toLocalDate()
        .toString();

    assertEquals( context.toDateString( value ), expected );
  }

  @Nonnull
  private TestSessionContext newContext( @Nonnull final EntityManager em )
  {
    return new TestSessionContext( em );
  }

  @Nonnull
  private ReplicantSession newSession()
  {
    final var webSocketSession = mock( Session.class );
    when( webSocketSession.getId() ).thenReturn( "session-1" );
    when( webSocketSession.isOpen() ).thenReturn( true );
    return new ReplicantSession( webSocketSession );
  }

  private static final class TestSessionContext
    extends AbstractSessionContextImpl
  {
    @Nonnull
    private final EntityManager _em;
    @Nonnull
    private final SchemaMetaData _schema = new SchemaMetaData(
      "Test",
      new ChannelMetaData( 0,
                           "Type0",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           true ),
      new ChannelMetaData( 1,
                           "Type1",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           true ),
      new ChannelMetaData( 2,
                           "Instance2",
                           1,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           true ) );
    @Nonnull
    private final List<BulkCollectCall> _bulkCollectCalls = new ArrayList<>();
    @Nonnull
    private final Map<Object, EntityMessage> _messages = new HashMap<>();
    @Nonnull
    private final List<ConvertCall> _convertCalls = new ArrayList<>();

    private TestSessionContext( @Nonnull final EntityManager em )
    {
      _em = em;
    }

    @Nonnull
    @Override
    protected EntityManager em()
    {
      return _em;
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
                              @Nullable final Object filter )
    {
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
    public void bulkCollectDataForSubscribe( @Nullable final ReplicantSession session,
                                             @Nonnull final List<ChannelAddress> addresses,
                                             @Nullable final Object filter,
                                             @Nonnull final ChangeSet changeSet,
                                             final boolean isExplicitSubscribe )
    {
      _bulkCollectCalls.add( new BulkCollectCall( session, addresses, filter, changeSet, isExplicitSubscribe ) );
    }

    @Nullable
    @Override
    protected EntityMessage convertToEntityMessage( @Nonnull final Object object,
                                                    final boolean isUpdate,
                                                    final boolean isInitialLoad )
    {
      _convertCalls.add( new ConvertCall( object, isUpdate, isInitialLoad ) );
      return _messages.get( object );
    }

    @Override
    public void bulkCollectDataForSubscriptionUpdate( @Nonnull final ReplicantSession session,
                                                      @Nonnull final List<ChannelAddress> addresses,
                                                      @Nullable final Object originalFilter,
                                                      @Nullable final Object filter,
                                                      @Nonnull final ChangeSet changeSet )
    {
    }

    @Nullable
    @Override
    public EntityMessage filterEntityMessage( @Nonnull final ReplicantSession session,
                                              @Nonnull final ChannelAddress address,
                                              @Nonnull final EntityMessage message )
    {
      return null;
    }

    @Override
    public boolean shouldFollowLink( @Nonnull final ChannelAddress source,
                                     final Object sourceFilter,
                                     @Nonnull final ChannelAddress target,
                                     @Nullable final Object targetFilter )
    {
      return false;
    }

    @Nonnull
    List<BulkCollectCall> getBulkCollectCalls()
    {
      return _bulkCollectCalls;
    }

    void registerMessageForObject( @Nonnull final Object object, @Nonnull final EntityMessage message )
    {
      _messages.put( object, message );
    }

    @Nonnull
    List<ConvertCall> getConvertCalls()
    {
      return _convertCalls;
    }
  }

  private record BulkCollectCall(@Nullable ReplicantSession session,
                                 @Nonnull List<ChannelAddress> addresses,
                                 @Nullable Object filter,
                                 @Nonnull ChangeSet changeSet,
                                 boolean explicitSubscribe)
  {
  }

  private record ConvertCall(@Nonnull Object object, boolean isUpdate, boolean isInitialLoad)
  {
  }
}
