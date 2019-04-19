package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import replicant.AreaOfInterest;
import replicant.ChannelAddress;

/**
 * Notification when an AreaOfInterest status has been updated.
 * Depending on the status, this may also mean that the Susbcription or erro has been updated.
 */
public final class AreaOfInterestStatusUpdatedEvent
  implements SerializableEvent
{
  @Nonnull
  private final AreaOfInterest _areaOfInterest;

  public AreaOfInterestStatusUpdatedEvent( @Nonnull final AreaOfInterest areaOfInterest )
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
    map.put( "type", "AreaOfInterest.StatusUpdated" );
    final AreaOfInterest areaOfInterest = getAreaOfInterest();
    final ChannelAddress address = areaOfInterest.getAddress();
    map.put( "channel.systemId", address.getSystemId() );
    map.put( "channel.channelId", address.getChannelId() );
    map.put( "channel.id", address.getId() );
    map.put( "channel.filter", areaOfInterest.getFilter() );
  }
}
