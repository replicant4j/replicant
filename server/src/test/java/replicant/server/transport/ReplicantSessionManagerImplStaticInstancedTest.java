package replicant.server.transport;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonObject;
import javax.websocket.Session;
import org.realityforge.guiceyloops.server.TestInitialContextFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import replicant.server.ChannelAddress;
import replicant.server.ChannelLink;
import replicant.server.ChangeSet;
import replicant.server.EntityMessage;
import replicant.server.ee.RegistryUtil;
import replicant.server.ee.TransactionSynchronizationRegistryUtil;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantSessionManagerImplStaticInstancedTest
{
  @BeforeMethod
  public void setup()
  {
    RegistryUtil.bind();
  }

  @AfterMethod
  public void clearContext()
  {
    TestInitialContextFactory.reset();
  }

  @Test
  public void createChannelLinkEntry_staticInstanced_usesDerivedInstanceId()
    throws Exception
  {
    final var sourceChannel =
      new ChannelMetaData( 0,
                           "Source",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var targetChannel =
      new ChannelMetaData( 1,
                           "Target",
                           1,
                           ChannelMetaData.FilterType.STATIC_INSTANCED,
                           json -> json,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", sourceChannel, targetChannel );

    final var context = new TestSessionContext( schema );
    final var manager = new ReplicantSessionManagerImpl();
    manager._context = context;
    manager._broker = mock( ReplicantMessageBroker.class );
    manager._registry = TransactionSynchronizationRegistryUtil.lookup();

    final Session webSocketSession = mock( Session.class );
    when( webSocketSession.getId() ).thenReturn( "session-1" );
    when( webSocketSession.isOpen() ).thenReturn( true );
    final var session = new ReplicantSession( webSocketSession );

    final ChannelAddress sourceAddress = new ChannelAddress( 0 );
    final ChannelAddress targetAddress = new ChannelAddress( 1, 7 );
    final ChannelLink link = new ChannelLink( sourceAddress, targetAddress );

    session.getLock().lock();
    try
    {
      final var sourceEntry = session.createSubscriptionEntry( sourceAddress );
      sourceEntry.setFilter( "source-filter" );

      final var routingKeys = new HashMap<String, Serializable>();
      final var attributes = new HashMap<String, Serializable>();
      final var message = new EntityMessage( 1, 1, 0L, routingKeys, attributes, null );

      final Method method =
        ReplicantSessionManagerImpl.class
          .getDeclaredMethod( "createChannelLinkEntryIfRequired",
                              EntityMessage.class,
                              ReplicantSession.class,
                              ChannelLink.class );
      method.setAccessible( true );
      final ChannelLinkEntry entry =
        (ChannelLinkEntry) method.invoke( manager, message, session, link );

      assertNotNull( entry );
      assertEquals( entry.target(), new ChannelAddress( 1, 7, "derived-inst" ) );
    }
    finally
    {
      session.getLock().unlock();
    }
  }

  private static final class TestSessionContext
    implements ReplicantSessionContext
  {
    @Nonnull
    private final SchemaMetaData _schema;

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
                              @Nullable final Object filter )
    {
    }

    @Nonnull
    @Override
    public Object deriveTargetFilter( @Nonnull final EntityMessage entityMessage,
                                      @Nonnull final ChannelAddress source,
                                      @Nullable final Object sourceFilter,
                                      @Nonnull final ChannelAddress target )
    {
      return "target-filter";
    }

    @Nonnull
    @Override
    public String deriveFilterInstanceId( @Nonnull final EntityMessage entityMessage,
                                          @Nonnull final ChannelLink link,
                                          @Nullable final Object sourceFilter,
                                          @Nullable final Object targetFilter )
    {
      return "derived-inst";
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
    public boolean shouldFollowLink( @Nonnull final SubscriptionEntry sourceEntry,
                                     @Nonnull final ChannelAddress target,
                                     @Nullable final Object filter )
    {
      return true;
    }
  }
}
