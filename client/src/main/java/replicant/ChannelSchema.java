package replicant;

import arez.component.CollectionsUtil;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * Describes a channel within a system.
 */
public final class ChannelSchema
{
  /**
   * The type of filtering applied to the channel.
   */
  public enum FilterType
  {
    /// No filtering
    NONE,
    /// Filtering is specified when the channel is created and is unable to be changed
    STATIC,
    /// Filtering can be changed after the channel has been created
    DYNAMIC
  }

  /**
   * The id of the channel. This is the value used when transmitting messages across network.
   */
  private final int _id;
  /**
   * A human consumable name for channel. It should be non-null if {@link Replicant#areNamesEnabled()} returns
   * true and <tt>null</tt> otherwise.
   */
  @Nullable
  private final String _name;
  /**
   * The type of the root entity if the graph is an instance graph.
   */
  @Nullable
  private final Class<?> _instanceType;
  /**
   * The filtering applied to the channel.
   */
  @Nonnull
  private final FilterType _filterType;
  /**
   * The hook to filter entities when filter changes. This should be null unless {@link #_filterType} is
   * {@link FilterType#DYNAMIC}.
   */
  @Nullable
  private final SubscriptionUpdateEntityFilter<?> _filter;
  /**
   * A flag indicating whether the results of the channel can be cached.
   */
  private final boolean _cacheable;
  /**
   * Flag indicating whether the channel should able to be subscribed to externally.
   * i.e. Can this be explicitly subscribed.
   */
  private final boolean _external;
  /**
   * The entities that are included within the graph
   */
  private final List<EntitySchema> _entities;

  public ChannelSchema( final int id,
                        @Nullable final String name,
                        @Nullable final Class<?> instanceType,
                        @Nonnull final FilterType filterType,
                        @Nullable final SubscriptionUpdateEntityFilter<?> filter,
                        final boolean cacheable,
                        final boolean external,
                        @Nonnull final List<EntitySchema> entities )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> Replicant.areNamesEnabled() || null == name,
                    () -> "Replicant-0045: ChannelSchema passed a name '" + name +
                          "' but Replicant.areNamesEnabled() is false" );
      apiInvariant( () -> FilterType.DYNAMIC != filterType || null != filter,
                    () -> "Replicant-0076: ChannelSchema " + id + " has a DYNAMIC filterType " +
                          "but has supplied no filter." );
      apiInvariant( () -> FilterType.DYNAMIC == filterType || null == filter,
                    () -> "Replicant-0077: ChannelSchema " + id + " does not have a DYNAMIC filterType " +
                          "but has supplied a filter." );
    }
    _id = id;
    _name = Replicant.areNamesEnabled() ? Objects.requireNonNull( name ) : null;
    _instanceType = instanceType;
    _filterType = Objects.requireNonNull( filterType );
    _filter = filter;
    _cacheable = cacheable;
    _external = external;
    _entities = entities;
  }

  /**
   * Return the id of channel.
   *
   * @return the id of channel.
   */
  public int getId()
  {
    return _id;
  }

  /**
   * Return the name of the channel.
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
                    () -> "Replicant-0044: ChannelSchema.getName() invoked when Replicant.areNamesEnabled() is false" );
    }
    assert null != _name;
    return _name;
  }

  /**
   * Return true if this channel is an "type" channel, false otherwise.
   *
   * @return true if this channel is an "type" channel, false otherwise.
   */
  public boolean isTypeChannel()
  {
    return null == _instanceType;
  }

  /**
   * Return true if this channel is an "instance" channel, false otherwise.
   *
   * @return true if this channel is an "instance" channel, false otherwise.
   */
  public boolean isInstanceChannel()
  {
    return !isTypeChannel();
  }

  /**
   * Return the type of the entity that is the root of an instance graph if channel is an instance channel.
   *
   * @return the type of the entity that is the root of an instance graph if channel is an instance channel.
   */
  @Nullable
  public Class<?> getInstanceType()
  {
    return _instanceType;
  }

  /**
   * Return the type of filtering applied to channel.
   *
   * @return the type of filtering applied to channel.
   */
  @Nonnull
  public FilterType getFilterType()
  {
    return _filterType;
  }

  /**
   * Return the hook that filters entities when the filter changes.
   * This will not be null if and only if {@link #_filterType} is {@link FilterType#DYNAMIC}.
   *
   * @return the hook to filter entities.
   */
  @Nullable
  public SubscriptionUpdateEntityFilter<?> getFilter()
  {
    return _filter;
  }

  /**
   * Return a flag indicating whether the results of the channel can be cached.
   *
   * @return a flag indicating whether the results of the channel can be cached.
   */
  public boolean isCacheable()
  {
    return _cacheable;
  }

  /**
   * Return the flag indicating whether the channel should able to be subscribed to externally.
   *
   * @return the flag indicating whether the channel should able to be subscribed to externally.
   */
  public boolean isExternal()
  {
    return _external;
  }

  /**
   * Return the entities transmitted over the channel.
   *
   * @return the entities transmitted over the channel.
   */
  @Nonnull
  public List<EntitySchema> getEntities()
  {
    return CollectionsUtil.wrap( _entities );
  }

  /**
   * Return the entity with specified id, if any.
   *
   * @param entityId the id of the entity to find.
   * @return the entity with specified id, if any.
   */
  @Nullable
  public EntitySchema findEntityById( final int entityId )
  {
    return _entities.stream().filter( e -> e.getId() == entityId ).findAny().orElse( null );
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
