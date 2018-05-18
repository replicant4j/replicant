package replicant;

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
   * A flag indicating whether the channel is instance based or type based.
   */
  private final boolean _typeChannel;
  /**
   * The filtering applied to the channel.
   */
  @Nonnull
  private final FilterType _filterType;
  /**
   * A flag indicating whether the results of the channel can be cached.
   */
  private final boolean _cacheable;
  /**
   * Flag indicating whether the channel should able to be subscribed to externally.
   * i.e. Can this be explicitly subscribed.
   */
  private final boolean _external;

  public ChannelSchema( final int id,
                        @Nullable final String name,
                        final boolean typeChannel,
                        @Nonnull final FilterType filterType,
                        final boolean cacheable,
                        final boolean external )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> Replicant.areNamesEnabled() || null == name,
                    () -> "Replicant-0045: ChannelSchema passed a name '" + name +
                          "' but Replicant.areNamesEnabled() is false" );
    }
    _id = id;
    _name = Replicant.areNamesEnabled() ? Objects.requireNonNull( name ) : null;
    _typeChannel = typeChannel;
    _filterType = Objects.requireNonNull( filterType );
    _cacheable = cacheable;
    _external = external;
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
    return _typeChannel;
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