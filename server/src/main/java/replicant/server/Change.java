package replicant.server;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Change
{
  @Nonnull
  private final String _key;
  @Nonnull
  private final EntityMessage _entityMessage;
  @Nonnull
  private final Map<Integer, Integer> _channels = new LinkedHashMap<>();

  public Change( @Nonnull final EntityMessage entityMessage )
  {
    _key = entityMessage.getTypeId() + "#" + entityMessage.getId();
    _entityMessage = Objects.requireNonNull( entityMessage );
  }

  public Change( @Nonnull final EntityMessage entityMessage,
                 final int channelId,
                 @Nullable final Integer rootId )
  {
    this( entityMessage );
    _channels.put( channelId, rootId );
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
  public Map<Integer, Integer> getChannels()
  {
    return _channels;
  }

  public void merge( @Nonnull final Change other )
  {
    getEntityMessage().merge( other.getEntityMessage() );
    getChannels().putAll( other.getChannels() );
  }

  @Nonnull
  public Change duplicate()
  {
    final Change change = new Change( getEntityMessage().duplicate() );
    change.getChannels().putAll( getChannels() );
    return change;
  }
}
