package org.realityforge.replicant.client.runtime;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.transport.DataLoaderListenerAdapter;
import org.realityforge.replicant.client.transport.DataLoaderService;

public abstract class ReplicantClientSystemImpl
  implements ReplicantClientSystem
{
  protected static final int CONVERGE_DELAY_IN_MS = 2000;

  private static final Logger LOG = Logger.getLogger( ReplicantClientSystemImpl.class.getName() );

  private final ReplicantSystemListenerSupport _listenerSupport = new ReplicantSystemListenerSupport();
  private final Listener _dataLoaderListener = new Listener();
  private DataLoaderEntry[] _dataLoaders;
  private List<DataLoaderEntry> _roDataLoaders;
  private State _state = State.DISCONNECTED;
  /**
   * If true then the desired state is CONNECTED while if false then the desired state is DISCONNECTED.
   */
  private boolean _active;

  protected void setDataLoaders( @Nonnull final DataLoaderEntry[] dataLoaders )
  {
    assert Arrays.stream( Objects.requireNonNull( dataLoaders ) ).allMatch( Objects::nonNull );
    removeListener();
    _dataLoaders = Objects.requireNonNull( dataLoaders );
    _roDataLoaders = Collections.unmodifiableList( Arrays.asList( _dataLoaders ) );
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
   * {@inheritDoc}
   */
  @Override
  public boolean isActive()
  {
    return _active;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public State getState()
  {
    return _state;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void activate()
  {
    setActive( true );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deactivate()
  {
    setActive( false );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void converge()
  {
    updateStatus();
    reflectActiveState();
  }

  @Override
  public boolean addReplicantSystemListener( @Nonnull final ReplicantSystemListener listener )
  {
    return _listenerSupport.addListener( listener );
  }

  @Override
  public boolean removeReplicantSystemListener( @Nonnull final ReplicantSystemListener listener )
  {
    return _listenerSupport.removeListener( listener );
  }

  @Nonnull
  protected ReplicantSystemListener getListener()
  {
    return _listenerSupport;
  }

  @Nonnull
  @Override
  public List<DataLoaderEntry> getDataLoaders()
  {
    return _roDataLoaders;
  }

  @Nonnull
  @Override
  public DataLoaderService getDataLoaderService( @Nonnull final Enum graph )
    throws IllegalArgumentException
  {
    for ( final DataLoaderEntry dataLoader : getDataLoaders() )
    {
      if ( dataLoader.getService().getGraphType().equals( graph.getClass() ) )
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
    LOG.info( "Connecting replicant data source " + service.getKey() + ". Initial state: " + service.getState() );
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
    final String message =
      "Disconnecting replicant data source " + service.getKey() + ". Initial state: " + service.getState();
    LOG.info( message );
    service.disconnect();
  }

  void updateStatus()
  {
    final State originalState = _state;

    // Are any required connecting?
    boolean connecting = false;
    // Are any required disconnecting?
    boolean disconnecting = false;
    // Are any required disconnecting?
    boolean disconnected = false;
    // Are any required in errror?
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
      _state = State.ERROR;
    }
    else if ( disconnected )
    {
      _state = State.DISCONNECTED;
    }
    else if ( disconnecting )
    {
      _state = State.DISCONNECTING;
    }
    else if ( connecting )
    {
      _state = State.CONNECTING;
    }
    else
    {
      _state = State.CONNECTED;
    }
    if ( originalState != _state )
    {
      getListener().stateChanged( this, _state, originalState );
    }
    reflectActiveState();
  }

  private void disconnectIfPossible( @Nonnull final DataLoaderService service )
  {
    if ( !isTransitionState( service.getState() ) )
    {
      service.disconnect();
    }
  }

  @Nonnull
  Listener getDataLoaderListener()
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
      updateStatus();
    }

    @Override
    public void onDataLoadFailure( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
    {
      LOG.log( Level.INFO,
               "DataLoadFailure: Attempting to disconnect data source " + service.getKey() + " and restart.",
               throwable );
      disconnectIfPossible( service );
      updateStatus();
    }

    @Override
    public void onPollFailure( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
    {
      LOG.log( Level.INFO,
               "PollFailure: Attempting to disconnect data source " + service.getKey() + " and restart.",
               throwable );
      disconnectIfPossible( service );
      updateStatus();
    }
  }
}
