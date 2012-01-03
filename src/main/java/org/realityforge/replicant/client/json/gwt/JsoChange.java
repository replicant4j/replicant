package org.realityforge.replicant.client.json.gwt;

import com.google.gwt.core.client.JavaScriptObject;
import org.realityforge.replicant.client.Change;

public final class JsoChange
  extends JavaScriptObject
  implements Change
{
  protected JsoChange()
  {
  }

  @Override
  public final native Object getDesignator() /*-{
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
    if (this.data) {
      return this.data[key] == null;
    }
    else {
      return null;
    }
  }-*/;

  @Override
  public final native int getIntegerValue( String key ) /*-{
    if (this.data && (typeof this.data[key] == "number")) {
      return this.data[key];
    }
    else {
      return null;
    }
  }-*/;

  @Override
  public final native String getStringValue( String key ) /*-{
    if (this.data && (typeof this.data[key] == "string")) {
      return this.data[key];
    }
    else {
      return null;
    }
  }-*/;

  @Override
  public final native boolean getBooleanValue( String key ) /*-{
    if (this.data && (typeof this.data[key] == "boolean")) {
      return this.data[key];
    }
    else {
      return null;
    }
  }-*/;
}
