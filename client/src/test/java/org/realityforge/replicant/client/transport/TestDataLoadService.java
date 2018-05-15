package org.realityforge.replicant.client.transport;

import arez.Arez;
import arez.annotations.ArezComponent;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.ChangeSet;
import replicant.ChannelAddress;
import replicant.Entity;
import replicant.Replicant;
import replicant.ReplicantContext;
import replicant.SafeProcedure;
import replicant.Subscription;
import replicant.spy.DataLoadStatus;
import static org.mockito.Mockito.*;

@ArezComponent
abstract class TestDataLoadService
  extends AbstractDataLoaderService
{
  private boolean _scheduleDataLoadCalled;
  private LinkedList<ChangeSet> _changeSets = new LinkedList<>();
  private DataLoadStatus _status;
  private final SessionContext _sessionContext;
  private final ChangeMapper _changeMapper;
  private int _validateRepositoryCallCount;

  @Nonnull
  static TestDataLoadService create()
  {
    return create( Replicant.areZonesEnabled() ? Replicant.context() : null );
  }

  @Nonnull
  private static TestDataLoadService create( @Nullable final ReplicantContext context )
  {
    return Arez.context().safeAction( () -> new Arez_TestDataLoadService( context ) );
  }

  TestDataLoadService( @Nullable final ReplicantContext context )
  {
    super( context, TestSystem.class );
    _sessionContext = new SessionContext( "X" );
    _changeMapper = mock( ChangeMapper.class );
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

  @Override
  protected void doConnect( @Nullable final SafeProcedure runnable )
  {
  }

  @Override
  protected void doDisconnect( @Nullable final SafeProcedure runnable )
  {
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

  void setChangeSets( final ChangeSet... changeSets )
  {
    _changeSets.addAll( Arrays.asList( changeSets ) );
  }

  LinkedList<ChangeSet> getChangeSets()
  {
    return _changeSets;
  }

  boolean isScheduleDataLoadCalled()
  {
    return _scheduleDataLoadCalled;
  }

  @Override
  protected void onMessageProcessed( @Nonnull final DataLoadStatus status )
  {
    super.onMessageProcessed( status );
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
  protected void activateScheduler()
  {
    _scheduleDataLoadCalled = true;
  }

  @Nonnull
  @Override
  protected ChangeSet parseChangeSet( @Nonnull final String rawJsonData )
  {
    return _changeSets.pop();
  }

  @Override
  protected void requestSubscribeToChannel( @Nonnull final ChannelAddress address,
                                            @Nullable final Object filter,
                                            @Nullable final String cacheKey,
                                            @Nullable final String eTag,
                                            @Nullable final Consumer<SafeProcedure> cacheAction,
                                            @Nonnull final Consumer<SafeProcedure> completionAction,
                                            @Nonnull final Consumer<SafeProcedure> failAction )
  {
  }

  @Override
  protected void requestUnsubscribeFromChannel( @Nonnull final ChannelAddress address,
                                                @Nonnull final Consumer<SafeProcedure> completionAction,
                                                @Nonnull final Consumer<SafeProcedure> failAction )
  {
  }

  @Override
  protected void requestUpdateSubscription( @Nonnull final ChannelAddress address,
                                            @Nonnull final Object filter,
                                            @Nonnull final Consumer<SafeProcedure> completionAction,
                                            @Nonnull final Consumer<SafeProcedure> failAction )
  {
  }

  @Override
  protected void requestBulkSubscribeToChannel( @Nonnull final List<ChannelAddress> addresses,
                                                @Nullable final Object filter,
                                                @Nonnull final Consumer<SafeProcedure> completionAction,
                                                @Nonnull final Consumer<SafeProcedure> failAction )
  {
  }

  @Override
  protected void requestBulkUnsubscribeFromChannel( @Nonnull final List<ChannelAddress> addresses,
                                                    @Nonnull final Consumer<SafeProcedure> completionAction,
                                                    @Nonnull final Consumer<SafeProcedure> failAction )
  {
  }

  @Override
  protected void requestBulkUpdateSubscription( @Nonnull final List<ChannelAddress> addresses,
                                                @Nonnull final Object filter,
                                                @Nonnull final Consumer<SafeProcedure> completionAction,
                                                @Nonnull final Consumer<SafeProcedure> failAction )
  {
  }

  @Override
  protected boolean doesEntityMatchFilter( @Nonnull final ChannelAddress address,
                                           @Nullable final Object filter,
                                           @Nonnull final Entity entity )
  {
    return entity.getId() < 0;
  }

  @Nonnull
  @Override
  protected String doFilterToString( @Nonnull final Object filter )
  {
    return String.valueOf( filter );
  }

  @Override
  protected void updateSubscriptionForFilteredEntities( @Nonnull final Subscription subscription,
                                                        @Nullable final Object filter )
  {
  }
}
