package replicant;

import arez.Arez;
import arez.annotations.ArezComponent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.transport.AreaOfInterestAction;

@SuppressWarnings( "SameParameterValue" )
@ArezComponent
abstract class TestConnector
  extends Connector
{
  private boolean _errorOnConnect;
  private boolean _errorOnDisconnect;

  static TestConnector create( @Nonnull final Class<?> systemType )
  {
    return create( systemType, ReplicantClientSystem.create() );
  }

  static TestConnector create( @Nonnull final Class<?> systemType,
                               @Nonnull final ReplicantClientSystem replicantClientSystem )
  {
    return Arez.context().safeAction( () -> new Arez_TestConnector( systemType, replicantClientSystem ) );
  }

  TestConnector( @Nonnull final Class<?> systemType, @Nonnull final ReplicantClientSystem replicantClientSystem )
  {
    super( systemType, replicantClientSystem );
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
  public boolean isAreaOfInterestActionPending( @Nonnull final AreaOfInterestAction action,
                                                @Nonnull final ChannelAddress address,
                                                @Nullable final Object filter )
  {
    return false;
  }

  @Override
  public int indexOfPendingAreaOfInterestAction( @Nonnull final AreaOfInterestAction action,
                                                 @Nonnull final ChannelAddress address,
                                                 @Nullable final Object filter )
  {
    return 0;
  }

  @Override
  public void requestSubscribe( @Nonnull final ChannelAddress address, @Nullable final Object filterParameter )
  {
  }

  @Override
  public void requestSubscriptionUpdate( @Nonnull final ChannelAddress address, @Nullable final Object filterParameter )
  {
  }

  @Override
  public void requestUnsubscribe( @Nonnull final ChannelAddress address )
  {
  }
}
