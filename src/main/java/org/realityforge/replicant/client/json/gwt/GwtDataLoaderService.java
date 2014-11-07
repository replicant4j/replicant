package org.realityforge.replicant.client.json.gwt;

import com.google.gwt.core.client.Scheduler;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.ChangeMapper;
import org.realityforge.replicant.client.ChangeSet;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.transport.AbstractDataLoaderService;
import org.realityforge.replicant.client.transport.CacheService;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.SessionContext;

public abstract class GwtDataLoaderService<T extends ClientSession<T, G>, G extends Enum>
  extends AbstractDataLoaderService<T, G>
{
  private final ReplicantConfig _replicantConfig;

  private boolean _incrementalDataLoadInProgress;

  protected GwtDataLoaderService( @Nonnull final SessionContext sessionContext,
                                  @Nonnull final ChangeMapper changeMapper,
                                  @Nonnull final EntityChangeBroker changeBroker,
                                  @Nonnull final EntityRepository repository,
                                  @Nonnull final CacheService cacheService,
                                  @Nonnull final EntitySubscriptionManager subscriptionManager,
                                  @Nonnull final ReplicantConfig replicantConfig )
  {
    super( sessionContext, changeMapper, changeBroker, repository, cacheService, subscriptionManager );
    _replicantConfig = replicantConfig;

    if ( _replicantConfig.repositoryDebugOutputEnabled() )
    {
      final String message =
        "RepositoryDebugOutput module is enabled. Run the javascript 'window.imitRepositoryDebug = true' " +
        "to enable debug output when change messages arrive.";
      LOG.info( message );
    }
  }

  @Override
  protected boolean shouldValidateOnLoad()
  {
    return _replicantConfig.shouldValidateRepositoryOnLoad();
  }

  //Static class to help check whether debug is enabled at the current time
  private static class RepositoryDebugEnabledChecker
  {
    public static native boolean isEnabled() /*-{
      return $wnd.imitRepositoryDebug == true;
    }-*/;
  }

  @Override
  protected boolean repositoryDebugOutputEnabled()
  {
    return _replicantConfig.repositoryDebugOutputEnabled() && RepositoryDebugEnabledChecker.isEnabled();
  }

  @Override
  protected ChangeSet parseChangeSet( final String rawJsonData )
  {
    return JsoChangeSet.asChangeSet( rawJsonData );
  }

  /**
   * Schedule data loads using incremental scheduler.
   */
  protected final void scheduleDataLoad()
  {
    if ( !_incrementalDataLoadInProgress )
    {
      _incrementalDataLoadInProgress = true;
      Scheduler.get().scheduleIncremental( new Scheduler.RepeatingCommand()
      {
        @Override
        public boolean execute()
        {
          try
          {
            final boolean aoiActionProgressed = progressAreaOfInterestActions();
            final boolean dataActionProgressed = progressDataLoad();
            _incrementalDataLoadInProgress = aoiActionProgressed || dataActionProgressed;
          }
          catch ( final Exception e )
          {
            progressDataLoadFailure( e );
            _incrementalDataLoadInProgress = false;
            return false;
          }
          return _incrementalDataLoadInProgress;
        }
      } );
    }
  }

  protected abstract void progressDataLoadFailure( @Nonnull Exception e );
}
