package org.realityforge.replicant.client.transport;

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
  private ChannelAction[] channel_actions;
  private Change[] changes;

  @JsOverlay
  public static ChangeSet create( final int sequence,
                                  @Nullable final String requestId,
                                  @Nullable final String eTag,
                                  @Nonnull final ChannelAction[] channelActions,
                                  @Nonnull final Change[] changes )
  {
    final ChangeSet changeSet = new ChangeSet();
    changeSet.last_id = sequence;
    changeSet.request_id = requestId;
    changeSet.etag = eTag;
    changeSet.channel_actions = channelActions;
    changeSet.changes = changes;
    return changeSet;
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
   * @return the number of changes in the set.
   */
  @JsOverlay
  public final int getChangeCount()
  {
    return changes.length;
  }

  /**
   * Return the change with specific index.
   *
   * @param index the index of the change.
   * @return the change.
   */
  @Nonnull
  @JsOverlay
  public final Change getChange( final int index )
  {
    return changes[ index ];
  }

  /**
   * @return the number of channel actions in the set.
   */
  @JsOverlay
  public final int getChannelActionCount()
  {
    return channel_actions.length;
  }

  /**
   * Return the change action at a specific index.
   *
   * @param index the index of the action.
   * @return the action.
   */
  @Nonnull
  @JsOverlay
  public final ChannelAction getChannelAction( final int index )
  {
    return channel_actions[ index ];
  }
}
