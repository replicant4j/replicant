package replicant.messages;

import java.util.HashMap;
import javax.annotation.Nonnull;

/**
 * An implementation of EntityChangeData suitable for use within the JVM.
 */
@GwtIncompatible
public class EntityChangeDataImpl
  implements EntityChangeData
{
  private final HashMap<String, Object> _data = new HashMap<>();

  public HashMap<String, Object> getData()
  {
    return _data;
  }

  @Override
  public boolean containsKey( @Nonnull final String key )
  {
    return _data.containsKey( key );
  }

  @Override
  public boolean isNull( @Nonnull final String key )
  {
    assert _data.containsKey( key );
    return null == _data.get( key );
  }

  @Override
  public int getIntegerValue( @Nonnull final String key )
  {
    assert _data.containsKey( key );
    return (int) _data.get( key );
  }

  @Nonnull
  @Override
  public String getStringValue( @Nonnull final String key )
  {
    assert _data.containsKey( key );
    return (String) _data.get( key );
  }

  @Override
  public boolean getBooleanValue( @Nonnull final String key )
  {
    assert _data.containsKey( key );
    return (Boolean) _data.get( key );
  }
}
