package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import replicant.ChannelAddress;

/**
 * Notification when a Connector completes an update of the filter of a subscription.
 */
public final class SubscriptionUpdateCompletedEvent
  implements SerializableEvent
{
  private final int _schemaId;
  @NonNull
  private final String _schemaName;
  @NonNull
  private final ChannelAddress _address;

  public SubscriptionUpdateCompletedEvent( final int schemaId,
                                           @NonNull final String schemaName,
                                           @NonNull final ChannelAddress address )
  {
    _schemaId = schemaId;
    _schemaName = Objects.requireNonNull( schemaName );
    _address = Objects.requireNonNull( address );
  }

  public int getSchemaId()
  {
    return _schemaId;
  }

  @NonNull
  public String getSchemaName()
  {
    return _schemaName;
  }

  @NonNull
  public ChannelAddress getAddress()
  {
    return _address;
  }

  @Override
  public void toMap( @NonNull final Map<String, Object> map )
  {
    map.put( "type", "Connector.SubscriptionUpdateCompleted" );
    map.put( "schema.id", getSchemaId() );
    map.put( "schema.name", getSchemaName() );
    final ChannelAddress address = getAddress();
    map.put( "channel.schemaId", address.schemaId() );
    map.put( "channel.channelId", address.channelId() );
    map.put( "channel.rootId", address.rootId() );
  }
}
