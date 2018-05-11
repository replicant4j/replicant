package org.realityforge.replicant.server.ee;

import java.io.Serializable;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.guiceyloops.server.TestTransactionSynchronizationRegistry;
import org.realityforge.replicant.server.Change;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.server.MessageTestUtil;
import org.realityforge.replicant.server.ServerConstants;
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
    assertEquals( ReplicantContextHolder.get( ServerConstants.REQUEST_COMPLETE_KEY ), "1" );
  }

  @Test
  public void ensureChangesResultInSave()
    throws Exception
  {
    final TestInvocationContext context = new TestInvocationContext();
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final EntityManager em = mock( EntityManager.class );
    final TestReplicationInterceptor interceptor = createInterceptor( registry, em );
    final EntityMessage message = MessageTestUtil.createMessage( 17, 1, 0, "r1", "r2", "a1", "a2" );
    enableReplicationContext( registry );
    EntityMessageCacheUtil.getEntityMessageSet( registry ).merge( message );
    disableReplicationContext( registry );

    when( em.isOpen() ).thenReturn( true );
    ReplicantContextHolder.put( ServerConstants.SESSION_ID_KEY, "s1" );
    ReplicantContextHolder.put( ServerConstants.REQUEST_ID_KEY, "r1" );
    final Object result = interceptor.businessIntercept( context );
    verify( em ).flush();

    // Make sure clear is called
    assertNull( ReplicantContextHolder.get( ServerConstants.SESSION_ID_KEY ) );

    assertTrue( context.isInvoked() );
    assertEquals( result, TestInvocationContext.RESULT );
    assertEquals( interceptor._sessionID, "s1" );
    assertEquals( interceptor._requestID, "r1" );
    assertNotNull( interceptor._messages );
    assertTrue( interceptor._messages.contains( message ) );
    assertEquals( ReplicantContextHolder.get( ServerConstants.REQUEST_COMPLETE_KEY ), "0" );
  }

  @Test
  public void ensureUserCanOverrideRequestCompleteFlag()
    throws Exception
  {
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final TestInvocationContext context = new TestInvocationContext();
    context.setRunnable( () -> registry.putResource( ServerConstants.REQUEST_COMPLETE_KEY, Boolean.FALSE ) );
    final EntityManager entityManager = mock( EntityManager.class );
    final TestReplicationInterceptor interceptor =
      createInterceptor( registry, entityManager );

    ReplicantContextHolder.put( ServerConstants.SESSION_ID_KEY, "s1" );
    ReplicantContextHolder.put( ServerConstants.REQUEST_ID_KEY, "r1" );

    when( entityManager.isOpen() ).thenReturn( true );

    interceptor.businessIntercept( context );

    assertTrue( context.isInvoked() );
    assertEquals( ReplicantContextHolder.get( ServerConstants.REQUEST_COMPLETE_KEY ), "0" );
  }

  @Test
  public void ensureSessionChangesResultInSave()
    throws Exception
  {
    final TestInvocationContext context = new TestInvocationContext();
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final EntityManager em = mock( EntityManager.class );
    final TestReplicationInterceptor interceptor = createInterceptor( registry, em );
    final EntityMessage message = MessageTestUtil.createMessage( 17, 1, 0, "r1", "r2", "a1", "a2" );
    enableReplicationContext( registry );
    EntityMessageCacheUtil.getSessionChanges( registry ).merge( new Change( message, 44, 77 ) );
    disableReplicationContext( registry );

    when( em.isOpen() ).thenReturn( true );
    ReplicantContextHolder.put( ServerConstants.SESSION_ID_KEY, "s1" );
    ReplicantContextHolder.put( ServerConstants.REQUEST_ID_KEY, "r1" );
    final Object result = interceptor.businessIntercept( context );
    verify( em ).flush();

    // Make sure clear is called
    assertNull( ReplicantContextHolder.get( ServerConstants.SESSION_ID_KEY ) );

    assertTrue( context.isInvoked() );
    assertEquals( result, TestInvocationContext.RESULT );
    assertEquals( interceptor._sessionID, "s1" );
    assertEquals( interceptor._requestID, "r1" );
    assertNotNull( interceptor._messages );
    assertEquals( interceptor._changeSet.getChanges().size(), 1 );
    final Change change = interceptor._changeSet.getChanges().iterator().next();
    assertEquals( change.getEntityMessage().getId(), message.getId() );
    final Serializable expected = 77;
    assertEquals( change.getChannels().get( 44 ), expected );
    assertEquals( ReplicantContextHolder.get( ServerConstants.REQUEST_COMPLETE_KEY ), "0" );
  }

  @Test
  public void ensureChanges_willBeCompleteIfNotRoutedToInitiatingSession()
    throws Exception
  {
    final TestInvocationContext context = new TestInvocationContext();
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final EntityManager em = mock( EntityManager.class );
    final TestReplicationInterceptor interceptor = createInterceptor( registry, em, false );
    final EntityMessage message = MessageTestUtil.createMessage( 17, 1, 0, "r1", "r2", "a1", "a2" );
    enableReplicationContext( registry );
    EntityMessageCacheUtil.getEntityMessageSet( registry ).merge( message );
    disableReplicationContext( registry );

    when( em.isOpen() ).thenReturn( true );

    interceptor.businessIntercept( context );

    assertTrue( context.isInvoked() );
    assertEquals( ReplicantContextHolder.get( ServerConstants.REQUEST_COMPLETE_KEY ), "1" );
  }

  @Test
  public void ensureNestedInvocationsShouldExcept()
    throws Exception
  {
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final EntityManager em = mock( EntityManager.class );
    final TestReplicationInterceptor interceptor = createInterceptor( registry, em );
    final EntityMessage message = MessageTestUtil.createMessage( 17, 1, 0, "r1", "r2", "a1", "a2" );
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
    ReplicantContextHolder.put( ServerConstants.SESSION_ID_KEY, "s1" );
    ReplicantContextHolder.put( ServerConstants.REQUEST_ID_KEY, "r1" );
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
    final EntityMessage message = MessageTestUtil.createMessage( 17, 1, 0, "r1", "r2", "a1", "a2" );
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
    assertEquals( ReplicantContextHolder.get( ServerConstants.REQUEST_COMPLETE_KEY ), "0" );
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
    final EntityMessage message = MessageTestUtil.createMessage( 17, 1, 0, "r1", "r2", "a1", "a2" );
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
    assertEquals( ReplicantContextHolder.get( ServerConstants.REQUEST_COMPLETE_KEY ), "1" );
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
    assertEquals( ReplicantContextHolder.get( ServerConstants.REQUEST_COMPLETE_KEY ), "1" );
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
    return new TestReplicationInterceptor( entityManager, registry, routeToSession );
  }

  private void enableReplicationContext( final TestTransactionSynchronizationRegistry registry )
  {
    registry.putResource( ServerConstants.REPLICATION_INVOCATION_KEY, "Test" );
  }

  private void disableReplicationContext( final TestTransactionSynchronizationRegistry registry )
  {
    registry.putResource( ServerConstants.REPLICATION_INVOCATION_KEY, null );
  }

  static class TestReplicationInterceptor
    extends AbstractReplicationInterceptor
    implements EntityMessageEndpoint
  {
    private String _sessionID;
    private String _requestID;
    private Collection<EntityMessage> _messages;
    private EntityManager _entityManager;
    private final TransactionSynchronizationRegistry _registry;
    private final boolean _routeToSession;
    private ChangeSet _changeSet;

    private TestReplicationInterceptor( final EntityManager entityManager,
                                        final TransactionSynchronizationRegistry registry,
                                        final boolean routeToSession )
    {
      _entityManager = entityManager;
      _registry = registry;
      _routeToSession = routeToSession;
    }

    @Nonnull
    @Override
    protected TransactionSynchronizationRegistry getRegistry()
    {
      return _registry;
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

    @Nonnull
    @Override
    protected EntityManager getEntityManager()
    {
      return _entityManager;
    }

    @Nonnull
    @Override
    protected EntityMessageEndpoint getEndpoint()
    {
      return this;
    }
  }
}
