package replicant;

import arez.Disposable;
import arez.ObservableValue;
import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.Feature;
import arez.annotations.Memoize;
import arez.annotations.Observable;
import arez.annotations.ObservableValueRef;
import arez.annotations.Observe;
import arez.component.DisposeNotifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

@ArezComponent( disposeNotifier = Feature.DISABLE, requireId = Feature.DISABLE )
abstract class ReplicantRuntime
{
  @Nonnull
  private final List<ConnectorEntry> _connectors = new ArrayList<>();
  private boolean _active = true;
  /**
   * Token used to authenticate replicant sessions.
   */
  @Nullable
  private String _authToken;

  @Nonnull
  static ReplicantRuntime create()
  {
    return new Arez_ReplicantRuntime();
  }

  /**
   * Request a resynchronization with the backend if necessary. This should rarely if ever be used
   * but can be required when bugs are present in the replicant code and it is not resynchronizing
   * with the backend.
   */
  @Action
  void requestSync()
  {
    getConnectors().forEach( c -> c.getConnector().maybeRequestSync() );
  }

  @Action( verifyRequired = false, reportParameters = false )
  public void setAuthToken( @Nullable final String authToken )
  {
    if ( !Objects.equals( _authToken, authToken ) )
    {
      _authToken = authToken;
      final List<ConnectorEntry> connectors = getConnectors();
      for ( final ConnectorEntry entry : connectors )
      {
        final Connector connector = entry.getConnector();
        if ( ConnectorState.CONNECTED == connector.getState() )
        {
          connector.getTransport().updateAuthToken( authToken );
        }
      }
    }
  }

  /**
   * @return a token used for authentication, if any.
   */
  @Nullable
  String getAuthToken()
  {
    return _authToken;
  }

  @Action
  void registerConnector( @Nonnull final Connector connector )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> _connectors.stream()
                   .noneMatch( e -> e.getConnector().getSchema().getId() == connector.getSchema().getId() ),
                 () -> "Replicant-0015: Invoked registerConnector for system schema named '" +
                       connector.getSchema().getName() + "' but a Connector for specified schema exists." );
    }
    getConnectorsObservableValue().preReportChanged();
    final ConnectorEntry entry = new ConnectorEntry( connector, true );
    _connectors.add( entry );
    DisposeNotifier
      .asDisposeNotifier( connector )
      .addOnDisposeListener( this, () -> deregisterConnector( connector ) );
    getConnectorsObservableValue().reportChanged();
  }

  void deregisterConnector( @Nonnull final Connector connector )
  {
    getConnectorsObservableValue().preReportChanged();
    detachConnector( connector );
    getConnectorsObservableValue().reportChanged();
  }

  private void detachConnector( @Nonnull final Connector connector )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> _connectors.stream()
                   .anyMatch( e -> e.getConnector().getSchema().getId() == connector.getSchema().getId() ),
                 () -> "Replicant-0006: Invoked deregisterConnector for schema named '" +
                       connector.getSchema().getName() + "' but no Connector for specified schema exists." );
    }
    _connectors.removeIf( e -> e.getConnector().getSchema().getId() == connector.getSchema().getId() );
    DisposeNotifier
      .asDisposeNotifier( connector )
      .removeOnDisposeListener( this );
  }

  @Observable( expectSetter = false )
  List<ConnectorEntry> getConnectors()
  {
    // When Arez can wrap @Observable(expectSetter=false) methods correctly, remove this explicit wrap
    return CollectionsUtil.wrap( _connectors );
  }

  @ObservableValueRef
  abstract ObservableValue<?> getConnectorsObservableValue();

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
  @Memoize( readOutsideTransaction = Feature.ENABLE )
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

  @Observable( readOutsideTransaction = Feature.ENABLE, writeOutsideTransaction = Feature.ENABLE )
  abstract int retryGeneration();

  abstract void setRetryGeneration( int value );

  private void incrementRetryGeneration()
  {
    /*
     * Called from timer that will trigger a change so that reflectActiveState() is reactivated
     */
    if ( Disposable.isNotDisposed( this ) )
    {
      setRetryGeneration( retryGeneration() + 1 );
    }
  }

  @Observe( mutation = true )
  void reflectActiveState()
  {
    // Need to watch retryGeneration so that observer is retriggered when it is changed
    retryGeneration();
    final boolean active = isActive();
    for ( final ConnectorEntry entry : getConnectors() )
    {
      final Connector connector = entry.getConnector();
      final ConnectorState state = connector.getState();
      if ( !ConnectorState.isTransitionState( state ) )
      {
        if ( active && ConnectorState.CONNECTED != state )
        {
          if ( !entry.attemptAction( Connector::connect ) )
          {
            final int delay = ( ConnectorEntry.REGEN_TIME_IN_SECONDS * 1000 ) + 50;
            Scheduler.scheduleOnceOff( this::incrementRetryGeneration, delay );
          }
        }
        else if ( !active && ConnectorState.DISCONNECTED != state && ConnectorState.ERROR != state )
        {
          entry.attemptAction( Connector::disconnect );
        }
      }
    }
  }
}
