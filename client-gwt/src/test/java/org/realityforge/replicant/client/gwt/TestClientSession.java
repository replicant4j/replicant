package org.realityforge.replicant.client.gwt;

import javax.annotation.Nonnull;
import org.realityforge.replicant.client.transport.AbstractDataLoaderService;
import org.realityforge.replicant.client.transport.ClientSession;
import static org.mockito.Mockito.*;

public class TestClientSession
  extends ClientSession<TestGraph>
{
  public TestClientSession( @Nonnull final AbstractDataLoaderService<TestClientSession, TestGraph> dataLoaderService,
                            @Nonnull final String sessionID )
  {
    super( dataLoaderService, sessionID );
  }

  public TestClientSession( @Nonnull final String sessionID )
  {
    this( mock( AbstractDataLoaderService.class ), sessionID );
  }
}
