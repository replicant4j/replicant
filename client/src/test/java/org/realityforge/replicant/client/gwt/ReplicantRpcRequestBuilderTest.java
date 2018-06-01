package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import javax.annotation.Nullable;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.shared.SharedConstants;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.Replicant;
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
    final replicant.Request r =
      Replicant.context().newRequest( schema.getId(), ValueUtil.randomString() );

    final RequestBuilder rb = mock( RequestBuilder.class );
    final RequestCallback chainedCallback = mock( RequestCallback.class );

    new TestReplicantRpcRequestBuilder( r ).doSetCallback( rb, chainedCallback );

    verify( rb ).setCallback( any( ReplicantRequestCallback.class ) );
    verify( rb ).setHeader( eq( SharedConstants.CONNECTION_ID_HEADER ), eq( r.getConnectionId() ) );
    verify( rb ).setHeader( eq( SharedConstants.REQUEST_ID_HEADER ), eq( String.valueOf( r.getRequestId() ) ) );
  }

  @Test
  public void onSuccess_NoRequest()
  {
    final RequestBuilder rb = mock( RequestBuilder.class );
    final RequestCallback callback = mock( RequestCallback.class );

    new TestReplicantRpcRequestBuilder( null ).doSetCallback( rb, callback );

    verify( rb ).setCallback( eq( callback ) );
    verify( rb, never() ).setHeader( eq( SharedConstants.CONNECTION_ID_HEADER ), anyString() );
    verify( rb, never() ).setHeader( eq( SharedConstants.REQUEST_ID_HEADER ), anyString() );
  }

  static class TestReplicantRpcRequestBuilder
    extends ReplicantRpcRequestBuilder
  {
    @Nullable
    private final replicant.Request _request;

    TestReplicantRpcRequestBuilder( @Nullable final replicant.Request request )
    {
      _request = request;
    }

    @Nullable
    @Override
    protected replicant.Request getRequest()
    {
      return _request;
    }
  }
}
