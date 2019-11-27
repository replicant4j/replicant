package org.realityforge.replicant.client.transport;

import javax.annotation.Nullable;

public abstract class ChainedAction
  implements Runnable
{
  private Runnable _next;

  public ChainedAction()
  {
    this( null );
  }

  public ChainedAction( final Runnable next )
  {
    _next = next;
  }

  @Nullable
  public Runnable getNext()
  {
    return _next;
  }

  public void setNext( @Nullable final Runnable next )
  {
    _next = next;
  }
}
