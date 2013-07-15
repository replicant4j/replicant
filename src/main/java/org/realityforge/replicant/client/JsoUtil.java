package org.realityforge.replicant.client;

import com.google.gwt.core.client.JavaScriptObject;
import java.util.List;
import javax.annotation.Nullable;

public final class JsoUtil<T>
{
  JsoUtil()
  {
  }

  /**
   * A Java Friendly way of working with Js Arrays using the Java.util.Collection API.
   *
   * @param <T>
   */
  public static <T> List<T> toList( @Nullable final JavaScriptObject data )
  {
    if ( null == data )
    {
      return new JsArrayList<T>();
    }
    else
    {
      return new JsArrayList<T>( data );
    }
  }
}
