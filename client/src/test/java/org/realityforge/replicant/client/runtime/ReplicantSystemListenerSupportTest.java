package org.realityforge.replicant.client.runtime;

import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantSystemListenerSupportTest
{
  @Test
  public void basicOperation()
  {
    final ReplicantSystemListenerSupport support = new ReplicantSystemListenerSupport();

    final ReplicantClientSystem service = mock( ReplicantClientSystem.class );

    final ReplicantSystemListener listener = mock( ReplicantSystemListener.class );

    assertEquals( support.getListeners().size(), 0 );

    support.addListener( listener );

    assertEquals( support.getListeners().size(), 1 );

    reset( listener );
    support.stateChanged( service, ReplicantClientSystem.State.CONNECTED, ReplicantClientSystem.State.DISCONNECTED );
    verify( listener ).stateChanged( service,
                                     ReplicantClientSystem.State.CONNECTED,
                                     ReplicantClientSystem.State.DISCONNECTED );

    assertEquals( support.getListeners().size(), 1 );

    support.removeListener( listener );

    assertEquals( support.getListeners().size(), 0 );

    reset( listener );
    support.stateChanged( service, ReplicantClientSystem.State.CONNECTED, ReplicantClientSystem.State.DISCONNECTED );
    verify( listener, never() ).stateChanged( service,
                                              ReplicantClientSystem.State.CONNECTED,
                                              ReplicantClientSystem.State.DISCONNECTED );
  }
}
