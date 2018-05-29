package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.shared.SharedConstants;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.Replicant;
import replicant.SystemSchema;
import replicant.TestConnector;
import static org.mockito.Mockito.*;

public class ReplicantRpcRequestBuilderTest
  extends AbstractReplicantTest
{
  @Test
  public void onSuccess()
  {
    final SystemSchema schema = newSchema();
    final TestConnector connector = TestConnector.create( schema );
    newConnection( connector );
    final replicant.Request r =
      Replicant.context().newRequest( schema.getId(), ValueUtil.randomString() );

    final RequestBuilder rb = mock( RequestBuilder.class );
    final RequestCallback chainedCallback = mock( RequestCallback.class );

    final AtomicReference<RequestCallback> callback = new AtomicReference<>();
    doAnswer( i -> {
      callback.set( (RequestCallback) i.getArguments()[ 0 ] );
      return null;
    } ).when( rb ).setCallback( any( RequestCallback.class ) );

    new TestReplicantRpcRequestBuilder( r ).doSetCallback( rb, chainedCallback );

    verify( rb ).setCallback( any( ReplicantRequestCallback.class ) );
    verify( rb ).setHeader( eq( SharedConstants.CONNECTION_ID_HEADER ), eq( r.getConnectionId() ) );
    verify( rb ).setHeader( eq( SharedConstants.REQUEST_ID_HEADER ), eq( String.valueOf( r.getRequestId() ) ) );
  }

  static class TestReplicantRpcRequestBuilder
    extends ReplicantRpcRequestBuilder
  {
    private final replicant.Request _request;

    TestReplicantRpcRequestBuilder( @Nonnull final replicant.Request request )
    {
      _request = Objects.requireNonNull( request );
    }

    @Nonnull
    @Override
    protected replicant.Request getRequest()
    {
      return _request;
    }
  }
}
