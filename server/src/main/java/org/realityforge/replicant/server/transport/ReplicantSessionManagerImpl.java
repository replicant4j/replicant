package org.realityforge.replicant.server.transport;

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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import org.realityforge.replicant.server.Change;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelAction;
import org.realityforge.replicant.server.ChannelAddress;
import org.realityforge.replicant.server.ChannelLink;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.server.ServerConstants;
import org.realityforge.replicant.server.ee.EntityMessageCacheUtil;
import org.realityforge.replicant.server.json.JsonEncoder;

/**
 * Base class for session managers.
 */
public abstract class ReplicantSessionManagerImpl
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

  @Nonnull
  protected abstract ReplicantMessageBroker getReplicantMessageBroker();

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

  @PreDestroy
  protected void preDestroy()
  {
    removeAllSessions();
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
    final TransactionSynchronizationRegistry registry = getRegistry();
    final Integer requestId = (Integer) registry.getResource( ServerConstants.REQUEST_ID_KEY );
    registry.putResource( ServerConstants.REQUEST_COMPLETE_KEY, "0" );
    registry.putResource( ServerConstants.CACHED_RESULT_HANDLED_KEY, "1" );
    getReplicantMessageBroker().queueChangeMessage( session,
                                                    true,
                                                    requestId,
                                                    null,
                                                    etag,
                                                    Collections.emptyList(),
                                                    changeSet );
  }

  /**
   * @return the transaction synchronization registry.
   */
  @Nonnull
  protected abstract TransactionSynchronizationRegistry getRegistry();

  @Override
  public boolean saveEntityMessages( @Nullable final String sessionId,
                                     @Nullable final Integer requestId,
                                     @Nullable final String response,
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
          if ( null == getRegistry().getResource( ServerConstants.CACHED_RESULT_HANDLED_KEY ) )
          {
            // We skip scenario when we have already sent a cached result
            changeSet.setRequired( true );
          }
        }
        final boolean altersExplicitSubscriptions =
          null != getRegistry().getResource( ServerConstants.SUBSCRIPTION_REQUEST_KEY );
        getReplicantMessageBroker().queueChangeMessage( session,
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
                                 @Nullable final String response,
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
      final Set<ChannelLinkEntry> pending = new HashSet<>();
      final Set<ChannelLinkEntry> subscribed = new HashSet<>();

      while ( true )
      {
        collectChannelLinksToFollow( session, changeSet, pending, subscribed );
        if ( pending.isEmpty() )
        {
          break;
        }
        final ChannelLinkEntry entry =
          pending
            .stream()
            .min( Comparator.comparing( ChannelLinkEntry::getTarget ) )
            .orElse( null );
        final List<ChannelLinkEntry> toSubscribe;
        final ChannelAddress target = entry.getTarget();
        if ( target.hasSubChannelId() )
        {
          toSubscribe =
            pending
              .stream()
              .filter( a -> a.getTarget().getChannelId() == target.getChannelId() &&
                            Objects.equals( a.getFilter(), entry.getFilter() ) )
              .collect( Collectors.toList() );
        }
        else
        {
          toSubscribe = Collections.singletonList( entry );
        }
        final ChannelMetaData channelMetaData = getSystemMetaData().getChannelMetaData( target.getChannelId() );
        if ( channelMetaData.areBulkLoadsSupported() )
        {
          doBulkSubscribe( session,
                           target.getChannelId(),
                           channelMetaData.isTypeGraph() ?
                           null :
                           toSubscribe
                             .stream()
                             .map( ChannelLinkEntry::getTarget )
                             .map( ChannelAddress::getSubChannelId )
                             .collect( Collectors.toList() ),
                           entry.getFilter(),
                           changeSet,
                           false );
        }
        else
        {
          for ( final ChannelLinkEntry e : toSubscribe )
          {
            final SubscriptionEntry targetEntry = session.createSubscriptionEntry( e.getTarget() );
            try
            {
              performSubscribe( session, targetEntry, false, entry.getFilter(), changeSet );
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
        for ( final ChannelLinkEntry e : toSubscribe )
        {
          final SubscriptionEntry sourceEntry = session.getSubscriptionEntry( e.getSource() );
          final SubscriptionEntry targetEntry = session.getSubscriptionEntry( e.getTarget() );
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
    for ( final Change change : changeSet.getChanges() )
    {
      final EntityMessage entityMessage = change.getEntityMessage();
      if ( entityMessage.isUpdate() )
      {
        final Set<ChannelLink> links = entityMessage.getLinks();
        if ( null != links )
        {
          for ( final ChannelLink link : links )
          {
            final boolean alreadyCollected =
              subscribed
                .stream()
                .anyMatch( s -> s.getSource().equals( link.getSourceChannel() ) &&
                                s.getTarget().equals( link.getTargetChannel() ) );
            if ( !alreadyCollected )
            {
              final ChannelLinkEntry entry = createChannelLinkEntryIfRequired( session, link );
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
   * needs to befollowed if the session is subscribed to the source channel and shouldFollowLink returns true.
   * The `shouldFollowLink` method is only invoked if the target graph is filtered otherwise the link
   * is always followed. If a link should be followed the source graph and target graph are linked.
   * <p>
   * This method does not perform the actual subscription and this is deferred to a separate process.
   */
  @Nullable
  ChannelLinkEntry createChannelLinkEntryIfRequired( @Nonnull final ReplicantSession session,
                                                     @Nonnull final ChannelLink link )
  {
    final ChannelAddress source = link.getSourceChannel();
    final SubscriptionEntry sourceEntry = session.findSubscriptionEntry( source );
    if ( null != sourceEntry )
    {
      final ChannelAddress target = link.getTargetChannel();
      final boolean targetHasFilter = getSystemMetaData().getChannelMetaData( target ).hasFilterParameter();
      if ( !targetHasFilter || shouldFollowLink( sourceEntry, target ) )
      {
        final SubscriptionEntry targetEntry = session.findSubscriptionEntry( target );
        if ( null == targetEntry )
        {
          return new ChannelLinkEntry( source,
                                       link.getTargetChannel(),
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

  @Nullable
  protected Object deriveFilterToPropagateFromSourceToTarget( @Nonnull final SubscriptionEntry sourceEntry )
  {
    return sourceEntry.getFilter();
  }

  private void processMessages( @Nonnull final Collection<EntityMessage> messages,
                                @Nonnull final ReplicantSession session,
                                @Nonnull final ChangeSet changeSet )
  {
    for ( final EntityMessage message : messages )
    {
      processDeleteMessages( message, session, changeSet );
    }

    for ( final EntityMessage message : messages )
    {
      processUpdateMessages( message, session, changeSet );
    }
  }

  private void updateSubscription( @Nonnull final ReplicantSession session,
                                   @Nonnull final ChannelAddress address,
                                   @Nullable final Object filter,
                                   @Nonnull final ChangeSet changeSet )
  {
    final ChannelMetaData channel = getSystemMetaData().getChannelMetaData( address );
    assert channel.hasFilterParameter();
    assert channel.getFilterType() == ChannelMetaData.FilterType.DYNAMIC;

    final SubscriptionEntry entry = session.getSubscriptionEntry( address );
    final Object originalFilter = entry.getFilter();
    if ( doFiltersNotMatch( filter, originalFilter ) )
    {
      entry.setFilter( filter );
      collectDataForSubscriptionUpdate( session, address, changeSet, originalFilter, filter );
      changeSet.mergeAction( address, ChannelAction.Action.UPDATE, filter );
      // If collectDataForSubscriptionUpdate indicates that we should unsubscribe from a channel
      // due to filter omitting entity (i.e. action == REMOVE) then we should explicitly unsubscribe
      // from the channel. It is expected the applications that use non-auto graph-links can signal
      // the removal of the target side by adding REMOVE action but it is up to this code to perform
      // the actual remove
      for ( final ChannelAction channelAction : new ArrayList<>( changeSet.getChannelActions() ) )
      {
        if ( ChannelAction.Action.REMOVE == channelAction.getAction() )
        {
          final SubscriptionEntry other = session.findSubscriptionEntry( channelAction.getAddress() );
          // It is unclear when other is ever allowed to be null. If it is null then it probably means
          // that collectDataForSubscriptionUpdate incorrectly added this action.z
          if ( null != other )
          {
            performUnsubscribe( session, other, true, false, changeSet );
          }
        }
      }
      propagateSubscriptionFilterUpdate( session, address, filter, changeSet );
    }
  }

  @SuppressWarnings( "unused" )
  protected void propagateSubscriptionFilterUpdate( @Nonnull final ReplicantSession session,
                                                    @Nonnull final ChannelAddress address,
                                                    @Nullable final Object filter,
                                                    @Nonnull final ChangeSet changeSet )
  {
  }

  @Override
  public void bulkSubscribe( @Nonnull final ReplicantSession session,
                             final int channelId,
                             @Nullable final Collection<Integer> subChannelIds,
                             @Nullable final Object filter )
    throws InterruptedException
  {
    if ( session.isOpen() )
    {
      final ReentrantLock lock = session.getLock();
      lock.lockInterruptibly();
      try
      {
        doBulkSubscribe( session, channelId, subChannelIds, filter, EntityMessageCacheUtil.getSessionChanges(), true );
      }
      finally
      {
        lock.unlock();
      }
    }
  }

  private void doBulkSubscribe( @Nonnull final ReplicantSession session,
                                final int channelId,
                                @Nullable final Collection<Integer> subChannelIds,
                                @Nullable final Object filter,
                                @Nonnull final ChangeSet changeSet,
                                final boolean isExplicitSubscribe )
  {
    final ChannelMetaData channel = getSystemMetaData().getChannelMetaData( channelId );
    assert ( channel.isInstanceGraph() && null != subChannelIds ) || ( channel.isTypeGraph() && null == subChannelIds );

    subscribeToRequiredTypeChannels( session, channel );

    final List<ChannelAddress> newChannels = new ArrayList<>();
    //OriginalFilter => Channels
    final Map<Object, List<ChannelAddress>> channelsToUpdate = new HashMap<>();

    if ( null == subChannelIds )
    {
      final ChannelAddress address = new ChannelAddress( channelId );
      final SubscriptionEntry entry = session.findSubscriptionEntry( address );
      if ( null == entry )
      {
        newChannels.add( address );
      }
      else
      {
        final Object existingFilter = entry.getFilter();
        if ( doFiltersNotMatch( filter, existingFilter ) )
        {
          channelsToUpdate.computeIfAbsent( existingFilter, k -> new ArrayList<>() ).add( address );
        }
      }
    }
    else
    {
      for ( final Integer root : subChannelIds )
      {
        final ChannelAddress address = new ChannelAddress( channelId, root );
        final SubscriptionEntry entry = session.findSubscriptionEntry( address );
        if ( null == entry )
        {
          newChannels.add( address );
        }
        else
        {
          final Object existingFilter = entry.getFilter();
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
        bulkCollectDataForSubscribe( session, newChannels, filter, changeSet, isExplicitSubscribe );
      }
      else
      {
        t = subscribeToAddresses( session, newChannels, filter, changeSet );
      }
    }
    if ( !channelsToUpdate.isEmpty() )
    {
      for ( final Map.Entry<Object, List<ChannelAddress>> update : channelsToUpdate.entrySet() )
      {
        final Object originalFilter = update.getKey();
        final List<ChannelAddress> addresses = update.getValue();

        if ( channel.areBulkLoadsSupported() )
        {
          if ( ChannelMetaData.FilterType.DYNAMIC == channel.getFilterType() )
          {
            bulkCollectDataForSubscriptionUpdate( session,
                                                  addresses,
                                                  originalFilter,
                                                  filter,
                                                  changeSet,
                                                  isExplicitSubscribe );
          }
          else
          {
            final String message =
              "Attempted to update filter on channel " + channel.getName() + " to " + filter + " but the " +
              "channel that has a static filter. Unsubscribe and resubscribe to channel.";
            throw new AttemptedToUpdateStaticFilterException( message );
          }
        }
        else
        {
          final Throwable error = subscribeToAddresses( session, addresses, filter, changeSet );
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
    for ( final ChannelAddress address : addresses )
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
    final ChannelMetaData channelMetaData = getSystemMetaData().getChannelMetaData( address );

    if ( session.isSubscriptionEntryPresent( address ) )
    {
      final SubscriptionEntry entry = session.getSubscriptionEntry( address );
      if ( explicitlySubscribe )
      {
        entry.setExplicitlySubscribed( true );
      }
      if ( ChannelMetaData.FilterType.DYNAMIC == channelMetaData.getFilterType() )
      {
        if ( channelMetaData.areBulkLoadsSupported() )
        {
          doBulkSubscribe( session,
                           address.getChannelId(),
                           channelMetaData.isTypeGraph() ?
                           null :
                           Collections.singletonList( address.getSubChannelId() ),
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
        final Object existingFilter = entry.getFilter();
        if ( doFiltersNotMatch( filter, existingFilter ) )
        {
          final String message =
            "Attempted to update filter on channel " + entry.getAddress() + " from " + existingFilter +
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
                         address.getChannelId(),
                         channelMetaData.isTypeGraph() ?
                         null :
                         Collections.singletonList( address.getSubChannelId() ),
                         filter,
                         changeSet,
                         true );
      }
      else
      {
        final SubscriptionEntry entry = session.createSubscriptionEntry( address );
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
    final ChannelAddress address = entry.getAddress();
    final ChannelMetaData channelMetaData = getSystemMetaData().getChannelMetaData( address );

    subscribeToRequiredTypeChannels( session, channelMetaData );

    if ( channelMetaData.isCacheable() )
    {
      final ChannelCacheEntry cacheEntry = tryGetCacheEntry( address );
      if ( null != cacheEntry )
      {
        if ( explicitSubscribe )
        {
          entry.setExplicitlySubscribed( true );
        }

        final String eTag = cacheEntry.getCacheKey();
        if ( eTag.equals( session.getETag( address ) ) )
        {
          if ( session.getWebSocketSession().isOpen() )
          {
            final Integer requestId = (Integer) getRegistry().getResource( ServerConstants.REQUEST_ID_KEY );
            WebSocketUtil.sendText( session.getWebSocketSession(),
                                    JsonEncoder.encodeUseCacheMessage( address, eTag, requestId ) );
            changeSet.setRequired( false );
            // We need to mark this as handled otherwise the wrapper will attempt to send
            // another ok message with same requestId
            getRegistry().putResource( ServerConstants.CACHED_RESULT_HANDLED_KEY, "1" );
          }
        }
        else
        {
          session.setETag( address, null );
          final ChangeSet cacheChangeSet = new ChangeSet();
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
        assert address.hasSubChannelId();
        final ChangeSet cacheChangeSet = new ChangeSet();
        cacheChangeSet.mergeAction( address, ChannelAction.Action.DELETE, null );
        queueCachedChangeSet( session, null, cacheChangeSet );
        changeSet.setRequired( false );
      }
    }
    else
    {
      if ( channelMetaData.areBulkLoadsSupported() )
      {
        final ChannelAddress channelAddress =
          new ChannelAddress( address.getChannelId(),
                              channelMetaData.isTypeGraph() ? null : address.getSubChannelId() );
        bulkCollectDataForSubscribe( session,
                                     Collections.singletonList( channelAddress ),
                                     filter,
                                     changeSet,
                                     explicitSubscribe );
      }
      else
      {
        final SubscribeResult result = collectDataForSubscribe( address, changeSet, filter );
        if ( result.isChannelRootDeleted() )
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
    final ChannelMetaData[] requiredTypeChannels = channelMetaData.getRequiredTypeChannels();
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
    for ( final ChannelMetaData requiredTypeChannel : requiredTypeChannels )
    {
      assert requiredTypeChannel.isTypeGraph();
      // At the moment we propagate no filters ... which is fine
      assert ChannelMetaData.FilterType.NONE == requiredTypeChannel.getFilterType();
      final ChannelAddress address = new ChannelAddress( requiredTypeChannel.getChannelId() );

      // This check is sufficient as it is not an explicit subscribe and there are no filters that can change
      if ( !session.isSubscriptionEntryPresent( address ) )
      {
        final TransactionSynchronizationRegistry registry = getRegistry();
        final Integer requestId = (Integer) registry.getResource( ServerConstants.REQUEST_ID_KEY );
        final String requestComplete = (String) registry.getResource( ServerConstants.REQUEST_COMPLETE_KEY );
        final String requestResponse = (String) registry.getResource( ServerConstants.REQUEST_RESPONSE_KEY );
        final String requestCachedResultHandled =
          (String) registry.getResource( ServerConstants.CACHED_RESULT_HANDLED_KEY );

        registry.putResource( ServerConstants.REQUEST_ID_KEY, null );
        registry.putResource( ServerConstants.REQUEST_COMPLETE_KEY, null );
        registry.putResource( ServerConstants.REQUEST_RESPONSE_KEY, null );
        registry.putResource( ServerConstants.CACHED_RESULT_HANDLED_KEY, null );

        final ChangeSet changeSet = new ChangeSet();
        subscribe( session, address, false, null, changeSet );
        if ( changeSet.hasContent() )
        {
          // In this scenario we have a non-cached changeset, so we send it along
          getReplicantMessageBroker().
            queueChangeMessage( session, true, null, null, null, Collections.emptyList(), changeSet );
        }

        registry.putResource( ServerConstants.REQUEST_ID_KEY, requestId );
        registry.putResource( ServerConstants.REQUEST_COMPLETE_KEY, requestComplete );
        registry.putResource( ServerConstants.REQUEST_RESPONSE_KEY, requestResponse );
        registry.putResource( ServerConstants.CACHED_RESULT_HANDLED_KEY, requestCachedResultHandled );
      }
    }
  }

  @SuppressWarnings( "WeakerAccess" )
  protected boolean deleteCacheEntry( @Nonnull final ChannelAddress address )
  {
    _cacheLock.writeLock().lock();
    try
    {
      final ChannelMetaData metaData = getSystemMetaData().getChannelMetaData( address );
      final boolean cacheRemoved = null != _cache.remove( address );
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
        for ( final ChannelMetaData channel : metaData.getDependentChannels() )
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
    final ChannelMetaData metaData = getSystemMetaData().getChannelMetaData( address );
    assert metaData.isCacheable();
    final ChannelCacheEntry entry = getCacheEntry( address );
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
      final ChangeSet changeSet = new ChangeSet();
      // TODO: At some point we should add support for bulk loads here
      assert !metaData.areBulkLoadsSupported();
      final SubscribeResult result = collectDataForSubscribe( address, changeSet, null );
      if ( result.isChannelRootDeleted() )
      {
        return null;
      }
      else
      {
        final String cacheKey = result.getCacheKey();
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
      final ChannelCacheEntry entry = _cache.get( address );
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
      ChannelCacheEntry entry = _cache.get( address );
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

  /**
   * @return the cacheKey if any. The return value is ignored for non-cacheable channels.
   */
  @Nonnull
  protected SubscribeResult collectDataForSubscribe( @Nonnull final ChannelAddress address,
                                                     @Nonnull final ChangeSet changeSet,
                                                     @Nullable final Object filter )
  {
    throw new IllegalStateException( "collectDataForSubscribe called for unsupported channel " + address );
  }

  /**
   * This method is called in an attempt to use a more efficient method for bulk loading instance graphs.
   * Subclasses may return false form this method, in which case collectDataForSubscribe will be called
   * for each independent channel.
   */
  @SuppressWarnings( "unused" )
  protected void bulkCollectDataForSubscribe( @Nonnull final ReplicantSession session,
                                              @Nonnull final List<ChannelAddress> addresses,
                                              @Nullable final Object filter,
                                              @Nonnull final ChangeSet changeSet,
                                              final boolean isExplicitSubscribe )
  {
    final ChannelAddress address = addresses.get( 0 );
    throw new IllegalStateException( "collectDataForSubscriptionUpdate called for unsupported channel " + address );
  }

  protected void collectDataForSubscriptionUpdate( @Nonnull final ReplicantSession session,
                                                   @Nonnull final ChannelAddress address,
                                                   @Nonnull final ChangeSet changeSet,
                                                   @Nullable final Object originalFilter,
                                                   @Nullable final Object filter )
  {
    throw new IllegalStateException( "collectDataForSubscriptionUpdate called for unsupported channel " + address );
  }

  /**
   * Hook method by which efficient bulk collection of data for subscription updates can occur.
   * It is expected that the hook does everything including updating SubscriptionEntry with new
   * filter, adding graph links etc.
   */
  protected void bulkCollectDataForSubscriptionUpdate( @Nonnull final ReplicantSession session,
                                                       @Nonnull final List<ChannelAddress> addresses,
                                                       @Nullable final Object originalFilter,
                                                       @Nullable final Object filter,
                                                       @Nonnull final ChangeSet changeSet,
                                                       final boolean isExplicitSubscribe )
  {
    final ChannelAddress address = addresses.get( 0 );
    throw new IllegalStateException( "bulkCollectDataForSubscriptionUpdate called for unknown channel " + address );
  }

  @Override
  public void unsubscribe( @Nonnull final ReplicantSession session, @Nonnull final ChannelAddress address )
    throws InterruptedException
  {
    if ( session.isOpen() )
    {
      final ReentrantLock lock = session.getLock();
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
    performUnsubscribe( session, address, changeSet );
  }

  private void performUnsubscribe( @Nonnull final ReplicantSession session,
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
                               @Nonnull final Collection<Integer> subChannelIds )
    throws InterruptedException
  {
    if ( session.isOpen() )
    {
      final ReentrantLock lock = session.getLock();
      lock.lockInterruptibly();
      try
      {
        doBulkUnsubscribe( session, channelId, subChannelIds );
      }
      finally
      {
        lock.unlock();
      }
    }
  }

  private void doBulkUnsubscribe( @Nonnull final ReplicantSession session,
                                  final int channelId,
                                  @Nonnull final Collection<Integer> subChannelIds )
  {
    final ChangeSet sessionChanges = EntityMessageCacheUtil.getSessionChanges();
    for ( final int subChannelId : subChannelIds )
    {
      performUnsubscribe( session, new ChannelAddress( channelId, subChannelId ), sessionChanges );
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
      changeSet.mergeAction( entry.getAddress(),
                             delete ? ChannelAction.Action.DELETE : ChannelAction.Action.REMOVE,
                             null );
      for ( final ChannelAddress downstream : new ArrayList<>( entry.getOutwardSubscriptions() ) )
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
    final SubscriptionEntry downstreamEntry = session.findSubscriptionEntry( downstream );
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
    final Set<ChannelLink> links = message.getLinks();
    if ( null != links )
    {
      for ( final ChannelLink link : links )
      {
        delinkDownstreamSubscription( session, entry, link.getTargetChannel(), changeSet );
      }
    }
  }

  /**
   * Configure the SubscriptionEntries to reflect an auto graph link between the source and target graph.
   */
  void linkSubscriptionEntries( @Nonnull final SubscriptionEntry sourceEntry,
                                @Nonnull final SubscriptionEntry targetEntry )
  {
    sourceEntry.registerOutwardSubscriptions( targetEntry.getAddress() );
    targetEntry.registerInwardSubscriptions( sourceEntry.getAddress() );
  }

  /**
   * Configure the SubscriptionEntries to reflect an auto graph delink between the source and target graph.
   */
  void delinkSubscriptionEntries( @Nonnull final SubscriptionEntry sourceEntry,
                                  @Nonnull final SubscriptionEntry targetEntry )
  {
    sourceEntry.deregisterOutwardSubscriptions( targetEntry.getAddress() );
    targetEntry.deregisterInwardSubscriptions( sourceEntry.getAddress() );
  }

  protected boolean shouldFollowLink( @Nonnull final SubscriptionEntry sourceEntry,
                                      @Nonnull final ChannelAddress target )
  {
    throw new IllegalStateException( "shouldFollowLink called for link between channel " +
                                     sourceEntry.getAddress() + " and " + target +
                                     " and the target has no filter or the link is unknown." );
  }

  @SuppressWarnings( "unused" )
  @Nullable
  protected EntityMessage filterEntityMessage( @Nonnull final ReplicantSession session,
                                               @Nonnull final ChannelAddress address,
                                               @Nonnull final EntityMessage message )
  {
    throw new IllegalStateException( "filterEntityMessage called for unfiltered channel " + address );
  }

  private void processCachePurge( @Nonnull final EntityMessage message )
  {
    final SystemMetaData schema = getSystemMetaData();
    final int channelCount = schema.getChannelCount();
    for ( int i = 0; i < channelCount; i++ )
    {
      final ChannelMetaData channel = schema.getChannelMetaData( i );
      if ( ChannelMetaData.CacheType.INTERNAL == channel.getCacheType() )
      {
        final List<ChannelAddress> addresses = extractChannelAddressesFromMessage( channel, message );
        if ( null != addresses )
        {
          for ( final ChannelAddress address : addresses )
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
    final SystemMetaData schema = getSystemMetaData();
    final int channelCount = schema.getChannelCount();
    for ( int i = 0; i < channelCount; i++ )
    {
      final ChannelMetaData channel = schema.getChannelMetaData( i );
      final List<ChannelAddress> addresses = extractChannelAddressesFromMessage( channel, message );
      if ( null != addresses )
      {
        for ( final ChannelAddress address : addresses )
        {
          final boolean isFiltered = ChannelMetaData.FilterType.NONE != schema.getChannelMetaData( i ).getFilterType();
          processUpdateMessage( address,
                                message,
                                session,
                                changeSet,
                                isFiltered ? m -> filterEntityMessage( session, address, m ) : null );
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
      final List<Integer> subChannelIds = (List<Integer>) message.getRoutingKeys().get( channel.getName() );
      if ( null != subChannelIds )
      {
        return subChannelIds
          .stream()
          .map( subChannelId -> new ChannelAddress( channel.getChannelId(), subChannelId ) )
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
    final SubscriptionEntry entry = session.findSubscriptionEntry( address );

    // If the session is not subscribed to graph then skip processing
    if ( null != entry )
    {
      final EntityMessage m = null == filter ? message : filter.apply( message );

      // Process any  messages that are in scope for session
      if ( null != m )
      {
        changeSet.merge( new Change( message, address.getChannelId(), address.getSubChannelId() ) );
      }
    }
  }

  private void processDeleteMessages( @Nonnull final EntityMessage message,
                                      @Nonnull final ReplicantSession session,
                                      @Nonnull final ChangeSet changeSet )
  {
    final SystemMetaData schema = getSystemMetaData();
    final int instanceChannelCount = schema.getInstanceChannelCount();
    for ( int i = 0; i < instanceChannelCount; i++ )
    {
      final ChannelMetaData channel = schema.getInstanceChannelByIndex( i );
      @SuppressWarnings( "unchecked" )
      final List<Integer> subChannelIds = (List<Integer>) message.getRoutingKeys().get( channel.getName() );
      if ( null != subChannelIds )
      {
        for ( final Integer subChannelId : subChannelIds )
        {
          final ChannelAddress address = new ChannelAddress( channel.getChannelId(), subChannelId );
          final boolean isFiltered =
            ChannelMetaData.FilterType.NONE != schema.getInstanceChannelByIndex( i ).getFilterType();
          processDeleteMessage( address,
                                message,
                                session,
                                changeSet,
                                isFiltered ? m -> filterEntityMessage( session, address, m ) : null );
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
    final SubscriptionEntry entry = session.findSubscriptionEntry( address );

    // If the session is not subscribed to graph then skip processing
    if ( null != entry )
    {
      final EntityMessage m = null == filter ? message : filter.apply( message );

      // Process any deleted messages that are in scope for session
      if ( null != m && m.isDelete() )
      {
        final ChannelMetaData channelMetaData = getSystemMetaData().getChannelMetaData( address );

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
