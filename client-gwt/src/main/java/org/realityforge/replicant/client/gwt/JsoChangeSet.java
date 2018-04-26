package org.realityforge.replicant.client.gwt;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.transport.Change;
import org.realityforge.replicant.client.transport.ChangeSet;
import org.realityforge.replicant.client.ChannelAction;

/**
 * An overlay type representing the change set received from the client.
 */
public final class JsoChangeSet
  extends JavaScriptObject
  implements ChangeSet
{
  protected JsoChangeSet()
  {
  }

  @Override
  public final native int getSequence() /*-{
    return this.last_id;
  }-*/;

  @Override
  @Nullable
  public final native String getRequestID()/*-{
    return this.request_id;
  }-*/;

  @Nullable
  @Override
  public final native String getETag()/*-{
    return this.etag;
  }-*/;

  @Override
  public final native int getChangeCount()/*-{
    if ( this.changes )
    {
      return this.changes.length;
    }
    else
    {
      return 0;
    }
  }-*/;

  @Nonnull
  @Override
  public final Change getChange( final int index )
  {
    return getChange0( index );
  }

  private native JsoChange getChange0( final int index )/*-{
    return this.changes[index];
  }-*/;

  @Override
  public native final int getChannelActionCount()/*-{
    if ( this.channel_actions )
    {
      return this.channel_actions.length;
    }
    else
    {
      return 0;
    }
  }-*/;

  @Nonnull
  @Override
  public ChannelAction getChannelAction( final int index )
  {
    return getChannelAction0( index );
  }

  private native JsoChannelAction getChannelAction0( final int index )/*-{
    return this.channel_actions[index];
  }-*/;

  public static ChangeSet asChangeSet( final String json )
  {
    return asChangeSet0( json );
  }

  private static JsoChangeSet asChangeSet0( final String json )
  {
    return JsonUtils.safeEval( json );
  }
}
