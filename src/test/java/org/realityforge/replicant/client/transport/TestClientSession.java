package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;

class TestClientSession
  extends ClientSession<TestGraph>
{
  TestClientSession()
  {
    this( "MySessionID" );
  }

  TestClientSession( @Nonnull final String sessionID )
  {
    super( sessionID );
  }
}
