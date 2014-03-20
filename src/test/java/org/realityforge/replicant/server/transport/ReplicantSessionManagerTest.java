package org.realityforge.replicant.server.transport;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.Change;
import org.realityforge.replicant.server.ChangeAccumulatorTest.TestSession;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.ee.ReplicantContextHolder;
import org.realityforge.replicant.server.ee.TestTransactionSynchronizationRegistry;
import org.realityforge.replicant.shared.transport.ReplicantContext;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ReplicantSessionManagerTest
{
  @Test
  public void sendPacket()
    throws Exception
  {
    final TestReplicantSessionManager sm = new TestReplicantSessionManager();
    set( sm, ReplicantSessionManager.class, "_registry", new TestTransactionSynchronizationRegistry() );
    final TestSession session = sm.createSession();

    sm.getRegistry().putResource( ReplicantContext.REQUEST_ID_KEY, "r1" );

    final Packet packet = sm.sendPacket( session, "X", new ArrayList<Change>() );
    assertEquals( packet.getETag(), "X" );
    assertEquals( packet.getRequestID(), "r1" );
    assertEquals( packet.getChanges().size(), 0 );
    assertEquals( ReplicantContextHolder.get( ReplicantContext.REQUEST_COMPLETE_KEY ), "0" );
  }

  @Test
  public void poll()
    throws Exception
  {
    final TestReplicantSessionManager sm = new TestReplicantSessionManager();
    final TestSession session = sm.createSession();
    final Packet p1 = session.getQueue().addPacket( null, null, new ArrayList<Change>() );
    final Packet p2 = session.getQueue().addPacket( null, null, new ArrayList<Change>() );
    final Packet p3 = session.getQueue().addPacket( null, null, new ArrayList<Change>() );

    assertEquals( sm.poll( session, 0 ), p1 );
    assertEquals( sm.poll( session, 0 ), p1 );

    assertEquals( sm.poll( session, p1.getSequence() ), p2 );
    assertEquals( sm.poll( session, p2.getSequence() ), p3 );
    assertEquals( sm.poll( session, p3.getSequence() ), null );
  }

  @Test
  public void ensureSession()
    throws Exception
  {
    final TestReplicantSessionManager sm = new TestReplicantSessionManager();
    try
    {
      sm.ensureSession( "X" );
      fail( "" );
    }
    catch ( final BadSessionException e )
    {
      //Ignore
    }
    final TestSession session = sm.createSession();
    assertEquals( sm.ensureSession( session.getSessionID() ), session );
  }

  private void set( final Object instance, final Class<?> clazz, final String fieldName, final Object value )
    throws Exception
  {
    final Field field = clazz.getDeclaredField( fieldName );
    field.setAccessible( true );
    field.set( instance, value );
  }

  static class TestReplicantSessionManager
    extends ReplicantSessionManager<TestSession>
  {
    @Override
    public boolean saveEntityMessages( @Nullable final String sessionID,
                                       @Nullable final String requestID,
                                       @Nonnull final Collection<EntityMessage> messages,
                                       @Nullable final Collection<Change> sessionMessages )
    {
      return false;
    }

    @Nonnull
    @Override
    protected TestSession newSessionInfo()
    {
      return new TestSession( UUID.randomUUID().toString() );
    }
  }
}
