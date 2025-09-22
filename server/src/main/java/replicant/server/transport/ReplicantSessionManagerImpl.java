package replicant.server.transport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.json.JsonValue;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.Transactional;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import org.jetbrains.annotations.VisibleForTesting;
import replicant.server.Change;
import replicant.server.ChangeSet;
import replicant.server.ChannelAction;
import replicant.server.ChannelAddress;
import replicant.server.ChannelLink;
import replicant.server.EntityMessage;
import replicant.server.EntityMessageEndpoint;
import replicant.server.ServerConstants;
import replicant.server.ee.EntityMessageCacheUtil;
import replicant.server.json.JsonEncoder;

/**
 * Session managers.
 */
@ApplicationScoped
@Transactional
@Typed( { EntityMessageEndpoint.class, ReplicantSessionManager.class } )
public class ReplicantSessionManagerImpl
  implements EntityMessageEndpoint, ReplicantSessionManager
{
  @Nonnull
  private static final Logger LOG = Logger.getLogger( ReplicantSessionManagerImpl.class.getName() );
  @Nonnull
  private final ReadWriteLock _lock = new ReentrantReadWriteLock();
  @Nonnull
  private final Map<String, ReplicantSession> _sessions = new HashMap<>();
  @Nonnull
  private final ReadWriteLock _cacheLock = new ReentrantReadWriteLock();
  @Nonnull
  private final Map<ChannelAddress, ChannelCacheEntry> _cache = new HashMap<>();
  @SuppressWarnings( "CdiInjectionPointsInspection" )
  @VisibleForTesting
  @Inject
  ReplicantSessionContext _context;
  @VisibleForTesting
  @Resource
  TransactionSynchronizationRegistry _registry;
  @VisibleForTesting
  @Inject
  ReplicantMessageBroker _broker;
  @Resource( lookup = "java:comp/DefaultManagedScheduledExecutorService" )
  ManagedScheduledExecutorService _executor;
  @Nullable
  private ScheduledFuture<?> _removeClosedSessionsFuture;
  @Nullable
  private ScheduledFuture<?> _pingSessionsFuture;

  @PostConstruct
  void postConstruct()
  {
    _removeClosedSessionsFuture = _executor.scheduleAtFixedRate( this::removeClosedSessions, 2, 1, TimeUnit.MINUTES );
    _pingSessionsFuture = _executor.scheduleAtFixedRate( this::pingSessions, 2, 1, TimeUnit.MINUTES );
  }

  @PreDestroy
  void preDestroy()
  {
    if ( null != _removeClosedSessionsFuture )
    {
      _removeClosedSessionsFuture.cancel( true );
      _removeClosedSessionsFuture = null;
    }
    if ( null != _pingSessionsFuture )
    {
      _pingSessionsFuture.cancel( true );
      _pingSessionsFuture = null;
    }
    removeAllSessions();
  }

  @Nonnull
  @Override
  public SchemaMetaData getSchemaMetaData()
  {
    return _context.getSchemaMetaData();
  }

  @SuppressWarnings( "resource" )
  @Override
  public boolean invalidateSession( @Nonnull final ReplicantSession session )
  {
    _lock.writeLock().lock();
    try
    {
      if ( null != _sessions.remove( session.getId() ) )
      {
        session.close();
        return true;
      }
      else
      {
        return false;
      }
    }
    finally
    {
      _lock.writeLock().unlock();
    }
  }

  @Override
  @Nullable
  public ReplicantSession getSession( @Nonnull final String sessionId )
  {
    _lock.readLock().lock();
    try
    {
      return _sessions.get( sessionId );
    }
    finally
    {
      _lock.readLock().unlock();
    }
  }

  @Nonnull
  @Override
  public Set<String> getSessionIDs()
  {
    _lock.readLock().lock();
    try
    {
      return new HashSet<>( _sessions.keySet() );
    }
    finally
    {
      _lock.readLock().unlock();
    }
  }

  @Nonnull
  Set<ReplicantSession> getSessions()
  {
    _lock.readLock().lock();
    try
    {
      return new HashSet<>( _sessions.values() );
    }
    finally
    {
      _lock.readLock().unlock();
    }
  }

  @Override
  @Nonnull
  public ReplicantSession createSession( @Nonnull final Session webSocketSession )
  {
    final ReplicantSession session = new ReplicantSession( webSocketSession );
    _lock.writeLock().lock();
    try
    {
      _sessions.put( session.getId(), session );
    }
    finally
    {
      _lock.writeLock().unlock();
    }
    return session;
  }

  @SuppressWarnings( { "WeakerAccess", "unused" } )
  public void pingSessions()
  {
    if ( _lock.readLock().tryLock() )
    {
      try
      {
        for ( final ReplicantSession session : _sessions.values() )
        {
          if ( LOG.isLoggable( Level.FINEST ) )
          {
            LOG.finest( "Pinging websocket for session " + session.getId() );
          }
          session.pingTransport();
        }
      }
      finally
      {
        _lock.readLock().unlock();
      }
    }
  }

  /**
   * Remove all sessions and force them to reconnect.
   */
  @SuppressWarnings( "WeakerAccess" )
  public void removeAllSessions()
  {
    if ( _lock.writeLock().tryLock() )
    {
      try
      {
        new ArrayList<>( _sessions.values() ).forEach( ReplicantSession::close );
        _sessions.clear();
      }
      finally
      {
        _lock.writeLock().unlock();
      }
    }
  }

  /**
   * Remove sessions that are associated with a closed WebSocket.
   */
  @SuppressWarnings( "WeakerAccess" )
  public void removeClosedSessions()
  {
    if ( _lock.writeLock().tryLock() )
    {
      try
      {
        final Iterator<Map.Entry<String, ReplicantSession>> iterator = _sessions.entrySet().iterator();
        while ( iterator.hasNext() )
        {
          final ReplicantSession session = iterator.next().getValue();
          if ( !session.getWebSocketSession().isOpen() )
          {
            iterator.remove();
          }
        }
      }
      finally
      {
        _lock.writeLock().unlock();
      }
    }
  }

  /**
   * Send messages to the specified session.
   * The requesting service must NOT have made any other changes that will be sent to the
   * client, otherwise this message will be discarded.
   *
   * @param session   the session.
   * @param etag      the etag for message if any.
   * @param changeSet the messages to be sent along to the client.
   */
  void queueCachedChangeSet( @Nonnull final ReplicantSession session,
                             @Nullable final String etag,
                             @Nonnull final ChangeSet changeSet )
  {
    final TransactionSynchronizationRegistry registry = _registry;
    final Integer requestId = (Integer) registry.getResource( ServerConstants.REQUEST_ID_KEY );
    registry.putResource( ServerConstants.REQUEST_COMPLETE_KEY, "0" );
    registry.putResource( ServerConstants.CACHED_RESULT_HANDLED_KEY, "1" );
    _broker.queueChangeMessage( session,
                                true,
                                requestId,
                                null,
                                etag,
                                Collections.emptyList(),
                                changeSet );
  }

  @Override
  public boolean saveEntityMessages( @Nullable final String sessionId,
                                     @Nullable final Integer requestId,
                                     @Nullable final JsonValue response,
                                     @Nonnull final Collection<EntityMessage> messages,
                                     @Nullable final ChangeSet sessionChanges )
  {
    boolean impactsInitiator = false;

    // Make sure if the message relates to an existing cache message then the cache is busted
    for ( final EntityMessage message : messages )
    {
      processCachePurge( message );
    }

    //TODO: Rewrite this so that we add clients to indexes rather than searching through everyone for each change!
    for ( final ReplicantSession session : getSessions() )
    {
      final boolean isInitiator = Objects.equals( session.getId(), sessionId );
      if ( isInitiator )
      {
        // The initiator has been impacted, even if the underlying session has been closed
        // so bring this logic outside of the session.isOpen() guard.
        impactsInitiator = true;
      }
      if ( session.isOpen() )
      {
        final ChangeSet changeSet = new ChangeSet();
        if ( isInitiator )
        {
          if ( null != sessionChanges )
          {
            changeSet.setRequired( sessionChanges.isRequired() );
            changeSet.merge( sessionChanges.getChanges() );
            changeSet.mergeActions( sessionChanges.getChannelActions() );
          }

          /*
           * We mark this as required and as impacting the initiator because we no longer know whether the
           * action did result in a message that needs to be sent to the client as routing occurs in a separate
           * thread. This change here now means every rpc will be paired with a replicant message even if it
           * is an empty ok message. This is acceptable in the short term as we expect to remove external rpc
           * at a later stage and move all rpc onto replicant channel.
           */
          if ( null == _registry.getResource( ServerConstants.CACHED_RESULT_HANDLED_KEY ) )
          {
            // We skip scenario when we have already sent a cached result
            changeSet.setRequired( true );
          }
        }
        final boolean altersExplicitSubscriptions =
          null != _registry.getResource( ServerConstants.SUBSCRIPTION_REQUEST_KEY );
        _broker.queueChangeMessage( session,
                                    altersExplicitSubscriptions,
                                    isInitiator ? requestId : null,
                                    isInitiator ? response : null,
                                    null,
                                    messages,
                                    changeSet );
      }
    }

    return impactsInitiator;
  }

  @Override
  public void sendChangeMessage( @Nonnull final ReplicantSession session,
                                 @Nullable final Integer requestId,
                                 @Nullable final JsonValue response,
                                 @Nullable final String etag,
                                 @Nonnull final Collection<EntityMessage> messages,
                                 @Nonnull final ChangeSet changeSet )
  {
    assert null == response || null != requestId;
    processMessages( messages, session, changeSet );

    // ChangeSets that occur during a subscription that result in a use-cache message
    // being sent to the client will still come through here. The hasContent() should
    // return false as there are no changes for in ChangeSet and the _required flag is unset.
    if ( changeSet.hasContent() )
    {
      completeMessageProcessing( session, changeSet );
      session.sendPacket( requestId, response, etag, changeSet );
    }
  }

  private void completeMessageProcessing( @Nonnull final ReplicantSession session, @Nonnull final ChangeSet changeSet )
  {
    try
    {
      final var pending = new HashSet<ChannelLinkEntry>();
      final var subscribed = new HashSet<ChannelLinkEntry>();

      while ( true )
      {
        collectChannelLinksToFollow( session, changeSet, pending, subscribed );
        if ( pending.isEmpty() )
        {
          break;
        }
        final var entry =
          pending
            .stream()
            .min( Comparator.comparing( ChannelLinkEntry::target ) )
            .orElse( null );
        final var target = entry.target();
        final var toSubscribe =
          target.hasRootId() ?
          pending
            .stream()
            .filter( a -> a.target().channelId() == target.channelId() &&
                          Objects.equals( a.filter(), entry.filter() ) )
            .toList() :
          Collections.singletonList( entry );
        final var channelMetaData = getSchemaMetaData().getChannelMetaData( target.channelId() );
        if ( channelMetaData.areBulkLoadsSupported() )
        {
          doBulkSubscribe( session,
                           target.channelId(),
                           channelMetaData.isTypeGraph() ?
                           null :
                           toSubscribe
                             .stream()
                             .map( ChannelLinkEntry::target )
                             .map( ChannelAddress::rootId )
                             .collect( Collectors.toList() ),
                           entry.filter(),
                           changeSet,
                           false );
        }
        else
        {
          for ( final var e : toSubscribe )
          {
            final var targetEntry = session.createSubscriptionEntry( e.target() );
            try
            {
              performSubscribe( session, targetEntry, false, entry.filter(), changeSet );
            }
            catch ( final Throwable t )
            {
              session.deleteSubscriptionEntry( targetEntry );
              throw t;
            }
          }
        }
        toSubscribe.forEach( pending::remove );
        subscribed.addAll( toSubscribe );
        for ( final var e : toSubscribe )
        {
          final var sourceEntry = session.getSubscriptionEntry( e.source() );
          final var targetEntry = session.getSubscriptionEntry( e.target() );
          linkSubscriptionEntries( sourceEntry, targetEntry );
        }
      }
    }
    catch ( final Exception e )
    {
      // This can occur when there is an error accessing the database
      if ( LOG.isLoggable( Level.INFO ) )
      {
        LOG.log( Level.INFO, "Error invoking expandLinks for session " + session.getId(), e );
      }
      session.close( new CloseReason( CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Expanding links failed" ) );
    }
  }

  /**
   * Collect a list of ChannelLinks in change set that may need to be followed.
   */
  void collectChannelLinksToFollow( @Nonnull final ReplicantSession session,
                                    @Nonnull final ChangeSet changeSet,
                                    @Nonnull final Set<ChannelLinkEntry> targets,
                                    @Nonnull final Set<ChannelLinkEntry> subscribed )
  {
    for ( final var change : changeSet.getChanges() )
    {
      final var entityMessage = change.getEntityMessage();
      if ( entityMessage.isUpdate() )
      {
        final var links = entityMessage.getLinks();
        if ( null != links )
        {
          for ( final var link : links )
          {
            final var alreadyCollected =
              subscribed
                .stream()
                .anyMatch( s -> s.source().equals( link.source() ) &&
                                s.target().equals( link.target() ) );
            if ( !alreadyCollected )
            {
              final var entry = createChannelLinkEntryIfRequired( session, link );
              if ( null != entry )
              {
                targets.add( entry );
              }
            }
          }
        }
      }
    }
  }

  /**
   * Determine if the specified ChannelLink needs to be follows and link channels if required. A ChannelLink
   * needs to be followed if the session is subscribed to the source channel and shouldFollowLink returns true.
   * The `shouldFollowLink` method is only invoked if the target graph is filtered otherwise the link
   * is always followed. If a link should be followed the source graph and target graph are linked.
   * <p>
   * This method does not perform the actual subscription and this is deferred to a separate process.
   */
  @Nullable
  ChannelLinkEntry createChannelLinkEntryIfRequired( @Nonnull final ReplicantSession session,
                                                     @Nonnull final ChannelLink link )
  {
    final var source = link.source();
    final var sourceEntry = session.findSubscriptionEntry( source );
    if ( null != sourceEntry )
    {
      final var target = link.target();
      final var targetHasFilter = getSchemaMetaData().getChannelMetaData( target ).hasFilterParameter();
      if ( !targetHasFilter || _context.shouldFollowLink( sourceEntry, target ) )
      {
        final var targetEntry = session.findSubscriptionEntry( target );
        if ( null == targetEntry )
        {
          return new ChannelLinkEntry( source,
                                       link.target(),
                                       targetHasFilter ?
                                       deriveFilterToPropagateFromSourceToTarget( sourceEntry ) :
                                       null );
        }
        else
        {
          linkSubscriptionEntries( sourceEntry, targetEntry );
        }
      }
    }
    return null;
  }

  private void processMessages( @Nonnull final Collection<EntityMessage> messages,
                                @Nonnull final ReplicantSession session,
                                @Nonnull final ChangeSet changeSet )
  {
    for ( final var message : messages )
    {
      processDeleteMessages( message, session, changeSet );
    }

    for ( final var message : messages )
    {
      processUpdateMessages( message, session, changeSet );
    }
  }

  private void updateSubscription( @Nonnull final ReplicantSession session,
                                   @Nonnull final ChannelAddress address,
                                   @Nullable final Object filter,
                                   @Nonnull final ChangeSet changeSet )
  {
    final var channel = getSchemaMetaData().getChannelMetaData( address );
    assert channel.hasFilterParameter();
    assert channel.getFilterType() == ChannelMetaData.FilterType.DYNAMIC;

    final var entry = session.getSubscriptionEntry( address );
    final var originalFilter = entry.getFilter();
    if ( doFiltersNotMatch( filter, originalFilter ) )
    {
      entry.setFilter( filter );
      _context.collectDataForSubscriptionUpdate( session, address, originalFilter, filter, changeSet );
      changeSet.mergeAction( address, ChannelAction.Action.UPDATE, filter );
      // If collectDataForSubscriptionUpdate indicates that we should unsubscribe from a channel
      // due to filter omitting entity (i.e. action == REMOVE) then we should explicitly unsubscribe
      // from the channel. It is expected the applications that use non-auto graph-links can signal
      // the removal of the target side by adding REMOVE action but it is up to this code to perform
      // the actual remove
      for ( final var channelAction : new ArrayList<>( changeSet.getChannelActions() ) )
      {
        if ( ChannelAction.Action.REMOVE == channelAction.action() )
        {
          final var other = session.findSubscriptionEntry( channelAction.address() );
          // It is unclear when other is ever allowed to be null. If it is null then it probably means
          // that collectDataForSubscriptionUpdate incorrectly added this action.z
          if ( null != other )
          {
            performUnsubscribe( session, other, true, false, changeSet );
          }
        }
      }
      _context.propagateSubscriptionFilterUpdate( session, address, filter, changeSet );
    }
  }

  @Override
  public void bulkSubscribe( @Nonnull final ReplicantSession session,
                             final int channelId,
                             @Nullable final Collection<Integer> rootIds,
                             @Nullable final Object filter )
    throws InterruptedException
  {
    if ( session.isOpen() )
    {
      final var lock = session.getLock();
      lock.lockInterruptibly();
      try
      {
        doBulkSubscribe( session, channelId, rootIds, filter, EntityMessageCacheUtil.getSessionChanges(), true );
      }
      finally
      {
        lock.unlock();
      }
    }
  }

  private void doBulkSubscribe( @Nonnull final ReplicantSession session,
                                final int channelId,
                                @Nullable final Collection<Integer> rootIds,
                                @Nullable final Object filter,
                                @Nonnull final ChangeSet changeSet,
                                final boolean isExplicitSubscribe )
  {
    final var channel = getSchemaMetaData().getChannelMetaData( channelId );
    assert ( channel.isInstanceGraph() && null != rootIds ) || ( channel.isTypeGraph() && null == rootIds );

    subscribeToRequiredTypeChannels( session, channel );

    final var newChannels = new ArrayList<ChannelAddress>();
    //OriginalFilter => Channels
    final var channelsToUpdate = new HashMap<Object, List<ChannelAddress>>();

    if ( null == rootIds )
    {
      final var address = new ChannelAddress( channelId );
      final var entry = session.findSubscriptionEntry( address );
      if ( null == entry )
      {
        newChannels.add( address );
      }
      else
      {
        final var existingFilter = entry.getFilter();
        if ( doFiltersNotMatch( filter, existingFilter ) )
        {
          channelsToUpdate.computeIfAbsent( existingFilter, k -> new ArrayList<>() ).add( address );
        }
      }
    }
    else
    {
      for ( final var root : rootIds )
      {
        final var address = new ChannelAddress( channelId, root );
        final var entry = session.findSubscriptionEntry( address );
        if ( null == entry )
        {
          newChannels.add( address );
        }
        else
        {
          final var existingFilter = entry.getFilter();
          if ( doFiltersNotMatch( filter, existingFilter ) )
          {
            channelsToUpdate.computeIfAbsent( existingFilter, k -> new ArrayList<>() ).add( address );
          }
          else if ( !entry.isExplicitlySubscribed() && isExplicitSubscribe )
          {
            entry.setExplicitlySubscribed( true );
          }
        }
      }
    }
    Throwable t = null;

    if ( !newChannels.isEmpty() )
    {
      if ( channel.areBulkLoadsSupported() )
      {
        _context.bulkCollectDataForSubscribe( session, newChannels, filter, changeSet, isExplicitSubscribe );
      }
      else
      {
        t = subscribeToAddresses( session, newChannels, filter, changeSet );
      }
    }
    if ( !channelsToUpdate.isEmpty() )
    {
      for ( final var update : channelsToUpdate.entrySet() )
      {
        final var originalFilter = update.getKey();
        final var addresses = update.getValue();

        if ( channel.areBulkLoadsSupported() )
        {
          if ( ChannelMetaData.FilterType.DYNAMIC == channel.getFilterType() )
          {
            _context.bulkCollectDataForSubscriptionUpdate( session, addresses, originalFilter, filter, changeSet );
          }
          else
          {
            final var message =
              "Attempted to update filter on channel " + channel.getName() + " to " + filter + " but the " +
              "channel that has a static filter. Unsubscribe and resubscribe to channel.";
            throw new AttemptedToUpdateStaticFilterException( message );
          }
        }
        else
        {
          final var error = subscribeToAddresses( session, addresses, filter, changeSet );
          if ( null != error )
          {
            t = error;
          }
        }
      }
    }
    if ( t instanceof Error )
    {
      throw (Error) t;
    }
    else if ( null != t )
    {
      throw (RuntimeException) t;
    }
  }

  @Nullable
  private Throwable subscribeToAddresses( @Nonnull final ReplicantSession session,
                                          @Nonnull final List<ChannelAddress> addresses,
                                          @Nullable final Object filter,
                                          @Nonnull final ChangeSet changeSet )
  {
    Throwable t = null;
    for ( final var address : addresses )
    {
      try
      {
        subscribe( session, address, true, filter, changeSet );
      }
      catch ( final Throwable e )
      {
        t = e;
      }
    }
    return t;
  }

  @Override
  public void subscribe( @Nonnull final ReplicantSession session,
                         @Nonnull final ChannelAddress address,
                         @Nullable final Object filter )
    throws InterruptedException
  {
    _context.preSubscribe( session, address, filter );
    if ( session.isOpen() )
    {
      final ReentrantLock lock = session.getLock();
      lock.lockInterruptibly();
      try
      {
        subscribe( session, address, true, filter, EntityMessageCacheUtil.getSessionChanges() );
      }
      finally
      {
        lock.unlock();
      }
    }
  }

  protected void subscribe( @Nonnull final ReplicantSession session,
                            @Nonnull final ChannelAddress address,
                            final boolean explicitlySubscribe,
                            @Nullable final Object filter,
                            @Nonnull final ChangeSet changeSet )
  {
    final var channelMetaData = getSchemaMetaData().getChannelMetaData( address );

    if ( session.isSubscriptionEntryPresent( address ) )
    {
      final var entry = session.getSubscriptionEntry( address );
      if ( explicitlySubscribe )
      {
        entry.setExplicitlySubscribed( true );
      }
      if ( ChannelMetaData.FilterType.DYNAMIC == channelMetaData.getFilterType() )
      {
        if ( channelMetaData.areBulkLoadsSupported() )
        {
          doBulkSubscribe( session,
                           address.channelId(),
                           channelMetaData.isTypeGraph() ?
                           null :
                           Collections.singletonList( address.rootId() ),
                           filter,
                           changeSet,
                           true );
        }
        else
        {
          updateSubscription( session, address, filter, changeSet );
        }
      }
      else if ( ChannelMetaData.FilterType.STATIC == channelMetaData.getFilterType() )
      {
        final var existingFilter = entry.getFilter();
        if ( doFiltersNotMatch( filter, existingFilter ) )
        {
          final var message =
            "Attempted to update filter on channel " + entry.address() + " from " + existingFilter +
            " to " + filter + " for channel that has a static filter. Unsubscribe and resubscribe to channel.";
          throw new AttemptedToUpdateStaticFilterException( message );
        }
      }
    }
    else
    {
      if ( channelMetaData.areBulkLoadsSupported() )
      {
        doBulkSubscribe( session,
                         address.channelId(),
                         channelMetaData.isTypeGraph() ?
                         null :
                         Collections.singletonList( address.rootId() ),
                         filter,
                         changeSet,
                         true );
      }
      else
      {
        final var entry = session.createSubscriptionEntry( address );
        try
        {
          performSubscribe( session, entry, explicitlySubscribe, filter, changeSet );
        }
        catch ( final Throwable e )
        {
          session.deleteSubscriptionEntry( entry );
          throw e;
        }
      }
    }
  }

  private boolean doFiltersNotMatch( final Object filter1, final Object filter2 )
  {
    return ( null != filter2 || null != filter1 ) && ( null == filter2 || !filter2.equals( filter1 ) );
  }

  void performSubscribe( @Nonnull final ReplicantSession session,
                         @Nonnull final SubscriptionEntry entry,
                         final boolean explicitSubscribe,
                         @Nullable final Object filter,
                         @Nonnull final ChangeSet changeSet )
  {
    entry.setFilter( filter );
    final var address = entry.address();
    final var channelMetaData = getSchemaMetaData().getChannelMetaData( address );

    subscribeToRequiredTypeChannels( session, channelMetaData );

    if ( channelMetaData.isCacheable() )
    {
      final var cacheEntry = tryGetCacheEntry( address );
      if ( null != cacheEntry )
      {
        if ( explicitSubscribe )
        {
          entry.setExplicitlySubscribed( true );
        }

        final var eTag = cacheEntry.getCacheKey();
        if ( eTag.equals( session.getETag( address ) ) )
        {
          if ( session.getWebSocketSession().isOpen() )
          {
            final var requestId = (Integer) _registry.getResource( ServerConstants.REQUEST_ID_KEY );
            WebSocketUtil.sendText( session.getWebSocketSession(),
                                    JsonEncoder.encodeUseCacheMessage( address, eTag, requestId ) );
            changeSet.setRequired( false );
            // We need to mark this as handled otherwise the wrapper will attempt to send
            // another ok message with same requestId
            _registry.putResource( ServerConstants.CACHED_RESULT_HANDLED_KEY, "1" );
          }
        }
        else
        {
          session.setETag( address, null );
          final var cacheChangeSet = new ChangeSet();
          cacheChangeSet.merge( cacheEntry.getChangeSet(), true );
          cacheChangeSet.mergeAction( address, ChannelAction.Action.ADD, filter );
          queueCachedChangeSet( session, eTag, cacheChangeSet );
          changeSet.setRequired( false );
        }
      }
      else
      {
        // If we get here then we have requested a cacheable instance channel
        // where the root has been removed
        assert address.hasRootId();
        final var cacheChangeSet = new ChangeSet();
        cacheChangeSet.mergeAction( address, ChannelAction.Action.DELETE, null );
        queueCachedChangeSet( session, null, cacheChangeSet );
        changeSet.setRequired( false );
      }
    }
    else
    {
      if ( channelMetaData.areBulkLoadsSupported() )
      {
        final var channelAddress =
          new ChannelAddress( address.channelId(),
                              channelMetaData.isTypeGraph() ? null : address.rootId() );
        _context.bulkCollectDataForSubscribe( session,
                                              Collections.singletonList( channelAddress ),
                                              filter,
                                              changeSet,
                                              explicitSubscribe );
      }
      else
      {
        final var result = _context.collectDataForSubscribe( address, filter, changeSet );
        if ( result.channelRootDeleted() )
        {
          changeSet.mergeAction( address, ChannelAction.Action.DELETE, null );
        }
        else
        {
          changeSet.mergeAction( address, ChannelAction.Action.ADD, filter );
          if ( explicitSubscribe )
          {
            entry.setExplicitlySubscribed( true );
          }
        }
      }
    }
  }

  private void subscribeToRequiredTypeChannels( @Nonnull final ReplicantSession session,
                                                @Nonnull final ChannelMetaData channelMetaData )
  {
    final var requiredTypeChannels = channelMetaData.getRequiredTypeChannels();
    if ( LOG.isLoggable( Level.FINE ) && requiredTypeChannels.length > 0 )
    {
      LOG.log( Level.FINE, "Subscribing to " +
                           channelMetaData.getName() +
                           " which has " +
                           requiredTypeChannels.length +
                           " required channels. " +
                           Arrays.stream( requiredTypeChannels )
                             .map( ChannelMetaData::getName )
                             .collect( Collectors.joining( "," ) ) );
    }
    for ( final var requiredTypeChannel : requiredTypeChannels )
    {
      assert requiredTypeChannel.isTypeGraph();
      // At the moment we propagate no filters ... which is fine
      assert ChannelMetaData.FilterType.NONE == requiredTypeChannel.getFilterType();
      final var address = new ChannelAddress( requiredTypeChannel.getChannelId() );

      // This check is sufficient as it is not an explicit subscribe and there are no filters that can change
      if ( !session.isSubscriptionEntryPresent( address ) )
      {
        final var requestId = (Integer) _registry.getResource( ServerConstants.REQUEST_ID_KEY );
        final var requestComplete = (String) _registry.getResource( ServerConstants.REQUEST_COMPLETE_KEY );
        final var requestResponse = (String) _registry.getResource( ServerConstants.REQUEST_RESPONSE_KEY );
        final var requestCachedResultHandled =
          (String) _registry.getResource( ServerConstants.CACHED_RESULT_HANDLED_KEY );

        _registry.putResource( ServerConstants.REQUEST_ID_KEY, null );
        _registry.putResource( ServerConstants.REQUEST_COMPLETE_KEY, null );
        _registry.putResource( ServerConstants.REQUEST_RESPONSE_KEY, null );
        _registry.putResource( ServerConstants.CACHED_RESULT_HANDLED_KEY, null );

        final var changeSet = new ChangeSet();
        subscribe( session, address, false, null, changeSet );
        if ( changeSet.hasContent() )
        {
          // In this scenario we have a non-cached changeset, so we send it along
          _broker.queueChangeMessage( session, true, null, null, null, Collections.emptyList(), changeSet );
        }

        _registry.putResource( ServerConstants.REQUEST_ID_KEY, requestId );
        _registry.putResource( ServerConstants.REQUEST_COMPLETE_KEY, requestComplete );
        _registry.putResource( ServerConstants.REQUEST_RESPONSE_KEY, requestResponse );
        _registry.putResource( ServerConstants.CACHED_RESULT_HANDLED_KEY, requestCachedResultHandled );
      }
    }
  }

  @SuppressWarnings( "WeakerAccess" )
  protected boolean deleteCacheEntry( @Nonnull final ChannelAddress address )
  {
    _cacheLock.writeLock().lock();
    try
    {
      final var metaData = getSchemaMetaData().getChannelMetaData( address );
      final var cacheRemoved = null != _cache.remove( address );
      if ( cacheRemoved )
      {
        // If we expire the cache then any dependent type graphs must also be expired. This is
        // required as when a cache is on a client then we send back a "use-cache" message immediately
        // whereas if a message for a cached has to be loaded and sent back then we queue it on
        // ReplicantSession._pendingSubscriptionPackets and will be sent back. Unfortunately as we chain
        // up required graphs when sending cached results this may cause the later "use-cached" to arrive
        // before cache response and thus causing a failure on client. The "fix" is to queue the use-cache
        // on _pendingSubscriptionPackets but until that is implemented when we invalidate a cache we
        // invalidate all dependent cached type graphs to avoid this scenario.
        for ( final var channel : metaData.getDependentChannels() )
        {
          if ( channel.isTypeGraph() && channel.isCacheable() )
          {
            _cache.remove( new ChannelAddress( channel.getChannelId() ) );
          }
        }
      }
      return cacheRemoved;
    }
    finally
    {
      _cacheLock.writeLock().unlock();
    }
  }

  @Override
  public void deleteAllCacheEntries()
  {
    _cacheLock.writeLock().lock();
    try
    {
      _cache.clear();
    }
    finally
    {
      _cacheLock.writeLock().unlock();
    }
  }

  /**
   * Return a CacheEntry for a specific channel. When this method returns the cache
   * data will have already been loaded. The cache data is loaded using a separate lock for
   * each channel cached.
   */
  @Nullable
  ChannelCacheEntry tryGetCacheEntry( @Nonnull final ChannelAddress address )
  {
    final var metaData = getSchemaMetaData().getChannelMetaData( address );
    assert metaData.isCacheable();
    final var entry = getCacheEntry( address );
    entry.getLock().readLock().lock();
    try
    {
      if ( entry.isInitialized() )
      {
        return entry;
      }
    }
    finally
    {
      entry.getLock().readLock().unlock();
    }
    entry.getLock().writeLock().lock();
    try
    {
      //Make sure check again once we re-acquire the lock
      if ( entry.isInitialized() )
      {
        return entry;
      }
      final var changeSet = new ChangeSet();
      // TODO: At some point we should add support for bulk loads here
      assert !metaData.areBulkLoadsSupported();
      final var result = _context.collectDataForSubscribe( address, null, changeSet );
      if ( result.channelRootDeleted() )
      {
        return null;
      }
      else
      {
        final var cacheKey = result.cacheKey();
        assert null != cacheKey;
        entry.init( cacheKey, changeSet );
        return entry;
      }
    }
    finally
    {
      entry.getLock().writeLock().unlock();
    }
  }

  /**
   * Get the CacheEntry for specified channel. Note that the cache is not necessarily
   * loaded at this stage. This is done to avoid using a global lock while loading data for a
   * particular cache entry.
   */
  ChannelCacheEntry getCacheEntry( @Nonnull final ChannelAddress address )
  {
    _cacheLock.readLock().lock();
    try
    {
      final var entry = _cache.get( address );
      if ( null != entry )
      {
        return entry;
      }
    }
    finally
    {
      _cacheLock.readLock().unlock();
    }
    _cacheLock.writeLock().lock();
    try
    {
      //Try again in case it has since been created
      var entry = _cache.get( address );
      if ( null != entry )
      {
        return entry;
      }
      entry = new ChannelCacheEntry( address );
      _cache.put( address, entry );
      return entry;
    }
    finally
    {
      _cacheLock.writeLock().unlock();
    }
  }

  @Override
  public void unsubscribe( @Nonnull final ReplicantSession session, @Nonnull final ChannelAddress address )
    throws InterruptedException
  {
    if ( session.isOpen() )
    {
      final var lock = session.getLock();
      lock.lockInterruptibly();
      try
      {
        unsubscribe( session, address, EntityMessageCacheUtil.getSessionChanges() );
      }
      finally
      {
        lock.unlock();
      }
    }
  }

  void unsubscribe( @Nonnull final ReplicantSession session,
                    @Nonnull final ChannelAddress address,
                    @Nonnull final ChangeSet changeSet )
  {
    final SubscriptionEntry entry = session.findSubscriptionEntry( address );
    if ( null != entry )
    {
      performUnsubscribe( session, entry, true, false, changeSet );
    }
  }

  @Override
  public void bulkUnsubscribe( @Nonnull final ReplicantSession session,
                               final int channelId,
                               @Nonnull final Collection<Integer> rootIds )
    throws InterruptedException
  {
    if ( session.isOpen() )
    {
      final var lock = session.getLock();
      lock.lockInterruptibly();
      try
      {
        doBulkUnsubscribe( session, channelId, rootIds );
      }
      finally
      {
        lock.unlock();
      }
    }
  }

  private void doBulkUnsubscribe( @Nonnull final ReplicantSession session,
                                  final int channelId,
                                  @Nonnull final Collection<Integer> rootIds )
  {
    final var sessionChanges = EntityMessageCacheUtil.getSessionChanges();
    for ( final var rootId : rootIds )
    {
      unsubscribe( session, new ChannelAddress( channelId, rootId ), sessionChanges );
    }
  }

  @SuppressWarnings( { "SameParameterValue", "WeakerAccess" } )
  protected void performUnsubscribe( @Nonnull final ReplicantSession session,
                                     @Nonnull final SubscriptionEntry entry,
                                     final boolean explicitUnsubscribe,
                                     final boolean delete,
                                     @Nonnull final ChangeSet changeSet )
  {
    if ( explicitUnsubscribe )
    {
      entry.setExplicitlySubscribed( false );
    }
    if ( entry.canUnsubscribe() )
    {
      changeSet.mergeAction( entry.address(),
                             delete ? ChannelAction.Action.DELETE : ChannelAction.Action.REMOVE,
                             null );
      for ( final var downstream : new ArrayList<>( entry.getOutwardSubscriptions() ) )
      {
        delinkDownstreamSubscription( session, entry, downstream, changeSet );
      }
      session.deleteSubscriptionEntry( entry );
    }
  }

  private void delinkDownstreamSubscription( @Nonnull final ReplicantSession session,
                                             @Nonnull final SubscriptionEntry sourceEntry,
                                             @Nonnull final ChannelAddress downstream,
                                             @Nonnull final ChangeSet changeSet )
  {
    final var downstreamEntry = session.findSubscriptionEntry( downstream );
    if ( null != downstreamEntry )
    {
      delinkSubscriptionEntries( sourceEntry, downstreamEntry );
      performUnsubscribe( session, downstreamEntry, false, false, changeSet );
    }
  }

  @SuppressWarnings( "unused" )
  protected void delinkDownstreamSubscriptions( @Nonnull final ReplicantSession session,
                                                @Nonnull final SubscriptionEntry entry,
                                                @Nonnull final EntityMessage message,
                                                @Nonnull final ChangeSet changeSet )
  {
    // Delink any implicit subscriptions that was a result of the deleted entity
    final var links = message.getLinks();
    if ( null != links )
    {
      for ( final var link : links )
      {
        delinkDownstreamSubscription( session, entry, link.target(), changeSet );
      }
    }
  }

  /**
   * Configure the SubscriptionEntries to reflect an auto graph link between the source and target graph.
   */
  void linkSubscriptionEntries( @Nonnull final SubscriptionEntry sourceEntry,
                                @Nonnull final SubscriptionEntry targetEntry )
  {
    sourceEntry.registerOutwardSubscriptions( targetEntry.address() );
    targetEntry.registerInwardSubscriptions( sourceEntry.address() );
  }

  /**
   * Configure the SubscriptionEntries to reflect an auto graph delink between the source and target graph.
   */
  void delinkSubscriptionEntries( @Nonnull final SubscriptionEntry sourceEntry,
                                  @Nonnull final SubscriptionEntry targetEntry )
  {
    sourceEntry.deregisterOutwardSubscriptions( targetEntry.address() );
    targetEntry.deregisterInwardSubscriptions( sourceEntry.address() );
  }

  private void processCachePurge( @Nonnull final EntityMessage message )
  {
    final var schema = getSchemaMetaData();
    final var channelCount = schema.getChannelCount();
    for ( var i = 0; i < channelCount; i++ )
    {
      final var channel = schema.getChannelMetaData( i );
      if ( ChannelMetaData.CacheType.INTERNAL == channel.getCacheType() )
      {
        final var addresses = extractChannelAddressesFromMessage( channel, message );
        if ( null != addresses )
        {
          for ( final var address : addresses )
          {
            deleteCacheEntry( address );
          }
        }
      }
    }
  }

  private void processUpdateMessages( @Nonnull final EntityMessage message,
                                      @Nonnull final ReplicantSession session,
                                      @Nonnull final ChangeSet changeSet )
  {
    final var schema = getSchemaMetaData();
    final var channelCount = schema.getChannelCount();
    for ( var i = 0; i < channelCount; i++ )
    {
      final var channel = schema.getChannelMetaData( i );
      final var addresses = extractChannelAddressesFromMessage( channel, message );
      if ( null != addresses )
      {
        for ( final var address : addresses )
        {
          final var isFiltered = ChannelMetaData.FilterType.NONE != schema.getChannelMetaData( i ).getFilterType();
          processUpdateMessage( address,
                                message,
                                session,
                                changeSet,
                                isFiltered ? m -> _context.filterEntityMessage( session, address, m ) : null );
        }
      }
    }
  }

  @Nullable
  private List<ChannelAddress> extractChannelAddressesFromMessage( @Nonnull final ChannelMetaData channel,
                                                                   @Nonnull final EntityMessage message )
  {
    if ( channel.isInstanceGraph() )
    {
      @SuppressWarnings( "unchecked" )
      final var rootIds = (List<Integer>) message.getRoutingKeys().get( channel.getName() );
      if ( null != rootIds )
      {
        return
          rootIds
            .stream()
            .map( rootId -> new ChannelAddress( channel.getChannelId(), rootId ) )
            .collect( Collectors.toList() );
      }
    }
    else
    {
      if ( message.getRoutingKeys().containsKey( channel.getName() ) )
      {
        return Collections.singletonList( new ChannelAddress( channel.getChannelId() ) );
      }
    }
    return null;
  }

  private void processUpdateMessage( @Nonnull final ChannelAddress address,
                                     @Nonnull final EntityMessage message,
                                     @Nonnull final ReplicantSession session,
                                     @Nonnull final ChangeSet changeSet,
                                     @Nullable final Function<EntityMessage, EntityMessage> filter )
  {
    final var entry = session.findSubscriptionEntry( address );

    // If the session is not subscribed to graph then skip processing
    if ( null != entry )
    {
      final var m = null == filter ? message : filter.apply( message );

      // Process any  messages that are in scope for session
      if ( null != m )
      {
        changeSet.merge( new Change( message, address.channelId(), address.rootId() ) );
      }
    }
  }

  private void processDeleteMessages( @Nonnull final EntityMessage message,
                                      @Nonnull final ReplicantSession session,
                                      @Nonnull final ChangeSet changeSet )
  {
    final var schema = getSchemaMetaData();
    final var instanceChannelCount = schema.getInstanceChannelCount();
    for ( var i = 0; i < instanceChannelCount; i++ )
    {
      final var channel = schema.getInstanceChannelByIndex( i );
      @SuppressWarnings( "unchecked" )
      final var rootIds = (List<Integer>) message.getRoutingKeys().get( channel.getName() );
      if ( null != rootIds )
      {
        for ( final var rootId : rootIds )
        {
          final var address = new ChannelAddress( channel.getChannelId(), rootId );
          final var isFiltered =
            ChannelMetaData.FilterType.NONE != schema.getInstanceChannelByIndex( i ).getFilterType();
          processDeleteMessage( address,
                                message,
                                session,
                                changeSet,
                                isFiltered ? m -> _context.filterEntityMessage( session, address, m ) : null );
        }
      }
    }
  }

  /**
   * Process message handling any logical deletes.
   *
   * @param address   the address of the graph.
   * @param message   the message to process
   * @param session   the session that message is being processed for.
   * @param changeSet for changeSet for session.
   * @param filter    a filter that transforms and or filters entity message before handling. May be null.
   */
  private void processDeleteMessage( @Nonnull final ChannelAddress address,
                                     @Nonnull final EntityMessage message,
                                     @Nonnull final ReplicantSession session,
                                     @Nonnull final ChangeSet changeSet,
                                     @Nullable final Function<EntityMessage, EntityMessage> filter )
  {
    final var entry = session.findSubscriptionEntry( address );

    // If the session is not subscribed to graph then skip processing
    if ( null != entry )
    {
      final var m = null == filter ? message : filter.apply( message );

      // Process any deleted messages that are in scope for session
      if ( null != m && m.isDelete() )
      {
        final var channelMetaData = getSchemaMetaData().getChannelMetaData( address );

        // if the deletion message is for the root of the graph then perform an unsubscribe on the graph
        if ( channelMetaData.isInstanceGraph() && channelMetaData.getInstanceRootEntityTypeId() == m.getTypeId() )
        {
          performUnsubscribe( session, entry, true, true, changeSet );
        }
        // Delink any implicit subscriptions that was a result of the deleted entity
        delinkDownstreamSubscriptions( session, entry, m, changeSet );
      }
    }
  }

  /**
   * @return the lock used to guard access to sessions map.
   */
  @Nonnull
  ReadWriteLock getLock()
  {
    return _lock;
  }
}
