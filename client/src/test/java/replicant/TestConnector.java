package replicant;

import arez.Arez;
import arez.annotations.ArezComponent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings( "SameParameterValue" )
@ArezComponent
public abstract class TestConnector
  extends Connector
{
  private boolean _errorOnConnect;
  private boolean _errorOnDisconnect;
  private int _progressAreaOfInterestRequestProcessingCount;
  private int _progressResponseProcessingCount;
  private int _activateSchedulerCount;
  private SafeFunction<Boolean> _progressAreaOfInterestRequestProcessing;
  private SafeFunction<Boolean> _progressResponseProcessing;

  public static TestConnector create( @Nonnull final Class<?> systemType )
  {
    return create( Replicant.areZonesEnabled() ? Replicant.context() : null, systemType );
  }

  static TestConnector create( @Nullable final ReplicantContext context, @Nonnull final Class<?> systemType )
  {
    return Arez.context().safeAction( () -> new Arez_TestConnector( context, systemType ) );
  }

  TestConnector( @Nullable final ReplicantContext context, @Nonnull final Class<?> systemType )
  {
    super( context, systemType );
  }

  void setErrorOnConnect( final boolean errorOnConnect )
  {
    _errorOnConnect = errorOnConnect;
  }

  void setErrorOnDisconnect( final boolean errorOnDisconnect )
  {
    _errorOnDisconnect = errorOnDisconnect;
  }

  @Override
  protected void doConnect( @Nonnull final SafeProcedure action )
  {
    if ( _errorOnConnect )
    {
      throw new IllegalStateException();
    }
  }

  @Override
  protected void doDisconnect( @Nonnull final SafeProcedure action )
  {
    if ( _errorOnDisconnect )
    {
      throw new IllegalStateException();
    }
  }

  @Override
  public void requestSubscribe( @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
  }

  @Override
  public void requestSubscriptionUpdate( @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
  }

  @Override
  public void requestUnsubscribe( @Nonnull final ChannelAddress address )
  {
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
}
