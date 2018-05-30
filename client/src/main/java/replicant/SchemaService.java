package replicant;

import arez.component.CollectionsUtil;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * The container of all schemas.
 */
final class SchemaService
{
  private final Map<Integer, SystemSchema> _schemas = new HashMap<>();

  static SchemaService create()
  {
    return new SchemaService();
  }

  /**
   * Return the schemas associated with the service.
   *
   * @return the schemas associated with the service.
   */
  @Nonnull
  Collection<SystemSchema> getSchemas()
  {
    return CollectionsUtil.wrap( schemas().values() );
  }

  /**
   * Return the schema with the specified schemaId or null if no such schema.
   *
   * @param schemaId the id of the schema.
   * @return the schema or null if no such schema.
   */
  @Nullable
  SystemSchema findById( final int schemaId )
  {
    return _schemas.get( schemaId );
  }

  /**
   * Return the schema with the specified schemaId.
   * This should not be invoked unless the schema with specified id exists.
   *
   * @param schemaId the id of the schema.
   * @return the schema.
   */
  @Nonnull
  SystemSchema getById( final int schemaId )
  {
    final SystemSchema schema = findById( schemaId );
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> null != schema, () -> "Replicant-0059: Unable to locate SystemSchema with id " + schemaId );
    }
    assert null != schema;
    return schema;
  }

  /**
   * Register specified schema in list of schemas managed by the container.
   * The schema should NOT already be registered in service.
   *
   * @param schema the schema to register.
   */
  final void registerSchema( @Nonnull final SystemSchema schema )
  {
    final int schemaId = schema.getId();
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> !_schemas.containsKey( schemaId ),
                 () -> "Replicant-0070: Attempted to register schema with id " + schemaId +
                       " when a schema with id already exists: " + _schemas.get( schemaId ) );
    }
    _schemas.put( schemaId, schema );
  }

  final void deregisterSchema( @Nonnull final SystemSchema schema )
  {
    final int schemaId = schema.getId();
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> _schemas.containsKey( schemaId ),
                 () -> "Replicant-0085: Attempted to deregister schema with id " + schemaId +
                       " but no such schema exists." );
    }
    _schemas.remove( schemaId );
  }

  /**
   * Return the schemas.
   *
   * @return the underlying entities.
   */
  @Nonnull
  Map<Integer, SystemSchema> schemas()
  {
    return _schemas;
  }
}
