package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * Describes a entity type within a system.
 */
public final class EntitySchema
{
  /**
   * The id of the entity type. This is the value used when transmitting messages across network.
   */
  private final int _id;
  /**
   * A human consumable name for entity type. It should be non-null if {@link Replicant#areNamesEnabled()} returns
   * true and <tt>null</tt> otherwise.
   */
  @Nullable
  private final String _name;
  /**
   * The java-type of the entity.
   */
  @Nonnull
  private final Class<?> _type;

  public EntitySchema( final int id, @Nullable final String name, @Nonnull final Class<?> type )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> Replicant.areNamesEnabled() || null == name,
                    () -> "Replicant-0049: EntitySchema passed a name '" + name +
                          "' but Replicant.areNamesEnabled() is false" );
    }
    _id = id;
    _name = Replicant.areNamesEnabled() ? Objects.requireNonNull( name ) : null;
    _type = Objects.requireNonNull( type );
  }

  /**
   * Return the id of EntitySchema.
   *
   * @return the id of EntitySchema.
   */
  public int getId()
  {
    return _id;
  }

  /**
   * Return the name of the EntitySchema.
   * This method should NOT be invoked unless {@link Replicant#areNamesEnabled()} returns true and will throw an
   * exception if invariant checking is enabled.
   *
   * @return the name of the channel.
   */
  @Nonnull
  public String getName()
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( Replicant::areNamesEnabled,
                    () -> "Replicant-0050: EntitySchema.getName() invoked when Replicant.areNamesEnabled() is false" );
    }
    assert null != _name;
    return _name;
  }

  /**
   * Return the java type of entity.
   *
   * @return the java type of entity.
   */
  @Nonnull
  public Class<?> getType()
  {
    return _type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    if ( Replicant.areNamesEnabled() )
    {
      return getName();
    }
    else
    {
      return super.toString();
    }
  }
}
