package replicant;

import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.Autorun;
import arez.annotations.Computed;
import arez.annotations.Observable;
import arez.annotations.ObservableRef;
import arez.component.RepositoryUtil;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

@ArezComponent
public abstract class ReplicantRuntime
{
  private final ArrayList<ConnectorEntry> _connectors = new ArrayList<>();
  private boolean _active = true;

  //TODO: Make this method package access
  public static ReplicantRuntime create()
  {
    return new Arez_ReplicantRuntime();
  }

  final void registerConnector( @Nonnull final Connector connector )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> _connectors.stream()
                   .noneMatch( e -> e.getConnector().getSystemType() == connector.getSystemType() ),
                 () -> "Replicant-0015: Invoked registerConnector for system type " + connector.getSystemType() +
                       " but a Connector for specified system type exists." );
    }
    getConnectorsObservable().preReportChanged();
    _connectors.add( new ConnectorEntry( connector, true ) );
    getConnectorsObservable().reportChanged();
  }

  final void deregisterConnector( @Nonnull final Connector connector )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> _connectors.stream()
                   .anyMatch( e -> e.getConnector().getSystemType() == connector.getSystemType() ),
                 () -> "Replicant-0006: Invoked deregisterConnector for system type " + connector.getSystemType() +
                       " but no Connector for specified system type exists." );
    }
    getConnectorsObservable().preReportChanged();
    _connectors.removeIf( e -> e.getConnector().getSystemType() == connector.getSystemType() );
    getConnectorsObservable().reportChanged();
  }

  @Observable( expectSetter = false )
  List<ConnectorEntry> getConnectors()
  {
    return RepositoryUtil.toResults( _connectors );
  }

  @ObservableRef
  protected abstract arez.Observable getConnectorsObservable();

  void setConnectorRequired( @Nonnull final Class<?> systemType, final boolean required )
  {
    getConnectorEntryBySystemType( systemType ).setRequired( required );
  }

  /**
   * Returns true when it is desired that all connectors should be connected.
   * This is a desired state rather than an actual state. Actual state is represented by {@link #getState()}.
   *
   * @return true if runtime should be connected, false otherwise.
   */
  @Observable
  boolean isActive()
  {
    return _active;
  }

  void setActive( final boolean active )
  {
    _active = active;
  }

  /**
   * Return the state of the runtime.
   *
   * @return the state of the runtime.
   */
  @Computed
  public RuntimeState getState()
  {
    // Are any required connecting?
    boolean connecting = false;
    // Are any required disconnecting?
    boolean disconnecting = false;
    // Are any required disconnecting?
    boolean disconnected = false;
    // Are any required in error?
    boolean error = false;

    for ( final ConnectorEntry entry : getConnectors() )
    {
      if ( entry.isRequired() )
      {
        final ConnectorState state = entry.getConnector().getState();
        if ( ConnectorState.DISCONNECTED == state )
        {
          disconnected = true;
        }
        else if ( ConnectorState.DISCONNECTING == state )
        {
          disconnecting = true;
        }
        else if ( ConnectorState.CONNECTING == state )
        {
          connecting = true;
        }
        else if ( ConnectorState.ERROR == state )
        {
          error = true;
        }
      }
    }
    if ( error )
    {
      return RuntimeState.ERROR;
    }
    else if ( disconnected )
    {
      return RuntimeState.DISCONNECTED;
    }
    else if ( disconnecting )
    {
      return RuntimeState.DISCONNECTING;
    }
    else if ( connecting )
    {
      return RuntimeState.CONNECTING;
    }
    else
    {
      return RuntimeState.CONNECTED;
    }
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
   * Retrieve the Connector service associated with the systemType.
   */
  @Nonnull
  Connector getConnector( @Nonnull final Class<?> systemType )
  {
    return getConnectorEntryBySystemType( systemType ).getConnector();
  }

  @Nonnull
  ConnectorEntry getConnectorEntryBySystemType( @Nonnull final Class<?> systemType )
  {
    final ConnectorEntry entry = findConnectorEntryBySystemType( systemType );
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> null != entry, () -> "Replicant-0007: Unable to locate Connector by systemType " + systemType );
    }
    assert null != entry;
    return entry;
  }

  @Nullable
  private ConnectorEntry findConnectorEntryBySystemType( @Nonnull final Class<?> systemType )
  {
    for ( final ConnectorEntry dataLoader : _connectors )
    {
      if ( dataLoader.getConnector().getSystemType().equals( systemType ) )
      {
        return dataLoader;
      }
    }
    return null;
  }

  @Autorun( mutation = true )
  void reflectActiveState()
  {
    final boolean active = isActive();
    for ( final ConnectorEntry entry : getConnectors() )
    {
      final ConnectorState state = entry.getConnector().getState();
      if ( !ConnectorState.isTransitionState( state ) )
      {
        if ( active && ConnectorState.CONNECTED != state )
        {
          entry.attemptAction( Connector::connect );
        }
        else if ( !active && ConnectorState.DISCONNECTED != state && ConnectorState.ERROR != state )
        {
          entry.attemptAction( Connector::disconnect );
        }
      }
    }
  }
}
