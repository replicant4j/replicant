package replicant;

import arez.Arez;
import arez.annotations.ArezComponent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.mockito.Mockito.*;

@SuppressWarnings( "SameParameterValue" )
@ArezComponent
public abstract class TestConnector
  extends Connector
{
  private final ChangeMapper _changeMapper = mock( ChangeMapper.class );
  private boolean _errorOnConnect;
  private boolean _errorOnDisconnect;
  private int _activateSchedulerCount;
  private int _connectCallCount;
  private int _disconnectCallCount;

  public static TestConnector create()
  {
    return create( TestData.ROSE_SYSTEM );
  }

  public static TestConnector create( @Nonnull final SystemSchema schema )
  {
    return create( Replicant.areZonesEnabled() ? Replicant.context() : null, schema );
  }

  static TestConnector create( @Nullable final ReplicantContext context, @Nonnull final SystemSchema schema )
  {
    return Arez.context().safeAction( () -> new Arez_TestConnector( context, schema ) );
  }

  TestConnector( @Nullable final ReplicantContext context, @Nonnull final SystemSchema schema )
  {
    super( context, schema, mock( Transport.class ) );
  }

  void setErrorOnConnect( final boolean errorOnConnect )
  {
    _errorOnConnect = errorOnConnect;
  }

  void setErrorOnDisconnect( final boolean errorOnDisconnect )
  {
    _errorOnDisconnect = errorOnDisconnect;
  }

  @Nonnull
  @Override
  protected SubscriptionUpdateEntityFilter getSubscriptionUpdateFilter()
  {
    return ( address, filter, entity ) -> entity.getId() > 0;
  }

  @Override
  protected void doConnect( @Nonnull final SafeProcedure action )
  {
    _connectCallCount++;
    if ( _errorOnConnect )
    {
      throw new IllegalStateException();
    }
  }

  @Override
  protected void doDisconnect( @Nonnull final SafeProcedure action )
  {
    _disconnectCallCount++;
    if ( _errorOnDisconnect )
    {
      throw new IllegalStateException();
    }
  }

  @Override
  protected void activateScheduler()
  {
    _activateSchedulerCount++;
  }

  int getActivateSchedulerCount()
  {
    return _activateSchedulerCount;
  }

  int getConnectCallCount()
  {
    return _connectCallCount;
  }

  int getDisconnectCallCount()
  {
    return _disconnectCallCount;
  }

  @Nonnull
  @Override
  protected ChangeMapper getChangeMapper()
  {
    return _changeMapper;
  }
}
