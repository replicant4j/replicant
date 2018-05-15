package org.realityforge.replicant.client.gwt;

import com.google.gwt.core.client.Scheduler;
import elemental2.core.Global;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.base.Js;
import org.realityforge.replicant.client.transport.SessionContext;
import org.realityforge.replicant.client.transport.WebPollerDataLoaderService;
import replicant.ChangeSet;
import replicant.ReplicantContext;

public abstract class GwtDataLoaderService
  extends WebPollerDataLoaderService
{
  private final SessionContext _sessionContext;

  protected GwtDataLoaderService( @Nullable final ReplicantContext context,
                                  @Nonnull final Class<?> systemType,
                                  @Nonnull final SessionContext sessionContext )
  {
    super( context, systemType );
    _sessionContext = sessionContext;
  }

  @Nonnull
  @Override
  protected SessionContext getSessionContext()
  {
    return _sessionContext;
  }

  @Nonnull
  @Override
  protected ChangeSet parseChangeSet( @Nonnull final String rawJsonData )
  {
    return Js.cast( Global.JSON.parse( rawJsonData ));
  }

  protected void activateScheduler()
  {
    Scheduler.get().scheduleIncremental( this::scheduleTick );
  }
}
