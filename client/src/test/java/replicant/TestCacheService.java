package replicant;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TestCacheService
  implements CacheService
{
  private final Map<String, String> _data = new HashMap<>();

  @Nullable
  @Override
  public CacheEntry lookup( @Nonnull final String key )
  {
    final String eTag = _data.get( key + "ETAG" );
    final String content = _data.get( key );
    if ( null != eTag && null != content )
    {
      return new CacheEntry( key, eTag, content );
    }
    else
    {
      return null;
    }
  }

  @Override
  public boolean store( @Nonnull final String key, @Nonnull final String eTag, @Nonnull final String content )
  {
    _data.put( key + "ETAG", eTag );
    _data.put( key, content );
    return true;

  }

  @Override
  public boolean invalidate( @Nonnull final String key )
  {
    if ( !_data.containsKey( key + "ETAG" ) )
    {
      return false;
    }
    else
    {
      _data.remove( key + "ETAG" );
      _data.remove( key );
      return true;
    }
  }
}
