package org.realityforge.replicant.server.ee;

import java.lang.reflect.Field;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.server.MessageTestUtil;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicationInterceptorTest
{
  @Test
  public void ensureNoChangesDoesNotResultInSave()
    throws Exception
  {
    final TestInvocationContext context = new TestInvocationContext();
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final EntityManager em = mock( EntityManager.class );
    final TestReplicationInterceptor interceptor = createInterceptor( registry, em );

    when( em.isOpen() ).thenReturn( true );
    final Object result = interceptor.businessIntercept( context );
    verify( em ).flush();

    assertTrue( context.isInvoked() );
    assertEquals( result, TestInvocationContext.RESULT );
    assertNull( interceptor._messages );
  }

  @Test
  public void ensureClosedEntityManagerDoesNotResultInFlushOrSave()
    throws Exception
  {
    final TestInvocationContext context = new TestInvocationContext();
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final EntityManager em = mock( EntityManager.class );
    final TestReplicationInterceptor interceptor = createInterceptor( registry, em );

    when( em.isOpen() ).thenReturn( false );
    final Object result = interceptor.businessIntercept( context );
    verify( em, never() ).flush();

    assertTrue( context.isInvoked() );
    assertEquals( result, TestInvocationContext.RESULT );
    assertNull( interceptor._messages );
  }

  @Test
  public void ensureChangesResultInSave()
    throws Exception
  {
    final TestInvocationContext context = new TestInvocationContext();
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final EntityManager em = mock( EntityManager.class );
    final TestReplicationInterceptor interceptor = createInterceptor( registry, em );
    final EntityMessage message = MessageTestUtil.createMessage( "ID", 1, 0, "r1", "r2", "a1", "a2" );
    EntityMessageCacheUtil.getEntityMessageSet( registry ).merge( message );

    when( em.isOpen() ).thenReturn( true );
    final Object result = interceptor.businessIntercept( context );
    verify( em ).flush();

    assertTrue( context.isInvoked() );
    assertEquals( result, TestInvocationContext.RESULT );
    assertNotNull( interceptor._messages );
    assertTrue( interceptor._messages.contains( message ) );
  }

  @Test
  public void ensureNestedInvokationDoNotCauseSave()
    throws Exception
  {
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final EntityManager em = mock( EntityManager.class );
    final TestReplicationInterceptor interceptor = createInterceptor( registry, em );
    final EntityMessage message = MessageTestUtil.createMessage( "ID", 1, 0, "r1", "r2", "a1", "a2" );
    EntityMessageCacheUtil.getEntityMessageSet( registry ).merge( message );

    final TestInvocationContext context = new TestInvocationContext()
    {
      public Object proceed()
        throws Exception
      {
        final TestInvocationContext innerContext = new TestInvocationContext();
        final EntityManager em = mock( EntityManager.class );
        final TestReplicationInterceptor innerInterceptor = createInterceptor( registry, em );
        when( em.isOpen() ).thenReturn( true );
        innerInterceptor.businessIntercept( innerContext );
        assertTrue( innerContext.isInvoked() );
        assertNull( innerInterceptor._messages );
        return super.proceed();
      }
    };

    when( em.isOpen() ).thenReturn( true );
    final Object result = interceptor.businessIntercept( context );
    verify( em ).flush();

    assertTrue( context.isInvoked() );
    assertEquals( result, TestInvocationContext.RESULT );
    assertNotNull( interceptor._messages );
    assertTrue( interceptor._messages.contains( message ) );
  }

  @Test
  public void ensureChangesResultInSaveEvenIfException()
    throws Exception
  {
    final TestInvocationContext context = new TestInvocationContext()
    {
      public Object proceed()
        throws Exception
      {
        super.proceed();
        throw new IllegalStateException();
      }
    };
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final EntityManager em = mock( EntityManager.class );
    final TestReplicationInterceptor interceptor = createInterceptor( registry, em );
    final EntityMessage message = MessageTestUtil.createMessage( "ID", 1, 0, "r1", "r2", "a1", "a2" );
    EntityMessageCacheUtil.getEntityMessageSet( registry ).merge( message );

    try
    {
      when( em.isOpen() ).thenReturn( true );
      interceptor.businessIntercept( context );
      fail( "Expected proceed to result in exception" );
    }
    catch ( final IllegalStateException ise )
    {
      assertNull( ise.getMessage() );
    }

    assertTrue( context.isInvoked() );
    assertNotNull( interceptor._messages );
    assertTrue( interceptor._messages.contains( message ) );
  }

  @Test
  public void ensureNoChangesResultIfInRollback()
    throws Exception
  {
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final TestInvocationContext context = new TestInvocationContext()
    {
      public Object proceed()
        throws Exception
      {
        registry.setRollbackOnly();
        return super.proceed();
      }
    };
    final EntityManager em = mock( EntityManager.class );
    final TestReplicationInterceptor interceptor = createInterceptor( registry, em );
    final EntityMessage message = MessageTestUtil.createMessage( "ID", 1, 0, "r1", "r2", "a1", "a2" );
    EntityMessageCacheUtil.getEntityMessageSet( registry ).merge( message );

    when( em.isOpen() ).thenReturn( true );
    final Object result = interceptor.businessIntercept( context );
    verify( em ).flush();

    assertTrue( context.isInvoked() );
    assertNull( interceptor._messages );
    assertEquals( result, TestInvocationContext.RESULT );
  }

  @Test
  public void ensureNoSaveIfNoChanges()
    throws Exception
  {
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final TestInvocationContext context = new TestInvocationContext();
    final EntityManager em = mock( EntityManager.class );
    final TestReplicationInterceptor interceptor = createInterceptor( registry, em );

    //Create registry but no changes
    EntityMessageCacheUtil.getEntityMessageSet( registry );

    when( em.isOpen() ).thenReturn( true );
    final Object result = interceptor.businessIntercept( context );
    verify( em ).flush();

    assertTrue( context.isInvoked() );
    assertNull( interceptor._messages );
    assertEquals( result, TestInvocationContext.RESULT );
  }

  private TestReplicationInterceptor createInterceptor( final TransactionSynchronizationRegistry registry,
                                                        final EntityManager entityManager )
    throws Exception
  {
    final TestReplicationInterceptor interceptor = new TestReplicationInterceptor(entityManager);
    setField( interceptor, "_registry", registry );
    return interceptor;
  }

  private static void setField( final AbstractReplicationInterceptor interceptor,
                                final String fieldName,
                                final Object value )
    throws Exception
  {
    final Field field = AbstractReplicationInterceptor.class.getDeclaredField( fieldName );
    field.setAccessible( true );
    field.set( interceptor, value );
  }

  static class TestReplicationInterceptor
    extends AbstractReplicationInterceptor
    implements EntityMessageEndpoint
  {
    Collection<EntityMessage> _messages;
    EntityManager _entityManager;

    TestReplicationInterceptor( final EntityManager entityManager )
    {
      _entityManager = entityManager;
    }

    @Override
    protected EntityMessageEndpoint getEndpoint()
    {
      return this;
    }

    public void saveEntityMessages( @Nonnull final Collection<EntityMessage> messages )
    {
      if ( null != _messages )
      {
        fail( "saveEntityMessages called multiple times" );
      }
      _messages = messages;
    }

    @Override
    protected EntityManager getEntityManager()
    {
      return _entityManager;
    }
  }
}
