package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.shared.GwtRpcConstants;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.Replicant;
import replicant.ReplicantContext;
import replicant.Request;
import replicant.SystemSchema;
import static org.mockito.Mockito.*;

public class ReplicantRpcRequestBuilderTest
  extends AbstractReplicantTest
{
  @Test
  public void onSuccess()
  {
    final SystemSchema schema = newSchema();
    newConnection( createConnector( schema ) );

    final int schemaId = schema.getId();
    final ReplicantContext context = Replicant.context();
    context.request( schemaId, ValueUtil.randomString(), () -> {
      final Request r = context.currentRequest( schemaId );

      final RequestBuilder rb = mock( RequestBuilder.class );
      final RequestCallback chainedCallback = mock( RequestCallback.class );

      new ReplicantRpcRequestBuilder( ValueUtil.randomString(), schemaId ).doSetCallback( rb, chainedCallback );

      verify( rb ).setCallback( any( ReplicantRequestCallback.class ) );
      verify( rb ).setHeader( eq( GwtRpcConstants.CONNECTION_ID_HEADER ), eq( r.getConnectionId() ) );
      verify( rb ).setHeader( eq( GwtRpcConstants.REQUEST_ID_HEADER ), eq( String.valueOf( r.getRequestId() ) ) );
    } );
  }

  @Test
  public void onSuccess_NoRequest()
  {
    final RequestBuilder rb = mock( RequestBuilder.class );
    final RequestCallback callback = mock( RequestCallback.class );

    final SystemSchema schema = newSchema();
    createConnector( schema );
    new ReplicantRpcRequestBuilder( ValueUtil.randomString(), schema.getId() ).doSetCallback( rb, callback );

    verify( rb ).setCallback( eq( callback ) );
    verify( rb, never() ).setHeader( eq( GwtRpcConstants.CONNECTION_ID_HEADER ), anyString() );
    verify( rb, never() ).setHeader( eq( GwtRpcConstants.REQUEST_ID_HEADER ), anyString() );
  }
}
