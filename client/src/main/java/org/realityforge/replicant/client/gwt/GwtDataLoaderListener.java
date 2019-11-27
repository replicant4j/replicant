package org.realityforge.replicant.client.gwt;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.web.bindery.event.shared.EventBus;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.transport.DataLoadStatus;
import org.realityforge.replicant.client.transport.DataLoaderListenerAdapter;
import org.realityforge.replicant.client.transport.DataLoaderService;

public class GwtDataLoaderListener
  extends DataLoaderListenerAdapter
{
  private final EventBus _eventBus;

  public GwtDataLoaderListener( @Nonnull final EventBus eventBus )
  {
    _eventBus = Objects.requireNonNull( eventBus );
  }

  @Override
  public void onConnect( @Nonnull final DataLoaderService service )
  {
    fireEvent( new ConnectEvent( service.getKey() ) );
  }

  @Override
  public void onInvalidConnect( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
    fireEvent( new InvalidConnectEvent( service.getKey(), toCause( throwable ) ) );
  }

  @Override
  public void onDisconnect( @Nonnull final DataLoaderService service )
  {
    fireEvent( new DisconnectEvent( service.getKey() ) );
  }

  @Override
  public void onInvalidDisconnect( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
    fireEvent( new InvalidDisconnectEvent( service.getKey(), toCause( throwable ) ) );
  }

  @Override
  public void onDataLoadComplete( @Nonnull final DataLoaderService service, @Nonnull final DataLoadStatus status )
  {
    fireEvent( new DataLoadCompleteEvent( status ) );
  }

  @Override
  public void onDataLoadFailure( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
    fireEvent( new DataLoadFailureEvent( service.getKey(), throwable ) );
  }

  @Override
  public void onPollFailure( @Nonnull final DataLoaderService service, @Nonnull final Throwable throwable )
  {
    fireEvent( new PollFailureEvent( service.getKey(), throwable ) );
  }

  Throwable toCause( @Nonnull final Throwable caught )
  {
    return caught instanceof InvocationException ? caught.getCause() : caught;
  }

  protected void fireEvent( final GwtEvent<?> event )
  {
    _eventBus.fireEvent( event );
  }
}
