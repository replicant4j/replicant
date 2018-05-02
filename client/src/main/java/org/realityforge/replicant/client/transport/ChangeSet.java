package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The interface representing the set of changes to be applied to the EntityRepository.
 *
 * <p>The change set may represent several transactions but may be merged when transmitted to the client.
 * The sequence number identifies the last transaction to be included in the set.</p>
 */
public interface ChangeSet
{
  /**
   * @return the sequence representing the last transaction in the change set.
   */
  int getSequence();

  /**
   * @return the id of the request that generated the changes. Null if not the originating session.
   */
  @Nullable
  String getRequestID();

  /**
   * @return the version under which this can be cached.
   */
  @Nullable
  String getETag();

  /**
   * @return the number of changes in the set.
   */
  int getChangeCount();

  /**
   * Return the change with specific index.
   *
   * @param index the index of the change.
   * @return the change.
   */
  @Nonnull
  Change getChange( int index );

  /**
   * @return the number of channel actions in the set.
   */
  int getChannelActionCount();

  /**
   * Return the changaction with specific index.
   *
   * @param index the index of the action.
   * @return the action.
   */
  @Nonnull
  ChannelAction getChannelAction( int index );
}
