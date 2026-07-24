package replicant;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TestCacheService
  implements CacheService
{
  @NonNull
  private final Map<Integer, Map<ChannelAddress, CacheEntry>> _data = new HashMap<>();

  @NonNull
  private Map<ChannelAddress, CacheEntry> getSystemCache( final int schemaId )
  {
    return _data.computeIfAbsent( schemaId, v -> new HashMap<>() );
  }

  @NonNull
  @Override
  public Set<ChannelAddress> keySet( final int schemaId )
  {
    return CollectionsUtil.wrap( getSystemCache( schemaId ).keySet() );
  }

  @Nullable
  @Override
  public String lookupEtag( @NonNull final ChannelAddress address )
  {
    final CacheEntry entry = getSystemCache( address.schemaId() ).get( address );
    return null != entry ? entry.getETag() : null;
  }

  @Nullable
  @Override
  public CacheEntry lookup( @NonNull final ChannelAddress address )
  {
    return getSystemCache( address.schemaId() ).get( address );
  }

  @Override
  public boolean store( @NonNull final ChannelAddress address,
                        @NonNull final String eTag,
                        @NonNull final Object content )
  {
    getSystemCache( address.schemaId() ).put( address, new CacheEntry( address, eTag, String.valueOf( content ) ) );
    return true;
  }

  @Override
  public boolean invalidate( @NonNull final ChannelAddress address )
  {
    final Map<ChannelAddress, CacheEntry> systemCache = getSystemCache( address.schemaId() );
    if ( !systemCache.containsKey( address ) )
    {
      return false;
    }
    else
    {
      systemCache.remove( address );
      return true;
    }
  }
}
