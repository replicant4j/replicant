package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import replicant.AreaOfInterest;
import replicant.ChannelAddress;

/**
 * Notification when an AreaOfInterest has been disposed.
 */
public final class AreaOfInterestDisposedEvent
  implements SerializableEvent
{
  @Nonnull
  private final AreaOfInterest _areaOfInterest;

  public AreaOfInterestDisposedEvent( @Nonnull final AreaOfInterest areaOfInterest )
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
    map.put( "type", "AreaOfInterest.Disposed" );
    final AreaOfInterest areaOfInterest = getAreaOfInterest();
    final ChannelAddress address = areaOfInterest.getAddress();
    map.put( "channel.schemaId", address.getSchemaId() );
    map.put( "channel.channelId", address.getChannelId() );
    map.put( "channel.rootId", address.getRootId() );
    map.put( "channel.filter", areaOfInterest.getFilter() );
  }
}
