package replicant.server.transport;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonObject;
import replicant.server.ChangeSet;
import replicant.server.ChannelAddress;
import replicant.server.ChannelLink;
import replicant.server.EntityMessage;

public interface ReplicantSessionContext
{
  @Nonnull
  SchemaMetaData getSchemaMetaData();

  boolean isAuthorized( @Nonnull ReplicantSession session );

  void preSubscribe( @Nonnull ReplicantSession session, @Nonnull ChannelAddress address, @Nullable Object filter );

  /**
   * Derive a filter for the target channel based on the source channel and filter.
   *
   * @param entityMessage the entityMessage in the context of which the links is being created.
   * @param source        the source channel.
   * @param sourceFilter  the filter for the source channel.
   * @param target        the target channel.
   * @return the filter for the target channel.
   */
  @Nonnull
  Object deriveTargetFilter( @Nonnull EntityMessage entityMessage,
                             @Nonnull ChannelAddress source,
                             @Nullable Object sourceFilter,
                             @Nonnull ChannelAddress target );

  /**
   * Flush the EntityManager that contains replicated entities.
   *
   * @return true if the EntityManager was open and flushed, false if was not open or could not be flushed.
   */
  boolean flushOpenEntityManager();

  void execCommand( @Nonnull ReplicantSession session,
                    @Nonnull String command,
                    int requestId,
                    @Nullable JsonObject payload );

  /**
   * @return the cacheKey if any. The return value is ignored for non-cacheable channels.
   */
  @Nonnull
  SubscribeResult collectDataForSubscribe( @Nonnull ChannelAddress address,
                                           @Nullable Object filter,
                                           @Nonnull ChangeSet changeSet );

  void collectDataForSubscriptionUpdate( @Nonnull ReplicantSession session,
                                         @Nonnull ChannelAddress address,
                                         @Nullable Object originalFilter,
                                         @Nullable Object filter,
                                         @Nonnull ChangeSet changeSet );

  /**
   * This method is called in an attempt to use a more efficient method for bulk loading instance graphs.
   * Subclasses may return false form this method, in which case collectDataForSubscribe will be called
   * for each independent channel.
   */
  void bulkCollectDataForSubscribe( @Nonnull ReplicantSession session,
                                    @Nonnull List<ChannelAddress> addresses,
                                    @Nullable Object filter,
                                    @Nonnull ChangeSet changeSet,
                                    boolean isExplicitSubscribe );

  /**
   * Hook method by which efficient bulk collection of data for subscription updates can occur.
   * It is expected that the hook does everything including updating SubscriptionEntry with new
   * filter, adding graph links etc.
   */
  void bulkCollectDataForSubscriptionUpdate( @Nonnull ReplicantSession session,
                                             @Nonnull List<ChannelAddress> addresses,
                                             @Nullable Object originalFilter,
                                             @Nullable Object filter,
                                             @Nonnull ChangeSet changeSet );

  @Nullable
  EntityMessage filterEntityMessage( @Nonnull ReplicantSession session,
                                     @Nonnull ChannelAddress address,
                                     @Nonnull EntityMessage message );

  @Nonnull
  Collection<ChannelLink> propagateSubscriptionFilterUpdate( @Nonnull ChannelAddress source,
                                                             @Nullable Object sourceFilter );

  boolean shouldFollowLink( @Nonnull SubscriptionEntry sourceEntry,
                            @Nonnull ChannelAddress target,
                            @Nullable Object filter );
}
