package org.realityforge.replicant.client.transport;

import java.util.logging.Logger;
import javax.annotation.Nonnull;
import replicant.Connection;
import replicant.RequestEntry;

public class RequestDebugger
{
  protected static final Logger LOG = Logger.getLogger( RequestDebugger.class.getName() );

  public void outputRequests( @Nonnull final String prefix, @Nonnull final Connection connection )
  {
    LOG.info( prefix + " Request Count: " + connection.getRequests().size() );
    for ( final RequestEntry entry : connection.getRequests().values() )
    {
      outputRequest( prefix, entry );
    }
  }

  protected void outputRequest( @Nonnull final String prefix, @Nonnull final RequestEntry entry )
  {
    LOG.info( prefix + " Request: " + entry.getName() +
              " / " + entry.getRequestId() +
              " CacheKey: " + entry.getCacheKey() +
              " CompletionDataPresent?: " + entry.isCompletionDataPresent() +
              " ExpectingResults?: " + entry.isExpectingResults() +
              " NormalCompletion?: " + ( entry.isCompletionDataPresent() ? entry.isNormalCompletion() : '?' ) );
  }
}
