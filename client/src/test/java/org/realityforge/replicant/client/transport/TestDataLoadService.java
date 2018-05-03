package org.realityforge.replicant.client.transport;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.Channel;
import replicant.ChannelAddress;
import replicant.Entity;
import replicant.Subscription;
import static org.mockito.Mockito.*;

final class TestDataLoadService
  extends AbstractDataLoaderService
{
  private final CacheService _cacheService;
  private boolean _scheduleDataLoadCalled;
  private LinkedList<TestChangeSet> _changeSets = new LinkedList<>();
  private int _terminateCount;
  private DataLoadStatus _status;
  private final SessionContext _sessionContext;
  private final ChangeMapper _changeMapper;
  private int _validateRepositoryCallCount;

  static TestDataLoadService create()
  {
    return new TestDataLoadService( mock( CacheService.class ) );
  }

  private TestDataLoadService( @Nonnull final CacheService cacheService )
  {
    super( cacheService );
    _sessionContext = new SessionContext( "X" );
    _changeMapper = mock( ChangeMapper.class );
    _cacheService = cacheService;
  }

  CacheService getCacheService()
  {
    return _cacheService;
  }

  @Nonnull
  @Override
  public Set<Class<?>> getEntityTypes()
  {
    return Collections.emptySet();
  }

  @Nonnull
  @Override
  protected SessionContext getSessionContext()
  {
    return _sessionContext;
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
  protected void doDisconnect( @Nullable final Runnable runnable )
  {
  }

  @Nonnull
  @Override
  public Class<? extends Enum> getSystemType()
  {
    return TestSystem.class;
  }

  @Override
  protected void validateRepository()
    throws IllegalStateException
  {
    _validateRepositoryCallCount += 1;
  }

  int getValidateRepositoryCallCount()
  {
    return _validateRepositoryCallCount;
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

  @Nonnull
  @Override
  protected DataLoaderServiceConfig config()
  {
    return new DataLoaderServiceConfig()
    {
      @Override
      public boolean subscriptionsDebugOutputEnabled()
      {
        return false;
      }

      @Override
      public boolean requestDebugOutputEnabled()
      {
        return false;
      }
    };
  }

  @Nonnull
  @Override
  protected ChangeSet parseChangeSet( @Nonnull final String rawJsonData )
  {
    return _changeSets.pop();
  }

  @Override
  protected void requestSubscribeToChannel( @Nonnull final ChannelAddress descriptor,
                                            @Nullable final Object filterParameter,
                                            @Nullable final String cacheKey,
                                            @Nullable final String eTag,
                                            @Nullable final Consumer<Runnable> cacheAction,
                                            @Nonnull final Consumer<Runnable> completionAction,
                                            @Nonnull final Consumer<Runnable> failAction )
  {
  }

  @Override
  protected void requestUnsubscribeFromChannel( @Nonnull final ChannelAddress descriptor,
                                                @Nonnull final Consumer<Runnable> completionAction,
                                                @Nonnull final Consumer<Runnable> failAction )
  {
  }

  @Override
  protected void requestUpdateSubscription( @Nonnull final ChannelAddress descriptor,
                                            @Nonnull final Object filterParameter,
                                            @Nonnull final Consumer<Runnable> completionAction,
                                            @Nonnull final Consumer<Runnable> failAction )
  {
  }

  @Override
  protected void requestBulkSubscribeToChannel( @Nonnull final List<ChannelAddress> descriptor,
                                                @Nullable final Object filterParameter,
                                                @Nonnull final Consumer<Runnable> completionAction,
                                                @Nonnull final Consumer<Runnable> failAction )
  {
  }

  @Override
  protected void requestBulkUnsubscribeFromChannel( @Nonnull final List<ChannelAddress> descriptors,
                                                    @Nonnull final Consumer<Runnable> completionAction,
                                                    @Nonnull final Consumer<Runnable> failAction )
  {
  }

  @Override
  protected void requestBulkUpdateSubscription( @Nonnull final List<ChannelAddress> descriptors,
                                                @Nonnull final Object filterParameter,
                                                @Nonnull final Consumer<Runnable> completionAction,
                                                @Nonnull final Consumer<Runnable> failAction )
  {
  }

  @Override
  protected boolean doesEntityMatchFilter( @Nonnull final Channel channel, @Nonnull final Entity entity )
  {
    return String.valueOf( entity.getId() ).startsWith( "X" );
  }

  @Nonnull
  @Override
  protected String doFilterToString( @Nonnull final Object filterParameter )
  {
    return String.valueOf( filterParameter );
  }

  @Override
  protected void updateSubscriptionForFilteredEntities( @Nonnull final Subscription subscription,
                                                        @Nullable final Object filter )
  {
  }
}
