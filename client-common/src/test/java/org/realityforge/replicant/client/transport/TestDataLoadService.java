package org.realityforge.replicant.client.transport;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChangeMapper;
import org.realityforge.replicant.client.ChangeSet;
import org.realityforge.replicant.client.ChannelDescriptor;
import org.realityforge.replicant.client.ChannelSubscriptionEntry;
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

  TestDataLoadService()
  {
    _sessionContext = new SessionContext( "X" );
    _cacheService = mock( CacheService.class );
    _changeMapper = mock( ChangeMapper.class );
    _validator = mock( EntityRepositoryValidator.class );
    _entitySystem = new EntitySystemImpl( mock( EntityRepository.class ),
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
  protected void doDisconnect( @Nullable final Runnable runnable )
  {
  }

  @Nonnull
  @Override
  public Class<? extends Enum> getGraphType()
  {
    return TestGraph.class;
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
      public boolean repositoryDebugOutputEnabled()
      {
        return false;
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
