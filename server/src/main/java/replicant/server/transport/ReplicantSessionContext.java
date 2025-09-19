package replicant.server.transport;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.server.ChangeSet;
import replicant.server.ChannelAddress;
import replicant.server.EntityMessage;

public interface ReplicantSessionContext
{
  @Nonnull
  SchemaMetaData getSchemaMetaData();

  void preSubscribe( @Nonnull ReplicantSession session, @Nonnull ChannelAddress address, @Nullable Object filter );

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

  void propagateSubscriptionFilterUpdate( @Nonnull ReplicantSession session,
                                          @Nonnull ChannelAddress address,
                                          @Nullable Object filter,
                                          @Nonnull ChangeSet changeSet );

  boolean shouldFollowLink( @Nonnull SubscriptionEntry sourceEntry, @Nonnull ChannelAddress target );
}
