package org.realityforge.replicant.client.json.gwt;

import com.google.gwt.core.client.JavaScriptObject;
import java.util.Date;
import org.realityforge.replicant.client.Change;

public final class JsoChange
  extends JavaScriptObject
  implements Change
{
  protected JsoChange()
  {
  }

  @Override
  public final native int getDesignatorAsInt() /*-{
    return this.id;
  }-*/;

  @Override
  public final native String getDesignatorAsString() /*-{
    return this.id;
  }-*/;

  @Override
  public final native int getTypeID() /*-{
    return this.type;
  }-*/;

  @Override
  public final native boolean isUpdate() /*-{
    return this.data != null;
  }-*/;

  @Override
  public final native boolean containsKey( String key ) /*-{
    return this.data != null && this.data[key] != null;
  }-*/;

  @Override
  public final native boolean isNull( final String key )/*-{
    if ( this.data )
    {
      return this.data[key] == null;
    }
    else
    {
      return null;
    }
  }-*/;

  @Override
  public final native int getIntegerValue( String key ) /*-{
    if ( this.data && (typeof this.data[key] == "number") )
    {
      return this.data[key];
    }
    else
    {
      return null;
    }
  }-*/;

  @Override
  public final native Date getDateValue( String key ) /*-{
    if ( this.data && (typeof this.data[key] == "string") )
    {
      var d = new Date( this.data[key] );
      return @java.util.Date::new(IIIIII)( d.getFullYear(),
                                           d.getMonth(),
                                           d.getDate(),
                                           d.getHours(),
                                           d.getMinutes(),
                                           d.getSeconds() );
    }
    else
    {
      return null;
    }
  }-*/;

  @Override
  public final native String getStringValue( String key ) /*-{
    if ( this.data && (typeof this.data[key] == "string") )
    {
      return this.data[key];
    }
    else
    {
      return null;
    }
  }-*/;

  @Override
  public final native boolean getBooleanValue( String key ) /*-{
    if ( this.data && (typeof this.data[key] == "boolean") )
    {
      return this.data[key];
    }
    else
    {
      return null;
    }
  }-*/;

  @Override
  public final native int getChannelCount() /*-{
    if ( this.channels )
    {
      return this.channels.length;
    }
    else
    {
      return null;
    }
  }-*/;

  @Override
  public final native int getChannelID( final int index ) /*-{
    if ( this.channels && index < this.channels.length )
    {
      return this.channels[index].cid;
    }
    else
    {
      return 0;
    }
  }-*/;

  @Override
  public final native int getSubChannelIDAsInt( final int index ) /*-{
    if ( this.channels && index < this.channels.length )
    {
      return this.channels[index].scid;
    }
    else
    {
      return 0;
    }
  }-*/;

  @Override
  public final native String getSubChannelIDAsString( final int index ) /*-{
    if ( this.channels && index < this.channels.length )
    {
      return this.channels[index].scid;
    }
    else
    {
      return null;
    }
  }-*/;
}
