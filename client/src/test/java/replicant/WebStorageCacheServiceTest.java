package replicant;

import elemental2.dom.Window;
import elemental2.webstorage.Storage;
import elemental2.webstorage.WebStorageWindow;
import java.lang.reflect.Field;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    setWindow( window );

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

  private void setWindow( @Nullable final Window window )
  {
    try
    {
      final Class<?> type = Class.forName( "elemental2.dom.DomGlobal__Constants" );
      final Field field = type.getDeclaredField( "window" );
      field.setAccessible( true );
      field.set( null, window );
    }
    catch ( final ClassNotFoundException | NoSuchFieldException | IllegalAccessException e )
    {
      throw new IllegalStateException( "Error setting field", e );
    }
  }
}
