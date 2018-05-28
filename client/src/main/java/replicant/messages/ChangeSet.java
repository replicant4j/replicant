package replicant.messages;

import java.util.HashSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import replicant.Replicant;
import static org.realityforge.braincheck.Guards.*;

/**
 * The message that represents a set of changes to subscriptions and entities that should be applied atomically.
 */
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
public class ChangeSet
{
  private int last_id;
  @Nullable
  private Double requestId;
  @Nullable
  private String etag;
  @Nullable
  private ChannelChange[] channel_actions;
  @Nullable
  private EntityChange[] changes;

  @JsOverlay
  public static ChangeSet create( final int sequence,
                                  final int requestId,
                                  @Nullable final String eTag,
                                  @Nullable final ChannelChange[] channelChanges,
                                  @Nullable final EntityChange[] entityChanges )
  {
    final ChangeSet changeSet = new ChangeSet();
    changeSet.last_id = sequence;
    changeSet.requestId = (double) requestId;
    changeSet.etag = eTag;
    changeSet.channel_actions = channelChanges;
    changeSet.changes = entityChanges;
    return changeSet;
  }

  @JsOverlay
  public static ChangeSet create( final int sequence,
                                  @Nullable final ChannelChange[] channelChanges,
                                  @Nullable final EntityChange[] entityChanges )
  {
    final ChangeSet changeSet = new ChangeSet();
    changeSet.last_id = sequence;
    changeSet.requestId = null;
    changeSet.etag = null;
    changeSet.channel_actions = channelChanges;
    changeSet.changes = entityChanges;
    return changeSet;
  }

  private ChangeSet()
  {
  }

  /**
   * @return the sequence representing the last transaction in the change set.
   */
  @JsOverlay
  public final int getSequence()
  {
    return last_id;
  }

  /**
   * @return the id of the request that generated the changes. Null if not the originating session.
   */
  @Nullable
  @JsOverlay
  public final Integer getRequestId()
  {
    return null == requestId ? null : requestId.intValue();
  }

  /**
   * @return the version under which this can be cached.
   */
  @Nullable
  @JsOverlay
  public final String getETag()
  {
    return etag;
  }

  /**
   * Return the channel changes that are part of the message.
   * This should only be invoked if {@link #hasChannelChanges()} return true.
   *
   * @return the channel changes.
   */
  @Nonnull
  @JsOverlay
  public final ChannelChange[] getChannelChanges()
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> null != channel_actions,
                    () -> "Replicant-0013: ChangeSet.getChannelChanges() invoked when no changes are present. Should guard call with ChangeSet.hasChannelChanges()." );
    }
    assert null != channel_actions;
    return channel_actions;
  }

  /**
   * Return true if this ChangeSet contains ChannelChanges.
   *
   * @return true if this ChangeSet contains ChannelChanges
   */
  @JsOverlay
  public final boolean hasChannelChanges()
  {
    return null != channel_actions;
  }

  /**
   * Return the entity changes that are part of the message.
   * This should only be invoked if {@link #hasEntityChanges()} return true.
   *
   * @return the entity changes.
   */
  @Nonnull
  @JsOverlay
  public final EntityChange[] getEntityChanges()
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> null != changes,
                    () -> "Replicant-0012: ChangeSet.getEntityChanges() invoked when no changes are present. Should guard call with ChangeSet.hasEntityChanges()." );
    }
    assert null != changes;
    return changes;
  }

  /**
   * Return true if this ChangeSet contains EntityChanges.
   *
   * @return true if this ChangeSet contains EntityChanges
   */
  @JsOverlay
  public final boolean hasEntityChanges()
  {
    return null != changes;
  }

  /**
   * This method will validate the ChangeSet to make sure it is internally consistent if invariants are enabled.
   * The validation will ensure that there is not multiple EntityChange messages for the same entity and that
   * there is not multiple ChannelChange messages for the same channel.
   */
  @JsOverlay
  public final void validate()
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      if ( null != changes )
      {
        final HashSet<String> existing = new HashSet<>();
        for ( final EntityChange change : changes )
        {
          final int typeId = change.getTypeId();
          final int id = change.getId();
          final String key = typeId + "-" + id;
          apiInvariant( () -> existing.add( key ),
                        () -> "Replicant-0014: ChangeSet " + last_id + " contains multiple EntityChange " +
                              "messages for the entity of type " + typeId + " and id " + id + "." );
        }
      }
      if ( null != channel_actions )
      {
        final HashSet<String> existing = new HashSet<>();
        for ( final ChannelChange channelAction : channel_actions )
        {
          final int channelId = channelAction.getChannelId();
          final Integer subChannelId = channelAction.hasSubChannelId() ? channelAction.getSubChannelId() : null;
          final String key = channelId + "-" + subChannelId;
          apiInvariant( () -> existing.add( key ),
                        () -> "Replicant-0022: ChangeSet " + last_id + " contains multiple ChannelChange " +
                              "messages for the channel with id " + channelId +
                              ( null != subChannelId ? " and the subChannel with id " + subChannelId : "" ) + "." );

        }
      }
    }
  }
}
