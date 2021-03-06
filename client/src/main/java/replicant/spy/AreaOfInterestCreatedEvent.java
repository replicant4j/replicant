package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import replicant.AreaOfInterest;
import replicant.ChannelAddress;

/**
 * Notification when an AreaOfInterest has been created.
 */
public final class AreaOfInterestCreatedEvent
  implements SerializableEvent
{
  @Nonnull
  private final AreaOfInterest _areaOfInterest;

  public AreaOfInterestCreatedEvent( @Nonnull final AreaOfInterest areaOfInterest )
  {
    _areaOfInterest = Objects.requireNonNull( areaOfInterest );
  }

  @Nonnull
  public AreaOfInterest getAreaOfInterest()
  {
    return _areaOfInterest;
  }

  @Override
  public void toMap( @Nonnull final Map<String, Object> map )
  {
    map.put( "type", "AreaOfInterest.Created" );
    final ChannelAddress address = getAreaOfInterest().getAddress();
    map.put( "channel.systemId", address.getSystemId() );
    map.put( "channel.channelId", address.getChannelId() );
    map.put( "channel.id", address.getId() );
    map.put( "channel.filter", getAreaOfInterest().getFilter() );
  }
}
