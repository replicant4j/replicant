package org.realityforge.replicant.client.json.gwt;

import com.google.gwt.core.client.JsArrayInteger;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

/**
 * An adapter converting the JsArrayString to support a Set interface.
 */
public final class JsoReadOnlyIntegerSet
  extends AbstractSet<Integer>
  implements Set<Integer>
{
  private final JsArrayInteger _data;

  public JsoReadOnlyIntegerSet( final JsArrayInteger data )
  {
    _data = data;
  }

  public JsArrayInteger asArray()
  {
    return _data;
  }

  @Override
  public Iterator<Integer> iterator()
  {
    return new JsoIterator( this );
  }

  @Override
  public int size()
  {
    return _data.length();
  }

  static class JsoIterator
    implements Iterator<Integer>
  {
    private JsoReadOnlyIntegerSet _set;
    private int _offset;

    JsoIterator( final JsoReadOnlyIntegerSet set )
    {
      _set = set;
    }

    @Override
    public boolean hasNext()
    {
      return _offset < _set.size();
    }

    @Override
    public Integer next()
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
