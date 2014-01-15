package org.realityforge.replicant.server;

/**
 * Service interface for classes that converts an entity into an EntityMessage instance.
 */
public interface EntityMessageGenerator
{
  EntityMessage convertToEntityMessage( Object object, boolean isUpdate );
}
