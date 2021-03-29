package replicant;

import akasha.Global;
import akasha.Storage;
import akasha.core.JSON;
import akasha.core.JsObject;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;

/**
 * An implementation of the CacheService that uses LocalStorage or SessionStorage.
 * The implementation will preferentially use local storage and then session storage.
 */
public class WebStorageCacheService
  implements CacheService
{
  static final String ETAG_INDEX = "REPLICANT_ETAG_INDEX";
  @Nonnull
  private final Storage _storage;

  /**
   * Install CacheService into the default context where persistence occurs in storage attached to root window.
   * The <code>localStorage</code> of window will be used if present, else the <code>sessionStorage</code> will be used.
   */
  public static void install()
  {
    install( Replicant.context() );
  }

  /**
   * Install CacheService into specified context where persistence occurs in storage attached to root window.
   * The <code>localStorage</code> of window will be used if present, else the <code>sessionStorage</code> will be used.
   *
   * @param context the replicant context.
   */
  public static void install( @Nonnull final ReplicantContext context )
  {
    install( context, Global.localStorage() );
  }

  /**
   * Install CacheService into specified context where persistence occurs in specified storage.
   *
   * @param context the replicant context.
   * @param storage the store used to cache data.
   */
  public static void install( @Nonnull final ReplicantContext context, @Nonnull final Storage storage )
  {
    Objects.requireNonNull( context ).setCacheService( new WebStorageCacheService( storage ) );
  }

  WebStorageCacheService( @Nonnull final Storage storage )
  {
    _storage = Objects.requireNonNull( storage );
  }

  @Nonnull
  @Override
  public Set<ChannelAddress> keySet( final int systemId )
  {
    final Set<ChannelAddress> keys = new HashSet<>();
    getIndex( systemId ).forEach( v -> keys.add( ChannelAddress.parse( systemId, v ) ) );
    return CollectionsUtil.wrap( keys );
  }

  @Nullable
  @Override
  public String lookupEtag( @Nonnull final ChannelAddress address )
  {
    return getIndex( address.getSystemId() ).get( Objects.requireNonNull( address ).asChannelDescriptor() );
  }

  @Nullable
  @Override
  public CacheEntry lookup( @Nonnull final ChannelAddress address )
  {
    Objects.requireNonNull( address );
    final String eTag = getIndex( address.getSystemId() ).get( address.asChannelDescriptor() );
    final String content = _storage.getItem( address.getCacheKey() );
    if ( null != eTag && null != content )
    {
      return new CacheEntry( address, eTag, content );
    }
    else
    {
      return null;
    }
  }

  @Override
  public boolean store( @Nonnull final ChannelAddress address,
                        @Nonnull final String eTag,
                        @Nonnull final String content )
  {
    Objects.requireNonNull( address );
    Objects.requireNonNull( eTag );
    Objects.requireNonNull( content );
    try
    {
      final int systemId = address.getSystemId();
      final JsPropertyMap<String> index = getIndex( systemId );
      index.set( address.asChannelDescriptor(), eTag );
      saveIndex( systemId, index );
      getStorage().setItem( address.getCacheKey(), content );
      return true;
    }
    catch ( final Throwable e )
    {
      // This exception can occur when storage is full
      invalidate( address );
      return false;
    }
  }

  private void saveIndex( final int systemId, @Nonnull final JsPropertyMap<String> index )
  {
    final Storage storage = getStorage();
    final String key = indexKey( systemId );
    if ( 0 == JsObject.keys( index ).length )
    {
      storage.removeItem( key );
    }
    else
    {
      storage.setItem( key, JSON.stringify( index ) );
    }
  }

  @Override
  public boolean invalidate( @Nonnull final ChannelAddress address )
  {
    Objects.requireNonNull( address );
    final int systemId = address.getSystemId();
    final JsPropertyMap<String> index = findIndex( systemId );
    final String key = address.asChannelDescriptor();
    if ( null == index || null == index.get( key ) )
    {
      return false;
    }
    else
    {
      index.delete( key );
      saveIndex( systemId, index );
      getStorage().removeItem( address.getCacheKey() );
      return true;
    }
  }

  @Nonnull
  final Storage getStorage()
  {
    return _storage;
  }

  @Nonnull
  private JsPropertyMap<String> getIndex( final int systemId )
  {
    final JsPropertyMap<String> index = findIndex( systemId );
    return null == index ? Js.uncheckedCast( JsPropertyMap.of() ) : index;
  }

  @Nullable
  private JsPropertyMap<String> findIndex( final int systemId )
  {
    final String indexData = _storage.getItem( indexKey( systemId ) );
    return null == indexData ? null : Js.uncheckedCast( JSON.parse( indexData ) );
  }

  @Nonnull
  private String indexKey( final int systemId )
  {
    return ETAG_INDEX + '-' + systemId;
  }
}
