package replicant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

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
  private ChannelChange[] channel_actions;
  private EntityChange[] changes;

  @JsOverlay
  public static ChangeSet create( final int sequence,
                                  @Nullable final String requestId,
                                  @Nullable final String eTag,
                                  @Nonnull final ChannelChange[] channelChanges,
                                  @Nonnull final EntityChange[] entityChanges )
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
  public final String getRequestID()
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
   *
   * @return the channel changes.
   */
  @Nonnull
  @JsOverlay
  public final ChannelChange[] getChannelChanges()
  {
    return channel_actions;
  }

  /**
   * Return the entity changes that are part of the message.
   *
   * @return the entity changes.
   */
  @Nonnull
  @JsOverlay
  public final EntityChange[] getEntityChanges()
  {
    return changes;
  }
}
