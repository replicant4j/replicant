package org.realityforge.replicant.client.json.gwt;

import com.google.gwt.core.client.JsArrayString;
import java.util.AbstractList;
import java.util.List;

/**
 * An adapter converting the JsArrayString to support a List interface.
 */
public final class JsoReadOnlyStringList
  extends AbstractList<String>
  implements List<String>
{
  private final JsArrayString _data;

  public JsoReadOnlyStringList( final JsArrayString data )
  {
    _data = data;
  }

  public JsArrayString asArray()
  {
    return _data;
  }

  @Override
  public String get( final int index )
  {
    return _data.get( index );
  }

  @Override
  public int size()
  {
    return _data.length();
  }
}
