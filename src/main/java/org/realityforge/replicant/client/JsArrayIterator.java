package org.realityforge.replicant.client;

import java.util.Iterator;

final class JsArrayIterator<T>
  implements Iterator<T>
{
  private JsArrayWrapper<T> _collection;
  int currentIndex;

  public JsArrayIterator( final JsArrayWrapper<T> collection )
  {
    _collection = collection;
    currentIndex = 0;
  }

  @Override
  public boolean hasNext()
  {
    return currentIndex < _collection.size();
  }

  @Override
  public T next()
  {
    currentIndex++;
    return _collection.get( currentIndex - 1 );
  }

  @Override
  public void remove()
  {
    _collection.slice( currentIndex - 1, currentIndex );
  }
}
