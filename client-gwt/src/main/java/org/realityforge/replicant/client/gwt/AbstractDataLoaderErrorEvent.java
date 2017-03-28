package org.realityforge.replicant.client.gwt;

import com.google.gwt.event.shared.EventHandler;
import java.util.Objects;
import javax.annotation.Nonnull;

public abstract class AbstractDataLoaderErrorEvent<H extends EventHandler>
  extends AbstractDataLoaderEvent<H>
{
  private final Throwable _throwable;

  public AbstractDataLoaderErrorEvent( @Nonnull final String systemKey, @Nonnull final Throwable throwable )
  {
    super( systemKey );
    _throwable = Objects.requireNonNull( throwable );
  }

  @Nonnull
  public final Throwable getThrowable()
  {
    return _throwable;
  }
}
