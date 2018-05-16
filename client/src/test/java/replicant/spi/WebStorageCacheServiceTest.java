package replicant.spi;

import elemental2.dom.DomGlobal;
import elemental2.webstorage.Storage;
import elemental2.webstorage.WebStorageWindow;
import org.mockito.Mockito;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.Replicant;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class WebStorageCacheServiceTest
  extends AbstractReplicantTest
{
  @Test
  public void install_defaults()
  {
    final WebStorageWindow window = new WebStorageWindow();
    final Storage storage = mock( Storage.class );
    window.localStorage = storage;
    DomGlobal.window = window;

    assertEquals( Replicant.context().getCacheService(), null );

    WebStorageCacheService.install();

    final CacheService cacheService = Replicant.context().getCacheService();
    assertNotNull( cacheService );
    assertTrue( cacheService instanceof WebStorageCacheService );
    assertEquals( ( (WebStorageCacheService) cacheService ).getStorage(), storage );
  }

  @Test
  public void install_withSpecificStorage()
  {
    assertEquals( Replicant.context().getCacheService(), null );

    final Storage storage = mock( Storage.class );

    WebStorageCacheService.install( storage );

    final CacheService cacheService = Replicant.context().getCacheService();
    assertNotNull( cacheService );
    assertTrue( cacheService instanceof WebStorageCacheService );
    assertEquals( ( (WebStorageCacheService) cacheService ).getStorage(), storage );
  }

  @Test
  public void install_withSpecificWindow()
  {
    final WebStorageWindow window = new WebStorageWindow();
    final Storage storage = mock( Storage.class );
    window.localStorage = storage;

    assertEquals( Replicant.context().getCacheService(), null );

    WebStorageCacheService.install( window );

    final CacheService cacheService = Replicant.context().getCacheService();
    assertNotNull( cacheService );
    assertTrue( cacheService instanceof WebStorageCacheService );
    assertEquals( ( (WebStorageCacheService) cacheService ).getStorage(), storage );
  }

  @Test
  public void lookupStorageWhenNotSupported()
  {
    DomGlobal.window = new WebStorageWindow();
    assertEquals( WebStorageCacheService.isSupported(), false );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> WebStorageCacheService.lookupStorage( DomGlobal.window ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0026: Attempted to create WebStorageCacheService on window that does not support WebStorage" );
  }

  @Test
  public void lookupStorageWithLocalStorage()
  {
    final WebStorageWindow window = new WebStorageWindow();
    window.localStorage = mock( Storage.class );
    DomGlobal.window = window;

    assertEquals( WebStorageCacheService.isSupported(), true );

    assertEquals( WebStorageCacheService.lookupStorage( DomGlobal.window ), window.localStorage );
  }

  @Test
  public void lookupStorageWithSessionStorage()
  {
    final WebStorageWindow window = new WebStorageWindow();
    window.sessionStorage = mock( Storage.class );
    DomGlobal.window = window;

    assertEquals( WebStorageCacheService.isSupported(), true );

    assertEquals( WebStorageCacheService.lookupStorage( DomGlobal.window ), window.sessionStorage );
  }

  @Test
  public void invalidate_whenNotPresent()
  {
    final Storage storage = mock( Storage.class );

    final WebStorageCacheService service = new WebStorageCacheService( storage );

    final String key = ValueUtil.randomString();

    when( storage.getItem( key + WebStorageCacheService.ETAG_SUFFIX ) ).thenReturn( null );

    final boolean removed = service.invalidate( key );

    assertEquals( removed, false );

    verify( storage ).getItem( key + WebStorageCacheService.ETAG_SUFFIX );
  }

  @Test
  public void invalidate_whenPresent()
  {
    final Storage storage = mock( Storage.class );

    final WebStorageCacheService service = new WebStorageCacheService( storage );

    final String key = ValueUtil.randomString();

    when( storage.getItem( key + WebStorageCacheService.ETAG_SUFFIX ) )
      .thenReturn( ValueUtil.randomString() );

    final boolean removed = service.invalidate( key );

    assertEquals( removed, true );

    verify( storage ).getItem( key + WebStorageCacheService.ETAG_SUFFIX );
    verify( storage ).removeItem( key + WebStorageCacheService.ETAG_SUFFIX );
    verify( storage ).removeItem( key );
  }

  @Test
  public void store_whenPresent()
  {
    final Storage storage = mock( Storage.class );

    final WebStorageCacheService service = new WebStorageCacheService( storage );

    final String key = ValueUtil.randomString();
    final String eTag = ValueUtil.randomString();
    final String content = ValueUtil.randomString();

    final boolean stored = service.store( key, eTag, content );

    assertEquals( stored, true );

    verify( storage ).setItem( key + WebStorageCacheService.ETAG_SUFFIX, eTag );
    verify( storage ).setItem( key, content );
  }

  @Test
  public void store_generatesError()
  {
    final Storage storage = mock( Storage.class );

    final WebStorageCacheService service = new WebStorageCacheService( storage );

    final String key = ValueUtil.randomString();
    final String eTag = ValueUtil.randomString();
    final String content = ValueUtil.randomString();

    when( storage.getItem( key + WebStorageCacheService.ETAG_SUFFIX ) )
      .thenReturn( null );

    Mockito.doAnswer( i -> {
      throw new IllegalStateException();
    } ).when( storage ).setItem( key + WebStorageCacheService.ETAG_SUFFIX, eTag );

    final boolean stored = service.store( key, eTag, content );

    assertEquals( stored, false );

    verify( storage ).setItem( key + WebStorageCacheService.ETAG_SUFFIX, eTag );
    verify( storage, never() ).setItem( key, content );
    verify( storage ).getItem( key + WebStorageCacheService.ETAG_SUFFIX );
  }

  @Test
  public void lookup()
  {
    final Storage storage = mock( Storage.class );

    final WebStorageCacheService service = new WebStorageCacheService( storage );

    final String key = ValueUtil.randomString();
    final String eTag = ValueUtil.randomString();
    final String content = ValueUtil.randomString();

    when( storage.getItem( key + WebStorageCacheService.ETAG_SUFFIX ) )
      .thenReturn( eTag );
    when( storage.getItem( key ) )
      .thenReturn( content );

    final CacheEntry entry = service.lookup( key );
    assertNotNull( entry );

    assertEquals( entry.getKey(), key );
    assertEquals( entry.getETag(), eTag );
    assertEquals( entry.getContent(), content );

    verify( storage ).getItem( key + WebStorageCacheService.ETAG_SUFFIX );
    verify( storage ).getItem( key );
  }

  @Test
  public void lookup_eTagMissing()
  {
    final Storage storage = mock( Storage.class );

    final WebStorageCacheService service = new WebStorageCacheService( storage );

    final String key = ValueUtil.randomString();
    final String content = ValueUtil.randomString();

    when( storage.getItem( key + WebStorageCacheService.ETAG_SUFFIX ) )
      .thenReturn( null );
    when( storage.getItem( key ) )
      .thenReturn( content );

    final CacheEntry entry = service.lookup( key );
    assertNull( entry );

    verify( storage ).getItem( key + WebStorageCacheService.ETAG_SUFFIX );
    verify( storage ).getItem( key );
  }

  @Test
  public void lookup_ContentMissing()
  {
    final Storage storage = mock( Storage.class );

    final WebStorageCacheService service = new WebStorageCacheService( storage );

    final String key = ValueUtil.randomString();
    final String eTag = ValueUtil.randomString();

    when( storage.getItem( key + WebStorageCacheService.ETAG_SUFFIX ) )
      .thenReturn( eTag );
    when( storage.getItem( key ) )
      .thenReturn( null );

    final CacheEntry entry = service.lookup( key );
    assertNull( entry );

    verify( storage ).getItem( key + WebStorageCacheService.ETAG_SUFFIX );
    verify( storage ).getItem( key );
  }
}
