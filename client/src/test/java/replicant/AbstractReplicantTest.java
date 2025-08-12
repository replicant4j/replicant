package replicant;

import arez.testng.ActionWrapper;
import arez.testng.ArezTestSupport;
import java.lang.reflect.Field;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@Listeners( MessageCollector.class )
@ActionWrapper( enable = false )
public abstract class AbstractReplicantTest
  implements ArezTestSupport
{
  @BeforeMethod
  @Override
  public void preTest()
    throws Exception
  {
    ArezTestSupport.super.preTest();
    ReplicantTestUtil.resetConfig( false );
    getProxyLogger().setLogger( new TestLogger() );
  }

  @AfterMethod
  @Override
  public void postTest()
  {
    ReplicantTestUtil.resetConfig( true );
    ArezTestSupport.super.postTest();
  }

  @Nonnull
  final Connection newConnection( @Nonnull final Connector connector )
  {
    connector.onConnection( ValueUtil.randomString() );
    final Connection connection = connector.ensureConnection();
    connection.setConnectionId( ValueUtil.randomString() );
    return connection;
  }

  @Nonnull
  final Entity findOrCreateEntity( @Nonnull final Class<?> type, final int id )
  {
    return safeAction( () -> Replicant
      .context()
      .getEntityService()
      .findOrCreateEntity( Replicant.areNamesEnabled() ? type.getSimpleName() + "/" + id : null,
                           type,
                           id ) );
  }

  @Nonnull
  protected final Subscription createSubscription( @Nonnull final ChannelAddress address,
                                                   @Nullable final Object filter,
                                                   final boolean explicitSubscription )
  {
    return safeAction( () -> Replicant.context()
      .getSubscriptionService()
      .createSubscription( address, filter, explicitSubscription ) );
  }

  @Nonnull
  final TestLogger getTestLogger()
  {
    return (TestLogger) getProxyLogger().getLogger();
  }

  @Nonnull
  private ReplicantLogger.ProxyLogger getProxyLogger()
  {
    return (ReplicantLogger.ProxyLogger) ReplicantLogger.getLogger();
  }

  @SuppressWarnings( "NonJREEmulationClassesInClientCode" )
  @Nonnull
  private Field toField( @Nonnull final Class<?> type, @Nonnull final String fieldName )
  {
    Class<?> clazz = type;
    while ( null != clazz && Object.class != clazz )
    {
      try
      {
        final Field field = clazz.getDeclaredField( fieldName );
        field.setAccessible( true );
        return field;
      }
      catch ( final Throwable t )
      {
        clazz = clazz.getSuperclass();
      }
    }
    fail();
    return null;
  }

  @SuppressWarnings( "SameParameterValue" )
  @Nullable
  final Object getFieldValue( @Nonnull final Object object, @Nonnull final String fieldName )
  {
    try
    {
      return toField( object.getClass(), fieldName ).get( object );
    }
    catch ( final Throwable t )
    {
      throw new AssertionError( t );
    }
  }

  @Nonnull
  protected final TestSpyEventHandler registerTestSpyEventHandler()
  {
    final TestSpyEventHandler handler = new TestSpyEventHandler();
    Replicant.context().getSpy().addSpyEventHandler( handler );
    return handler;
  }

  @Nonnull
  protected final SystemSchema newSchema()
  {
    return newSchema( ValueUtil.randomInt() );
  }

  @Nonnull
  final SystemSchema newSchema( final int schemaId )
  {
    final ChannelSchema[] channels = new ChannelSchema[ 0 ];
    final EntitySchema[] entities = new EntitySchema[ 0 ];
    return new SystemSchema( schemaId,
                             replicant.Replicant.areNamesEnabled() ? ValueUtil.randomString() : null,
                             channels,
                             entities );
  }

  @Nonnull
  final Connector createConnector()
  {
    return createConnector( newSchema( 1 ) );
  }

  @Nonnull
  final Connector createConnector( @Nonnull final SystemSchema schema )
  {
    return (Connector) Replicant.context().registerConnector( schema, mock( Transport.class ) );
  }

  @Nonnull
  final Connection createConnection()
  {
    final Connection connection = Connection.create( createConnector() );
    connection.setConnectionId( ValueUtil.randomString() );
    return connection;
  }
}
