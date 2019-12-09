package org.realityforge.replicant.client.gwt;

import com.google.gwt.core.client.JavaScriptObject;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.ChannelAction;

public class JsoChannelAction
  extends JavaScriptObject
  implements ChannelAction
{
  protected JsoChannelAction()
  {
  }

  @Override
  public final native int getChannelId() /*-{
    return this.cid;
  }-*/;

  @Override
  public final native Integer getSubChannelId() /*-{
    if ( typeof(this.scid) == 'number' )
    {
      return new @java.lang.Integer::new(I)( this.scid );
    }
    else
    {
      return null;
    }
  }-*/;

  @Nonnull
  @Override
  public final Action getAction()
  {
    return Action.valueOf( getAction0().toUpperCase() );
  }

  private native String getAction0() /*-{
    return this.action;
  }-*/;

  @Override
  public final native Object getChannelFilter() /*-{

    if ( typeof(this.filter) == 'object' )
    {
      return this.filter;
    }
    return null;
  }-*/;
}
