package org.realityforge.replicant.client;

import com.google.gwt.core.client.JavaScriptObject;
import java.util.AbstractSet;
import java.util.Iterator;
import javax.annotation.Nonnull;

final class JsHashSet<T>
  extends AbstractSet<T>
{
  private final JsArrayWrapper<T> _data;

  JsHashSet()
  {
    this( JsArrayWrapper.create() );
  }

  JsHashSet( final JavaScriptObject data )
  {
    _data = data.cast();
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