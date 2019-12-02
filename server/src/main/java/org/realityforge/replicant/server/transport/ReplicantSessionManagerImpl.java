package org.realityforge.replicant.server.transport;

import java.io.Serializable;
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
import org.realityforge.replicant.server.json.JsonEncoder;
import org.realityforge.replicant.shared.SharedConstants;

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

  /**
   * {@inheritDoc}
   */
  @Override
  @Nonnull
  public String getSessionKey()
  {
    return "sid";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean invalidateSession( @Nonnull final String sessionID )
  {
    return null != removeSession( sessionID );
  }

  /**
   * Remove session with specified id.
   *
   * @param sessionID the session id.
   * @return the session removed if any.
   */
  protected ReplicantSession removeSession( final String sessionID )
  {
    _lock.writeLock().lock();
    try
    {
      return _sessions.remove( sessionID );
    }
    finally
    {
      _lock.writeLock().unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Nullable
  public ReplicantSession getSession( @Nonnull final String sessionID )
  {
    _lock.readLock().lock();
    final ReplicantSession sessionInfo;
    try
    {
      sessionInfo = _sessions.get( sessionID );
    }
    finally
    {
      _lock.readLock().unlock();
    }
    if ( null != sessionInfo )
    {
      sessionInfo.updateAccessTime();
    }
    return sessionInfo;
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

  /**
   * {@inheritDoc}
   */
  @Override
  @Nonnull
  public ReplicantSession createSession()
  {
    final ReplicantSession sessionInfo = newReplicantSession();
    _lock.writeLock().lock();
    try
    {
      _sessions.put( sessionInfo.getSessionID(), sessionInfo );
    }
    finally
    {
      _lock.writeLock().unlock();
    }
    return sessionInfo;
  }

  /**
   * Return an unmodifiable map containing the set of sessions.
   * The user should also acquire a read lock via {@link #getLock()} prior to invoking
   * this method ensure it is not modified while being inspected.
   *
   * @return an unmodifiable map containing the set of sessions.
   */
  @Nonnull
  protected Map<String, ReplicantSession> getSessions()
  {
    return _roSessions;
  }

  /**
   * @return the lock used to guard access to sessions map.
   */
  @Nonnull
  protected ReadWriteLock getLock()
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
   * Remove sessions that have not been accessed for the specified idle time.
   *
   * @param maxIdleTime the max idle time for a session.
   * @return the number of sessions removed.
   */
  protected int removeIdleSessions( final long maxIdleTime )
  {
    int removedSessions = 0;
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
          removedSessions++;
        }
      }
    }
    finally
    {
      _lock.writeLock().unlock();
    }
    return removedSessions;
  }

  @Nullable
  protected String pollJsonData( @Nonnull final ReplicantSession session, final int lastSequenceAcked )
  {
    final Packet packet = pollPacket( session, lastSequenceAcked );
    if ( null != packet )
    {
      return JsonEncoder.
        encodeChangeSet( packet.getSequence(), packet.getRequestID(), packet.getETag(), packet.getChangeSet() );
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
   * @return the packet created.
   */
  protected Packet sendPacket( @Nonnull final ReplicantSession session,
                               @Nullable final String etag,
                               @Nonnull final ChangeSet changeSet )
  {
    final String requestID = (String) getRegistry().getResource( SharedConstants.REQUEST_ID_KEY );
    getRegistry().putResource( SharedConstants.REQUEST_COMPLETE_KEY, Boolean.FALSE );
    return session.getQueue().addPacket( requestID, etag, changeSet );
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
  protected ReplicantSession ensureSession( @Nonnull final String sessionID )
  {
    final ReplicantSession session = getSession( sessionID );
    if ( null == session )
    {
      throw newBadSessionException( sessionID );
    }
    return session;
  }

  @Nonnull
  protected abstract RuntimeException newBadSessionException( @Nonnull String sessionID );

  @Override
  public boolean saveEntityMessages( @Nullable final String sessionID,
                                     @Nullable final String requestID,
                                     @Nonnull final Collection<EntityMessage> messages,
                                     @Nullable final ChangeSet sessionChanges )
  {
    //TODO: Rewrite this so that we add clients to indexes rather than searching through everyone for each change!
    getLock().readLock().lock();
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
      final ReplicantSession initiatorSession = null != sessionID ? getSession( sessionID ) : null;
      if ( null != initiatorSession && null != sessionChanges )
      {
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

    return accumulator.complete( sessionID, requestID );
  }

  protected abstract void processUpdateMessages( @Nonnull EntityMessage message,
                                                 @Nonnull Collection<ReplicantSession> sessions,
                                                 @Nonnull ChangeAccumulator accumulator );

  protected abstract void processDeleteMessages( @Nonnull EntityMessage message,
                                                 @Nonnull Collection<ReplicantSession> sessions,
                                                 @Nonnull ChangeAccumulator accumulator );

  @Override
  public void delinkSubscription( @Nonnull final ReplicantSession session,
                                  @Nonnull final ChannelAddress sourceGraph,
                                  @Nonnull final ChannelAddress targetGraph,
                                  @Nonnull final ChangeSet changeSet )
  {
    final SubscriptionEntry sourceEntry = session.findSubscriptionEntry( sourceGraph );
    final SubscriptionEntry targetEntry = session.findSubscriptionEntry( targetGraph );
    if ( null != sourceEntry && null != targetEntry )
    {
      delinkSubscriptionEntries( sourceEntry, targetEntry );
      if ( targetEntry.canUnsubscribe() )
      {
        performUnsubscribe( session, targetEntry, false, changeSet );
      }
    }
  }

  @Override
  public void bulkDelinkSubscription( @Nonnull final ReplicantSession session,
                                      @Nonnull final ChannelAddress sourceGraph,
                                      final int channelID,
                                      @Nonnull final Collection<Serializable> subChannelIds,
                                      @Nonnull final ChangeSet changeSet )
  {
    for ( final Serializable id : subChannelIds )
    {
      delinkSubscription( session, sourceGraph, new ChannelAddress( channelID, id ), changeSet );
    }
  }

  /**
   * Perform a a subscribe.
   *
   * @param cacheKey the opaque string that represents version of message client has cached locally.
   * @return The cache status of data returned as part of subscribe.
   */
  @Nonnull
  protected CacheStatus subscribe( @Nonnull final String sessionID,
                                   @Nonnull final ChannelAddress descriptor,
                                   @Nullable final Object filter,
                                   @Nullable final String cacheKey,
                                   @Nonnull final ChangeSet changeSet )
  {
    setupRegistryContext( sessionID );
    final ReplicantSession session = ensureSession( sessionID );
    session.setETag( descriptor, cacheKey );
    final CacheStatus status = subscribe( session, descriptor, true, filter, changeSet );
    if ( status != CacheStatus.USE )
    {
      session.setETag( descriptor, null );
      expandLinks( session, changeSet );
    }
    return status;
  }

  protected void bulkSubscribe( @Nonnull final String sessionID,
                                final int channelID,
                                @Nonnull final Collection<Serializable> subChannelIds,
                                @Nullable final Object filter,
                                final boolean explicitSubscribe,
                                @Nonnull final ChangeSet changeSet )
  {
    setupRegistryContext( sessionID );
    final ReplicantSession session = ensureSession( sessionID );
    bulkSubscribe( session, channelID, subChannelIds, filter, explicitSubscribe, changeSet );
  }

  protected void updateSubscription( @Nonnull final String sessionID,
                                     @Nonnull final ChannelAddress descriptor,
                                     @Nullable final Object filter,
                                     @Nonnull final ChangeSet changeSet )
  {
    setupRegistryContext( sessionID );
    final ReplicantSession session = ensureSession( sessionID );
    updateSubscription( session, descriptor, filter, changeSet );
    expandLinks( session, changeSet );
  }

  protected void bulkUpdateSubscription( @Nonnull final String sessionID,
                                         final int channelID,
                                         @Nonnull final Collection<Serializable> subChannelIds,
                                         @Nullable final Object filter,
                                         @Nonnull final ChangeSet changeSet )
  {
    setupRegistryContext( sessionID );
    final ReplicantSession session = ensureSession( sessionID );
    bulkUpdateSubscription( session, channelID, subChannelIds, filter, changeSet );
    expandLinks( session, changeSet );
  }

  protected void unsubscribe( @Nonnull final String sessionID,
                              @Nonnull final ChannelAddress descriptor,
                              @Nonnull final ChangeSet changeSet )
  {
    setupRegistryContext( sessionID );
    unsubscribe( ensureSession( sessionID ), descriptor, true, changeSet );
  }

  @Override
  public void updateSubscription( @Nonnull final ReplicantSession session,
                                  @Nonnull final ChannelAddress descriptor,
                                  @Nullable final Object filter,
                                  @Nonnull final ChangeSet changeSet )
  {
    assert getSystemMetaData().getChannelMetaData( descriptor ).getFilterType() == ChannelMetaData.FilterType.DYNAMIC;

    final SubscriptionEntry entry = session.getSubscriptionEntry( descriptor );
    final Object originalFilter = entry.getFilter();
    if ( !doFiltersMatch( filter, originalFilter ) )
    {
      performUpdateSubscription( session, entry, originalFilter, filter, changeSet );
    }
  }

  @Override
  public void bulkUpdateSubscription( @Nonnull final ReplicantSession session,
                                      final int channelID,
                                      @Nonnull final Collection<Serializable> subChannelIds,
                                      @Nullable final Object filter,
                                      @Nonnull final ChangeSet changeSet )
  {
    final ChannelMetaData channelMetaData = getSystemMetaData().getChannelMetaData( channelID );
    assert channelMetaData.getFilterType() == ChannelMetaData.FilterType.DYNAMIC;

    final ArrayList<ChannelAddress> channelsToUpdate = new ArrayList<>();

    for ( final Serializable subChannelId : subChannelIds )
    {
      final ChannelAddress descriptor = new ChannelAddress( channelID, subChannelId );
      final SubscriptionEntry entry = session.getSubscriptionEntry( descriptor );
      if ( !doFiltersMatch( filter, entry.getFilter() ) )
      {
        channelsToUpdate.add( descriptor );
      }
    }

    if ( channelsToUpdate.isEmpty() )
    {
      return;
    }
    else if ( 1 == channelsToUpdate.size() )
    {
      updateSubscription( session, channelsToUpdate.get( 0 ), filter, changeSet );
    }
    else
    {
      final Object originalFilter = session.getSubscriptionEntry( channelsToUpdate.get( 0 ) ).getFilter();
      final boolean bulkLoaded =
        bulkCollectDataForSubscriptionUpdate( session,
                                              channelsToUpdate,
                                              changeSet,
                                              originalFilter,
                                              filter );
      if ( !bulkLoaded )
      {
        for ( final ChannelAddress descriptor : channelsToUpdate )
        {
          updateSubscription( session, descriptor, filter, changeSet );
        }
      }
    }
  }

  @Override
  public void bulkSubscribe( @Nonnull final ReplicantSession session,
                             final int channelID,
                             @Nonnull final Collection<Serializable> subChannelIds,
                             @Nullable final Object filter,
                             final boolean explicitSubscribe,
                             @Nonnull final ChangeSet changeSet )
  {
    assert getSystemMetaData().getChannelMetaData( channelID ).isInstanceGraph();

    final ArrayList<ChannelAddress> newChannels = new ArrayList<>();
    //OriginalFilter => Channels
    final HashMap<Object, ArrayList<ChannelAddress>> channelsToUpdate = new HashMap<>();

    for ( final Serializable root : subChannelIds )
    {
      final ChannelAddress descriptor = new ChannelAddress( channelID, root );
      final SubscriptionEntry entry = session.findSubscriptionEntry( descriptor );
      if ( null == entry )
      {
        newChannels.add( descriptor );
      }
      else
      {
        final ArrayList<ChannelAddress> descriptors =
          channelsToUpdate.computeIfAbsent( entry.getFilter(), k -> new ArrayList<>() );
        descriptors.add( descriptor );
      }
    }

    if ( !newChannels.isEmpty() )
    {
      final boolean bulkLoaded =
        bulkCollectDataForSubscribe( session,
                                     newChannels,
                                     changeSet,
                                     filter,
                                     explicitSubscribe );
      if ( !bulkLoaded )
      {
        for ( final ChannelAddress descriptor : newChannels )
        {
          subscribe( session, descriptor, true, filter, changeSet );
        }
      }
    }
    if ( !channelsToUpdate.isEmpty() )
    {
      for ( final Map.Entry<Object, ArrayList<ChannelAddress>> update : channelsToUpdate.entrySet() )
      {
        final Object originalFilter = update.getKey();
        final ArrayList<ChannelAddress> descriptors = update.getValue();
        boolean bulkLoaded = false;

        if ( descriptors.size() > 1 )
        {
          bulkLoaded = bulkCollectDataForSubscriptionUpdate( session,
                                                             descriptors,
                                                             changeSet,
                                                             originalFilter,
                                                             filter );
        }
        if ( !bulkLoaded )
        {
          for ( final ChannelAddress descriptor : descriptors )
          {
            //Just call subscribe as it will do the "right" thing wrt to checking if it needs updates etc.
            subscribe( session, descriptor, true, filter, changeSet );
          }
        }
      }
    }
  }

  @Nonnull
  @Override
  public CacheStatus subscribe( @Nonnull final ReplicantSession session,
                                @Nonnull final ChannelAddress descriptor,
                                final boolean explicitlySubscribe,
                                @Nullable final Object filter,
                                @Nonnull final ChangeSet changeSet )
  {
    if ( session.isSubscriptionEntryPresent( descriptor ) )
    {
      final SubscriptionEntry entry = session.getSubscriptionEntry( descriptor );
      if ( explicitlySubscribe )
      {
        entry.setExplicitlySubscribed( true );
      }
      final ChannelMetaData channelMetaData = getSystemMetaData().getChannelMetaData( descriptor );
      if ( channelMetaData.getFilterType() == ChannelMetaData.FilterType.DYNAMIC )
      {
        updateSubscription( session, descriptor, filter, changeSet );
      }
      else if ( channelMetaData.getFilterType() == ChannelMetaData.FilterType.STATIC )
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
      return CacheStatus.IGNORE;
    }
    else
    {
      return performSubscribe( session,
                               session.createSubscriptionEntry( descriptor ),
                               explicitlySubscribe,
                               filter,
                               changeSet );
    }
  }

  private boolean doFiltersMatch( final Object filter1, final Object filter2 )
  {
    return ( null == filter2 && null == filter1 ) ||
           ( null != filter2 && filter2.equals( filter1 ) );
  }

  @Nonnull
  CacheStatus performSubscribe( @Nonnull final ReplicantSession session,
                                @Nonnull final SubscriptionEntry entry,
                                final boolean explicitSubscribe,
                                @Nullable final Object filter,
                                @Nonnull final ChangeSet changeSet )
  {
    if ( explicitSubscribe )
    {
      entry.setExplicitlySubscribed( true );
    }
    entry.setFilter( filter );
    final ChannelAddress descriptor = entry.getDescriptor();
    final ChannelMetaData channelMetaData = getSystemMetaData().getChannelMetaData( descriptor );
    if ( channelMetaData.isCacheable() )
    {
      final ChannelCacheEntry cacheEntry = ensureCacheEntry( descriptor );
      final String eTag = cacheEntry.getCacheKey();
      if ( eTag.equals( session.getETag( descriptor ) ) )
      {
        return CacheStatus.USE;
      }
      else
      {
        final ChangeSet cacheChangeSet = new ChangeSet();
        cacheChangeSet.merge( cacheEntry.getChangeSet(), true );
        cacheChangeSet.addAction( descriptor, ChannelAction.Action.ADD, filter );
        sendPacket( session, eTag, cacheChangeSet );
        return CacheStatus.REFRESH;
      }
    }

    collectDataForSubscribe( session, descriptor, changeSet, filter );
    changeSet.addAction( descriptor, ChannelAction.Action.ADD, filter );
    return CacheStatus.REFRESH;
  }

  protected boolean deleteCacheEntry( @Nonnull final ChannelAddress descriptor )
  {
    _cacheLock.writeLock().lock();
    try
    {
      return null != _cache.remove( descriptor );
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
  @Nonnull
  protected ChannelCacheEntry ensureCacheEntry( @Nonnull final ChannelAddress descriptor )
  {
    assert getSystemMetaData().getChannelMetaData( descriptor ).isCacheable();
    final ChannelCacheEntry entry = getCacheEntry( descriptor );
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
      final String cacheKey = collectDataForSubscribe( null, descriptor, changeSet, null );
      assert null != cacheKey;
      entry.init( cacheKey, changeSet );
      return entry;
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
  ChannelCacheEntry getCacheEntry( @Nonnull final ChannelAddress descriptor )
  {
    _cacheLock.readLock().lock();
    try
    {
      final ChannelCacheEntry entry = _cache.get( descriptor );
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
      ChannelCacheEntry entry = _cache.get( descriptor );
      if ( null != entry )
      {
        return entry;
      }
      entry = new ChannelCacheEntry( descriptor );
      _cache.put( descriptor, entry );
      return entry;
    }
    finally
    {
      _cacheLock.writeLock().unlock();
    }
  }

  void performUpdateSubscription( @Nonnull final ReplicantSession session,
                                  @Nonnull final SubscriptionEntry entry,
                                  @Nullable final Object originalFilter,
                                  @Nullable final Object filter,
                                  @Nonnull final ChangeSet changeSet )
  {
    assert getSystemMetaData().getChannelMetaData( entry.getDescriptor() ).getFilterType() !=
           ChannelMetaData.FilterType.NONE;
    entry.setFilter( filter );
    final ChannelAddress descriptor = entry.getDescriptor();
    collectDataForSubscriptionUpdate( session, entry.getDescriptor(), changeSet, originalFilter, filter );
    changeSet.addAction( descriptor, ChannelAction.Action.UPDATE, filter );
  }

  /**
   * @param session the client session performing subscribe or null if loading as part of cache
   * @return the cacheKey if any. The return value is ignored for non-cacheable channels.
   */
  @Nullable
  protected abstract String collectDataForSubscribe( @Nullable final ReplicantSession session,
                                                     @Nonnull final ChannelAddress descriptor,
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
                                                          @Nonnull ArrayList<ChannelAddress> descriptors,
                                                          @Nonnull ChangeSet changeSet,
                                                          @Nullable Object filter,
                                                          boolean explicitSubscribe );

  protected abstract void collectDataForSubscriptionUpdate( @Nonnull ReplicantSession session,
                                                            @Nonnull ChannelAddress descriptor,
                                                            @Nonnull ChangeSet changeSet,
                                                            @Nullable Object originalFilter,
                                                            @Nullable Object filter );

  /**
   * Hook method by which efficient bulk collection of data for subscription updates can occur.
   * It is expected that the hook does everything including updating SubscriptionEntry with new
   * filter, adding graph links etc.
   */
  protected abstract boolean bulkCollectDataForSubscriptionUpdate( @Nonnull ReplicantSession session,
                                                                   @Nonnull ArrayList<ChannelAddress> descriptors,
                                                                   @Nonnull ChangeSet changeSet,
                                                                   @Nullable Object originalFilter,
                                                                   @Nullable Object filter );

  protected void bulkUnsubscribe( @Nonnull final String sessionID,
                                  final int channelID,
                                  @Nonnull final Collection<Serializable> subChannelIds,
                                  final boolean explicitUnsubscribe,
                                  @Nonnull final ChangeSet changeSet )
  {
    setupRegistryContext( sessionID );
    bulkUnsubscribe( ensureSession( sessionID ), channelID, subChannelIds, explicitUnsubscribe, changeSet );
  }

  @Override
  public void unsubscribe( @Nonnull final ReplicantSession session,
                           @Nonnull final ChannelAddress descriptor,
                           final boolean explicitUnsubscribe,
                           @Nonnull final ChangeSet changeSet )
  {
    final SubscriptionEntry entry = session.findSubscriptionEntry( descriptor );
    if ( null != entry )
    {
      performUnsubscribe( session, entry, explicitUnsubscribe, changeSet );
    }
  }

  @Override
  public void bulkUnsubscribe( @Nonnull final ReplicantSession session,
                               final int channelID,
                               @Nonnull final Collection<Serializable> subChannelIds,
                               final boolean explicitUnsubscribe,
                               @Nonnull final ChangeSet changeSet )
  {
    for ( final Serializable subChannelId : subChannelIds )
    {
      unsubscribe( session, new ChannelAddress( channelID, subChannelId ), explicitUnsubscribe, changeSet );
    }
  }

  protected void performUnsubscribe( @Nonnull final ReplicantSession session,
                                     @Nonnull final SubscriptionEntry entry,
                                     final boolean explicitUnsubscribe,
                                     @Nonnull final ChangeSet changeSet )
  {
    if ( explicitUnsubscribe )
    {
      entry.setExplicitlySubscribed( false );
    }
    if ( entry.canUnsubscribe() )
    {
      changeSet.addAction( entry.getDescriptor(), ChannelAction.Action.REMOVE, null );
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
      performUnsubscribe( session, downstreamEntry, false, changeSet );
    }
  }

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
  protected void linkSubscriptionEntries( @Nonnull final SubscriptionEntry sourceEntry,
                                          @Nonnull final SubscriptionEntry targetEntry )
  {
    sourceEntry.registerOutwardSubscriptions( targetEntry.getDescriptor() );
    targetEntry.registerInwardSubscriptions( sourceEntry.getDescriptor() );
  }

  /**
   * Configure the SubscriptionEntries to reflect an auto graph delink between the source and target graph.
   */
  protected void delinkSubscriptionEntries( @Nonnull final SubscriptionEntry sourceEntry,
                                            @Nonnull final SubscriptionEntry targetEntry )
  {
    sourceEntry.deregisterOutwardSubscriptions( targetEntry.getDescriptor() );
    targetEntry.deregisterInwardSubscriptions( sourceEntry.getDescriptor() );
  }

  @SuppressWarnings( { "PMD.WhileLoopsMustUseBraces", "StatementWithEmptyBody" } )
  protected void expandLinks( @Nonnull final ReplicantSession session, @Nonnull final ChangeSet changeSet )
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
  protected boolean expandLink( @Nonnull final ReplicantSession session, @Nonnull final ChangeSet changeSet )
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
      final boolean targetUnfiltered =
        getSystemMetaData().getChannelMetaData( target ).getFilterType() == ChannelMetaData.FilterType.NONE;
      if ( targetUnfiltered || shouldFollowLink( sourceEntry, target ) )
      {
        final SubscriptionEntry targetEntry = session.findSubscriptionEntry( target );
        if ( null == targetEntry )
        {
          subscribe( session, target, false, targetUnfiltered ? null : sourceEntry.getFilter(), changeSet );
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

  private void setupRegistryContext( @Nonnull final String sessionID )
  {
    //Force the sessionID to the desired session in case call has not been set up by boundary
    getRegistry().putResource( SharedConstants.SESSION_ID_KEY, sessionID );
  }
}
