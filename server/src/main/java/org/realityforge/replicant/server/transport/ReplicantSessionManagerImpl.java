package org.realityforge.replicant.server.transport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.Change;
import org.realityforge.replicant.server.ChangeAccumulator;
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
  private final ReadWriteLock _lock = new ReentrantReadWriteLock();
  private final Map<String, ReplicantSession> _sessions = new HashMap<>();
  private final Map<String, ReplicantSession> _roSessions = Collections.unmodifiableMap( _sessions );
  private final ReadWriteLock _cacheLock = new ReentrantReadWriteLock();
  private final HashMap<ChannelAddress, ChannelCacheEntry> _cache = new HashMap<>();

  @Override
  public boolean invalidateSession( @Nonnull final ReplicantSession session )
  {
    _lock.writeLock().lock();
    try
    {
      return null != _sessions.remove( session.getId() );
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
    final ReplicantSession session;
    try
    {
      session = _sessions.get( sessionId );
    }
    finally
    {
      _lock.readLock().unlock();
    }
    if ( null != session )
    {
      session.updateAccessTime();
    }
    return session;
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

  @Override
  @Nonnull
  public ReplicantSession createSession()
  {
    final ReplicantSession session = newReplicantSession();
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

  /**
   * Return an unmodifiable map containing the set of sessions.
   * The user should also acquire a read lock via {@link #getLock()} prior to invoking
   * this method ensure it is not modified while being inspected.
   *
   * @return an unmodifiable map containing the set of sessions.
   */
  @Nonnull
  Map<String, ReplicantSession> getSessions()
  {
    return _roSessions;
  }

  /**
   * @return the lock used to guard access to sessions map.
   */
  @Nonnull
  ReadWriteLock getLock()
  {
    return _lock;
  }

  @PreDestroy
  protected void preDestroy()
  {
    removeAllSessions();
  }

  /**
   * Remove all sessions and force them to reconnect.
   */
  @SuppressWarnings( "WeakerAccess" )
  public void removeAllSessions()
  {
    _lock.writeLock().lock();
    try
    {
      _sessions.clear();
    }
    finally
    {
      _lock.writeLock().unlock();
    }
  }

  /**
   * Remove any sessions that have been idle for 5 minutes.
   */
  @SuppressWarnings( "unused" )
  public void removeIdleSessions()
  {
    removeIdleSessions( 1000 * 60 * 5 );
  }

  /**
   * Remove sessions that have not been accessed for the specified idle time.
   *
   * @param maxIdleTime the max idle time for a session.
   */
  @SuppressWarnings( "WeakerAccess" )
  protected void removeIdleSessions( final long maxIdleTime )
  {
    final long now = System.currentTimeMillis();
    _lock.writeLock().lock();
    try
    {
      final Iterator<Map.Entry<String, ReplicantSession>> iterator = _sessions.entrySet().iterator();
      while ( iterator.hasNext() )
      {
        final ReplicantSession session = iterator.next().getValue();
        if ( now - session.getLastAccessedAt() > maxIdleTime )
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

  @Nullable
  protected String pollJsonData( @Nonnull final ReplicantSession session, final int lastSequenceAcked )
  {
    final Packet packet = pollPacket( session, lastSequenceAcked );
    if ( null != packet )
    {
      return JsonEncoder.
        encodeChangeSet( packet.getSequence(), packet.getRequestId(), packet.getETag(), packet.getChangeSet() );
    }
    else
    {
      return null;
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
  void sendPacket( @Nonnull final ReplicantSession session,
                   @Nullable final String etag,
                   @Nonnull final ChangeSet changeSet )
  {
    final Integer requestId = (Integer) getRegistry().getResource( ServerConstants.REQUEST_ID_KEY );
    getRegistry().putResource( ServerConstants.REQUEST_COMPLETE_KEY, "0" );
    session.sendPacket( requestId, etag, changeSet );
  }

  /**
   * @return the transaction synchronization registry.
   */
  @Nonnull
  protected abstract TransactionSynchronizationRegistry getRegistry();

  @Nonnull
  protected ReplicantSession newReplicantSession()
  {
    return new ReplicantSession( null, UUID.randomUUID().toString() );
  }

  /**
   * Return the next packet to send to the client.
   * The packet is only returned if the client has acked the previous message.
   *
   * @param session           the session.
   * @param lastSequenceAcked the sequence that the client last ack'ed.
   * @return the packet or null if no packet is ready.
   */
  @Nullable
  protected Packet pollPacket( @Nonnull final ReplicantSession session, final int lastSequenceAcked )
  {
    final PacketQueue queue = session.getQueue();
    queue.ack( lastSequenceAcked );
    return queue.nextPacketToProcess();
  }

  /**
   * Return session associated with specified ID.
   *
   * @throws RuntimeException if no such session is available.
   */
  @Nonnull
  protected ReplicantSession ensureSession( @Nonnull final String sessionId )
  {
    final ReplicantSession session = getSession( sessionId );
    if ( null == session )
    {
      throw newBadSessionException( sessionId );
    }
    return session;
  }

  @Nonnull
  protected abstract RuntimeException newBadSessionException( @Nonnull String sessionId );

  @Override
  public boolean saveEntityMessages( @Nullable final String sessionId,
                                     @Nullable final Integer requestId,
                                     @Nonnull final Collection<EntityMessage> messages,
                                     @Nullable final ChangeSet sessionChanges )
  {
    //TODO: Rewrite this so that we add clients to indexes rather than searching through everyone for each change!
    getLock().readLock().lock();
    final ReplicantSession initiatorSession = null != sessionId ? getSession( sessionId ) : null;
    final ChangeAccumulator accumulator = new ChangeAccumulator();
    try
    {
      final Collection<ReplicantSession> sessions = getSessions().values();
      for ( final EntityMessage message : messages )
      {
        processDeleteMessages( message, sessions, accumulator );
      }

      for ( final EntityMessage message : messages )
      {
        processUpdateMessages( message, sessions, accumulator );
      }
      if ( null != initiatorSession && null != sessionChanges )
      {
        accumulator.getChangeSet( initiatorSession ).setRequired( sessionChanges.isRequired() );
        accumulator.addChanges( initiatorSession, sessionChanges.getChanges() );
        accumulator.addActions( initiatorSession, sessionChanges.getChannelActions() );
      }
      for ( final ReplicantSession session : getSessions().values() )
      {
        expandLinks( session, accumulator.getChangeSet( session ) );
      }
    }
    finally
    {
      getLock().readLock().unlock();
    }

    return accumulator.complete( initiatorSession, requestId );
  }

  protected abstract void processUpdateMessages( @Nonnull EntityMessage message,
                                                 @Nonnull Collection<ReplicantSession> sessions,
                                                 @Nonnull ChangeAccumulator accumulator );

  protected abstract void processDeleteMessages( @Nonnull EntityMessage message,
                                                 @Nonnull Collection<ReplicantSession> sessions,
                                                 @Nonnull ChangeAccumulator accumulator );

  private void updateSubscription( @Nonnull final ReplicantSession session,
                                   @Nonnull final ChannelAddress address,
                                   @Nullable final Object filter,
                                   @Nonnull final ChangeSet changeSet )
  {
    assert getSystemMetaData().getChannelMetaData( address ).getFilterType() == ChannelMetaData.FilterType.DYNAMIC;

    final SubscriptionEntry entry = session.getSubscriptionEntry( address );
    final Object originalFilter = entry.getFilter();
    if ( !doFiltersMatch( filter, originalFilter ) )
    {
      performUpdateSubscription( entry, originalFilter, filter, changeSet );
    }
  }

  @Override
  public void bulkSubscribe( @Nonnull final ReplicantSession session,
                             final int channelId,
                             @Nonnull final Collection<Integer> subChannelIds,
                             @Nullable final Object filter )
  {
    assert getSystemMetaData().getChannelMetaData( channelId ).isInstanceGraph();

    final ArrayList<ChannelAddress> newChannels = new ArrayList<>();
    //OriginalFilter => Channels
    final HashMap<Object, ArrayList<ChannelAddress>> channelsToUpdate = new HashMap<>();

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
        final ArrayList<ChannelAddress> addresses =
          channelsToUpdate.computeIfAbsent( entry.getFilter(), k -> new ArrayList<>() );
        addresses.add( address );
      }
    }
    Throwable t = null;

    if ( !newChannels.isEmpty() )
    {
      if ( !bulkCollectDataForSubscribe( session, newChannels, filter ) )
      {
        t = subscribeToAddresses( session, newChannels, filter );
      }
    }
    if ( !channelsToUpdate.isEmpty() )
    {
      for ( final Map.Entry<Object, ArrayList<ChannelAddress>> update : channelsToUpdate.entrySet() )
      {
        final Object originalFilter = update.getKey();
        final ArrayList<ChannelAddress> addresses = update.getValue();
        boolean bulkLoaded = false;

        if ( addresses.size() > 1 )
        {
          bulkLoaded = bulkCollectDataForSubscriptionUpdate( session, addresses, originalFilter, filter );
        }
        if ( !bulkLoaded )
        {
          final Throwable error = subscribeToAddresses( session, addresses, filter );
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
    else if ( t != null )
    {
      throw (RuntimeException) t;
    }
  }

  @Nullable
  private Throwable subscribeToAddresses( @Nonnull final ReplicantSession session,
                                          @Nonnull final ArrayList<ChannelAddress> addresses,
                                          @Nullable final Object filter )
  {
    Throwable t = null;
    for ( final ChannelAddress address : addresses )
    {
      try
      {
        subscribe( session, address, filter );
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
  {
    subscribe( session, address, true, filter, EntityMessageCacheUtil.getSessionChanges() );
  }

  void subscribe( @Nonnull final ReplicantSession session,
                  @Nonnull final ChannelAddress address,
                  final boolean explicitlySubscribe,
                  @Nullable final Object filter,
                  @Nonnull final ChangeSet changeSet )
  {
    if ( session.isSubscriptionEntryPresent( address ) )
    {
      final SubscriptionEntry entry = session.getSubscriptionEntry( address );
      if ( explicitlySubscribe )
      {
        entry.setExplicitlySubscribed( true );
      }
      final ChannelMetaData channelMetaData = getSystemMetaData().getChannelMetaData( address );
      if ( ChannelMetaData.FilterType.DYNAMIC == channelMetaData.getFilterType() )
      {
        updateSubscription( session, address, filter, changeSet );
      }
      else if ( ChannelMetaData.FilterType.STATIC == channelMetaData.getFilterType() )
      {
        final Object existingFilter = entry.getFilter();
        if ( !doFiltersMatch( filter, existingFilter ) )
        {
          final String message =
            "Attempted to update filter on channel " + entry.getDescriptor() + " from " + existingFilter +
            " to " + filter + " for channel that has a static filter. Unsubscribe and resubscribe to channel.";
          throw new AttemptedToUpdateStaticFilterException( message );
        }
      }
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

  private boolean doFiltersMatch( final Object filter1, final Object filter2 )
  {
    return ( null == filter2 && null == filter1 ) ||
           ( null != filter2 && filter2.equals( filter1 ) );
  }

  void performSubscribe( @Nonnull final ReplicantSession session,
                         @Nonnull final SubscriptionEntry entry,
                         final boolean explicitSubscribe,
                         @Nullable final Object filter,
                         @Nonnull final ChangeSet changeSet )
  {
    entry.setFilter( filter );
    final ChannelAddress address = entry.getDescriptor();
    final ChannelMetaData channelMetaData = getSystemMetaData().getChannelMetaData( address );
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
        if ( false && eTag.equals( session.getETag( address ) ) )
        {
          //TODO: Re-add support for cache hits
        }
        else
        {
          session.setETag( address, null );
          final ChangeSet cacheChangeSet = new ChangeSet();
          cacheChangeSet.merge( cacheEntry.getChangeSet(), true );
          cacheChangeSet.mergeAction( address, ChannelAction.Action.ADD, filter );
          sendPacket( session, eTag, cacheChangeSet );
          changeSet.setRequired( false );
        }
        return;
      }
      else
      {
        // If we get here then we have requested a cacheable instance channel
        // where the root has been removed
        assert address.hasSubChannelId();
        final ChangeSet cacheChangeSet = new ChangeSet();
        cacheChangeSet.mergeAction( address, ChannelAction.Action.DELETE, null );
        sendPacket( session, null, cacheChangeSet );
        changeSet.setRequired( false );
        return;
      }
    }

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

  @SuppressWarnings( "WeakerAccess" )
  protected boolean deleteCacheEntry( @Nonnull final ChannelAddress address )
  {
    _cacheLock.writeLock().lock();
    try
    {
      return null != _cache.remove( address );
    }
    finally
    {
      _cacheLock.writeLock().unlock();
    }
  }

  void deleteAllCacheEntries()
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
    assert getSystemMetaData().getChannelMetaData( address ).isCacheable();
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
      //Make sure check again once we re-aquire the lock
      if ( entry.isInitialized() )
      {
        return entry;
      }
      final ChangeSet changeSet = new ChangeSet();
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

  void performUpdateSubscription( @Nonnull final SubscriptionEntry entry,
                                  @Nullable final Object originalFilter,
                                  @Nullable final Object filter,
                                  @Nonnull final ChangeSet changeSet )
  {
    assert getSystemMetaData().getChannelMetaData( entry.getDescriptor() ).hasFilterParameter();
    entry.setFilter( filter );
    final ChannelAddress address = entry.getDescriptor();
    collectDataForSubscriptionUpdate( entry.getDescriptor(), changeSet, originalFilter, filter );
    changeSet.mergeAction( address, ChannelAction.Action.UPDATE, filter );
  }

  /**
   * @return the cacheKey if any. The return value is ignored for non-cacheable channels.
   */
  @Nonnull
  protected abstract SubscribeResult collectDataForSubscribe( @Nonnull final ChannelAddress address,
                                                              @Nonnull final ChangeSet changeSet,
                                                              @Nullable final Object filter );

  /**
   * This method is called in an attempt to use a more efficient method for bulk loading instance graphs.
   * Subclasses may return false form this method, in which case collectDataForSubscribe will be called
   * for each independent channel.
   *
   * @return true if method has actually bulk loaded all data, false otherwise.
   */
  protected abstract boolean bulkCollectDataForSubscribe( @Nonnull ReplicantSession session,
                                                          @Nonnull ArrayList<ChannelAddress> addresses,
                                                          @Nullable Object filter );

  protected abstract void collectDataForSubscriptionUpdate( @Nonnull ChannelAddress address,
                                                            @Nonnull ChangeSet changeSet,
                                                            @Nullable Object originalFilter,
                                                            @Nullable Object filter );

  /**
   * Hook method by which efficient bulk collection of data for subscription updates can occur.
   * It is expected that the hook does everything including updating SubscriptionEntry with new
   * filter, adding graph links etc.
   */
  protected abstract boolean bulkCollectDataForSubscriptionUpdate( @Nonnull ReplicantSession session,
                                                                   @Nonnull ArrayList<ChannelAddress> addresses,
                                                                   @Nullable Object originalFilter,
                                                                   @Nullable Object filter );

  @Override
  public void unsubscribe( @Nonnull final ReplicantSession session, @Nonnull final ChannelAddress address )
  {
    unsubscribe( session, address, EntityMessageCacheUtil.getSessionChanges() );
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
  {
    final ChangeSet sessionChanges = EntityMessageCacheUtil.getSessionChanges();
    for ( final Integer subChannelId : subChannelIds )
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
      changeSet.mergeAction( entry.getDescriptor(),
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
                                                @Nonnull final ChangeAccumulator accumulator )
  {
    // Delink any implicit subscriptions that was a result of the deleted entity
    final Set<ChannelLink> links = message.getLinks();
    if ( null != links )
    {
      for ( final ChannelLink link : links )
      {
        delinkDownstreamSubscription( session, entry, link.getTargetChannel(), accumulator.getChangeSet( session ) );
      }
    }
  }

  /**
   * Configure the SubscriptionEntries to reflect an auto graph link between the source and target graph.
   */
  void linkSubscriptionEntries( @Nonnull final SubscriptionEntry sourceEntry,
                                @Nonnull final SubscriptionEntry targetEntry )
  {
    sourceEntry.registerOutwardSubscriptions( targetEntry.getDescriptor() );
    targetEntry.registerInwardSubscriptions( sourceEntry.getDescriptor() );
  }

  /**
   * Configure the SubscriptionEntries to reflect an auto graph delink between the source and target graph.
   */
  void delinkSubscriptionEntries( @Nonnull final SubscriptionEntry sourceEntry,
                                  @Nonnull final SubscriptionEntry targetEntry )
  {
    sourceEntry.deregisterOutwardSubscriptions( targetEntry.getDescriptor() );
    targetEntry.deregisterInwardSubscriptions( sourceEntry.getDescriptor() );
  }

  @SuppressWarnings( { "PMD.WhileLoopsMustUseBraces", "StatementWithEmptyBody" } )
  void expandLinks( @Nonnull final ReplicantSession session, @Nonnull final ChangeSet changeSet )
  {
    while ( expandLink( session, changeSet ) )
    {
      //Ignore.
    }
  }

  /**
   * Iterate over all the ChannelLinks in change set attempting to "expand" them if they have to be
   * subscribed. The expand involves subscribing to the target graph. As soon as one is expanded
   * terminate search and return true, otherwise return false.
   */
  boolean expandLink( @Nonnull final ReplicantSession session, @Nonnull final ChangeSet changeSet )
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
            if ( expandLinkIfRequired( session, link, changeSet ) )
            {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  /**
   * Determine if the specified ChannelLink needs to be expanded and do so. A ChannelLink needs to be
   * expanded if the session is subscribed to the source channel and shouldFollowLink returns true.
   * The `shouldFollowLink` method is only invoked if the target graph is filtered otherwise the link
   * is always followed. If a link should be followed the source graph and target graph are linked.
   *
   * If a subscription occurs then this method will immediately return false. This occurs as the changes
   * in the ChangeSet may have been modified as a result of the subscription and thus scanning of changeSet
   * needs to start again.
   */
  boolean expandLinkIfRequired( @Nonnull final ReplicantSession session,
                                @Nonnull final ChannelLink link,
                                @Nonnull final ChangeSet changeSet )
  {
    final ChannelAddress source = link.getSourceChannel();
    final SubscriptionEntry sourceEntry = session.findSubscriptionEntry( source );
    if ( null != sourceEntry )
    {
      final ChannelAddress target = link.getTargetChannel();
      final boolean linkingConditional = !getSystemMetaData().getChannelMetaData( target ).hasFilterParameter();
      if ( linkingConditional || shouldFollowLink( sourceEntry, target ) )
      {
        final SubscriptionEntry targetEntry = session.findSubscriptionEntry( target );
        if ( null == targetEntry )
        {
          subscribe( session, target, false, linkingConditional ? null : sourceEntry.getFilter(), changeSet );
          linkSubscriptionEntries( sourceEntry, session.getSubscriptionEntry( target ) );
          return true;
        }
        linkSubscriptionEntries( sourceEntry, targetEntry );
      }
    }
    return false;
  }

  protected abstract boolean shouldFollowLink( @Nonnull final SubscriptionEntry sourceEntry,
                                               @Nonnull final ChannelAddress target );
}
