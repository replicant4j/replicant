package org.realityforge.replicant.client.runtime;

import java.util.Date;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChannelAddress;
import org.realityforge.replicant.client.transport.AreaOfInterestAction;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.DataLoaderListener;
import org.realityforge.replicant.client.transport.DataLoaderListenerSupport;
import org.realityforge.replicant.client.transport.DataLoaderService;

final class TestDataLoaderService
  implements DataLoaderService
{
  private final String _key;
  private final Class<? extends Enum> _systemType;
  private State _state;
  private boolean _connectCalled;
  private boolean _disconnectCalled;
  private final DataLoaderListenerSupport _listenerSupport = new DataLoaderListenerSupport();

  TestDataLoaderService( @Nonnull final String key, @Nonnull final Class<? extends Enum> systemType )
  {
    _key = key;
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

  @Override
  public boolean addDataLoaderListener( @Nonnull final DataLoaderListener listener )
  {
    return _listenerSupport.addListener( listener );
  }

  @Override
  public boolean removeDataLoaderListener( @Nonnull final DataLoaderListener listener )
  {
    return _listenerSupport.removeListener( listener );
  }

  DataLoaderListenerSupport getListener()
  {
    return _listenerSupport;
  }

  @Nonnull
  @Override
  public String getKey()
  {
    return _key;
  }

  @Nonnull
  @Override
  public Class<? extends Enum> getSystemType()
  {
    return _systemType;
  }

  @Override
  public boolean isSubscribed( @Nonnull final ChannelAddress descriptor )
  {
    return false;
  }

  @Override
  public boolean isIdle()
  {
    return true;
  }

  @Nullable
  @Override
  public Date getConnectingAt()
  {
    return null;
  }

  @Nullable
  @Override
  public Date getConnectedAt()
  {
    return null;
  }

  @Nullable
  @Override
  public Date getDisconnectedAt()
  {
    return null;
  }

  @Nullable
  @Override
  public Throwable getLastErrorDuringConnection()
  {
    return null;
  }

  @Nullable
  @Override
  public Throwable getLastError()
  {
    return null;
  }

  @Nullable
  @Override
  public Date getLastErrorAt()
  {
    return null;
  }

  @Nullable
  @Override
  public ClientSession getSession()
  {
    return null;
  }

  @SuppressWarnings( "ConstantConditions" )
  @Nonnull
  @Override
  public ClientSession ensureSession()
  {
    return null;
  }

  @Override
  public boolean isAreaOfInterestActionPending( @Nonnull final AreaOfInterestAction action,
                                                @Nonnull final ChannelAddress descriptor,
                                                @Nullable final Object filter )
  {
    return false;
  }

  @Override
  public int indexOfPendingAreaOfInterestAction( @Nonnull final AreaOfInterestAction action,
                                                 @Nonnull final ChannelAddress descriptor,
                                                 @Nullable final Object filter )
  {
    return -1;
  }
}
