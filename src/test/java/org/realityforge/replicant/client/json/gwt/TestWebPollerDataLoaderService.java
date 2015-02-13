package org.realityforge.replicant.client.json.gwt;

import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChangeMapper;
import org.realityforge.replicant.client.ChannelDescriptor;
import org.realityforge.replicant.client.ChannelSubscriptionEntry;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.transport.CacheService;
import org.realityforge.replicant.client.transport.SessionContext;
import org.realityforge.replicant.client.transport.TestClientSession;
import org.realityforge.replicant.client.transport.TestGraph;
import static org.mockito.Mockito.*;

final class TestWebPollerDataLoaderService
  extends WebPollerDataLoaderService<TestClientSession, TestGraph>
{
  public TestWebPollerDataLoaderService()
  {
    super( new SessionContext( "X" ),
           mock( ChangeMapper.class ),
           mock( EntityChangeBroker.class ),
           mock( EntityRepository.class ),
           mock( CacheService.class ),
           mock( EntitySubscriptionManager.class ),
           new SimpleEventBus(),
           mock( ReplicantConfig.class ) );
  }

  @Nonnull
  @Override
  protected Class<TestGraph> getGraphType()
  {
    return TestGraph.class;
  }

  @Nonnull
  @Override
  protected String deriveDefaultURL()
  {
    return "";
  }

  @Override
  protected void setupCloseHandler()
  {
    //Override to eliminate javascript call
  }

  @Nonnull
  public final EventBus lookupEventBus()
  {
    return getEventBus();
  }

  @Nonnull
  @Override
  protected TestClientSession createSession( @Nonnull final String sessionID )
  {
    return new TestClientSession( this, sessionID );
  }

  @Nonnull
  @Override
  protected TestGraph channelToGraph( final int channel )
    throws IllegalArgumentException
  {
    return TestGraph.values()[ channel ];
  }

  @Override
  protected boolean doesEntityMatchFilter( @Nonnull final ChannelDescriptor descriptor,
                                           @Nullable final Object filter,
                                           @Nonnull final Class entityType,
                                           @Nonnull final Object entityID )
  {
    return false;
  }

  @Override
  protected int updateSubscriptionForFilteredEntities( @Nonnull final ChannelSubscriptionEntry graphEntry,
                                                       @Nonnull final Object filter )
  {
    return 0;
  }

  @Nonnull
  @Override
  protected String getSystemKey()
  {
    return "TEST";
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
                                              @Nonnull final Runnable completionAction )
  {
  }

  @Override
  protected void requestUpdateSubscription( @Nonnull final TestGraph graph,
                                            @Nullable final Object id,
                                            @Nonnull final Object filterParameter,
                                            @Nonnull final Runnable completionAction )
  {
  }
}
