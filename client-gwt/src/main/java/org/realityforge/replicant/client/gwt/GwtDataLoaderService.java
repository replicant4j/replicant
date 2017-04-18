package org.realityforge.replicant.client.gwt;

import com.google.gwt.core.client.Scheduler;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.ChangeSet;
import org.realityforge.replicant.client.transport.DataLoaderServiceConfig;
import org.realityforge.replicant.client.transport.SessionContext;
import org.realityforge.replicant.client.transport.WebPollerDataLoaderService;

public abstract class GwtDataLoaderService
  extends WebPollerDataLoaderService
{
  private final SessionContext _sessionContext;
  private final DataLoaderServiceConfig _config;

  protected GwtDataLoaderService( @Nonnull final SessionContext sessionContext,
                                  @Nonnull final ReplicantConfig replicantConfig )
  {
    _sessionContext = sessionContext;
    _config = new GwtDataLoaderServiceConfigImpl( getKey(), replicantConfig );
  }

  @Nonnull
  @Override
  protected DataLoaderServiceConfig config()
  {
    return _config;
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
    return JsoChangeSet.asChangeSet( rawJsonData );
  }

  protected void doScheduleDataLoad()
  {
    Scheduler.get().scheduleIncremental( this::stepDataLoad );
  }
}
