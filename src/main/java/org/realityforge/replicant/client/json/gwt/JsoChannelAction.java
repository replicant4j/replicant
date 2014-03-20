package org.realityforge.replicant.client.json.gwt;

import com.google.gwt.core.client.JavaScriptObject;
import org.realityforge.replicant.client.ChannelAction;

public class JsoChannelAction
  extends JavaScriptObject
  implements ChannelAction
{
  protected JsoChannelAction()
  {
  }

  @Override
  public final native int getChannelID() /*-{
    return this.cid;
  }-*/;

  @Override
  public final native String getSubChannelIDAsString() /*-{
    return this.scid;
  }-*/;

  @Override
  public final native int getSubChannelIDAsInt() /*-{
    return this.scid;
  }-*/;

  @Override
  public final Action getAction()
  {
    return Action.valueOf( getAction0().toUpperCase() );
  }

  private native String getAction0() /*-{
    return this.action;
  }-*/;

}
