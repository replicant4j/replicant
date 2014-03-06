package org.realityforge.replicant.client.json.gwt;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.ChangeMapper;
import org.realityforge.replicant.client.ChangeSet;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;
import org.realityforge.replicant.client.transport.AbstractDataLoaderService;
import org.realityforge.replicant.client.transport.CacheService;
import org.realityforge.replicant.client.transport.ClientSession;

public abstract class GwtDataLoaderService<T extends ClientSession>
  extends AbstractDataLoaderService<T>
{
  private static final ReplicantConfig CONFIG = GWT.create( ReplicantConfig.class );

  private boolean _incrementalDataLoadInProgress;

  protected GwtDataLoaderService( final ChangeMapper changeMapper,
                                  final EntityChangeBroker changeBroker,
                                  final EntityRepository repository,
                                  final CacheService cacheService )
  {
    super( changeMapper, changeBroker, repository, cacheService );
  }

  @Override
  protected boolean shouldValidateOnLoad()
  {
    return CONFIG.shouldValidateRepositoryOnLoad();
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
            _incrementalDataLoadInProgress = progressDataLoad();
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
