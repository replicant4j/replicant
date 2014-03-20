package org.realityforge.replicant.client.json.gwt;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.Change;
import org.realityforge.replicant.client.ChangeSet;

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

  @Override
  public final Change getChange( final int index )
  {
    return getChange0( index );
  }

  private native JsoChange getChange0( final int index )/*-{
    return this.changes[ index ];
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
