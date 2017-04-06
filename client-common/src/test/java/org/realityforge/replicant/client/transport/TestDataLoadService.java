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
import org.realityforge.replicant.client.EntitySystem;
import org.realityforge.replicant.client.EntitySystemImpl;
import static org.mockito.Mockito.*;

public final class TestDataLoadService
  extends AbstractDataLoaderService
{
  private final EntityRepositoryValidator _validator;
  private boolean _validateOnLoad;
  private boolean _scheduleDataLoadCalled;
  private LinkedList<TestChangeSet> _changeSets = new LinkedList<>();
  private int _terminateCount;
  private DataLoadStatus _status;
  private final CacheService _cacheService;
  private final SessionContext _sessionContext;
  private final ChangeMapper _changeMapper;
  private final EntitySystem _entitySystem;

  public TestDataLoadService()
  {
    _sessionContext = new SessionContext( "X" );
    _cacheService = mock( CacheService.class );
    _changeMapper = mock( ChangeMapper.class );
    _validator = mock( EntityRepositoryValidator.class );
    _entitySystem = new EntitySystemImpl( mock( EntityRepository.class ),
                                          mock( EntityChangeBroker.class ),
                                          mock( EntitySubscriptionManager.class ) );
  }

  @Nonnull
  @Override
  protected SessionContext getSessionContext()
  {
    return _sessionContext;
  }

  @Nonnull
  @Override
  protected CacheService getCacheService()
  {
    return _cacheService;
  }

  @Override
  protected EntitySystem getEntitySystem()
  {
    return _entitySystem;
  }

  @Nonnull
  @Override
  protected ChangeMapper getChangeMapper()
  {
    return _changeMapper;
  }

  @Nonnull
  @Override
  public ClientSession ensureSession()
  {
    final ClientSession session = getSession();
    assert null != session;
    return session;
  }

  @Override
  protected void doConnect( @Nullable final Runnable runnable )
  {
  }

  @Override
  protected void doDisconnect( @Nonnull final ClientSession session, @Nullable final Runnable runnable )
  {
  }

  @SuppressWarnings( "unchecked" )
  @Nonnull
  @Override
  public Class<Enum> getGraphType()
  {
    return (Class<Enum>) (Class) TestGraph.class;
  }

  @Nonnull
  @Override
  protected EntityRepositoryValidator getEntityRepositoryValidator()
  {
    return _validator;
  }

  void setValidateOnLoad( final boolean validateOnLoad )
  {
    _validateOnLoad = validateOnLoad;
  }

  void setChangeSets( final TestChangeSet... changeSets )
  {
    _changeSets.addAll( Arrays.asList( changeSets ) );
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

  int getTerminateCount()
  {
    return _terminateCount;
  }

  boolean isBulkLoadCompleteCalled()
  {
    return isDataLoadComplete() && getStatus().isBulkLoad();
  }

  boolean isIncrementalLoadCompleteCalled()
  {
    return isDataLoadComplete() && !getStatus().isBulkLoad();
  }

  boolean isScheduleDataLoadCalled()
  {
    return _scheduleDataLoadCalled;
  }

  @Override
  protected void onDataLoadComplete( @Nonnull final DataLoadStatus status )
  {
    _status = status;
  }

  DataLoadStatus getStatus()
  {
    return _status;
  }

  boolean isDataLoadComplete()
  {
    return null != _status;
  }

  @Override
  protected void doScheduleDataLoad()
  {
    _scheduleDataLoadCalled = true;
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
  protected void requestSubscribeToGraph( @Nonnull final Enum graph,
                                          @Nullable final Object id,
                                          @Nullable final Object filterParameter,
                                          @Nullable final String eTag,
                                          @Nullable final Runnable cacheAction,
                                          @Nonnull final Runnable completionAction )
  {
  }

  @Override
  protected void requestUnsubscribeFromGraph( @Nonnull final Enum graph,
                                              @Nullable final Object id,
                                              @Nonnull final Runnable runnable )
  {
  }

  @Override
  protected void requestUpdateSubscription( @Nonnull final Enum graph,
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
                                                       @Nullable final Object filter )
  {
    return 0;
  }
}
