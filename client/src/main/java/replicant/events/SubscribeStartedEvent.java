package replicant.events;

import java.util.Objects;
import javax.annotation.Nonnull;
import replicant.ChannelAddress;

/**
 * Notification when a Connector starts to subscribe to a channel.
 */
public final class SubscribeStartedEvent
{
  private final int _schemaId;
  @Nonnull
  private final ChannelAddress _address;

  public SubscribeStartedEvent( final int schemaId, @Nonnull final ChannelAddress address )
  {
    _schemaId = schemaId;
    _address = Objects.requireNonNull( address );
  }

  public int getSchemaId()
  {
    return _schemaId;
  }

  @Nonnull
  public ChannelAddress getAddress()
  {
    return _address;
  }
}
