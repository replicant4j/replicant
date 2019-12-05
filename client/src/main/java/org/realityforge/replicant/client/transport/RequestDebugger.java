package org.realityforge.replicant.client.transport;

import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class RequestDebugger
{
  protected static final Logger LOG = Logger.getLogger( RequestDebugger.class.getName() );

  public void outputRequests( @Nonnull final String prefix, @Nonnull final ClientSession session )
  {
    LOG.info( prefix + " Request Count: " + session.getRequests().size() );
    for ( final RequestEntry entry : session.getRequests().values() )
    {
      outputRequest( prefix, entry );
    }
  }

  protected void outputRequest( @Nonnull final String prefix, @Nonnull final RequestEntry entry )
  {
    LOG.info( prefix + " Request: " + entry.getRequestKey() +
              " / " + entry.getRequestID() +
              " CacheKey: " + entry.getCacheKey() +
              " CompletionDataPresent?: " + entry.isCompletionDataPresent() +
              " ExpectingResults?: " + entry.isExpectingResults() +
              " NormalCompletion?: " +
              ( entry.isCompletionDataPresent() ? String.valueOf( entry.isNormalCompletion() ) : "?" ) );
  }
}
