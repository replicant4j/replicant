package org.realityforge.replicant.client.json.gwt;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import java.util.AbstractList;
import java.util.List;

/**
 * An adapter converting the JsArray to support a List interface.
 */
public final class JsoReadOnlyList<E extends JavaScriptObject>
  extends AbstractList<E>
  implements List<E>
{
  private final JsArray<E> _data;

  public JsoReadOnlyList( final JsArray<E> data )
  {
    _data = data;
  }

  @Override
  public E get( final int index )
  {
    return _data.get( index );
  }

  @Override
  public int size()
  {
    return _data.length();
  }
}
