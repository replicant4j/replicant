package org.realityforge.replicant.client.runtime.gwt;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;

public class StateChangedEvent
  extends AbstractReplicantClientSystemEvent<StateChangedEvent.Handler>
{
  public interface Handler
    extends EventHandler
  {
    void onStateChanged( @Nonnull StateChangedEvent event );
  }

  public static final GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

  @Nonnull
  private final ReplicantClientSystem.State _newState;
  @Nonnull
  private final ReplicantClientSystem.State _oldState;

  public StateChangedEvent( @Nonnull final ReplicantClientSystem replicantClientSystem,
                            @Nonnull final ReplicantClientSystem.State newState,
                            @Nonnull final ReplicantClientSystem.State oldState )
  {
    super( replicantClientSystem );
    _newState = newState;
    _oldState = oldState;
  }

  @Nonnull
  public ReplicantClientSystem.State getNewState()
  {
    return _newState;
  }

  @Nonnull
  public ReplicantClientSystem.State getOldState()
  {
    return _oldState;
  }

  @Override
  public GwtEvent.Type<Handler> getAssociatedType()
  {
    return TYPE;
  }

  @Override
  protected void dispatch( final StateChangedEvent.Handler handler )
  {
    handler.onStateChanged( this );
  }

  @Override
  public String toDebugString()
  {
    return toString();
  }

  public String toString()
  {
    return "StateChanged[NewState=" + getNewState() + ",OldState=" + getOldState() + "]";
  }
}
