package replicant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import static org.realityforge.braincheck.Guards.*;

/**
 * The message that represents a set of changes to subscriptions and entities that should be applied atomically.
 */
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
public class ChangeSet
{
  private int last_id;
  @Nullable
  private String request_id;
  @Nullable
  private String etag;
  @Nullable
  private ChannelChange[] channel_actions;
  @Nullable
  private EntityChange[] changes;

  @JsOverlay
  public static ChangeSet create( final int sequence,
                                  @Nullable final String requestId,
                                  @Nullable final String eTag,
                                  @Nullable final ChannelChange[] channelChanges,
                                  @Nullable final EntityChange[] entityChanges )
  {
    final ChangeSet changeSet = new ChangeSet();
    changeSet.last_id = sequence;
    changeSet.request_id = requestId;
    changeSet.etag = eTag;
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
  public final String getRequestId()
  {
    return request_id;
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
}
