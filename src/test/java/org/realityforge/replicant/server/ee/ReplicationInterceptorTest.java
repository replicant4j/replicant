package org.realityforge.replicant.server.ee;

import java.lang.reflect.Field;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.MessageTestUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ReplicationInterceptorTest
{
  @Test
  public void ensureNoChangesDoesNotResultInSave()
      throws Exception
  {
    final TestInvocationContext context = new TestInvocationContext();
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final TestReplicationInterceptor interceptor = createInterceptor( registry );

    final Object result = interceptor.businessIntercept( context );

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
    final TestReplicationInterceptor interceptor = createInterceptor( registry );
    final EntityMessage message = MessageTestUtil.createMessage( "ID", 1, "r1", "r2", "a1", "a2" );
    EntityMessageCacheUtil.getEntityMessageSet( registry ).merge( message );

    final Object result = interceptor.businessIntercept( context );

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
    final TestReplicationInterceptor interceptor = createInterceptor( registry );
    final EntityMessage message = MessageTestUtil.createMessage( "ID", 1, "r1", "r2", "a1", "a2" );
    EntityMessageCacheUtil.getEntityMessageSet( registry ).merge( message );

    try
    {
      interceptor.businessIntercept( context );
      fail( "Expected proceed to result in exception" );
    }
    catch( final IllegalStateException ise )
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
    final TestReplicationInterceptor interceptor = createInterceptor( registry );
    final EntityMessage message = MessageTestUtil.createMessage( "ID", 1, "r1", "r2", "a1", "a2" );
    EntityMessageCacheUtil.getEntityMessageSet( registry ).merge( message );

    final Object result = interceptor.businessIntercept( context );

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
    final TestReplicationInterceptor interceptor = createInterceptor( registry );

    //Create registry but no changes
    EntityMessageCacheUtil.getEntityMessageSet( registry );

    final Object result = interceptor.businessIntercept( context );

    assertTrue( context.isInvoked() );
    assertNull( interceptor._messages );
    assertEquals( result, TestInvocationContext.RESULT );
  }

  private TestReplicationInterceptor createInterceptor( final TransactionSynchronizationRegistry registry )
      throws Exception
  {
    final TestReplicationInterceptor interceptor = new TestReplicationInterceptor();
    final Field field = AbstractReplicationInterceptor.class.getDeclaredField( "_registry" );
    field.setAccessible( true );
    field.set( interceptor, registry );
    return interceptor;
  }

  static class TestReplicationInterceptor
      extends AbstractReplicationInterceptor
  {
    Collection<EntityMessage> _messages;

    protected void saveEntityMessages( @Nonnull final Collection<EntityMessage> messages )
    {
      if( null != _messages )
      {
        fail( "saveEntityMessages called multiple times" );
      }
      _messages = messages;
    }
  }
}
