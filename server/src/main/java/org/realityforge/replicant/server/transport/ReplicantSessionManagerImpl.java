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
  public boolean invalidateSession( @Nonnull final String sessionId )
  {
    return null != removeSession( sessionId );
  }

  /**
   * Remove session with specified id.
   *
   * @param sessionId the session id.
   * @return the session removed if any.
   */
  protected ReplicantSession removeSession( final String sessionId )
  {
    _lock.writeLock().lock();
    try
    {
      return _sessions.remove( sessionId );
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
  public ReplicantSession getSession( @Nonnull final String sessionId )
  {
    _lock.readLock().lock();
    final ReplicantSession sessionInfo;
    try
    {
      sessionInfo = _sessions.get( sessionId );
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
   * @return the packet created.
   */
  protected Packet sendPacket( @Nonnull final ReplicantSession session,
                               @Nullable final String etag,
                               @Nonnull final ChangeSet changeSet )
  {
    final Integer requestId = (Integer) getRegistry().getResource( ServerConstants.REQUEST_ID_KEY );
    getRegistry().putResource( ServerConstants.REQUEST_COMPLETE_KEY, Boolean.FALSE );
    return session.getQueue().addPacket( requestId, etag, changeSet );
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
      final ReplicantSession initiatorSession = null != sessionId ? getSession( sessionId ) : null;
      if ( null != initiatorSession && null != sessionChanges )
      {
        accumulator.getChangeSet( initiatorSession ).setPingResponse( sessionChanges.isPingResponse() );
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

    return accumulator.complete( sessionId, requestId );
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
                                      final int channelId,
                                      @Nonnull final Collection<Integer> subChannelIds,
                                      @Nonnull final ChangeSet changeSet )
  {
    for ( final Integer id : subChannelIds )
    {
      delinkSubscription( session, sourceGraph, new ChannelAddress( channelId, id ), changeSet );
    }
  }

  /**
   * Perform a a subscribe.
   *
   * @param cacheKey the opaque string that represents version of message client has cached locally.
   * @return The cache status of data returned as part of subscribe.
   */
  @Nonnull
  protected CacheStatus subscribe( @Nonnull final String sessionId,
                                   @Nonnull final ChannelAddress address,
                                   @Nullable final Object filter,
                                   @Nullable final String cacheKey,
                                   @Nonnull final ChangeSet changeSet )
  {
    setupRegistryContext( sessionId );
    final ReplicantSession session = ensureSession( sessionId );
    session.setETag( address, cacheKey );
    final CacheStatus status = subscribe( session, address, true, filter, changeSet );
    if ( status != CacheStatus.USE )
    {
      session.setETag( address, null );
      expandLinks( session, changeSet );
    }
    return status;
  }

  protected void bulkSubscribe( @Nonnull final String sessionId,
                                final int channelId,
                                @Nonnull final Collection<Integer> subChannelIds,
                                @Nullable final Object filter,
                                final boolean explicitSubscribe,
                                @Nonnull final ChangeSet changeSet )
  {
    setupRegistryContext( sessionId );
    final ReplicantSession session = ensureSession( sessionId );
    bulkSubscribe( session, channelId, subChannelIds, filter, explicitSubscribe, changeSet );
  }

  protected void updateSubscription( @Nonnull final String sessionId,
                                     @Nonnull final ChannelAddress address,
                                     @Nullable final Object filter,
                                     @Nonnull final ChangeSet changeSet )
  {
    setupRegistryContext( sessionId );
    final ReplicantSession session = ensureSession( sessionId );
    updateSubscription( session, address, filter, changeSet );
    expandLinks( session, changeSet );
  }

  protected void bulkUpdateSubscription( @Nonnull final String sessionId,
                                         final int channelId,
                                         @Nonnull final Collection<Integer> subChannelIds,
                                         @Nullable final Object filter,
                                         @Nonnull final ChangeSet changeSet )
  {
    setupRegistryContext( sessionId );
    final ReplicantSession session = ensureSession( sessionId );
    bulkUpdateSubscription( session, channelId, subChannelIds, filter, changeSet );
    expandLinks( session, changeSet );
  }

  protected void unsubscribe( @Nonnull final String sessionId,
                              @Nonnull final ChannelAddress address,
                              @Nonnull final ChangeSet changeSet )
  {
    setupRegistryContext( sessionId );
    unsubscribe( ensureSession( sessionId ), address, true, changeSet );
  }

  @Override
  public void updateSubscription( @Nonnull final ReplicantSession session,
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
  public void bulkUpdateSubscription( @Nonnull final ReplicantSession session,
                                      final int channelId,
                                      @Nonnull final Collection<Integer> subChannelIds,
                                      @Nullable final Object filter,
                                      @Nonnull final ChangeSet changeSet )
  {
    final ChannelMetaData channelMetaData = getSystemMetaData().getChannelMetaData( channelId );
    assert channelMetaData.getFilterType() == ChannelMetaData.FilterType.DYNAMIC;

    final ArrayList<ChannelAddress> channelsToUpdate = new ArrayList<>();

    for ( final Integer subChannelId : subChannelIds )
    {
      final ChannelAddress address = new ChannelAddress( channelId, subChannelId );
      final SubscriptionEntry entry = session.getSubscriptionEntry( address );
      if ( !doFiltersMatch( filter, entry.getFilter() ) )
      {
        channelsToUpdate.add( address );
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
        for ( final ChannelAddress address : channelsToUpdate )
        {
          updateSubscription( session, address, filter, changeSet );
        }
      }
    }
  }

  @Override
  public void bulkSubscribe( @Nonnull final ReplicantSession session,
                             final int channelId,
                             @Nonnull final Collection<Integer> subChannelIds,
                             @Nullable final Object filter,
                             final boolean explicitSubscribe,
                             @Nonnull final ChangeSet changeSet )
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
      final boolean bulkLoaded =
        bulkCollectDataForSubscribe( session,
                                     newChannels,
                                     changeSet,
                                     filter,
                                     explicitSubscribe );
      if ( !bulkLoaded )
      {
        for ( final ChannelAddress address : newChannels )
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
          bulkLoaded = bulkCollectDataForSubscriptionUpdate( session,
                                                             addresses,
                                                             changeSet,
                                                             originalFilter,
                                                             filter );
        }
        if ( !bulkLoaded )
        {
          for ( final ChannelAddress address : addresses )
          {
            //Just call subscribe as it will do the "right" thing wrt to checking if it needs updates etc.
            try
            {
              subscribe( session, address, true, filter, changeSet );
            }
            catch ( final Throwable e )
            {
              t = e;
            }
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

  @Nonnull
  @Override
  public CacheStatus subscribe( @Nonnull final ReplicantSession session,
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
      if ( channelMetaData.getFilterType() == ChannelMetaData.FilterType.DYNAMIC )
      {
        updateSubscription( session, address, filter, changeSet );
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
      final SubscriptionEntry entry = session.createSubscriptionEntry( address );
      try
      {
        return performSubscribe( session, entry, explicitlySubscribe, filter, changeSet );
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
    final ChannelAddress address = entry.getDescriptor();
    final ChannelMetaData channelMetaData = getSystemMetaData().getChannelMetaData( address );
    if ( channelMetaData.isCacheable() )
    {
      final ChannelCacheEntry cacheEntry = ensureCacheEntry( address );
      final String eTag = cacheEntry.getCacheKey();
      if ( eTag.equals( session.getETag( address ) ) )
      {
        return CacheStatus.USE;
      }
      else
      {
        final ChangeSet cacheChangeSet = new ChangeSet();
        cacheChangeSet.merge( cacheEntry.getChangeSet(), true );
        cacheChangeSet.mergeAction( address, ChannelAction.Action.ADD, filter );
        sendPacket( session, eTag, cacheChangeSet );
        return CacheStatus.REFRESH;
      }
    }

    collectDataForSubscribe( address, changeSet, filter );
    changeSet.mergeAction( address, ChannelAction.Action.ADD, filter );
    return CacheStatus.REFRESH;
  }

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

  /**
   * Return a CacheEntry for a specific channel. When this method returns the cache
   * data will have already been loaded. The cache data is loaded using a separate lock for
   * each channel cached.
   */
  @Nonnull
  protected ChannelCacheEntry ensureCacheEntry( @Nonnull final ChannelAddress address )
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
      final String cacheKey = collectDataForSubscribe( address, changeSet, null );
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
    assert getSystemMetaData().getChannelMetaData( entry.getDescriptor() ).getFilterType() !=
           ChannelMetaData.FilterType.NONE;
    entry.setFilter( filter );
    final ChannelAddress address = entry.getDescriptor();
    collectDataForSubscriptionUpdate( entry.getDescriptor(), changeSet, originalFilter, filter );
    changeSet.mergeAction( address, ChannelAction.Action.UPDATE, filter );
  }

  /**
   * @return the cacheKey if any. The return value is ignored for non-cacheable channels.
   */
  @Nullable
  protected abstract String collectDataForSubscribe( @Nonnull final ChannelAddress address,
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
                                                          @Nonnull ChangeSet changeSet,
                                                          @Nullable Object filter,
                                                          boolean explicitSubscribe );

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
                                                                   @Nonnull ChangeSet changeSet,
                                                                   @Nullable Object originalFilter,
                                                                   @Nullable Object filter );

  protected void bulkUnsubscribe( @Nonnull final String sessionId,
                                  final int channelId,
                                  @Nonnull final Collection<Integer> subChannelIds,
                                  final boolean explicitUnsubscribe,
                                  @Nonnull final ChangeSet changeSet )
  {
    setupRegistryContext( sessionId );
    bulkUnsubscribe( ensureSession( sessionId ), channelId, subChannelIds, explicitUnsubscribe, changeSet );
  }

  @Override
  public void unsubscribe( @Nonnull final ReplicantSession session,
                           @Nonnull final ChannelAddress address,
                           final boolean explicitUnsubscribe,
                           @Nonnull final ChangeSet changeSet )
  {
    final SubscriptionEntry entry = session.findSubscriptionEntry( address );
    if ( null != entry )
    {
      performUnsubscribe( session, entry, explicitUnsubscribe, changeSet );
    }
  }

  @Override
  public void bulkUnsubscribe( @Nonnull final ReplicantSession session,
                               final int channelId,
                               @Nonnull final Collection<Integer> subChannelIds,
                               final boolean explicitUnsubscribe,
                               @Nonnull final ChangeSet changeSet )
  {
    for ( final Integer subChannelId : subChannelIds )
    {
      unsubscribe( session, new ChannelAddress( channelId, subChannelId ), explicitUnsubscribe, changeSet );
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
      changeSet.mergeAction( entry.getDescriptor(), ChannelAction.Action.REMOVE, null );
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

  private void setupRegistryContext( @Nonnull final String sessionId )
  {
    //Force the sessionId to the desired session in case call has not been set up by boundary
    getRegistry().putResource( ServerConstants.SESSION_ID_KEY, sessionId );
  }
}
