package org.realityforge.replicant.server.transport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonObject;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.Change;
import org.realityforge.replicant.server.ChangeAccumulator;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelAction;
import org.realityforge.replicant.server.ChannelDescriptor;
import org.realityforge.replicant.server.ChannelLink;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.server.ee.EntityMessageCacheUtil;
import org.realityforge.replicant.server.ee.JsonUtil;
import org.realityforge.replicant.server.json.JsonEncoder;
import org.realityforge.replicant.shared.transport.ReplicantContext;
import org.realityforge.ssf.InMemorySessionManager;

/**
 * Base class for session managers.
 */
public abstract class ReplicantSessionManagerImpl
  extends InMemorySessionManager<ReplicantSession>
  implements EntityMessageEndpoint, ReplicantSessionManager
{
  /**
   * Status returned when attempting to subscribe.
   */
  public enum CacheStatus
  {
    /**
     * The client supplied cacheKey is still valid and cached data should be reused.
     */
    USE,
    /**
     * The client did not supply cacheKey or it is out of date. Client cache should be refreshed from supplied data.
     */
    REFRESH,
    /**
     * The client did not supply cacheKey or it is out of date and the response is not cacheable. This may occur
     * if multiple subscriptions occur in a single subscribe call or attempting to subscribe to channels that are
     * already on the client.
     *
     * One day this may not be needed if the client can generate the cache from the in-memory representation rather
     * than the representation as it passes over the network.
     * TODO: Fix this.
     */
    IGNORE
  }

  private final ReadWriteLock _cacheLock = new ReentrantReadWriteLock();
  private final HashMap<ChannelDescriptor, ChannelCacheEntry> _cache = new HashMap<>();

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
    final String requestID = (String) getRegistry().getResource( ReplicantContext.REQUEST_ID_KEY );
    getRegistry().putResource( ReplicantContext.REQUEST_COMPLETE_KEY, Boolean.FALSE );
    return session.getQueue().addPacket( requestID, etag, changeSet );
  }

  /**
   * @return the transaction synchronization registry.
   */
  @Nonnull
  protected abstract TransactionSynchronizationRegistry getRegistry();

  /**
   * @return the metadata for all the channels as an array.
   */
  @Nonnull
  protected abstract ChannelMetaData[] getChannelMetaData();

  @Nonnull
  @Override
  protected ReplicantSession newSessionInfo()
  {
    return new ReplicantSession( UUID.randomUUID().toString() );
  }

  /**
   * @return the channel metadata.
   */
  @Nonnull
  public ChannelMetaData getChannelMetaData( @Nonnull final ChannelDescriptor descriptor )
  {
    final ChannelMetaData[] channelMetaData = getChannelMetaData();
    if ( descriptor.getChannelID() >= channelMetaData.length )
    {
      final String message =
        "Descriptor " + descriptor + " not part of declared metadata: " + Arrays.asList( channelMetaData );
      throw new IllegalStateException( message );
    }
    return channelMetaData[ descriptor.getChannelID() ];
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
        processDeleteMessages( message, sessions );
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
                                                 @Nonnull Collection<ReplicantSession> sessions );

  protected void subscribe( @Nonnull final String sessionID,
                            @Nonnull final ChannelDescriptor descriptor,
                            @Nullable final Object filter )
  {
    setupRegistryContext( sessionID );
    final ReplicantSession session = ensureSession( sessionID );
    subscribe( session, descriptor, true, filter );
    expandLinks( session, EntityMessageCacheUtil.getSessionChanges() );
  }

  protected void updateSubscription( @Nonnull final String sessionID,
                                     @Nonnull final ChannelDescriptor descriptor,
                                     @Nullable final Object filter )
  {
    setupRegistryContext( sessionID );
    final ReplicantSession session = ensureSession( sessionID );
    updateSubscription( session, descriptor, filter );
    expandLinks( session, EntityMessageCacheUtil.getSessionChanges() );
  }

  protected void unsubscribe( @Nonnull final String sessionID,
                              @Nonnull final ChannelDescriptor descriptor )
  {
    setupRegistryContext( sessionID );
    unsubscribe( descriptor, ensureSession( sessionID ), true );
  }

  private void setupRegistryContext( @Nonnull final String sessionID )
  {
    //Force the sessionID to the desired session in case call has not been set up by boundary
    getRegistry().putResource( ReplicantContext.SESSION_ID_KEY, sessionID );
  }

  protected void updateSubscription( @Nonnull final ReplicantSession session,
                                     @Nonnull final ChannelDescriptor descriptor,
                                     @Nullable final Object filter )
  {
    final SubscriptionEntry entry = session.getSubscriptionEntry( descriptor );
    final Object originalFilter = entry.getFilter();
    if ( !( ( null == originalFilter && null == filter ) ||
            ( null != originalFilter && originalFilter.equals( filter ) ) ) )
    {
      performUpdateSubscription( session, entry, originalFilter, filter );
    }
  }

  protected void subscribe( @Nonnull final ReplicantSession session,
                            @Nonnull final ChannelDescriptor descriptor,
                            final boolean explicitlySubscribe,
                            @Nullable final Object filter )
  {
    if ( session.isSubscriptionEntryPresent( descriptor ) )
    {
      final SubscriptionEntry entry = session.getSubscriptionEntry( descriptor );
      if ( explicitlySubscribe )
      {
        entry.setExplicitlySubscribed( true );
      }
      final ChannelMetaData channelMetaData = getChannelMetaData( descriptor );
      if ( channelMetaData.getFilterType() == ChannelMetaData.FilterType.DYNAMIC )
      {
        updateSubscription( session, descriptor, filter );
      }
      else if ( channelMetaData.getFilterType() == ChannelMetaData.FilterType.STATIC )
      {
        final Object existingFilter = entry.getFilter();
        if ( !( ( null == existingFilter && null == filter ) ||
                ( null != existingFilter && existingFilter.equals( filter ) ) ) )
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
      performSubscribe( session, session.createSubscriptionEntry( descriptor ), explicitlySubscribe, filter );
    }
  }

  @Nonnull
  CacheStatus performSubscribe( @Nonnull final ReplicantSession session,
                                @Nonnull final SubscriptionEntry entry,
                                final boolean explicitSubscribe,
                                @Nullable final Object filter )
  {
    if ( explicitSubscribe )
    {
      entry.setExplicitlySubscribed( true );
    }
    entry.setFilter( filter );
    final ChannelDescriptor descriptor = entry.getDescriptor();
    final ChannelMetaData channelMetaData = getChannelMetaData( descriptor );
    if ( channelMetaData.isCacheable() )
    {
      final ChannelCacheEntry cacheEntry = ensureCacheEntry( descriptor );
      if ( cacheEntry.getCacheKey().equals( session.getCacheKey( descriptor ) ) )
      {
        return CacheStatus.USE;
      }
      else
      {
        final ChangeSet changeSet = EntityMessageCacheUtil.getSessionChanges();
        changeSet.merge( cacheEntry.getChangeSet(), true );
        changeSet.addAction( new ChannelAction( descriptor, ChannelAction.Action.ADD, filterToJsonObject( filter ) ) );
        return CacheStatus.REFRESH;
      }
    }

    final ChangeSet changeSet = EntityMessageCacheUtil.getSessionChanges();
    collectDataForSubscribe( session, descriptor, changeSet, filter );
    changeSet.addAction( new ChannelAction( descriptor,
                                            ChannelAction.Action.ADD,
                                            filterToJsonObject( filter ) ) );
    return CacheStatus.REFRESH;
  }

  /**
   * Return a CacheEntry for a specific channel. When this method returns the cache
   * data will have already been loaded. The cache data is loaded using a separate lock for
   * each channel cached.
   */
  @Nonnull
  ChannelCacheEntry ensureCacheEntry( @Nonnull final ChannelDescriptor descriptor )
  {
    assert getChannelMetaData( descriptor ).isCacheable();
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
  ChannelCacheEntry getCacheEntry( @Nonnull final ChannelDescriptor descriptor )
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
                                  @Nullable final Object filter )
  {
    assert getChannelMetaData( entry.getDescriptor() ).getFilterType() != ChannelMetaData.FilterType.NONE;
    entry.setFilter( filter );
    final ChannelDescriptor descriptor = entry.getDescriptor();
    final ChangeSet changeSet = EntityMessageCacheUtil.getSessionChanges();
    collectDataForSubscriptionUpdate( session, entry.getDescriptor(), changeSet, originalFilter, filter );
    changeSet.addAction( new ChannelAction( descriptor,
                                            ChannelAction.Action.UPDATE,
                                            filterToJsonObject( filter ) ) );
  }

  /**
   * @param session the client session performing subscribe or null if loading as part of cache
   * @return the cacheKey if any. The return value is ignored for non-cacheable channels.
   */
  @Nullable
  protected abstract String collectDataForSubscribe( @Nullable final ReplicantSession session,
                                                     @Nonnull final ChannelDescriptor descriptor,
                                                     @Nonnull final ChangeSet changeSet,
                                                     @Nullable final Object filter );

  protected abstract void collectDataForSubscriptionUpdate( @Nonnull final ReplicantSession session,
                                                            @Nonnull final ChannelDescriptor descriptor,
                                                            @Nonnull final ChangeSet changeSet,
                                                            @Nullable final Object originalFilter,
                                                            @Nullable final Object filter );

  protected void unsubscribe( @Nonnull final ChannelDescriptor descriptor,
                              final ReplicantSession session,
                              final boolean explicitUnsubscribe )
  {
    final SubscriptionEntry entry = session.findSubscriptionEntry( descriptor );
    if ( null != entry )
    {
      performUnsubscribe( session, entry, explicitUnsubscribe );
    }
  }

  void performUnsubscribe( @Nonnull final ReplicantSession session,
                           @Nonnull final SubscriptionEntry entry,
                           final boolean explicitUnsubscribe )
  {
    if ( explicitUnsubscribe )
    {
      entry.setExplicitlySubscribed( false );
    }
    if ( entry.canUnsubscribe() )
    {
      EntityMessageCacheUtil.getSessionChanges().
        addAction( new ChannelAction( entry.getDescriptor(), ChannelAction.Action.REMOVE, null ) );
      for ( final ChannelDescriptor downstream : new ArrayList<>( entry.getOutwardSubscriptions() ) )
      {
        final SubscriptionEntry downstreamEntry = session.findSubscriptionEntry( downstream );
        if ( null != downstreamEntry )
        {
          delinkSubscriptionEntries( entry, downstreamEntry );
          performUnsubscribe( session, downstreamEntry, false );
        }
      }
      session.deleteSubscriptionEntry( entry );
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
      final Set<ChannelLink> links = change.getEntityMessage().getLinks();
      if ( null != links )
      {
        for ( final ChannelLink link : links )
        {
          if ( expandLinkIfRequired( session, link ) )
          {
            return true;
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
  boolean expandLinkIfRequired( @Nonnull final ReplicantSession session, @Nonnull final ChannelLink link )
  {
    final ChannelDescriptor source = link.getSourceChannel();
    final SubscriptionEntry sourceEntry = session.findSubscriptionEntry( source );
    if ( null != sourceEntry )
    {
      final ChannelDescriptor target = link.getTargetChannel();
      final boolean targetUnfiltered = getChannelMetaData( target ).getFilterType() == ChannelMetaData.FilterType.NONE;
      if ( targetUnfiltered || shouldFollowLink( source, target ) )
      {
        final SubscriptionEntry targetEntry = session.findSubscriptionEntry( target );
        if ( null == targetEntry )
        {
          subscribe( session, target, false, targetUnfiltered ? null : sourceEntry.getFilter() );
          linkSubscriptionEntries( sourceEntry, session.getSubscriptionEntry( target ) );
          return true;
        }
        linkSubscriptionEntries( sourceEntry, targetEntry );
      }
    }
    return false;
  }

  protected abstract boolean shouldFollowLink( @Nonnull final ChannelDescriptor source,
                                               @Nonnull final ChannelDescriptor target );

  private JsonObject filterToJsonObject( final @Nullable Object filter )
  {
    return null == filter ? null : JsonUtil.toJsonObject( filter );
  }
}
