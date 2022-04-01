package replicant;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * Describes an isolated System containing channel and entity types.
 */
public final class SystemSchema
{
  /**
   * A unique id of the system. Multiple systems with the same id can not be present in a single Context.
   */
  private final int _id;
  /**
   * A human consumable name for the syste,. It should be non-null if {@link Replicant#areNamesEnabled()} returns
   * true and <tt>null</tt> otherwise.
   */
  @Nullable
  private final String _name;
  @Nullable
  private final OnEntityUpdateAction _onEntityUpdateAction;
  /**
   * The entities within the system.
   */
  @Nonnull
  private final ChannelSchema[] _channels;
  /**
   * The entities within the system.
   */
  @Nonnull
  private final EntitySchema[] _entities;

  public SystemSchema( final int id,
                       @Nullable final String name,
                       @Nonnull final ChannelSchema[] channels,
                       @Nonnull final EntitySchema[] entities )
  {
    this( id, name, null, channels, entities );
  }

  public SystemSchema( final int id,
                       @Nullable final String name,
                       @Nullable final OnEntityUpdateAction onEntityUpdateAction,
                       @Nonnull final ChannelSchema[] channels,
                       @Nonnull final EntitySchema[] entities )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> Replicant.areNamesEnabled() || null == name,
                    () -> "Replicant-0051: SystemSchema passed a name '" + name +
                          "' but Replicant.areNamesEnabled() is false" );
      apiInvariant( () -> Arrays.stream( entities ).allMatch( Objects::nonNull ),
                    () -> "Replicant-0053: SystemSchema named '" + ( null == name ? "?" : name ) +
                          "' passed an array of entities that has a null element" );
      for ( int i = 0; i < entities.length; i++ )
      {
        final int index = i;
        apiInvariant( () -> index == entities[ index ].getId(),
                      () -> "Replicant-0054: SystemSchema named '" + ( null == name ? "?" : name ) +
                            "' passed an array of entities where entity at index " + index + " does not " +
                            "have id matching index." );
      }
      apiInvariant( () -> Arrays.stream( channels ).allMatch( Objects::nonNull ),
                    () -> "Replicant-0055: SystemSchema named '" + ( null == name ? "?" : name ) +
                          "' passed an array of channels that has a null element" );
      for ( int i = 0; i < channels.length; i++ )
      {
        final int index = i;
        apiInvariant( () -> index == channels[ index ].getId(),
                      () -> "Replicant-0056: SystemSchema named '" + ( null == name ? "?" : name ) +
                            "' passed an array of channels where channel at index " + index + " does not " +
                            "have id matching index." );
      }
    }
    _id = id;
    _name = Replicant.areNamesEnabled() ? Objects.requireNonNull( name ) : null;
    _onEntityUpdateAction = onEntityUpdateAction;
    _entities = Objects.requireNonNull( entities );
    _channels = Objects.requireNonNull( channels );
  }

  /**
   * Return the id of the system.
   *
   * @return the id of the system.
   */
  public int getId()
  {
    return _id;
  }

  /**
   * Return the name of the SystemSchema.
   * This method should NOT be invoked unless {@link Replicant#areNamesEnabled()} returns true and will throw an
   * exception if invariant checking is enabled.
   *
   * @return the name of the SystemSchema.
   */
  @Nonnull
  public String getName()
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( Replicant::areNamesEnabled,
                    () -> "Replicant-0052: SystemSchema.getName() invoked when Replicant.areNamesEnabled() is false" );
    }
    assert null != _name;
    return _name;
  }

  @Nullable
  public OnEntityUpdateAction getOnEntityUpdateAction()
  {
    return _onEntityUpdateAction;
  }

  /**
   * Return the number of entities in system.
   *
   * @return the number of entities in system.
   */
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
  public EntitySchema getEntity( final int typeId )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> typeId >= 0 && typeId < _entities.length,
                    () -> "Replicant-0057: SystemSchema.getEntity(id) passed an id that is out of range." );
    }
    return _entities[ typeId ];
  }

  /**
   * Return the number of channels in system.
   *
   * @return the number of channels in system.
   */
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
  public ChannelSchema getChannel( final int channelId )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> channelId >= 0 && channelId < _channels.length,
                    () -> "Replicant-0058: SystemSchema.getChannel(id) passed an id that is out of range." );
    }
    return _channels[ channelId ];
  }

  @Nonnull
  public List<ChannelLinkSchema> getInwardChannelLinks( final int channelId, final int entityId )
  {
    return
      Stream
        .of( _channels )
        .flatMap( channelSchema ->
                    channelSchema
                      .getEntities()
                      .stream()
                      .filter( e -> e.getId() == entityId )
                      .flatMap( entity ->
                                  Stream
                                    .of( entity.getChannelLinks() )
                                    .filter( l -> l.getTargetChannelId() == channelId ) ) )
        .distinct()
        .collect( Collectors.toList() );
  }

  @Nonnull
  public List<ChannelLinkSchema> getInwardChannelLinks( final int channelId )
  {
    return
      Stream
        .of( _channels )
        .flatMap( channelSchema ->
                    channelSchema
                      .getEntities()
                      .stream()
                      .flatMap( entity ->
                                  Stream
                                    .of( entity.getChannelLinks() )
                                    .filter( l -> l.getTargetChannelId() == channelId ) ) )
        .distinct()
        .collect( Collectors.toList() );
  }

  @Nonnull
  public List<ChannelLinkSchema> getOutwardChannelLinks( final int channelId )
  {
    return
      getChannel( channelId )
        .getEntities()
        .stream()
        .flatMap( e -> e.getOutwardChannelLinks( channelId ).stream() )
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
