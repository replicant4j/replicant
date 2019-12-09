package org.realityforge.replicant.client.gwt;

import com.google.gwt.core.client.JavaScriptObject;
import java.util.Date;
import javax.annotation.Nonnull;
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
  public final native int getTypeId() /*-{
    return this.type;
  }-*/;

  @Override
  public final native boolean isUpdate() /*-{
    return this.data != null;
  }-*/;

  @Override
  public final native boolean containsKey( @Nonnull String key ) /*-{
    return this.data != null && (key in this.data);
  }-*/;

  @Override
  public final native boolean isNull( @Nonnull final String key )/*-{
    if (this.data) {
      return this.data[key] == null;
    } else {
      return false;
    }
  }-*/;

  @Override
  public final native int getIntegerValue( @Nonnull String key ) /*-{
    if (this.data && (typeof this.data[key] == "number")) {
      return this.data[key];
    } else {
      return null;
    }
  }-*/;

  @Nonnull
  @Override
  public final native Date getDateValue( @Nonnull String key ) /*-{
    if (this.data && (typeof this.data[key] == "string")) {
      var d = new Date(this.data[key]);
      return @java.util.Date::new(IIIIII)(d.getFullYear(),
        d.getMonth(),
        d.getDate(),
        d.getHours(),
        d.getMinutes(),
        d.getSeconds());
    } else {
      return null;
    }
  }-*/;

  @Nonnull
  @Override
  public final native String getStringValue( @Nonnull String key ) /*-{
    if (this.data && (typeof this.data[key] == "string")) {
      return this.data[key];
    } else {
      return null;
    }
  }-*/;

  @Nonnull
  @Override
  public final native boolean getBooleanValue( @Nonnull String key ) /*-{
    if (this.data && (typeof this.data[key] == "boolean")) {
      return this.data[key];
    } else {
      return null;
    }
  }-*/;

  @Override
  public final native int getChannelCount() /*-{
    if (this.channels) {
      return this.channels.length;
    } else {
      return null;
    }
  }-*/;

  @Override
  public final native int getChannelId( final int index ) /*-{
    if (this.channels && index < this.channels.length) {
      return this.channels[index].cid;
    } else {
      return 0;
    }
  }-*/;

  @Override
  public final native Integer getSubChannelId( final int index ) /*-{
    if (this.channels && index < this.channels.length) {
      return new @java.lang.Integer::new(I)(this.channels[index].scid);
    }
    return null;
  }-*/;
}
