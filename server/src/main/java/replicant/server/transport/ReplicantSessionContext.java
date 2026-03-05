package replicant.server.transport;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonObject;
import replicant.server.ChangeSet;
import replicant.server.ChannelAddress;
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
   * Add changes to the ChangeSet as a result of subscribing to a channel.
   * If the session is not null, then the method implementation is expected to update session state to reflect subscription addition.
   *
   * @param session             the session. May be null if data is being collected for caching.
   * @param addresses           the addresses of the channels to collect data for. All addresses must be for a single channelId.
   * @param filter              the filter to apply to the channels. May be null if the channel has no filter parameter.
   * @param changeSet           the changeSet to add the collected data to.
   * @param isExplicitSubscribe true if the subscribe action is explicit, false if it is implicit, ignored unless session is non-null.
   */
  void bulkCollectDataForSubscribe( @Nullable ReplicantSession session,
                                    @Nonnull List<ChannelAddress> addresses,
                                    @Nullable Object filter,
                                    @Nonnull ChangeSet changeSet,
                                    boolean isExplicitSubscribe );

  /**
   * Add changes to the ChangeSet as a result of changing the channel filter.
   * It is expected that the hook does everything including updating SubscriptionEntry with new
   * filter, adding graph links etc.
   *
   * @param session        the session.
   * @param addresses      the addresses of the channels to collect data for. All addresses must be for a single channelId.
   * @param originalFilter the old filter that was applied to the channels.
   * @param newFilter      the new filter to apply to the channels.
   * @param changeSet      the changeSet to add the collected data to.
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

  boolean shouldFollowLink( @Nonnull ChannelAddress source,
                            @Nullable Object sourceFilter,
                            @Nonnull ChannelAddress target,
                            @Nullable Object targetFilter );
}
