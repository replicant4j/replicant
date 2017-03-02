package org.realityforge.replicant.client;

public class NoResultException
  extends RuntimeException
{
  public NoResultException()
  {
  }

  public NoResultException( final String message )
  {
    super( message );
  }

  public NoResultException( final String message, final Throwable cause )
  {
    super( message, cause );
  }
}
