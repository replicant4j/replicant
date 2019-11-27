package org.realityforge.replicant.client.gwt;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import java.util.Objects;
import javax.annotation.Nonnull;

public abstract class AbstractDataLoaderEvent<H extends EventHandler>
  extends GwtEvent<H>
{
  private final String _systemKey;

  public AbstractDataLoaderEvent( @Nonnull final String systemKey )
  {
    _systemKey = Objects.requireNonNull( systemKey );
  }

  @Nonnull
  public final String getSystemKey()
  {
    return _systemKey;
  }
}
