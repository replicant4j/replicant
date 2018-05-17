package replicant;

import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * Describes an isolated System containing channel and entity types.
 */
public final class SystemType
{
  /**
   * A human consumable name for the syste,. It should be non-null if {@link Replicant#areNamesEnabled()} returns
   * true and <tt>null</tt> otherwise.
   */
  @Nullable
  private final String _name;
  /**
   * The entities within the system.
   */
  @Nonnull
  private final ChannelType[] _channels;
  /**
   * The entities within the system.
   */
  @Nonnull
  private final EntityType[] _entities;

  public SystemType( @Nullable final String name,
                     @Nonnull final ChannelType[] channels,
                     @Nonnull final EntityType[] entities )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> Replicant.areNamesEnabled() || null == name,
                    () -> "Replicant-0051: SystemType passed a name '" + name +
                          "' but Replicant.areNamesEnabled() is false" );
      apiInvariant( () -> Arrays.stream( entities ).allMatch( Objects::nonNull ),
                    () -> "Replicant-0053: SystemType named '" + ( null == name ? '?' : name ) +
                          "' passed an array of entities that has a null element" );
      for ( int i = 0; i < entities.length; i++ )
      {
        final int index = i;
        apiInvariant( () -> index == entities[ index ].getId(),
                      () -> "Replicant-0054: SystemType named '" + ( null == name ? '?' : name ) +
                            "' passed an array of entities where entity at index " + index + " does not " +
                            "have id matching index." );
      }
      apiInvariant( () -> Arrays.stream( channels ).allMatch( Objects::nonNull ),
                    () -> "Replicant-0055: SystemType named '" + ( null == name ? '?' : name ) +
                          "' passed an array of channels that has a null element" );
      for ( int i = 0; i < channels.length; i++ )
      {
        final int index = i;
        apiInvariant( () -> index == channels[ index ].getId(),
                      () -> "Replicant-0056: SystemType named '" + ( null == name ? '?' : name ) +
                            "' passed an array of channels where channel at index " + index + " does not " +
                            "have id matching index." );
      }
    }
    _name = Replicant.areNamesEnabled() ? Objects.requireNonNull( name ) : null;
    _entities = Objects.requireNonNull( entities );
    _channels = Objects.requireNonNull( channels );
  }

  /**
   * Return the name of the SystemType.
   * This method should NOT be invoked unless {@link Replicant#areNamesEnabled()} returns true and will throw an
   * exception if invariant checking is enabled.
   *
   * @return the name of the SystemType.
   */
  @Nonnull
  public String getName()
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( Replicant::areNamesEnabled,
                    () -> "Replicant-0052: SystemType.getName() invoked when Replicant.areNamesEnabled() is false" );
    }
    assert null != _name;
    return _name;
  }

  /**
   * Return the number of entities in system.
   *
   * @return the number of entities in system.
   */
  @Nonnegative
  public int getEntityCount()
  {
    return _entities.length;
  }

  /**
   * Return the entity with specified typeId.
   * The typeId MUST be 0 or more and less than {@link #getEntityCount()}.
   *
   * @param typeId the entity type id.
   * @return the entity matching typeId.
   */
  @Nonnull
  public EntityType getEntity( @Nonnegative final int typeId )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> typeId >= 0 && typeId < _entities.length,
                    () -> "Replicant-0057: SystemType.getEntity(id) passed an id that is out of range." );
    }
    return _entities[ typeId ];
  }

  /**
   * Return the number of channels in system.
   *
   * @return the number of channels in system.
   */
  @Nonnegative
  public int getChannelCount()
  {
    return _channels.length;
  }

  /**
   * Return the Channel with specified channelId.
   * The typeId MUST be 0 or more and less than {@link #getChannelCount()}.
   *
   * @param channelId the channel id.
   * @return the Channel matching channelId.
   */
  @Nonnull
  public ChannelType getChannel( @Nonnegative final int channelId )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> channelId >= 0 && channelId < _channels.length,
                    () -> "Replicant-0058: SystemType.getChannel(id) passed an id that is out of range." );
    }
    return _channels[ channelId ];
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
