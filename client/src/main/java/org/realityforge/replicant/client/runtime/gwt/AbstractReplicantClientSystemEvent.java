package org.realityforge.replicant.client.runtime.gwt;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;

public abstract class AbstractReplicantClientSystemEvent<H extends EventHandler>
  extends GwtEvent<H>
{
  private final ReplicantClientSystem _replicantClientSystem;

  public AbstractReplicantClientSystemEvent( @Nonnull final ReplicantClientSystem replicantClientSystem )
  {
    _replicantClientSystem = Objects.requireNonNull( replicantClientSystem );
  }

  @Nonnull
  public ReplicantClientSystem getReplicantClientSystem()
  {
    return _replicantClientSystem;
  }
}
