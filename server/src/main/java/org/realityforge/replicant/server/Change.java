package org.realityforge.replicant.server;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Change
{
  @Nonnull
  private final String _key;
  @Nonnull
  private final EntityMessage _entityMessage;
  private final Map<Integer, Serializable> _channels;

  public Change( @Nonnull final EntityMessage entityMessage )
  {
    _key = entityMessage.getTypeId() + "#" + entityMessage.getId();
    _entityMessage = entityMessage;
    _channels = new LinkedHashMap<>();
  }

  public Change( @Nonnull final EntityMessage entityMessage,
                 final int channelID,
                 @Nullable final Serializable subChannelID )
  {
    this( entityMessage );
    _channels.put( channelID, null == subChannelID ? 0 : subChannelID );
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
  public Map<Integer, Serializable> getChannels()
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
