package org.realityforge.replicant.client;

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
   * @return the id of the job that generated the changes. Null if not the originating session.
   */
  String getJobID();

  /**
   * @return the number of changes in the set. Must be &gt; 0.
   */
  int getChangeCount();

  /**
   * Return the change with specific index.
   *
   * @param index the index of the change.
   * @return the change.
   */
  Change getChange( int index );
}
