package org.realityforge.replicant.client;

import com.google.gwt.core.client.JavaScriptObject;
import java.util.AbstractList;
import java.util.Iterator;
import javax.annotation.Nonnull;

final class JsArrayList<T>
  extends AbstractList<T>
{
  private final JsArrayWrapper<T> _data;

  JsArrayList()
  {
    this( JsArrayWrapper.create() );
  }

  JsArrayList( final JavaScriptObject data )
  {
    _data = data.cast();
  }

  @Override
  public T get( final int i )
  {
    return _data.get( i );
  }

  @Override
  @Nonnull
  public Iterator<T> iterator()
  {
    return new JsArrayIterator<T>( _data );
  }

  @Override
  public int size()
  {
    return _data.size();
  }
}