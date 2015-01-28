package org.realityforge.replicant.server.ee;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.Change;
import org.realityforge.replicant.server.ChangeSet;
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
    enableReplicationContext( registry );
    EntityMessageCacheUtil.getEntityMessageSet( registry ).merge( message );
    disableReplicationContext( registry );

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
  public void ensureUserCanOverrideRequestCompleteFlag()
    throws Exception
  {
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final TestInvocationContext context = new TestInvocationContext();
    context.setRunnable( new Runnable()
    {
      @Override
      public void run()
      {
        registry.putResource( ReplicantContext.REQUEST_COMPLETE_KEY, Boolean.FALSE );
      }
    } );
    final EntityManager entityManager = mock( EntityManager.class );
    final TestReplicationInterceptor interceptor =
      createInterceptor( registry, entityManager );

    ReplicantContextHolder.put( ReplicantContext.SESSION_ID_KEY, "s1" );
    ReplicantContextHolder.put( ReplicantContext.REQUEST_ID_KEY, "r1" );

    when( entityManager.isOpen() ).thenReturn( true );

    interceptor.businessIntercept( context );

    assertTrue( context.isInvoked() );
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
    enableReplicationContext( registry );
    EntityMessageCacheUtil.getSessionChanges( registry ).merge( new Change( message, 44, 77 ) );
    disableReplicationContext( registry );

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
    assertEquals( interceptor._changeSet.getChanges().size(), 1 );
    final Change change = interceptor._changeSet.getChanges().iterator().next();
    assertEquals( change.getEntityMessage().getID(), message.getID() );
    final Serializable expected = 77;
    assertEquals( change.getChannels().get( 44 ), expected );
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
    enableReplicationContext( registry );
    EntityMessageCacheUtil.getEntityMessageSet( registry ).merge( message );
    disableReplicationContext( registry );

    when( em.isOpen() ).thenReturn( true );

    interceptor.businessIntercept( context );

    assertTrue( context.isInvoked() );
    assertEquals( ReplicantContextHolder.get( ReplicantContext.REQUEST_COMPLETE_KEY ), "1" );
  }

  @Test
  public void ensureNestedInvocationsShouldExcept()
    throws Exception
  {
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final EntityManager em = mock( EntityManager.class );
    final TestReplicationInterceptor interceptor = createInterceptor( registry, em );
    final EntityMessage message = MessageTestUtil.createMessage( "ID", 1, 0, "r1", "r2", "a1", "a2" );
    enableReplicationContext( registry );
    EntityMessageCacheUtil.getEntityMessageSet( registry ).merge( message );
    disableReplicationContext( registry );

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
        return super.proceed();
      }
    };

    when( em.isOpen() ).thenReturn( true );
    ReplicantContextHolder.put( ReplicantContext.SESSION_ID_KEY, "s1" );
    ReplicantContextHolder.put( ReplicantContext.REQUEST_ID_KEY, "r1" );
    try
    {
      interceptor.businessIntercept( context );
    }
    catch ( Exception e )
    {
      return;
    }
    fail( "Expected to generate session due to nested contexts" );
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
    enableReplicationContext( registry );
    EntityMessageCacheUtil.getEntityMessageSet( registry ).merge( message );
    disableReplicationContext( registry );

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
    enableReplicationContext( registry );
    EntityMessageCacheUtil.getEntityMessageSet( registry ).merge( message );
    disableReplicationContext( registry );

    when( em.isOpen() ).thenReturn( true );
    final Object result = interceptor.businessIntercept( context );

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
    enableReplicationContext( registry );
    EntityMessageCacheUtil.getEntityMessageSet( registry );
    disableReplicationContext( registry );

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

  private static void setField( final TestReplicationInterceptor interceptor,
                                final String fieldName,
                                final Object value )
    throws Exception
  {
    final Field field = interceptor.getClass().getSuperclass().getDeclaredField( fieldName );
    field.setAccessible( true );
    field.set( interceptor, value );
  }

  private void enableReplicationContext( final TestTransactionSynchronizationRegistry registry )
  {
    registry.putResource( ReplicantContext.REPLICATION_INVOCATION_KEY, "Test" );
  }

  private void disableReplicationContext( final TestTransactionSynchronizationRegistry registry )
  {
    registry.putResource( ReplicantContext.REPLICATION_INVOCATION_KEY, null );
  }

  static class TestReplicationInterceptor
    extends AbstractReplicationInterceptor
    implements EntityMessageEndpoint
  {
    String _sessionID;
    String _requestID;
    Collection<EntityMessage> _messages;
    EntityManager _entityManager;
    private final boolean _routeToSession;
    private ChangeSet _changeSet;

    TestReplicationInterceptor( final EntityManager entityManager, final boolean routeToSession )
    {
      _entityManager = entityManager;
      _routeToSession = routeToSession;
    }

    @Override
    public boolean saveEntityMessages( @Nullable final String sessionID,
                                       @Nullable final String requestID,
                                       @Nonnull final Collection<EntityMessage> messages,
                                       @Nullable final ChangeSet changeSet )
    {
      if ( null != _messages )
      {
        fail( "saveEntityMessages called multiple times" );
      }
      _sessionID = sessionID;
      _requestID = requestID;
      _messages = messages;
      _changeSet = changeSet;
      return _routeToSession;
    }

    @Override
    protected EntityManager getEntityManager()
    {
      return _entityManager;
    }

    @Override
    protected EntityMessageEndpoint getEndpoint()
    {
      return this;
    }
  }
}
