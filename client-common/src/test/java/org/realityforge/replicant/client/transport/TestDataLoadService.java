package org.realityforge.replicant.client.transport;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChangeMapper;
import org.realityforge.replicant.client.ChangeSet;
import org.realityforge.replicant.client.ChannelDescriptor;
import org.realityforge.replicant.client.ChannelSubscriptionEntry;
import org.realityforge.replicant.client.EntityLocator;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import static org.mockito.Mockito.*;

public final class TestDataLoadService
  extends AbstractDataLoaderService
{
  private boolean _validateOnLoad;
  private boolean _scheduleDataLoadCalled;
  private LinkedList<TestChangeSet> _changeSets = new LinkedList<>();
  private int _terminateCount;
  private DataLoadStatus _status;
  private final CacheService _cacheService;
  private final SessionContext _sessionContext;
  private final ChangeMapper _changeMapper;
  private final EntitySubscriptionManager _subscriptionManager;
  private final EntityLocator _entityLocator;
  private int _validateRepositoryCallCount;

  TestDataLoadService()
  {
    _sessionContext = new SessionContext( "X" );
    _cacheService = mock( CacheService.class );
    _changeMapper = mock( ChangeMapper.class );
    _subscriptionManager = mock( EntitySubscriptionManager.class );
    _entityLocator = mock( EntityLocator.class );
  }

  @Nonnull
  @Override
  protected EntityLocator getEntityLocator()
  {
    return _entityLocator;
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
  protected CacheService getCacheService()
  {
    return _cacheService;
  }

  @Nonnull
  @Override
  protected EntitySubscriptionManager getSubscriptionManager()
  {
    return _subscriptionManager;
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
  public Class<? extends Enum> getGraphType()
  {
    return TestGraph.class;
  }

  @Override
  protected void validateRepository()
    throws IllegalStateException
  {
    _validateRepositoryCallCount += 1;
  }

  void setValidateOnLoad( final boolean validateOnLoad )
  {
    _validateOnLoad = validateOnLoad;
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
      public boolean shouldRecordRequestKey()
      {
        return false;
      }

      @Override
      public boolean shouldValidateRepositoryOnLoad()
      {
        return _validateOnLoad;
      }

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
  protected void requestSubscribeToGraph( @Nonnull final ChannelDescriptor descriptor,
                                          @Nullable final Object filterParameter,
                                          @Nullable final String cacheKey,
                                          @Nullable final String eTag,
                                          @Nullable final Consumer<Runnable> cacheAction,
                                          @Nonnull final Consumer<Runnable> completionAction,
                                          @Nonnull final Consumer<Runnable> failAction )
  {
  }

  @Override
  protected void requestUnsubscribeFromGraph( @Nonnull final ChannelDescriptor descriptor,
                                              @Nonnull final Consumer<Runnable> completionAction,
                                              @Nonnull final Consumer<Runnable> failAction )
  {
  }

  @Override
  protected void requestUpdateSubscription( @Nonnull final ChannelDescriptor descriptor,
                                            @Nonnull final Object filterParameter,
                                            @Nonnull final Consumer<Runnable> completionAction,
                                            @Nonnull final Consumer<Runnable> failAction )
  {
  }

  @Override
  protected void requestBulkSubscribeToGraph( @Nonnull final List<ChannelDescriptor> descriptor,
                                              @Nullable final Object filterParameter,
                                              @Nonnull final Consumer<Runnable> completionAction,
                                              @Nonnull final Consumer<Runnable> failAction )
  {
  }

  @Override
  protected void requestBulkUnsubscribeFromGraph( @Nonnull final List<ChannelDescriptor> descriptors,
                                                  @Nonnull final Consumer<Runnable> completionAction,
                                                  @Nonnull final Consumer<Runnable> failAction )
  {
  }

  @Override
  protected void requestBulkUpdateSubscription( @Nonnull final List<ChannelDescriptor> descriptors,
                                                @Nonnull final Object filterParameter,
                                                @Nonnull final Consumer<Runnable> completionAction,
                                                @Nonnull final Consumer<Runnable> failAction )
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

  @Nonnull
  @Override
  protected String doFilterToString( @Nonnull final Object filterParameter )
  {
    return String.valueOf( filterParameter );
  }

  @Override
  protected int updateSubscriptionForFilteredEntities( @Nonnull final ChannelSubscriptionEntry graphEntry,
                                                       @Nullable final Object filter )
  {
    return 0;
  }
}
