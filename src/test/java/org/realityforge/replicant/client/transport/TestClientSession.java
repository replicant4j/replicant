package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;

public class TestClientSession
  extends ClientSession<TestClientSession, TestGraph>
{
  public TestClientSession()
  {
    this( "MySessionID" );
  }

  public TestClientSession( @Nonnull final AbstractDataLoaderService<TestClientSession, TestGraph> dataLoaderService,
                     @Nonnull final String sessionID )
  {
    super( dataLoaderService, sessionID );
  }

  public TestClientSession( @Nonnull final String sessionID )
  {
    this( new TestDataLoadService(), sessionID );
  }
}
