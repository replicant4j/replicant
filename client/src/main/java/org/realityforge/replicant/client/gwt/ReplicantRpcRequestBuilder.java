package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import javax.annotation.Nullable;
import org.realityforge.replicant.shared.SharedConstants;

public abstract class ReplicantRpcRequestBuilder
  extends RpcRequestBuilder
{
  @Nullable
  protected abstract replicant.Request getRequest();

  @Override
  protected void doSetCallback( final RequestBuilder rb, final RequestCallback callback )
  {
    /*
     * The request may be null if the service is not replicant enabled.
     * In which case we assume that we can just treat it like a normal
     * gwt-rpc call.
     *
     * Note: A non-replicant enabled method can occur on an otherwise replicant enabled service.
     */
    final replicant.Request request = getRequest();
    if ( null != request )
    {
      rb.setHeader( SharedConstants.CONNECTION_ID_HEADER, request.getConnectionId() );
      rb.setHeader( SharedConstants.REQUEST_ID_HEADER, String.valueOf( request.getRequestId() ) );
      rb.setCallback( new ReplicantRequestCallback( request, callback ) );
    }
    else
    {
      super.doSetCallback( rb, callback );
    }
  }
}
