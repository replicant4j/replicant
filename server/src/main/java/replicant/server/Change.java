package replicant.server;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Change
{
  @Nonnull
  private final String _key;
  @Nonnull
  private final EntityMessage _entityMessage;
  @Nonnull
  private final Set<ChannelAddress> _channels = new LinkedHashSet<>();

  public Change( @Nonnull final EntityMessage entityMessage )
  {
    _key = entityMessage.getTypeId() + "#" + entityMessage.getId();
    _entityMessage = Objects.requireNonNull( entityMessage );
  }

  public Change( @Nonnull final EntityMessage entityMessage,
                 final int channelId,
                 @Nullable final Integer rootId )
  {
    this( entityMessage, new ChannelAddress( channelId, rootId ) );
  }

  public Change( @Nonnull final EntityMessage entityMessage, @Nonnull final ChannelAddress address )
  {
    this( entityMessage );
    _channels.add( Objects.requireNonNull( address ) );
  }

  @Nonnull
  public String getKey()
  {
    return _key;
  }

  @Nonnull
  public EntityMessage getEntityMessage()
  {
    return _entityMessage;
  }

  @Nonnull
  public Set<ChannelAddress> getChannels()
  {
    return _channels;
  }

  public void merge( @Nonnull final Change other )
  {
    getEntityMessage().merge( other.getEntityMessage() );
    getChannels().addAll( other.getChannels() );
  }

  @Nonnull
  public Change duplicate()
  {
    final Change change = new Change( getEntityMessage().duplicate() );
    change.getChannels().addAll( getChannels() );
    return change;
  }
}
