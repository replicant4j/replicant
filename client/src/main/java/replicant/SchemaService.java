package replicant;

import arez.Arez;
import arez.annotations.ArezComponent;
import arez.annotations.Observable;
import arez.annotations.ObservableRef;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * The container of all schemas.
 */
@ArezComponent
abstract class SchemaService
{
  private final Map<Integer, SystemSchema> _schemas = new HashMap<>();

  static SchemaService create()
  {
    return new Arez_SchemaService();
  }

  /**
   * Return the schemas associated with the service.
   *
   * @return the schemas associated with the service.
   */
  @Nonnull
  Collection<SystemSchema> getSchemas()
  {
    final Collection<SystemSchema> schemas = schemas().values();
    return Arez.areRepositoryResultsModifiable() ? schemas : Collections.unmodifiableCollection( schemas );
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
    getSchemasObservable().reportObserved();
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
      invariant( () -> null != schema,
                 () -> "Replicant-0059: Unable to locate SystemSchema with id " + schemaId );
    }
    assert null != schema;
    return schema;
  }

  /**
   * Return true if the specified schema is contained in the container.
   *
   * @param schema the schema.
   * @return true if the specified schema is contained in the container, false otherwise.
   */
  final boolean contains( @Nonnull final SystemSchema schema )
  {
    getSchemasObservable().reportObserved();
    return _schemas.containsKey( schema.getId() );
  }

  /**
   * Register specified schema in list of schemas managed by the container.
   * The schema should NOT already be registered in service.
   *
   * @param schema the schema to register.
   */
  final void registerSchema( @Nonnull final SystemSchema schema )
  {
    getSchemasObservable().preReportChanged();
    _schemas.put( schema.getId(), schema );
    getSchemasObservable().reportChanged();
  }

  /**
   * Return the observable associated with schemas.
   *
   * @return the Arez observable associated with schemas observable property.
   */
  @ObservableRef
  @Nonnull
  abstract arez.Observable getSchemasObservable();

  /**
   * Return the schemas.
   *
   * @return the underlying entities.
   */
  @Observable( expectSetter = false )
  @Nonnull
  Map<Integer, SystemSchema> schemas()
  {
    return _schemas;
  }
}
