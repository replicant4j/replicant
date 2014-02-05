package org.realityforge.replicant.server.ee;

import java.lang.reflect.Field;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.server.MessageTestUtil;
import org.realityforge.replicant.shared.transport.ReplicantContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicationInterceptorTest
{
  @BeforeMethod
  public void setup()
  {
    ReplicantContextHolder.clean();
  }

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
    assertEquals( ReplicantContextHolder.get( ReplicantContext.REQUEST_COMPLETE_KEY ), "1" );
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
    ReplicantContextHolder.put( ReplicantContext.SESSION_ID_KEY, "s1" );
    ReplicantContextHolder.put( ReplicantContext.REQUEST_ID_KEY, "r1" );
    final Object result = interceptor.businessIntercept( context );
    verify( em ).flush();

    // Make sure clear is called
    assertNull( ReplicantContextHolder.get( ReplicantContext.SESSION_ID_KEY ) );

    assertTrue( context.isInvoked() );
    assertEquals( result, TestInvocationContext.RESULT );
    assertEquals( interceptor._sessionID, "s1" );
    assertEquals( interceptor._requestID, "r1" );
    assertNotNull( interceptor._messages );
    assertTrue( interceptor._messages.contains( message ) );
    assertEquals( ReplicantContextHolder.get( ReplicantContext.REQUEST_COMPLETE_KEY ), "0" );
  }

  @Test
  public void ensureSessionChangesResultInSave()
    throws Exception
  {
    final TestInvocationContext context = new TestInvocationContext();
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final EntityManager em = mock( EntityManager.class );
    final TestReplicationInterceptor interceptor = createInterceptor( registry, em );
    final EntityMessage message = MessageTestUtil.createMessage( "ID", 1, 0, "r1", "r2", "a1", "a2" );
    EntityMessageCacheUtil.getSessionEntityMessageSet( registry ).merge( message );

    when( em.isOpen() ).thenReturn( true );
    ReplicantContextHolder.put( ReplicantContext.SESSION_ID_KEY, "s1" );
    ReplicantContextHolder.put( ReplicantContext.REQUEST_ID_KEY, "r1" );
    final Object result = interceptor.businessIntercept( context );
    verify( em ).flush();

    // Make sure clear is called
    assertNull( ReplicantContextHolder.get( ReplicantContext.SESSION_ID_KEY ) );

    assertTrue( context.isInvoked() );
    assertEquals( result, TestInvocationContext.RESULT );
    assertEquals( interceptor._sessionID, "s1" );
    assertEquals( interceptor._requestID, "r1" );
    assertNotNull( interceptor._messages );
    assertTrue( interceptor._sessionMessages.contains( message ) );
    assertEquals( ReplicantContextHolder.get( ReplicantContext.REQUEST_COMPLETE_KEY ), "0" );
  }

  @Test
  public void ensureChanges_willBeCompleteIfNotRoutedToInitiatingSession()
    throws Exception
  {
    final TestInvocationContext context = new TestInvocationContext();
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final EntityManager em = mock( EntityManager.class );
    final TestReplicationInterceptor interceptor = createInterceptor( registry, em, false );
    final EntityMessage message = MessageTestUtil.createMessage( "ID", 1, 0, "r1", "r2", "a1", "a2" );
    EntityMessageCacheUtil.getEntityMessageSet( registry ).merge( message );

    when( em.isOpen() ).thenReturn( true );

    interceptor.businessIntercept( context );

    assertTrue( context.isInvoked() );
    assertEquals( ReplicantContextHolder.get( ReplicantContext.REQUEST_COMPLETE_KEY ), "1" );
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
        assertNull( interceptor._sessionID );
        assertNull( interceptor._requestID );
        assertNull( innerInterceptor._messages );
        assertNull( ReplicantContextHolder.get( ReplicantContext.REQUEST_COMPLETE_KEY ) );
        return super.proceed();
      }
    };

    when( em.isOpen() ).thenReturn( true );
    ReplicantContextHolder.put( ReplicantContext.SESSION_ID_KEY, "s1" );
    ReplicantContextHolder.put( ReplicantContext.REQUEST_ID_KEY, "r1" );
    final Object result = interceptor.businessIntercept( context );
    verify( em ).flush();

    // Make sure clear is called
    assertNull( ReplicantContextHolder.get( ReplicantContext.SESSION_ID_KEY ) );

    assertTrue( context.isInvoked() );
    assertEquals( result, TestInvocationContext.RESULT );
    assertEquals( interceptor._sessionID, "s1" );
    assertEquals( interceptor._requestID, "r1" );
    assertNotNull( interceptor._messages );
    assertEquals( ReplicantContextHolder.get( ReplicantContext.REQUEST_COMPLETE_KEY ), "0" );
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
    assertNull( interceptor._sessionID );
    assertNull( interceptor._requestID );
    assertNotNull( interceptor._messages );
    assertTrue( interceptor._messages.contains( message ) );
    assertEquals( ReplicantContextHolder.get( ReplicantContext.REQUEST_COMPLETE_KEY ), "0" );
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
    assertNull( interceptor._sessionID );
    assertNull( interceptor._requestID );
    assertNull( interceptor._messages );
    assertEquals( result, TestInvocationContext.RESULT );
    assertEquals( ReplicantContextHolder.get( ReplicantContext.REQUEST_COMPLETE_KEY ), "1" );
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
    assertNull( interceptor._sessionID );
    assertNull( interceptor._requestID );
    assertNull( interceptor._messages );
    assertEquals( ReplicantContextHolder.get( ReplicantContext.REQUEST_COMPLETE_KEY ), "1" );
    assertEquals( result, TestInvocationContext.RESULT );
  }

  private TestReplicationInterceptor createInterceptor( final TransactionSynchronizationRegistry registry,
                                                        final EntityManager entityManager )
    throws Exception
  {
    return createInterceptor( registry, entityManager, true );
  }

  private TestReplicationInterceptor createInterceptor( final TransactionSynchronizationRegistry registry,
                                                        final EntityManager entityManager,
                                                        final boolean routeToSession )
    throws Exception
  {
    final TestReplicationInterceptor interceptor = new TestReplicationInterceptor( entityManager, routeToSession );
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
    String _sessionID;
    String _requestID;
    Collection<EntityMessage> _messages;
    Collection<EntityMessage> _sessionMessages;
    EntityManager _entityManager;
    private final boolean _routeToSession;

    TestReplicationInterceptor( final EntityManager entityManager, final boolean routeToSession )
    {
      _entityManager = entityManager;
      _routeToSession = routeToSession;
    }

    @Override
    protected EntityMessageEndpoint getEndpoint()
    {
      return this;
    }

    @Override
    public boolean saveEntityMessages( @Nullable final String sessionID,
                                       @Nullable final String requestID,
                                       @Nonnull final Collection<EntityMessage> messages,
                                       @Nonnull final Collection<EntityMessage> sessionMessages )
    {
      if ( null != _messages )
      {
        fail( "saveEntityMessages called multiple times" );
      }
      _sessionID = sessionID;
      _requestID = requestID;
      _messages = messages;
      _sessionMessages = sessionMessages;
      return _routeToSession;
    }

    @Override
    protected EntityManager getEntityManager()
    {
      return _entityManager;
    }
  }
}
