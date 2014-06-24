package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChannelDescriptor;

/**
 * Representation the result of channel change action in data load.
 */
public final class ChannelChangeStatus
{
  private final ChannelDescriptor _descriptor;
  private final Object _filter;
  private final int _entityRemoveCount;

  public ChannelChangeStatus( @Nonnull final ChannelDescriptor descriptor, @Nullable final Object filter, final int entityRemoveCount )
  {
    _descriptor = descriptor;
    _filter = filter;
    _entityRemoveCount = entityRemoveCount;
  }

  /**
   * @return the descriptor for the channel.
   */
  @Nonnull
  public ChannelDescriptor getDescriptor()
  {
    return _descriptor;
  }

  /**
   * @return the filter associated with channel change. Null for remove, possible non-null for update and add if channel is filtered.
   */
  @Nullable
  public Object getFilter()
  {
    return _filter;
  }

  /**
   * @return the number of entities removed as a result of this channel action. Typically an unsubscribe or
   *         update can result in entities being no longer relevant and thus removed.
   */
  public int getEntityRemoveCount()
  {
    return _entityRemoveCount;
  }
}
