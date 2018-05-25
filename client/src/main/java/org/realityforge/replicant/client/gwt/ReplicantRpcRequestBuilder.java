package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import javax.annotation.Nonnull;
import org.realityforge.replicant.shared.SharedConstants;

public abstract class ReplicantRpcRequestBuilder
  extends RpcRequestBuilder
{
  @Nonnull
  protected abstract replicant.Request getRequest();

  @Override
  protected void doSetCallback( final RequestBuilder rb, final RequestCallback callback )
  {
    final replicant.Request r = getRequest();
    rb.setHeader( SharedConstants.CONNECTION_ID_HEADER, r.getConnectionId() );
    rb.setHeader( SharedConstants.REQUEST_ID_HEADER, String.valueOf( r.getRequestId() ) );
    rb.setCallback( new ReplicantRequestCallback( r, callback ) );
  }
}
