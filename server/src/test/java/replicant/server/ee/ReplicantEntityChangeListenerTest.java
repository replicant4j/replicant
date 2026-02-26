package replicant.server.ee;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.guiceyloops.server.TestInitialContextFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import replicant.server.EntityMessage;
import replicant.server.transport.ReplicantChangeRecorder;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantEntityChangeListenerTest
{
  @BeforeMethod
  public void setup()
  {
    RegistryUtil.bind();
    EntityMessageCacheUtil.removeEntityMessageSet();
  }

  @AfterMethod
  public void teardown()
  {
    TestInitialContextFactory.reset();
  }

  @Test
  public void postUpdate_mergesEntityMessageWhenPresent()
  {
    final var registry = TransactionSynchronizationRegistryUtil.lookup();
    final var recorder = mock( ReplicantChangeRecorder.class );
    final var listener = newListener( registry, recorder );
    final var entity = new Object();

    when( recorder.convertToEntityMessage( entity, true ) )
      .thenReturn( new EntityMessage( 11, 7, 0L, new HashMap<>(), Map.of( "a", "b" ) ) );

    listener.postUpdate( entity );

    final var set = EntityMessageCacheUtil.lookupEntityMessageSet();
    assertNotNull( set );
    assertTrue( set.containsEntityMessage( 7, 11 ) );
    verify( recorder ).convertToEntityMessage( entity, true );
  }

  @Test
  public void postUpdate_ignoresEventWhenRollbackOnly()
  {
    final var registry = TransactionSynchronizationRegistryUtil.lookup();
    registry.setRollbackOnly();

    final var recorder = mock( ReplicantChangeRecorder.class );
    final var listener = newListener( registry, recorder );

    listener.postUpdate( new Object() );

    assertNull( EntityMessageCacheUtil.lookupEntityMessageSet() );
    verifyNoInteractions( recorder );
  }

  @Test
  public void preRemove_mergesEntityMessageWhenPresent()
  {
    final var registry = TransactionSynchronizationRegistryUtil.lookup();
    final var recorder = mock( ReplicantChangeRecorder.class );
    final var listener = newListener( registry, recorder );
    final var entity = new Object();
    final var routingKeys = new HashMap<String, Serializable>();

    when( recorder.convertToEntityMessage( entity, false ) )
      .thenReturn( new EntityMessage( 12, 8, 0L, routingKeys, null ) );

    listener.preRemove( entity );

    final var set = EntityMessageCacheUtil.lookupEntityMessageSet();
    assertNotNull( set );
    assertTrue( set.containsEntityMessage( 8, 12 ) );
    verify( recorder ).convertToEntityMessage( entity, false );
  }

  @Test
  public void preRemove_ignoresNullEntityMessage()
  {
    final var registry = TransactionSynchronizationRegistryUtil.lookup();
    final var recorder = mock( ReplicantChangeRecorder.class );
    final var listener = newListener( registry, recorder );
    final var entity = new Object();

    when( recorder.convertToEntityMessage( entity, false ) ).thenReturn( null );

    listener.preRemove( entity );

    assertNull( EntityMessageCacheUtil.lookupEntityMessageSet() );
    verify( recorder ).convertToEntityMessage( entity, false );
  }

  @Nonnull
  private ReplicantEntityChangeListener newListener( @Nonnull final TransactionSynchronizationRegistry registry,
                                                     @Nonnull final ReplicantChangeRecorder recorder )
  {
    final var listener = new ReplicantEntityChangeListener();
    setField( listener, "_registry", registry );
    setField( listener, "_recorder", recorder );
    return listener;
  }

  private void setField( @Nonnull final Object target, @Nonnull final String name, @Nullable final Object value )
  {
    try
    {
      final Field field = ReplicantEntityChangeListener.class.getDeclaredField( name );
      field.setAccessible( true );
      field.set( target, value );
    }
    catch ( final Exception e )
    {
      throw new AssertionError( e );
    }
  }
}
