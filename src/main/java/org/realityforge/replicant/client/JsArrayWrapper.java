package org.realityforge.replicant.client;

import com.google.gwt.core.client.JavaScriptObject;

final class JsArrayWrapper<T>
  extends JavaScriptObject
{
  JsArrayWrapper()
  {
  }

  protected native final JsArrayWrapper<T> slice( int start, int end )/*-{
    return this.slice( start, end );
  }-*/;

  protected static native <T> JsArrayWrapper<T> create() /*-{
    return [];
  }-*/;

  protected final native int size() /*-{
    return this.length;
  }-*/;

  protected final native T get( int i ) /*-{
    return this[i];
  }-*/;
}
