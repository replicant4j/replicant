package org.realityforge.replicant.client.transport;

import arez.Arez;
import arez.ArezContext;
import arez.annotations.Action;
import arez.annotations.Observable;
import arez.annotations.PreDispose;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.AreaOfInterest;
import replicant.ChannelAddress;
import replicant.Replicant;
import replicant.spy.DataLoadStatus;
import replicant.spy.DataLoaderConnectEvent;
import replicant.spy.DataLoaderDisconnectEvent;
import replicant.spy.DataLoaderInvalidConnectEvent;
import replicant.spy.DataLoaderInvalidDisconnectEvent;

/**
 * Base class responsible for managing data loading from a particular source.
 *
 * TODO: This class exists as separate base class to make it easier to incrementally test code but should
 * eventually be migrated into AbstractDataLoaderService.
 */
public abstract class AbstractDataLoaderService2
  implements DataLoaderService
{
  private final ReplicantClientSystem _replicantClientSystem;

  @Nonnull
  private State _state = State.DISCONNECTED;

  protected AbstractDataLoaderService2( @Nonnull final ReplicantClientSystem replicantClientSystem )
  {
    _replicantClientSystem = Objects.requireNonNull( replicantClientSystem );
    _replicantClientSystem.registerDataSource( this );
  }

  @PreDispose
  void preDispose()
  {
    _replicantClientSystem.deregisterDataSource( this );
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  @Observable
  public State getState()
  {
    return _state;
  }

  protected void setState( @Nonnull final State state )
  {
    _state = Objects.requireNonNull( state );
  }

  /**
   * Invoked to fire an event when disconnect has completed.
   */
  protected final void onDisconnect()
  {
    setState( State.DISCONNECTED );
    _replicantClientSystem.updateStatus();
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context().getSpy().reportSpyEvent( new DataLoaderDisconnectEvent( getSystemType() ) );
    }
  }

  /**
   * Invoked to fire an event when failed to connect.
   */
  protected final void onInvalidDisconnect( @Nonnull final Throwable error )
  {
    setState( State.ERROR );
    _replicantClientSystem.updateStatus();
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context().getSpy().reportSpyEvent( new DataLoaderInvalidDisconnectEvent( getSystemType(), error ) );
    }
  }

  /**
   * Invoked to fire an event when disconnect has completed.
   */
  protected final void onConnect()
  {
    setState( State.CONNECTED );
    _replicantClientSystem.updateStatus();
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context().getSpy().reportSpyEvent( new DataLoaderConnectEvent( getSystemType() ) );
    }
  }

  /**
   * Invoked to fire an event when failed to connect.
   */
  protected final void onInvalidConnect( @Nonnull final Throwable error )
  {
    setState( State.ERROR );
    _replicantClientSystem.updateStatus();
    if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
    {
      Replicant.context().getSpy().reportSpyEvent( new DataLoaderInvalidConnectEvent( getSystemType(), error ) );
    }
  }

  /**
   * Invoked when a change set has been completely processed.
   *
   * @param status the status describing the results of data load.
   */
  protected void onMessageProcessed( @Nonnull final DataLoadStatus status )
  {
    //TODO: Add spy event
  }

  /**
   * Called when a data load has resulted in a failure.
   */
  protected final void onDataLoadFailure( @Nonnull final Throwable error )
  {
    _replicantClientSystem.disconnectIfPossible( this, error );
    //TODO: Add spy event
  }

  /**
   * Attempted to retrieve data from backend and failed.
   */
  protected final void onPollFailure( @Nonnull final Throwable error )
  {
    _replicantClientSystem.disconnectIfPossible( this, error );
    //TODO: Add spy event
  }

  protected final void onSubscribeStarted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.LOADING, null );
    //TODO: Add spy event
  }

  protected final void onSubscribeCompleted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.LOADED, null );
    //TODO: Add spy event
  }

  protected final void onSubscribeFailed( @Nonnull final ChannelAddress address, @Nonnull final Throwable error )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.LOAD_FAILED, error );
    //TODO: Add spy event
  }

  protected final void onUnsubscribeStarted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UNLOADING, null );
    //TODO: Add spy event
  }

  protected final void onUnsubscribeCompleted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UNLOADED, null );
    //TODO: Add spy event
  }

  protected final void onUnsubscribeFailed( @Nonnull final ChannelAddress address, @Nonnull final Throwable error )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UNLOADED, error );
    //TODO: Add spy event
  }

  protected final void onSubscriptionUpdateStarted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UPDATING, null );
    //TODO: Add spy event
  }

  protected final void onSubscriptionUpdateCompleted( @Nonnull final ChannelAddress address )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UPDATED, null );
    //TODO: Add spy event
  }

  protected final void onSubscriptionUpdateFailed( @Nonnull final ChannelAddress address,
                                                   @Nonnull final Throwable error )
  {
    updateAreaOfInterest( address, AreaOfInterest.Status.UPDATE_FAILED, error );
    //TODO: Add spy event
  }

  @Action
  protected void updateAreaOfInterest( @Nonnull final ChannelAddress address,
                                       @Nonnull final AreaOfInterest.Status status,
                                       @Nullable final Throwable error )
  {
    final AreaOfInterest areaOfInterest = Replicant.context().findAreaOfInterestByAddress( address );
    if ( null != areaOfInterest )
    {
      areaOfInterest.updateAreaOfInterest( status, error );
    }
  }

  @Nonnull
  protected final ReplicantClientSystem getReplicantClientSystem()
  {
    return _replicantClientSystem;
  }

  @Nonnull
  protected final ArezContext context()
  {
    return Arez.context();
  }
}
