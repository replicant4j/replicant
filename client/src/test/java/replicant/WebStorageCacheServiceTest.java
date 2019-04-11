package replicant;

import elemental2.dom.DomGlobal;
import elemental2.webstorage.Storage;
import elemental2.webstorage.WebStorageWindow;
import javax.annotation.Nonnull;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
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

    assertNull( Replicant.context().getCacheService() );

    WebStorageCacheService.install();

    final CacheService cacheService = Replicant.context().getCacheService();
    assertNotNull( cacheService );
    assertTrue( cacheService instanceof WebStorageCacheService );
    assertEquals( ( (WebStorageCacheService) cacheService ).getStorage(), storage );
  }

  @Test
  public void install_withSpecificStorage()
  {
    assertNull( Replicant.context().getCacheService() );

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

    assertNull( Replicant.context().getCacheService() );

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
    assertFalse( WebStorageCacheService.isSupported() );

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

    assertTrue( WebStorageCacheService.isSupported() );

    assertEquals( WebStorageCacheService.lookupStorage( DomGlobal.window ), window.localStorage );
  }

  @Test
  public void lookupStorageWithSessionStorage()
  {
    final WebStorageWindow window = new WebStorageWindow();
    window.sessionStorage = mock( Storage.class );
    DomGlobal.window = window;

    assertTrue( WebStorageCacheService.isSupported() );

    assertEquals( WebStorageCacheService.lookupStorage( DomGlobal.window ), window.sessionStorage );
  }

  @Test
  public void invalidate_whenNotPresent()
  {
    final Storage storage = mock( Storage.class );

    final WebStorageCacheService service = new WebStorageCacheService( storage );

    final ChannelAddress address = newAddress();

    when( storage.getItem( WebStorageCacheService.ETAG_INDEX + '-' + address.getSystemId() ) ).thenReturn( null );

    assertFalse( service.invalidate( address ) );

    verify( storage ).getItem( WebStorageCacheService.ETAG_INDEX + '-' + address.getSystemId() );
  }

  @Nonnull
  private ChannelAddress newAddress()
  {
    return new ChannelAddress( ValueUtil.randomInt(), ValueUtil.randomInt() );
  }
}
