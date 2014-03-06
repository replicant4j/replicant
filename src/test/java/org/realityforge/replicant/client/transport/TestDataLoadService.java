package org.realityforge.replicant.client.transport;

import java.util.Arrays;
import java.util.LinkedList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChangeMapper;
import org.realityforge.replicant.client.ChangeSet;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;
import static org.mockito.Mockito.*;

final class TestDataLoadService
  extends AbstractDataLoaderService<TestClientSession, TestGraph>
{
  private final boolean _validateOnLoad;
  private boolean _scheduleDataLoadCalled;
  private LinkedList<TestChangeSet> _changeSets;
  private boolean _dataLoadComplete;
  private Boolean _bulkLoad;
  private String _requestID;
  private int _terminateCount;

  TestDataLoadService()
  {
    this( false );
  }

  TestDataLoadService( final boolean validateOnLoad,
                       final TestChangeSet... changeSets )
  {
    super( mock( ChangeMapper.class ),
           mock( EntityChangeBroker.class ),
           mock( EntityRepository.class ),
           mock( CacheService.class ) );
    _validateOnLoad = validateOnLoad;
    _changeSets = new LinkedList<TestChangeSet>( Arrays.asList( changeSets ) );
  }

  LinkedList<TestChangeSet> getChangeSets()
  {
    return _changeSets;
  }

  @Override
  protected void onTerminatingIncrementalDataLoadProcess()
  {
    _terminateCount++;
  }

  protected int getTerminateCount()
  {
    return _terminateCount;
  }

  protected boolean isBulkLoadCompleteCalled()
  {
    return null != _bulkLoad && _bulkLoad;
  }

  protected boolean isIncrementalLoadCompleteCalled()
  {
    return null != _bulkLoad && !_bulkLoad;
  }

  protected boolean isScheduleDataLoadCalled()
  {
    return _scheduleDataLoadCalled;
  }

  @Override
  protected void onDataLoadComplete( final boolean bulkLoad, @Nullable final String requestID )
  {
    _dataLoadComplete = true;
    _bulkLoad = bulkLoad;
    _requestID = requestID;
  }

  public boolean isDataLoadComplete()
  {
    return _dataLoadComplete;
  }

  public boolean isBulkLoad()
  {
    return _bulkLoad;
  }

  public String getRequestID()
  {
    return _requestID;
  }

  @Override
  protected void scheduleDataLoad()
  {
    _scheduleDataLoadCalled = true;
  }

  @Override
  protected boolean shouldValidateOnLoad()
  {
    return _validateOnLoad;
  }

  @Override
  protected ChangeSet parseChangeSet( final String rawJsonData )
  {
    return _changeSets.pop();
  }

  @Override
  protected void updateGraph( @Nonnull final TestGraph graph,
                              @Nullable final Object id,
                              @Nullable final Object filterParameter,
                              @Nullable final Object originalFilterParameter )
  {
  }

  @Override
  protected void subscribeToGraph( @Nonnull final TestGraph graph,
                                   @Nullable final Object id,
                                   @Nullable final Object filterParameter,
                                   @Nullable final String eTag,
                                   @Nullable final Runnable cacheAction,
                                   @Nonnull final Runnable completionAction )
  {
  }

  @Override
  protected void unsubscribeFromGraph( @Nonnull final TestGraph graph,
                                       @Nullable final Object id,
                                       @Nonnull final Runnable runnable )
  {
  }

  @Override
  protected void unloadGraph( @Nonnull final TestGraph graph, @Nullable final Object id )
  {
  }

  @Override
  protected void updateSubscription( @Nonnull final TestGraph graph,
                                     @Nullable final Object id,
                                     @Nullable final Object filterParameter,
                                     @Nonnull final Runnable completionAction )
  {
  }
}
