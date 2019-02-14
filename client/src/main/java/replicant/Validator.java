package replicant;

import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.Feature;
import arez.component.Verifiable;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * A utility class that is used to validate state within the replicant context is consistent.
 */
@ArezComponent( disposeNotifier = Feature.DISABLE )
abstract class Validator
  extends ReplicantService
{
  static Validator create( @Nullable final ReplicantContext context )
  {
    return new Arez_Validator( context );
  }

  Validator( @Nullable final ReplicantContext context )
  {
    super( context );
  }

  /**
   * Verify that all entities contained within the EntityService will pass verification.
   * An entity can be verified by implementing the {@link Verifiable} interface.
   */
  @Action
  void validateEntities()
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      for ( final Class<?> entityType : getReplicantContext().findAllEntityTypes() )
      {
        for ( final Entity entity : getReplicantContext().findAllEntitiesByType( entityType ) )
        {
          try
          {
            final Object userObject = entity.maybeUserObject();
            if ( null != userObject )
            {
              Verifiable.verify( userObject );
            }
          }
          catch ( final Exception e )
          {
            fail( () -> "Replicant-0065: Entity failed to verify during validation process. Entity = " + entity );
          }
        }
      }
    }
  }
}
