package org.realityforge.replicant.server.transport;

import java.io.Serializable;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelDescriptor;
import org.realityforge.ssf.SessionManager;

public interface ReplicantSessionManager
  extends SessionManager<ReplicantSession>
{

  /**
   * Status returned when attempting to subscribe.
   */
  enum CacheStatus
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

  @Nonnull
  ChannelMetaData getChannelMetaData( @Nonnull ChannelDescriptor descriptor );

  @Nonnull
  ChannelMetaData getChannelMetaData( int channelID );

  @Nonnull
  CacheStatus subscribe( @Nonnull ReplicantSession session,
                         @Nonnull ChannelDescriptor descriptor,
                         boolean explicitlySubscribe,
                         @Nullable Object filter,
                         @Nonnull ChangeSet changeSet );

  void bulkSubscribe( @Nonnull ReplicantSession session,
                      int channelID,
                      @Nonnull Collection<Serializable> subChannelIDs,
                      @Nullable Object filter,
                      boolean explicitSubscribe,
                      @Nonnull ChangeSet changeSet );

  void updateSubscription( @Nonnull ReplicantSession session,
                           @Nonnull ChannelDescriptor descriptor,
                           @Nullable Object filter,
                           @Nonnull ChangeSet changeSet );

  void bulkUpdateSubscription( @Nonnull ReplicantSession session,
                               int channelID,
                               @Nonnull Collection<Serializable> subChannelIDs,
                               @Nullable Object filter,
                               @Nonnull ChangeSet changeSet );

  void unsubscribe( @Nonnull ReplicantSession session,
                    @Nonnull ChannelDescriptor descriptor,
                    boolean explicitUnsubscribe,
                    @Nonnull ChangeSet changeSet );

  void bulkUnsubscribe( @Nonnull ReplicantSession session,
                        int channelID,
                        @Nonnull Collection<Serializable> subChannelIDs,
                        boolean explicitUnsubscribe,
                        @Nonnull ChangeSet changeSet );
}
