package org.realityforge.replicant.client.runtime;

import arez.annotations.Action;
import arez.annotations.Observable;
import arez.component.RepositoryUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.transport.DataLoaderListenerAdapter;
import org.realityforge.replicant.client.transport.DataLoaderService;

public abstract class ReplicantClientSystem
{
  public enum State
  {
    /// The service is not yet connected or has been disconnected
    DISCONNECTED,
    /// The service has started connecting but connection has not completed.
    CONNECTING,
    /// The service is connected.
    CONNECTED,
    /// The service has started disconnecting but disconnection has not completed.
    DISCONNECTING,
    /// The service is in error state. This error may occur during connection, disconnection or in normal operation.
    ERROR
  }

  protected static final int CONVERGE_DELAY_IN_MS = 2000;

  private static final Logger LOG = Logger.getLogger( ReplicantClientSystem.class.getName() );

  private final Listener _dataLoaderListener = new Listener();
  private DataLoaderEntry[] _dataLoaders;
  private State _state = State.DISCONNECTED;
  /**
   * If true then the desired state is CONNECTED while if false then the desired state is DISCONNECTED.
   */
  private boolean _active;

  public ReplicantClientSystem( final DataLoaderEntry[] dataLoaders )
  {
    _dataLoaders = Objects.requireNonNull( dataLoaders );
    addListener();
  }

  /**
   * Release resources associated with the system.
   */
  protected void release()
  {
    removeListener();
  }

  /**
   * Returns true if the system is expected to be active and connected to all data sources.
   * This is a desired state rather than an actual state that is represented by {@link #getState()}
   */
  public boolean isActive()
  {
    return _active;
  }

  /**
   * Return the actual state of the system.
   */
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
   * Mark the client system as active and start to converge to being CONNECTED.
   */
  public void activate()
  {
    setActive( true );
  }

  /**
   * Mark the client system as inactive and start to converge to being DISCONNECTED.
   */
  public void deactivate()
  {
    setActive( false );
  }

  /**
   * Attempt to converge the state of the system towards the desired state.
   * This should be invoked periodically.
   */
  public void converge()
  {
    updateStatus();
    reflectActiveState();
  }

  @Nonnull
  public List<DataLoaderEntry> getDataLoaders()
  {
    return RepositoryUtil.toResults( Arrays.asList( _dataLoaders ) );
  }

  /**
   * Retrieve the dataloader service associated with the channelType.
   */
  @Nonnull
  public DataLoaderService getDataLoaderService( @Nonnull final Enum channelType )
    throws IllegalArgumentException
  {
    for ( final DataLoaderEntry dataLoader : _dataLoaders )
    {
      if ( dataLoader.getService().getSystemType().equals( channelType.getClass() ) )
      {
        return dataLoader.getService();
      }
    }
    throw new IllegalArgumentException();
  }

  private void setActive( final boolean active )
  {
    _active = active;
    reflectActiveState();
  }

  private void reflectActiveState()
  {
    if ( isActive() )
    {
      doActivate();
    }
    else if ( !isActive() )
    {
      doDeactivate();
    }
  }

  private void doActivate()
  {
    for ( final DataLoaderEntry dataLoader : _dataLoaders )
    {
      final DataLoaderService service = dataLoader.getService();
      final DataLoaderService.State state = service.getState();
      if ( !isTransitionState( state ) && DataLoaderService.State.CONNECTED != state )
      {
        dataLoader.attemptAction( this::doConnectDataLoaderService );
      }
    }
  }

  private void doConnectDataLoaderService( @Nonnull final DataLoaderService service )
  {
    LOG.info( "Connecting " + service + ". Initial state: " + service.getState() );
    service.connect();
  }

  private boolean isTransitionState( @Nonnull final DataLoaderService.State state )
  {
    return DataLoaderService.State.DISCONNECTING == state ||
           DataLoaderService.State.CONNECTING == state;
  }

  private void doDeactivate()
  {
    for ( final DataLoaderEntry dataLoader : _dataLoaders )
    {
      final DataLoaderService service = dataLoader.getService();
      final DataLoaderService.State state = service.getState();
      if ( !isTransitionState( state ) &&
           DataLoaderService.State.DISCONNECTED != state &&
           DataLoaderService.State.ERROR != state )
      {
        dataLoader.attemptAction( this::doDisconnectDataLoaderService );
      }
    }
  }

  private void doDisconnectDataLoaderService( final DataLoaderService service )
  {
    final String message = "Disconnecting " + service + ". Initial state: " + service.getState();
    LOG.info( message );
    service.disconnect();
  }

  @Action
  protected void updateStatus()
  {
    // Are any required connecting?
    boolean connecting = false;
    // Are any required disconnecting?
    boolean disconnecting = false;
    // Are any required disconnecting?
    boolean disconnected = false;
    // Are any required in error?
    boolean error = false;

    for ( final DataLoaderEntry entry : _dataLoaders )
    {
      if ( entry.isRequired() )
      {
        final DataLoaderService.State state = entry.getService().getState();
        if ( DataLoaderService.State.DISCONNECTED == state )
        {
          disconnected = true;
        }
        else if ( DataLoaderService.State.DISCONNECTING == state )
        {
          disconnecting = true;
        }
        else if ( DataLoaderService.State.CONNECTING == state )
        {
          connecting = true;
        }
        else if ( DataLoaderService.State.ERROR == state )
        {
          error = true;
        }
      }
    }
    if ( error )
    {
      setState( State.ERROR );
    }
    else if ( disconnected )
    {
      setState( State.DISCONNECTED );
    }
    else if ( disconnecting )
    {
      setState( State.DISCONNECTING );
    }
    else if ( connecting )
    {
      setState( State.CONNECTING );
    }
    else
    {
      setState( State.CONNECTED );
    }
    reflectActiveState();
  }

  private void disconnectIfPossible( @Nonnull final DataLoaderService service, @Nonnull final Throwable cause )
  {
    LOG.log( Level.INFO, "Attempting to disconnect " + service + " and restart.", cause );
    if ( !isTransitionState( service.getState() ) )
    {
      service.disconnect();
    }
    updateStatus();
  }

  @Nonnull
  private Listener getDataLoaderListener()
  {
    return _dataLoaderListener;
  }

  private void addListener()
  {
    for ( final DataLoaderEntry dataLoader : _dataLoaders )
    {
      dataLoader.getService().addDataLoaderListener( getDataLoaderListener() );
    }
  }

  private void removeListener()
  {
    if ( null != _dataLoaders )
    {
      for ( final DataLoaderEntry dataLoader : _dataLoaders )
      {
        dataLoader.getService().removeDataLoaderListener( getDataLoaderListener() );
      }
    }
  }

  final class Listener
    extends DataLoaderListenerAdapter
  {
    @Override
    public void onDisconnect( @Nonnull final DataLoaderService service )
    {
      updateStatus();
    }

    @Override
    public void onInvalidDisconnect( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
    {
      updateStatus();
    }

    @Override
    public void onConnect( @Nonnull final DataLoaderService service )
    {
      updateStatus();
    }

    @Override
    public void onInvalidConnect( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
    {
      LOG.log( Level.INFO,
               "InvalidConnect: Error connecting " + service + ": " + throwable.toString() );
      updateStatus();
    }

    @Override
    public void onDataLoadFailure( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
    {
      disconnectIfPossible( service, throwable );
    }

    @Override
    public void onPollFailure( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
    {
      disconnectIfPossible( service, throwable );
    }
  }
}
