package org.realityforge.replicant.client.transport;

import org.realityforge.replicant.client.ChannelDescriptor;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class DataLoaderListenerSupportTest
{
  enum TestGraph
  {
    A
  }

  @SuppressWarnings( "ThrowableNotThrown" )
  @Test
  public void basicOperation()
  {
    final DataLoaderListenerSupport support = new DataLoaderListenerSupport();

    final DataLoaderService service = mock( DataLoaderService.class );

    final DataLoaderListener listener = mock( DataLoaderListener.class );
    final Throwable throwable = new Throwable();
    final ChannelDescriptor descriptor = new ChannelDescriptor( TestGraph.A );

    assertEquals( support.getListeners().size(), 0 );

    assertTrue( support.addListener( listener ) );
    assertFalse( support.addListener( listener ), "Can not add duplicate" );

    assertEquals( support.getListeners().size(), 1 );

    reset( listener );
    support.onConnect( service );
    verify( listener ).onConnect( service );

    reset( listener );
    support.onInvalidConnect( service, throwable );
    verify( listener ).onInvalidConnect( service, throwable );

    reset( listener );
    support.onDisconnect( service );
    verify( listener ).onDisconnect( service );

    reset( listener );
    support.onInvalidDisconnect( service, throwable );
    verify( listener ).onInvalidDisconnect( service, throwable );

    reset( listener );
    support.onSubscribeStarted( service, descriptor );
    verify( listener ).onSubscribeStarted( service, descriptor );

    reset( listener );
    support.onSubscribeCompleted( service, descriptor );
    verify( listener ).onSubscribeCompleted( service, descriptor );

    reset( listener );
    support.onSubscribeFailed( service, descriptor, throwable );
    verify( listener ).onSubscribeFailed( service, descriptor, throwable );

    reset( listener );
    support.onSubscriptionUpdateStarted( service, descriptor );
    verify( listener ).onSubscriptionUpdateStarted( service, descriptor );

    reset( listener );
    support.onSubscriptionUpdateCompleted( service, descriptor );
    verify( listener ).onSubscriptionUpdateCompleted( service, descriptor );

    reset( listener );
    support.onSubscriptionUpdateFailed( service, descriptor, throwable );
    verify( listener ).onSubscriptionUpdateFailed( service, descriptor, throwable );

    reset( listener );
    support.onUnsubscribeStarted( service, descriptor );
    verify( listener ).onUnsubscribeStarted( service, descriptor );

    reset( listener );
    support.onUnsubscribeCompleted( service, descriptor );
    verify( listener ).onUnsubscribeCompleted( service, descriptor );

    reset( listener );
    support.onUnsubscribeFailed( service, descriptor, throwable );
    verify( listener ).onUnsubscribeFailed( service, descriptor, throwable );

    assertEquals( support.getListeners().size(), 1 );

    assertTrue( support.removeListener( listener ) );
    assertFalse( support.removeListener( listener ), "Can not remove duplicate" );

    assertEquals( support.getListeners().size(), 0 );

    reset( listener );
    support.onConnect( service );
    verify( listener, never() ).onConnect( service );
  }
}
