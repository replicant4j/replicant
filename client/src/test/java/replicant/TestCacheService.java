package replicant;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TestCacheService
  implements CacheService
{
  @Nonnull
  private final Map<Integer, Map<ChannelAddress, CacheEntry>> _data = new HashMap<>();

  @Nonnull
  private Map<ChannelAddress, CacheEntry> getSystemCache( final int systemId )
  {
    return _data.computeIfAbsent( systemId, v -> new HashMap<>() );
  }

  @Nonnull
  @Override
  public Set<ChannelAddress> keySet( final int systemId )
  {
    return CollectionsUtil.wrap( getSystemCache( systemId ).keySet() );
  }

  @Nullable
  @Override
  public String lookupEtag( @Nonnull final ChannelAddress address )
  {
    final CacheEntry entry = getSystemCache( address.getSystemId() ).get( address );
    return null != entry ? entry.getETag() : null;
  }

  @Nullable
  @Override
  public CacheEntry lookup( @Nonnull final ChannelAddress address )
  {
    return getSystemCache( address.getSystemId() ).get( address );
  }

  @Override
  public boolean store( @Nonnull final ChannelAddress address,
                        @Nonnull final String eTag,
                        @Nonnull final Object content )
  {
    getSystemCache( address.getSystemId() ).put( address, new CacheEntry( address, eTag, String.valueOf( content) ) );
    return true;
  }

  @Override
  public boolean invalidate( @Nonnull final ChannelAddress address )
  {
    final Map<ChannelAddress, CacheEntry> systemCache = getSystemCache( address.getSystemId() );
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
