package org.realityforge.replicant.client.gwt;

import com.google.gwt.storage.client.Storage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.transport.CacheEntry;
import org.realityforge.replicant.client.transport.CacheService;

/**
 * An implementation of the CacheService that uses LocalStorage or SessionStorage if available.
 */
public class LocalCacheService
  implements CacheService
{
  private static final Logger LOG = Logger.getLogger( LocalCacheService.class.getName() );

  private static final String ETAG_SUFFIX = "_ETAG_";
  private boolean _loggedNoLocalStorage;

  /**
   * {@inheritDoc}
   */
  @Override
  public CacheEntry lookup( @Nonnull final String key )
  {
    final Storage storage = getStorage();
    if ( null == storage )
    {
      return null;
    }
    else
    {
      final String eTag = storage.getItem( key + ETAG_SUFFIX );
      final String content = storage.getItem( key );
      return new CacheEntry( key, eTag, content );
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean store( @Nonnull final String key, @Nonnull final String eTag, @Nonnull final String content )
  {
    final Storage storage = getStorage();
    if ( null == storage )
    {
      return false;
    }
    else
    {
      try
      {
        storage.setItem( key + ETAG_SUFFIX, eTag );
        storage.setItem( key, content );
      }
      catch ( final Exception e )
      {
        invalidate( key );
        return false;
      }
      return true;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean invalidate( @Nonnull final String key )
  {
    final Storage storage = getStorage();
    if ( null == storage || null == storage.getItem( key + ETAG_SUFFIX ) )
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

  /**
   * Return Storage for system.
   * Will preferentially use local storage and then session storage.
   *
   * @return the storage instance if available, null otherwise.
   */
  @Nullable
  private Storage getStorage()
  {
    final Storage localStorage = Storage.getLocalStorageIfSupported();
    if ( null != localStorage )
    {
      return localStorage;
    }
    else
    {
      final Storage sessionStorage = Storage.getSessionStorageIfSupported();
      if ( null != sessionStorage )
      {
        return sessionStorage;
      }
      else
      {
        logNoLocalData();
        return null;
      }
    }
  }

  /**
   * Once off warning if unable to retrieve local or session storage.
   */
  private void logNoLocalData()
  {
    if ( !_loggedNoLocalStorage )
    {
      if ( LOG.isLoggable( Level.INFO ) )
      {
        LOG.log( Level.INFO, "Local and Session storage not present. Application data caching disabled." );
      }
      _loggedNoLocalStorage = true;
    }
  }
}
