package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.ChannelAddress;

final class TestDataLoaderService
  implements DataLoaderService
{
  private final Class<? extends Enum> _systemType;
  private State _state;
  private boolean _connectCalled;
  private boolean _disconnectCalled;

  TestDataLoaderService( @Nonnull final Class<? extends Enum> systemType )
  {
    _systemType = systemType;
    _state = State.DISCONNECTED;
  }

  void setState( @Nonnull final State state )
  {
    _state = state;
  }

  @Nonnull
  @Override
  public State getState()
  {
    return _state;
  }

  void reset()
  {
    _connectCalled = false;
    _disconnectCalled = false;
  }

  @Override
  public void connect()
  {
    _connectCalled = true;
  }

  boolean isConnectCalled()
  {
    return _connectCalled;
  }

  @Override
  public void disconnect()
  {
    _disconnectCalled = true;
  }

  boolean isDisconnectCalled()
  {
    return _disconnectCalled;
  }

  @Override
  public void scheduleDataLoad()
  {
  }

  @Nonnull
  @Override
  public Class<? extends Enum> getSystemType()
  {
    return _systemType;
  }

  @Override
  public boolean isAreaOfInterestActionPending( @Nonnull final AreaOfInterestAction action,
                                                @Nonnull final ChannelAddress address,
                                                @Nullable final Object filter )
  {
    return false;
  }

  @Override
  public void requestSubscribe( @Nonnull final ChannelAddress address, @Nullable final Object filterParameter )
  {
  }

  @Override
  public void requestSubscriptionUpdate( @Nonnull final ChannelAddress address,
                                         @Nullable final Object filterParameter )
  {
  }

  @Override
  public void requestUnsubscribe( @Nonnull final ChannelAddress address )
  {
  }

  @Override
  public int indexOfPendingAreaOfInterestAction( @Nonnull final AreaOfInterestAction action,
                                                 @Nonnull final ChannelAddress address,
                                                 @Nullable final Object filter )
  {
    return -1;
  }
}
