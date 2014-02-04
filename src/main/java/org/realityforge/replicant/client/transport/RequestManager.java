package org.realityforge.replicant.client.transport;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RequestManager
{
  private final Map<String, RequestEntry> _requests = new HashMap<>();
  private final Map<String, RequestEntry> _roRequests = Collections.unmodifiableMap( _requests );
  private int _requestID;

  @Nonnull
  public final RequestEntry newRequestRegistration( final boolean bulkLoad )
  {
    final RequestEntry entry = new RequestEntry( newRequestID(), bulkLoad );
    _requests.put( entry.getRequestID(), entry );
    return entry;
  }

  @Nullable
  public final RequestEntry getRequest( @Nonnull final String requestID )
  {
    return _requests.get( requestID );
  }

  public Map<String, RequestEntry> getRequests()
  {
    return _requests;
  }

  public final boolean removeRequest( @Nonnull final String requestID )
  {
    return null != _requests.remove( requestID );
  }

  protected String newRequestID()
  {
    return String.valueOf( ++_requestID );
  }
}
