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

  @Nonnull
  SubscribeResult collectDataForSubscribe( @Nonnull ChannelAddress address,
                                           @Nullable Object filter,
                                           @Nonnull ChangeSet changeSet );

  void collectDataForSubscriptionUpdate( @Nonnull ReplicantSession session,
                                         @Nonnull ChannelAddress address,
                                         @Nullable Object originalFilter,
                                         @Nullable Object filter,
                                         @Nonnull ChangeSet changeSet );

  void bulkCollectDataForSubscribe( @Nonnull ReplicantSession session,
                                    @Nonnull List<ChannelAddress> addresses,
                                    @Nullable Object filter,
                                    @Nonnull ChangeSet changeSet,
                                    boolean isExplicitSubscribe );

  void bulkCollectDataForSubscriptionUpdate( @Nonnull ReplicantSession session,
                                             @Nonnull List<ChannelAddress> addresses,
                                             @Nullable Object originalFilter,
                                             @Nullable Object filter,
                                             @Nonnull ChangeSet changeSet,
                                             boolean isExplicitSubscribe );

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
