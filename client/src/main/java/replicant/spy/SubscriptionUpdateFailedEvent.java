package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import replicant.ChannelAddress;

public final class SubscriptionUpdateFailedEvent
  implements SerializableEvent
{
  private final int _schemaId;
  @Nonnull
  private final String _schemaName;
  @Nonnull
  private final ChannelAddress _address;
  @Nonnull
  private final Throwable _error;

  public SubscriptionUpdateFailedEvent( final int schemaId, @Nonnull final String schemaName,
                                        @Nonnull final ChannelAddress address,
                                        @Nonnull final Throwable error )
  {
    _schemaId = schemaId;
    _schemaName = Objects.requireNonNull( schemaName );
    _address = Objects.requireNonNull( address );
    _error = Objects.requireNonNull( error );
  }

  public int getSchemaId()
  {
    return _schemaId;
  }

  @Nonnull
  public String getSchemaName()
  {
    return _schemaName;
  }

  @Nonnull
  public ChannelAddress getAddress()
  {
    return _address;
  }

  @Nonnull
  public Throwable getError()
  {
    return _error;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void toMap( @Nonnull final Map<String, Object> map )
  {
    map.put( "type", "Connector.SubscriptionUpdateFailed" );
    map.put( "schema.id", getSchemaId() );
    map.put( "schema.name", getSchemaName() );
    final ChannelAddress address = getAddress();
    map.put( "channel.type", address.getChannelType().name() );
    map.put( "channel.id", address.getId() );
    final Throwable throwable = getError();
    map.put( "message", null == throwable.getMessage() ? throwable.toString() : throwable.getMessage() );
  }
}
