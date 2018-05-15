package replicant.spi;

import elemental2.dom.DomGlobal;
import elemental2.dom.Window;
import elemental2.webstorage.Storage;
import elemental2.webstorage.WebStorageWindow;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.braincheck.Guards;
import replicant.Replicant;

/**
 * An implementation of the CacheService that uses LocalStorage or SessionStorage.
 * The implementation will preferentially use local storage and then session storage.
 */
public class WebStorageCacheService
  implements CacheService
{
  static final String ETAG_SUFFIX = "_ETAG_";
  @Nonnull
  private final Storage _storage;

  /**
   * Return true if WebStorageCacheService is supported in the current environment.
   *
   * @return true if WebStorageCacheService is supported in the current environment.
   */
  public static boolean isSupported()
  {
    return isSupported( DomGlobal.window );
  }

  /**
   * Return true if WebStorageCacheService is supported for the specified window.
   *
   * @param window the window on which to lookup storage.
   * @return true if WebStorageCacheService is supported for the specified window.
   */
  public static boolean isSupported( @Nonnull final Window window )
  {
    final WebStorageWindow wsWindow = WebStorageWindow.of( Objects.requireNonNull( window ) );
    return null != wsWindow.localStorage || null != wsWindow.sessionStorage;
  }

  WebStorageCacheService( @Nonnull final Window window )
  {
    this( lookupStorage( window ) );
  }

  WebStorageCacheService( @Nonnull final Storage storage )
  {
    _storage = Objects.requireNonNull( storage );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Nullable
  public CacheEntry lookup( @Nonnull final String key )
  {
    Objects.requireNonNull( key );
    final String eTag = _storage.getItem( key + ETAG_SUFFIX );
    final String content = _storage.getItem( key );
    if ( null != eTag && null != content )
    {
      return new CacheEntry( key, eTag, content );
    }
    else
    {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean store( @Nonnull final String key, @Nonnull final String eTag, @Nonnull final String content )
  {
    Objects.requireNonNull( key );
    Objects.requireNonNull( eTag );
    Objects.requireNonNull( content );
    try
    {
      final Storage storage = getStorage();
      storage.setItem( key + ETAG_SUFFIX, eTag );
      storage.setItem( key, content );
      return true;
    }
    catch ( final Throwable e )
    {
      // This exception can occur when storage is full
      invalidate( key );
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean invalidate( @Nonnull final String key )
  {
    Objects.requireNonNull( key );
    final Storage storage = getStorage();
    if ( null == storage.getItem( key + ETAG_SUFFIX ) )
    {
      return false;
    }
    else
    {
      storage.removeItem( key + ETAG_SUFFIX );
      storage.removeItem( key );
      return true;
    }
  }

  @Nonnull
  private static Storage lookupStorage( @Nonnull final Window window )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      Guards.invariant( () -> isSupported( window ),
                        () -> "Replicant-0026: Attempted to create WebStorageCacheService on window that does not support WebStorage" );
    }
    final WebStorageWindow wsWindow = WebStorageWindow.of( Objects.requireNonNull( window ) );
    if ( null != wsWindow.localStorage )
    {
      return wsWindow.localStorage;
    }
    else
    {
      return wsWindow.sessionStorage;
    }
  }

  @Nonnull
  final Storage getStorage()
  {
    return _storage;
  }
}
