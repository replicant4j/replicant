package replicant;

import arez.Arez;
import arez.ArezContext;
import arez.Component;
import arez.Observer;
import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.Autorun;
import arez.annotations.ComponentNameRef;
import arez.annotations.ComponentRef;
import arez.annotations.Computed;
import arez.annotations.ContextRef;
import arez.annotations.Observable;
import arez.annotations.ObservableRef;
import arez.component.CollectionsUtil;
import arez.component.ComponentObservable;
import arez.component.Identifiable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

@ArezComponent
abstract class ReplicantRuntime
{
  private final ArrayList<ConnectorEntry> _connectors = new ArrayList<>();
  private boolean _active = true;

  @Nonnull
  static ReplicantRuntime create()
  {
    return new Arez_ReplicantRuntime();
  }

  final void registerConnector( @Nonnull final Connector connector )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> _connectors.stream()
                   .noneMatch( e -> e.getConnector().getSchema().getId() == connector.getSchema().getId() ),
                 () -> "Replicant-0015: Invoked registerConnector for system schema named '" +
                       connector.getSchema().getName() + "' but a Connector for specified schema exists." );
    }
    getConnectorsObservable().preReportChanged();
    final ConnectorEntry entry = new ConnectorEntry( connector, true );
    _connectors.add( entry );
    final Object arezId = Identifiable.getArezId( connector );
    final Observer monitor =
      getContext().when( Arez.areNativeComponentsEnabled() ? component() : null,
                         Arez.areNamesEnabled() ? getComponentName() + ".Watcher." + arezId : null,
                         true,
                         () -> !ComponentObservable.observe( connector ),
                         () -> deregisterConnector( connector ),
                         true,
                         true );
    entry.setMonitor( monitor );
    getConnectorsObservable().reportChanged();
  }

  final void deregisterConnector( @Nonnull final Connector connector )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> _connectors.stream()
                   .anyMatch( e -> e.getConnector().getSchema().getId() == connector.getSchema().getId() ),
                 () -> "Replicant-0006: Invoked deregisterConnector for schema named '" +
                       connector.getSchema().getName() + "' but no Connector for specified schema exists." );
    }
    getConnectorsObservable().preReportChanged();
    _connectors.removeIf( e -> e.getConnector().getSchema().getId() == connector.getSchema().getId() );
    getConnectorsObservable().reportChanged();
  }

  @Observable( expectSetter = false )
  List<ConnectorEntry> getConnectors()
  {
    // When Arez can wrap @Observable(expectSetter=false) methods correctly, remove this explicit wrap
    return CollectionsUtil.wrap( _connectors );
  }

  @ObservableRef
  protected abstract arez.Observable getConnectorsObservable();

  /**
   * Set the "required" flag for connector for specified type.
   *
   * @param schemaId the if of the schema handled by connector.
   * @param required true if connector is required for the context to be active, false otherwise.
   */
  void setConnectorRequired( final int schemaId, final boolean required )
  {
    getConnectorEntryBySchemaId( schemaId ).setRequired( required );
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
  RuntimeState getState()
  {
    // Are any required connecting?
    boolean connecting = false;
    // Are any required disconnecting?
    boolean disconnecting = false;
    // Are any required disconnecting?
    boolean disconnected = false;
    // Are any required in error?
    boolean error = false;

    final List<ConnectorEntry> connectors = getConnectors();
    if ( connectors.isEmpty() )
    {
      // If there are no connectors then we just mirror the desired state (i.e. isActive flag)
      // to the actual state
      if ( isActive() )
      {
        return RuntimeState.CONNECTED;
      }
      else
      {
        return RuntimeState.DISCONNECTED;
      }
    }
    else
    {
      for ( final ConnectorEntry entry : connectors )
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
  void activate()
  {
    setActive( true );
  }

  /**
   * Mark the client system as inactive and start to converge to being DISCONNECTED.
   */
  @Action
  void deactivate()
  {
    setActive( false );
  }

  /**
   * Retrieve the Connector service associated with the schema.
   */
  @Nonnull
  Connector getConnector( final int schemaId )
  {
    return getConnectorEntryBySchemaId( schemaId ).getConnector();
  }

  @Nonnull
  ConnectorEntry getConnectorEntryBySchemaId( final int schemaId )
  {
    final ConnectorEntry entry = findConnectorEntryBySchemaId( schemaId );
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> null != entry,
                 () -> "Replicant-0007: Unable to locate Connector by schemaId " + schemaId );
    }
    assert null != entry;
    return entry;
  }

  @Nullable
  private ConnectorEntry findConnectorEntryBySchemaId( final int schemaId )
  {
    for ( final ConnectorEntry dataLoader : _connectors )
    {
      if ( dataLoader.getConnector().getSchema().getId() == schemaId )
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

  /**
   * Return the context associated with the service.
   *
   * @return the context associated with the service.
   */
  @ContextRef
  @Nonnull
  abstract ArezContext getContext();

  /**
   * Return the name associated with the service.
   *
   * @return the name associated with the service.
   */
  @ComponentNameRef
  @Nonnull
  abstract String getComponentName();

  /**
   * Return the component associated with service if native components enabled.
   *
   * @return the component associated with service if native components enabled.
   */
  @ComponentRef
  @Nonnull
  abstract Component component();
}
