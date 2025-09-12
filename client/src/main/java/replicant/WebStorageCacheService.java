package replicant;

import akasha.Storage;
import akasha.WindowGlobal;
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
public final class WebStorageCacheService
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
    install( context, WindowGlobal.localStorage() );
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
  public Set<ChannelAddress> keySet( final int schemaId )
  {
    final Set<ChannelAddress> keys = new HashSet<>();
    getIndex( schemaId ).forEach( v -> keys.add( ChannelAddress.parse( schemaId, v ) ) );
    return CollectionsUtil.wrap( keys );
  }

  @Nullable
  @Override
  public String lookupEtag( @Nonnull final ChannelAddress address )
  {
    return getIndex( address.getSchemaId() ).get( Objects.requireNonNull( address ).asChannelDescriptor() );
  }

  @Nullable
  @Override
  public CacheEntry lookup( @Nonnull final ChannelAddress address )
  {
    Objects.requireNonNull( address );
    final String eTag = getIndex( address.getSchemaId() ).get( address.asChannelDescriptor() );
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
                        @Nonnull final Object content )
  {
    Objects.requireNonNull( address );
    Objects.requireNonNull( eTag );
    Objects.requireNonNull( content );
    try
    {
      final int schemaId = address.getSchemaId();
      final JsPropertyMap<String> index = getIndex( schemaId );
      index.set( address.asChannelDescriptor(), eTag );
      saveIndex( schemaId, index );
      getStorage().setItem( address.getCacheKey(), JSON.stringify( content ) );
      return true;
    }
    catch ( final Throwable e )
    {
      // This exception can occur when storage is full
      invalidate( address );
      return false;
    }
  }

  private void saveIndex( final int schemaId, @Nonnull final JsPropertyMap<String> index )
  {
    final Storage storage = getStorage();
    final String key = indexKey( schemaId );
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
    final int schemaId = address.getSchemaId();
    final JsPropertyMap<String> index = findIndex( schemaId );
    final String key = address.asChannelDescriptor();
    if ( null == index || null == index.get( key ) )
    {
      return false;
    }
    else
    {
      index.delete( key );
      saveIndex( schemaId, index );
      getStorage().removeItem( address.getCacheKey() );
      return true;
    }
  }

  @Nonnull
  Storage getStorage()
  {
    return _storage;
  }

  @Nonnull
  private JsPropertyMap<String> getIndex( final int schemaId )
  {
    final JsPropertyMap<String> index = findIndex( schemaId );
    return null == index ? Js.uncheckedCast( JsPropertyMap.of() ) : index;
  }

  @Nullable
  private JsPropertyMap<String> findIndex( final int schemaId )
  {
    final String indexData = _storage.getItem( indexKey( schemaId ) );
    return null == indexData ? null : Js.uncheckedCast( JSON.parse( indexData ) );
  }

  @Nonnull
  private String indexKey( final int schemaId )
  {
    return ETAG_INDEX + '-' + schemaId;
  }
}
