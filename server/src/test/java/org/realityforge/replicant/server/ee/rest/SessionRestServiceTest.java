package org.realityforge.replicant.server.ee.rest;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.ws.rs.core.Response;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.server.transport.ReplicantSession;
import org.realityforge.replicant.server.transport.ReplicantSessionManager;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class SessionRestServiceTest
{
  @Test
  public void poll_dataAvailable()
    throws Exception
  {
    final ReplicantSessionManager sessionManager = mock( ReplicantSessionManager.class );
    final AbstractSessionRestService resource = newResource( sessionManager );

    final ReplicantSession session = new ReplicantSession( null, null );
    when( sessionManager.createSession() ).
      thenReturn( session );

    final Response token = resource.createSession();

    assertEquals( token.getEntity(), session.getSessionID() );
  }

  private AbstractSessionRestService newResource( final ReplicantSessionManager sessionManager )
    throws Exception
  {
    return new AbstractSessionRestService()
    {
      @Nonnull
      @Override
      protected EntityMessageEndpoint getEntityMessageEndpoint()
      {
        return mock( EntityMessageEndpoint.class );
      }

      @Nonnull
      @Override
      protected EntityManager getEntityManager()
      {
        return mock( EntityManager.class );
      }

      @Nonnull
      @Override
      protected ReplicantSessionManager getSessionManager()
      {
        return sessionManager;
      }

      @Nonnull
      @Override
      protected TransactionSynchronizationRegistry getRegistry()
      {
        return mock( TransactionSynchronizationRegistry.class );
      }
    };
  }
}
