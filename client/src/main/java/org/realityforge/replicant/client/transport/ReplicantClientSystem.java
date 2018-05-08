package org.realityforge.replicant.client.transport;

import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.Observable;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import org.realityforge.anodoc.TestOnly;
import replicant.Replicant;
import static org.realityforge.braincheck.Guards.*;

@Singleton
@ArezComponent
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

  private static final Logger LOG = Logger.getLogger( ReplicantClientSystem.class.getName() );

  private final ArrayList<DataLoaderEntry> _dataLoaders = new ArrayList<>();
  private State _state = State.DISCONNECTED;

  final void registerDataSource( @Nonnull final DataLoaderService service )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> _dataLoaders.stream()
                   .noneMatch( e -> e.getService().getSystemType() == service.getSystemType() ),
                 () -> "Replicant-0015: Invoked registerDataSource for system type " + service.getSystemType() +
                       " but a DataLoaderService for specified system type exists." );
    }
    _dataLoaders.add( new DataLoaderEntry( service, true ) );
  }

  final void deregisterDataSource( @Nonnull final DataLoaderService service )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> _dataLoaders.stream()
                   .anyMatch( e -> e.getService().getSystemType() == service.getSystemType() ),
                 () -> "Replicant-0006: Invoked deregisterDataSource for system type " + service.getSystemType() +
                       " but no DataLoaderService for specified system type exists." );
    }
    _dataLoaders.removeIf( e -> e.getService().getSystemType() == service.getSystemType() );
  }

  //TODO: This should be package access
  public void setDataSourceRequired( @Nonnull final Class<?> systemType, final boolean required )
  {
    getDataLoaderEntryBySystemType( systemType ).setRequired( required );
  }

  /**
   * Returns true if the system is expected to be active and connected to all data sources.
   * This is a desired state rather than an actual state that is represented by {@link #getState()}
   */
  @Observable
  public abstract boolean isActive();

  //TODO: This should be package access
  protected abstract void setActive( boolean active );

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
  @Action
  public void activate()
  {
    setActive( true );
  }

  /**
   * Mark the client system as inactive and start to converge to being DISCONNECTED.
   */
  @Action
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

  /**
   * Retrieve the dataloader service associated with the systemType.
   */
  @Nonnull
  public DataLoaderService getDataLoaderService( @Nonnull final Class<?> systemType )
  {
    return getDataLoaderEntryBySystemType( systemType ).getService();
  }

  @Nonnull
  DataLoaderEntry getDataLoaderEntryBySystemType( @Nonnull final Class<?> systemType )
  {
    final DataLoaderEntry entry = findDataLoaderEntryBySystemType( systemType );
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> null != entry,
                 () -> "Replicant-0007: Unable to locate DataLoaderService by systemType " + systemType );
    }
    assert null != entry;
    return entry;
  }

  @Nullable
  private DataLoaderEntry findDataLoaderEntryBySystemType( @Nonnull final Class<?> systemType )
  {
    for ( final DataLoaderEntry dataLoader : _dataLoaders )
    {
      if ( dataLoader.getService().getSystemType().equals( systemType ) )
      {
        return dataLoader;
      }
    }
    return null;
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
      final DataLoaderService.State state = dataLoader.getService().getState();
      if ( !isTransitionState( state ) && DataLoaderService.State.CONNECTED != state )
      {
        dataLoader.attemptAction( DataLoaderService::connect );
      }
    }
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
      final DataLoaderService.State state = dataLoader.getService().getState();
      if ( !isTransitionState( state ) &&
           DataLoaderService.State.DISCONNECTED != state &&
           DataLoaderService.State.ERROR != state )
      {
        dataLoader.attemptAction( DataLoaderService::disconnect );
      }
    }
  }

  // TODO: This should be package access once the services are consolidated
  @Action
  public void updateStatus()
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

  final void disconnectIfPossible( @Nonnull final DataLoaderService service, @Nonnull final Throwable cause )
  {
    // TODO: Add spy event for this scenario
    LOG.log( Level.INFO, "Attempting to disconnect " + service + " and restart.", cause );
    if ( !isTransitionState( service.getState() ) )
    {
      service.disconnect();
    }
    updateStatus();
  }

  @TestOnly
  final ArrayList<DataLoaderEntry> getDataLoaders()
  {
    return _dataLoaders;
  }
}
