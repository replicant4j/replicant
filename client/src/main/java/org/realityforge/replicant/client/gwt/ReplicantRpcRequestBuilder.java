package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.replicant.shared.SharedConstants;
import replicant.Replicant;
import replicant.ReplicantContext;
import replicant.Request;

public class ReplicantRpcRequestBuilder
  extends RpcRequestBuilder
{
  @Nonnull
  private final String _baseUrl;
  private final int _schemaId;

  public ReplicantRpcRequestBuilder( @Nonnull final String baseUrl, final int schemaId )
  {
    _baseUrl = Objects.requireNonNull( baseUrl );
    _schemaId = schemaId;
  }

  @Override
  protected void doFinish( final RequestBuilder rb )
  {
    super.doFinish( rb );
    rb.setHeader( MODULE_BASE_HEADER, _baseUrl );
  }

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
    final ReplicantContext context = Replicant.context();
    if ( context.hasCurrentRequest( _schemaId ) )
    {
      final Request request = context.currentRequest( _schemaId );
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
