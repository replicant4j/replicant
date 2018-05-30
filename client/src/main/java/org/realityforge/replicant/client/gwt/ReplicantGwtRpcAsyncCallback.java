package org.realityforge.replicant.client.gwt;

import com.google.gwt.user.client.rpc.AsyncCallback;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class ReplicantGwtRpcAsyncCallback<T>
  implements AsyncCallback<T>
{
  @Nonnull
  private final replicant.Request _r;
  @Nonnull
  private final AsyncCallback<T> _callback;

  public ReplicantGwtRpcAsyncCallback( @Nonnull final replicant.Request r, @Nonnull final AsyncCallback<T> callback )
  {
    _r = Objects.requireNonNull( r );
    _callback = Objects.requireNonNull( callback );
  }

  @Override
  public void onSuccess( final T result )
  {
    _r.onSuccess( false, () -> _callback.onSuccess( result ) );
  }

  @Override
  public void onFailure( final Throwable caught )
  {
    _r.onFailure( () -> _callback.onFailure( caught ) );
  }
}
