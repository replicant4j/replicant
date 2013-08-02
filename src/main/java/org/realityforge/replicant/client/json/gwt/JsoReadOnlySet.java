package org.realityforge.replicant.client.json.gwt;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

/**
 * An adapter converting the JsArray to support a Set interface.
 */
public final class JsoReadOnlySet<E extends JavaScriptObject>
  extends AbstractSet<E>
  implements Set<E>
{
  private final JsArray<E> _data;

  public JsoReadOnlySet( final JsArray<E> data )
  {
    _data = data;
  }

  public JsArray<E> asArray()
  {
    return _data;
  }

  @Override
  public Iterator<E> iterator()
  {
    return new JsoIterator<E>( this );
  }

  @Override
  public int size()
  {
    return _data.length();
  }

  static class JsoIterator<E extends JavaScriptObject>
    implements Iterator<E>
  {
    private JsoReadOnlySet<E> _set;
    private int _offset;

    JsoIterator( final JsoReadOnlySet<E> set )
    {
      _set = set;
    }

    @Override
    public boolean hasNext()
    {
      return _offset < _set.size();
    }

    @Override
    public E next()
    {
      return _set._data.get( _offset++ );
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
