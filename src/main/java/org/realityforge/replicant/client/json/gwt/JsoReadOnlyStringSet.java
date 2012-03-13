package org.realityforge.replicant.client.json.gwt;

import com.google.gwt.core.client.JsArrayString;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

/**
 * An adapter converting the JsArrayString to support a Set interface.
 */
public final class JsoReadOnlyStringSet
  extends AbstractSet<String>
  implements Set<String>
{
  private final JsArrayString _data;

  public JsoReadOnlyStringSet( final JsArrayString data )
  {
    _data = data;
  }

  @Override
  public Iterator<String> iterator()
  {
    return new JsoIterator( this );
  }

  @Override
  public int size()
  {
    return _data.length();
  }

  static class JsoIterator
    implements Iterator<String>
  {
    private JsoReadOnlyStringSet _set;
    private int _offset;

    JsoIterator( final JsoReadOnlyStringSet set )
    {
      _set = set;
    }

    @Override
    public boolean hasNext()
    {
      return _offset < _set.size();
    }

    @Override
    public String next()
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
