package replicant;

import arez.Arez;
import arez.ArezTestUtil;
import arez.Disposable;
import arez.Observer;
import arez.ObserverError;
import arez.Procedure;
import arez.SafeFunction;
import arez.SafeProcedure;
import elemental2.dom.DomGlobal;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.braincheck.BrainCheckTestUtil;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public abstract class AbstractReplicantTest
{
  private final ArrayList<String> _observerErrors = new ArrayList<>();
  private boolean _ignoreObserverErrors;
  private boolean _printObserverErrors;

  @BeforeMethod
  protected void beforeTest()
    throws Exception
  {
    BrainCheckTestUtil.resetConfig( false );
    ArezTestUtil.resetConfig( false );
    ReplicantTestUtil.resetConfig( false );
    getProxyLogger().setLogger( new TestLogger() );
    _ignoreObserverErrors = false;
    _printObserverErrors = true;
    _observerErrors.clear();
    Arez.context().addObserverErrorHandler( this::onObserverError );
    DomGlobal.window = null;
  }

  @Nonnull
  protected final Disposable pauseScheduler()
  {
    return Arez.context().pauseScheduler();
  }

  final void observer( @Nonnull final Procedure procedure )
  {
    Arez.context().observer( () -> {
      Arez.context().observable().reportObserved();
      procedure.call();
    } );
  }

  protected final void safeAction( @Nonnull final SafeProcedure action )
  {
    Arez.context().safeAction( action );
  }

  protected final <T> T safeAction( @Nonnull final SafeFunction<T> action )
  {
    return Arez.context().safeAction( action );
  }

  @Nonnull
  protected final Connection newConnection( @Nonnull final Connector connector )
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

  @AfterMethod
  protected void afterTest()
    throws Exception
  {
    BrainCheckTestUtil.resetConfig( true );
    ArezTestUtil.resetConfig( true );
    ReplicantTestUtil.resetConfig( true );
    if ( !_ignoreObserverErrors && !_observerErrors.isEmpty() )
    {
      fail( "Unexpected Observer Errors: " + _observerErrors.stream().collect( Collectors.joining( "\n" ) ) );
    }
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

  private void onObserverError( @Nonnull final Observer observer,
                                @Nonnull final ObserverError error,
                                @Nullable final Throwable throwable )
  {
    if ( !_ignoreObserverErrors )
    {
      final String message = "Observer: " + observer.getName() + " Error: " + error + " " + throwable;
      _observerErrors.add( message );
      if ( _printObserverErrors )
      {
        System.out.println( message );
      }
    }
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
  protected final Connector createConnector()
  {
    return createConnector( newSchema( 1 ) );
  }

  @Nonnull
  protected final Connector createConnector( @Nonnull final SystemSchema schema )
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
