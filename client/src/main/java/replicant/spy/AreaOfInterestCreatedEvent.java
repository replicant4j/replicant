package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import replicant.AreaOfInterest;
import replicant.ChannelAddress;

/**
 * Notification when an AreaOfInterest has been created.
 */
public final class AreaOfInterestCreatedEvent
  implements SerializableEvent
{
  @NonNull
  private final AreaOfInterest _areaOfInterest;

  public AreaOfInterestCreatedEvent( @NonNull final AreaOfInterest areaOfInterest )
  {
    _areaOfInterest = Objects.requireNonNull( areaOfInterest );
  }

  @NonNull
  public AreaOfInterest getAreaOfInterest()
  {
    return _areaOfInterest;
  }

  @Override
  public void toMap( @NonNull final Map<String, Object> map )
  {
    map.put( "type", "AreaOfInterest.Created" );
    final ChannelAddress address = getAreaOfInterest().getAddress();
    map.put( "channel.schemaId", address.schemaId() );
    map.put( "channel.channelId", address.channelId() );
    map.put( "channel.rootId", address.rootId() );
    map.put( "channel.filter", getAreaOfInterest().getFilter() );
  }
}
