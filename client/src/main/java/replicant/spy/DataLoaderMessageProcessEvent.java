package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.transport.DataLoadStatus;

/**
 * Notification when an DataLoader generated an error processing a message from the datasource.
 */
public final class DataLoaderMessageProcessEvent
  implements SerializableEvent
{
  @Nonnull
  private final Class<?> _systemType;
  @Nonnull
  private final DataLoadStatus _dataLoadStatus;

  public DataLoaderMessageProcessEvent( @Nonnull final Class<?> systemType,
                                        @Nonnull final DataLoadStatus dataLoadStatus )
  {
    _systemType = Objects.requireNonNull( systemType );
    _dataLoadStatus = Objects.requireNonNull( dataLoadStatus );
  }

  @Nonnull
  public Class<?> getSystemType()
  {
    return _systemType;
  }

  @Nonnull
  public DataLoadStatus getDataLoadStatus()
  {
    return _dataLoadStatus;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void toMap( @Nonnull final Map<String, Object> map )
  {
    map.put( "type", "DataLoader.MessageProcess" );
    map.put( "systemType", getSystemType().getSimpleName() );
  }
}
