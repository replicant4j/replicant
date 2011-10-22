package org.realityforge.replicant.client;

/**
 * The types of events that can be raised.
 *
 * Note: There is no OBJECT_CREATED as it is expected that you will be listening to a RELATED_ADDED
 * at the relevant scope.
 */
public enum EntityChangeType
{
  ATTRIBUTE_CHANGED, RELATED_ADDED, RELATED_REMOVED, ENTITY_REMOVED
}
