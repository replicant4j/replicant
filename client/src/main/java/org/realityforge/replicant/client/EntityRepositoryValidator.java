package org.realityforge.replicant.client;

import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class EntityRepositoryValidator
{
  protected static final Logger LOG = Logger.getLogger( EntityRepositoryValidator.class.getName() );

  /**
   * Check all the entities in the repository and raise an exception if an entity fails to validate.
   *
   * An entity can fail to validate if it is {@link Linkable} and {@link Linkable#isValid()} returns
   * false. An entity can also fail to validate if it is {@link Verifiable} and {@link Verifiable#verify()}
   * throws an exception.
   *
   * @throws IllegalStateException if an invalid entity is found in the repository.
   */
  public void validate( @Nonnull final EntityRepository repository )
    throws IllegalStateException
  {
    for ( final Class entityType : repository.getTypes() )
    {
      for ( final Object entityID : repository.findAllIDs( entityType ) )
      {
        final Object entity = getEntityByID( repository, entityType, entityID );
        if ( entity instanceof Linkable )
        {
          final Linkable linkable = (Linkable) entity;
          if ( !linkable.isValid() )
          {
            final String message =
              "Invalid entity " + entityType.getSimpleName() + "/" + entityID + " found. Entity = " + entity;
            LOG.warning( message );
            throw new IllegalStateException( message );
          }
        }
        if ( entity instanceof Verifiable )
        {
          final Verifiable verifiable = (Verifiable) entity;
          try
          {
            verifiable.verify();
          }
          catch ( final Exception e )
          {
            final String message =
              "Entity " + entityType.getSimpleName() + "/" + entityID + " failed to verify. Entity = " + entity;
            LOG.warning( message );
            throw new IllegalStateException( message, e );
          }
        }
      }
    }
  }

  protected final Object getEntityByID( final EntityRepository repository,
                                        final Class entityType,
                                        final Object entityID )
  {
    try
    {
      return repository.getByID( entityType, entityID );
    }
    catch ( final Throwable e )
    {
      final String message =
        "Unable to retrieve entity " + entityType.getSimpleName() + "/" + entityID + ".";
      LOG.warning( message );
      throw new IllegalStateException( message );

    }
  }
}
