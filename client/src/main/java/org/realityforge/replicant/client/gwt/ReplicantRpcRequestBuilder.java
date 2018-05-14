package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.transport.Connection;
import org.realityforge.replicant.client.transport.SessionContext;
import org.realityforge.replicant.shared.SharedConstants;
import replicant.RequestEntry;

public class ReplicantRpcRequestBuilder
  extends RpcRequestBuilder
{
  @Nonnull
  private final SessionContext _sessionContext;

  public ReplicantRpcRequestBuilder( @Nonnull final SessionContext sessionContext )
  {
    _sessionContext = sessionContext;
  }

  @Override
  protected void doSetCallback( final RequestBuilder rb, final RequestCallback callback )
  {
    final Connection connection = _sessionContext.getConnection();
    if ( null != connection )
    {
      rb.setHeader( SharedConstants.CONNECTION_ID_HEADER, connection.getConnectionId() );
    }
    final RequestEntry entry = _sessionContext.getRequest();
    if ( null == entry )
    {
      rb.setCallback( callback );
    }
    else
    {
      rb.setHeader( SharedConstants.REQUEST_ID_HEADER, entry.getRequestId() );
      rb.setCallback( new RequestCallback()
      {
        @Override
        public void onResponseReceived( final Request request, final Response response )
        {
          final boolean messageComplete = "1".equals( response.getHeader( SharedConstants.REQUEST_COMPLETE_HEADER ) );
          entry.setExpectingResults( !messageComplete );
          if ( null != callback )
          {
            callback.onResponseReceived( request, response );
          }
        }

        @Override
        public void onError( final Request request, final Throwable exception )
        {
          entry.setExpectingResults( false );
          if ( null != callback )
          {
            callback.onError( request, exception );
          }
        }
      } );
    }
  }
}
