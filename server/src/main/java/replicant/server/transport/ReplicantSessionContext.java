package replicant.server.transport;

import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import javax.json.JsonObject;
import replicant.server.ChangeSet;
import replicant.server.ChannelAddress;
import replicant.server.EntityMessage;

public interface ReplicantSessionContext
{
  @NonNull
  SchemaMetaData getSchemaMetaData();

  boolean isAuthorized( @NonNull ReplicantSession session );

  void preSubscribe( @NonNull ReplicantSession session, @NonNull ChannelAddress address, @Nullable JsonObject filter );

  /**
   * Hook invoked before sending a change message to the given session.
   * Used to optimise expansion of change messages prior to performing the normal expand cycle.
   *
   * @param session the session in which the change message is being sent. Must not be null.
   * @param packet  the packet representing the change message to be sent. Must not be null.
   */
  void preSendChangeMessage( @NonNull ReplicantSession session, @NonNull Packet packet );

  /**
   * Derive a filter for the target channel based on the source channel and filter.
   *
   * @param entityMessage the entityMessage in the context of which the links is being created.
   * @param source        the source channel.
   * @param sourceFilter  the filter for the source channel.
   * @param target        the target channel.
   * @return the filter for the target channel.
   */
  @NonNull
  JsonObject deriveTargetFilter( @NonNull EntityMessage entityMessage,
                                 @NonNull ChannelAddress source,
                                 @Nullable JsonObject sourceFilter,
                                 @NonNull ChannelAddress target );

  /**
   * Derive the target filter instance id for a partially specified target address.
   *
   * @param entityMessage the entityMessage in the context of which the link is being created.
   * @param source        the concrete source channel.
   * @param sourceFilter  the filter for the source channel.
   * @param target        the target channel template with a missing instance id.
   * @param targetFilter  the target filter if already known, null otherwise.
   * @return the filter instance id for the target channel.
   */
  @NonNull
  String deriveTargetFilterInstanceId( @NonNull EntityMessage entityMessage,
                                       @NonNull ChannelAddress source,
                                       @Nullable JsonObject sourceFilter,
                                       @NonNull ChannelAddress target,
                                       @Nullable JsonObject targetFilter );

  /**
   * Flush the EntityManager that contains replicated entities.
   *
   * @return true if the EntityManager was open and flushed, false if was not open or could not be flushed.
   */
  boolean flushOpenEntityManager();

  void execCommand( @NonNull ReplicantSession session,
                    @NonNull String command,
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
  void collectChannelData( @Nullable ReplicantSession session,
                           @NonNull List<ChannelAddress> addresses,
                           @Nullable JsonObject filter,
                           @NonNull ChangeSet changeSet,
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
  void collectChannelDataForFilterChange( @NonNull ReplicantSession session,
                                          @NonNull List<ChannelAddress> addresses,
                                          @NonNull JsonObject originalFilter,
                                          @NonNull JsonObject newFilter,
                                          @NonNull ChangeSet changeSet );

  @Nullable
  EntityMessage filterEntityMessage( @NonNull ReplicantSession session,
                                     @NonNull ChannelAddress address,
                                     @NonNull EntityMessage message );

  boolean shouldFollowLink( @NonNull ChannelAddress source,
                            @Nullable JsonObject sourceFilter,
                            @NonNull ChannelAddress target,
                            @Nullable JsonObject targetFilter );
}
