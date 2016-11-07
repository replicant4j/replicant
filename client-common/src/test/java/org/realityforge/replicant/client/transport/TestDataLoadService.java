package org.realityforge.replicant.client.transport;

import java.util.Arrays;
import java.util.LinkedList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChangeMapper;
import org.realityforge.replicant.client.ChangeSet;
import org.realityforge.replicant.client.ChannelDescriptor;
import org.realityforge.replicant.client.ChannelSubscriptionEntry;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;
import org.realityforge.replicant.client.EntityRepositoryValidator;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import static org.mockito.Mockito.*;

final class TestDataLoadService
  extends AbstractDataLoaderService<TestClientSession, TestGraph>
{
  private final EntityRepositoryValidator _validator;
  private boolean _validateOnLoad;
  private boolean _scheduleDataLoadCalled;
  private LinkedList<TestChangeSet> _changeSets = new LinkedList<>();
  private int _terminateCount;
  private DataLoadStatus _status;

  TestDataLoadService()
  {
    super( new SessionContext( "X" ),
           mock( EntityChangeBroker.class ),
           mock( EntityRepository.class ),
           mock( CacheService.class ),
           mock( EntitySubscriptionManager.class ) );
    _validator = mock( EntityRepositoryValidator.class );
  }

  @Nonnull
  @Override
  protected ChangeMapper newChangeMapper()
  {
    return mock( ChangeMapper.class );
  }

  @Nonnull
  @Override
  protected TestClientSession ensureSession()
  {
    final TestClientSession session = getSession();
    assert null != session;
    return session;
  }

  @Nonnull
  @Override
  protected Class<TestGraph> getGraphType()
  {
    return TestGraph.class;
  }

  @Nonnull
  @Override
  protected String getSystemKey()
  {
    return "TEST";
  }

  @Nonnull
  @Override
  protected EntityRepositoryValidator getEntityRepositoryValidator()
  {
    return _validator;
  }

  public void setValidateOnLoad( final boolean validateOnLoad )
  {
    _validateOnLoad = validateOnLoad;
  }

  public void setChangeSets( final TestChangeSet... changeSets )
  {
    _changeSets.addAll( Arrays.asList( changeSets ) );
  }

  @Nonnull
  @Override
  protected TestGraph channelToGraph( final int channel )
    throws IllegalArgumentException
  {
    return TestGraph.values()[ channel ];
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
    return isDataLoadComplete() && getStatus().isBulkLoad();
  }

  protected boolean isIncrementalLoadCompleteCalled()
  {
    return isDataLoadComplete() && !getStatus().isBulkLoad();
  }

  protected boolean isScheduleDataLoadCalled()
  {
    return _scheduleDataLoadCalled;
  }

  @Override
  protected void onDataLoadComplete( @Nonnull final DataLoadStatus status )
  {
    _status = status;
  }

  public DataLoadStatus getStatus()
  {
    return _status;
  }

  public boolean isDataLoadComplete()
  {
    return null != _status;
  }

  @Override
  protected void doScheduleDataLoad()
  {
    _scheduleDataLoadCalled = true;
  }

  @Override
  protected void progressDataLoadFailure( @Nonnull final Exception e )
  {
  }

  @Override
  protected boolean shouldValidateOnLoad()
  {
    return _validateOnLoad;
  }

  @Nonnull
  @Override
  protected ChangeSet parseChangeSet( @Nonnull final String rawJsonData )
  {
    return _changeSets.pop();
  }

  @Override
  protected void requestSubscribeToGraph( @Nonnull final TestGraph graph,
                                          @Nullable final Object id,
                                          @Nullable final Object filterParameter,
                                          @Nullable final String eTag,
                                          @Nullable final Runnable cacheAction,
                                          @Nonnull final Runnable completionAction )
  {
  }

  @Override
  protected void requestUnsubscribeFromGraph( @Nonnull final TestGraph graph,
                                              @Nullable final Object id,
                                              @Nonnull final Runnable runnable )
  {
  }

  @Override
  protected void requestUpdateSubscription( @Nonnull final TestGraph graph,
                                            @Nullable final Object id,
                                            @Nullable final Object filterParameter,
                                            @Nonnull final Runnable completionAction )
  {
  }

  @Override
  protected boolean doesEntityMatchFilter( @Nonnull final ChannelDescriptor descriptor,
                                           @Nullable final Object filter,
                                           @Nonnull final Class<?> entityType,
                                           @Nonnull final Object entityID )
  {
    return String.valueOf( entityID ).startsWith( "X" );
  }

  @Override
  protected int updateSubscriptionForFilteredEntities( @Nonnull final ChannelSubscriptionEntry graphEntry,
                                                       @Nonnull final Object filter )
  {
    return 0;
  }
}
