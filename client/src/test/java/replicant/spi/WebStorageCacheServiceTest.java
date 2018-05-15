package replicant.spi;

import elemental2.dom.DomGlobal;
import elemental2.webstorage.Storage;
import elemental2.webstorage.WebStorageWindow;
import org.mockito.Mockito;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class WebStorageCacheServiceTest
  extends AbstractReplicantTest
{
  @Test
  public void constructWhenNotSupported()
  {
    DomGlobal.window = new WebStorageWindow();
    assertEquals( WebStorageCacheService.isSupported(), false );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> new WebStorageCacheService( DomGlobal.window ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0026: Attempted to create WebStorageCacheService on window that does not support WebStorage" );
  }

  @Test
  public void constructWithLocalStorage()
  {
    final WebStorageWindow window = new WebStorageWindow();
    window.localStorage = mock( Storage.class );
    DomGlobal.window = window;

    assertEquals( WebStorageCacheService.isSupported(), true );

    final WebStorageCacheService service = new WebStorageCacheService( DomGlobal.window );

    assertEquals( service.getStorage(), window.localStorage );
  }

  @Test
  public void constructWithSessionStorage()
  {
    final WebStorageWindow window = new WebStorageWindow();
    window.sessionStorage = mock( Storage.class );
    DomGlobal.window = window;

    assertEquals( WebStorageCacheService.isSupported(), true );

    final WebStorageCacheService service = new WebStorageCacheService( DomGlobal.window );

    assertEquals( service.getStorage(), window.sessionStorage );
  }

  @Test
  public void invalidate_whenNotPresent()
  {
    final WebStorageWindow window = new WebStorageWindow();
    window.localStorage = mock( Storage.class );
    DomGlobal.window = window;

    assertEquals( WebStorageCacheService.isSupported(), true );

    final WebStorageCacheService service = new WebStorageCacheService( DomGlobal.window );

    final String key = ValueUtil.randomString();

    when( window.localStorage.getItem( key + WebStorageCacheService.ETAG_SUFFIX ) ).thenReturn( null );

    final boolean removed = service.invalidate( key );

    assertEquals( removed, false );

    verify( window.localStorage ).getItem( key + WebStorageCacheService.ETAG_SUFFIX );
  }

  @Test
  public void invalidate_whenPresent()
  {
    final WebStorageWindow window = new WebStorageWindow();
    window.localStorage = mock( Storage.class );
    DomGlobal.window = window;

    assertEquals( WebStorageCacheService.isSupported(), true );

    final WebStorageCacheService service = new WebStorageCacheService( DomGlobal.window );

    final String key = ValueUtil.randomString();

    when( window.localStorage.getItem( key + WebStorageCacheService.ETAG_SUFFIX ) )
      .thenReturn( ValueUtil.randomString() );

    final boolean removed = service.invalidate( key );

    assertEquals( removed, true );

    verify( window.localStorage ).getItem( key + WebStorageCacheService.ETAG_SUFFIX );
    verify( window.localStorage ).removeItem( key + WebStorageCacheService.ETAG_SUFFIX );
    verify( window.localStorage ).removeItem( key );
  }

  @Test
  public void store_whenPresent()
  {
    final WebStorageWindow window = new WebStorageWindow();
    window.localStorage = mock( Storage.class );
    DomGlobal.window = window;

    assertEquals( WebStorageCacheService.isSupported(), true );

    final WebStorageCacheService service = new WebStorageCacheService( DomGlobal.window );

    final String key = ValueUtil.randomString();
    final String eTag = ValueUtil.randomString();
    final String content = ValueUtil.randomString();

    final boolean stored = service.store( key, eTag, content );

    assertEquals( stored, true );

    verify( window.localStorage ).setItem( key + WebStorageCacheService.ETAG_SUFFIX, eTag );
    verify( window.localStorage ).setItem( key, content );
  }

  @Test
  public void store_generatesError()
  {
    final WebStorageWindow window = new WebStorageWindow();
    window.localStorage = mock( Storage.class );
    DomGlobal.window = window;

    assertEquals( WebStorageCacheService.isSupported(), true );

    final WebStorageCacheService service = new WebStorageCacheService( DomGlobal.window );

    final String key = ValueUtil.randomString();
    final String eTag = ValueUtil.randomString();
    final String content = ValueUtil.randomString();

    when( window.localStorage.getItem( key + WebStorageCacheService.ETAG_SUFFIX ) )
      .thenReturn( null );

    Mockito.doAnswer( i -> {
      throw new IllegalStateException();
    } ).when( window.localStorage ).setItem( key + WebStorageCacheService.ETAG_SUFFIX, eTag );

    final boolean stored = service.store( key, eTag, content );

    assertEquals( stored, false );

    verify( window.localStorage ).setItem( key + WebStorageCacheService.ETAG_SUFFIX, eTag );
    verify( window.localStorage, never() ).setItem( key, content );
    verify( window.localStorage ).getItem( key + WebStorageCacheService.ETAG_SUFFIX );
  }

  @Test
  public void lookup()
  {
    final WebStorageWindow window = new WebStorageWindow();
    window.localStorage = mock( Storage.class );
    DomGlobal.window = window;

    assertEquals( WebStorageCacheService.isSupported(), true );

    final WebStorageCacheService service = new WebStorageCacheService( DomGlobal.window );

    final String key = ValueUtil.randomString();
    final String eTag = ValueUtil.randomString();
    final String content = ValueUtil.randomString();

    when( window.localStorage.getItem( key + WebStorageCacheService.ETAG_SUFFIX ) )
      .thenReturn( eTag );
    when( window.localStorage.getItem( key ) )
      .thenReturn( content );

    final CacheEntry entry = service.lookup( key );
    assertNotNull( entry );

    assertEquals( entry.getKey(), key );
    assertEquals( entry.getETag(), eTag );
    assertEquals( entry.getContent(), content );

    verify( window.localStorage ).getItem( key + WebStorageCacheService.ETAG_SUFFIX );
    verify( window.localStorage ).getItem( key );
  }

  @Test
  public void lookup_eTagMissing()
  {
    final WebStorageWindow window = new WebStorageWindow();
    window.localStorage = mock( Storage.class );
    DomGlobal.window = window;

    assertEquals( WebStorageCacheService.isSupported(), true );

    final WebStorageCacheService service = new WebStorageCacheService( DomGlobal.window );

    final String key = ValueUtil.randomString();
    final String content = ValueUtil.randomString();

    when( window.localStorage.getItem( key + WebStorageCacheService.ETAG_SUFFIX ) )
      .thenReturn( null );
    when( window.localStorage.getItem( key ) )
      .thenReturn( content );

    final CacheEntry entry = service.lookup( key );
    assertNull( entry );

    verify( window.localStorage ).getItem( key + WebStorageCacheService.ETAG_SUFFIX );
    verify( window.localStorage ).getItem( key );
  }

  @Test
  public void lookup_ContentMissing()
  {
    final WebStorageWindow window = new WebStorageWindow();
    window.localStorage = mock( Storage.class );
    DomGlobal.window = window;

    assertEquals( WebStorageCacheService.isSupported(), true );

    final WebStorageCacheService service = new WebStorageCacheService( DomGlobal.window );

    final String key = ValueUtil.randomString();
    final String eTag = ValueUtil.randomString();

    when( window.localStorage.getItem( key + WebStorageCacheService.ETAG_SUFFIX ) )
      .thenReturn( eTag );
    when( window.localStorage.getItem( key ) )
      .thenReturn( null );

    final CacheEntry entry = service.lookup( key );
    assertNull( entry );

    verify( window.localStorage ).getItem( key + WebStorageCacheService.ETAG_SUFFIX );
    verify( window.localStorage ).getItem( key );
  }
}
