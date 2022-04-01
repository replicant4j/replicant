package replicant;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.messages.EntityChangeData;
import static org.realityforge.braincheck.Guards.*;

/**
 * Describes a entity type within a system.
 */
public final class EntitySchema
{
  /**
   * Function used to create entity on receipt of initial message.
   *
   * @param <T> the type of the entity.
   */
  @FunctionalInterface
  public interface Creator<T>
  {
    /**
     * Create entity from supplied entity data.
     *
     * @param id   the entity identifier.
     * @param data the state to use to create entity.
     */
    @Nonnull
    T createEntity( int id, @Nonnull EntityChangeData data );
  }

  /**
   * Function used to update entity on receipt of subsequent messages.
   *
   * @param <T> the type of the entity.
   */
  @FunctionalInterface
  public interface Updater<T>
  {
    /**
     * Update specified entity from supplied entity data.
     *
     * @param entity the entity.
     * @param data   the state to use to create entity.
     */
    void updateEntity( @Nonnull T entity, @Nonnull EntityChangeData data );
  }

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
  /**
   * The function to create entity.
   */
  @Nonnull
  private final Creator<?> _creator;
  /**
   * The function to update entity.
   * This may be null if the entity has no fields that can be updated.
   */
  @Nullable
  private final Updater<?> _updater;
  @Nonnull
  private final ChannelLinkSchema[] _channelLinks;

  // TODO: Delete me once everything is up to date...
  public <T> EntitySchema( final int id,
                           @Nullable final String name,
                           @Nonnull final Class<T> type,
                           @Nonnull final Creator<T> creator,
                           @Nullable final Updater<T> updater )
  {
    this( id, name, type, creator, updater, new ChannelLinkSchema[ 0 ] );
  }

  public <T> EntitySchema( final int id,
                           @Nullable final String name,
                           @Nonnull final Class<T> type,
                           @Nonnull final Creator<T> creator,
                           @Nullable final Updater<T> updater,
                           @Nonnull final ChannelLinkSchema[] channelLinks )
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
    _creator = Objects.requireNonNull( creator );
    _updater = updater;
    _channelLinks = Objects.requireNonNull( channelLinks );
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
   * Return the function to create entity.
   *
   * @return the function to create entity.
   */
  @Nonnull
  public Creator<?> getCreator()
  {
    return _creator;
  }

  /**
   * Return the function to update entity.
   *
   * @return the function to update entity.
   */
  @Nullable
  public Updater<?> getUpdater()
  {
    return _updater;
  }

  @Nonnull
  public ChannelLinkSchema[] getChannelLinks()
  {
    return _channelLinks;
  }

  @Nonnull
  public List<ChannelLinkSchema> getOutwardChannelLinks( final int channelId )
  {
    return
      Stream
        .of( getChannelLinks() )
        .filter( l -> l.getSourceChannelId() == channelId )
        .collect( Collectors.toList() );
  }

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
