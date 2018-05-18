package replicant;

import arez.Arez;
import arez.annotations.ArezComponent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.transport.ChangeMapper;
import static org.mockito.Mockito.*;

@SuppressWarnings( "SameParameterValue" )
@ArezComponent
public abstract class TestConnector
  extends Connector
{
  private final ChangeMapper _changeMapper = mock( ChangeMapper.class );
  private boolean _errorOnConnect;
  private boolean _errorOnDisconnect;
  private int _progressAreaOfInterestRequestProcessingCount;
  private int _progressResponseProcessingCount;
  private int _activateSchedulerCount;
  private SafeFunction<Boolean> _progressAreaOfInterestRequestProcessing;
  private SafeFunction<Boolean> _progressResponseProcessing;
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
    super( context, schema );
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

  @Override
  protected boolean progressAreaOfInterestRequestProcessing()
  {
    _progressAreaOfInterestRequestProcessingCount++;
    return null == _progressAreaOfInterestRequestProcessing ? false : _progressAreaOfInterestRequestProcessing.call();
  }

  @Override
  protected boolean progressResponseProcessing()
  {
    _progressResponseProcessingCount++;
    return null == _progressResponseProcessing ? false : _progressResponseProcessing.call();
  }

  int getProgressAreaOfInterestRequestProcessingCount()
  {
    return _progressAreaOfInterestRequestProcessingCount;
  }

  int getProgressResponseProcessingCount()
  {
    return _progressResponseProcessingCount;
  }

  int getActivateSchedulerCount()
  {
    return _activateSchedulerCount;
  }

  void setProgressAreaOfInterestRequestProcessing( final SafeFunction<Boolean> progressAreaOfInterestRequestProcessing )
  {
    _progressAreaOfInterestRequestProcessing = progressAreaOfInterestRequestProcessing;
  }

  void setProgressResponseProcessing( final SafeFunction<Boolean> progressResponseProcessing )
  {
    _progressResponseProcessing = progressResponseProcessing;
  }

  public int getConnectCallCount()
  {
    return _connectCallCount;
  }

  public int getDisconnectCallCount()
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
