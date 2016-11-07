package org.realityforge.replicant.client;

/**
 * Interface implemented by imitations that can verify their state.
 */
public interface Verifiable
{
  /**
   * Check the state of the entity and raise an exception if invalid oe any related entities are invalid..
   */
  void verify()
    throws Exception;
}
