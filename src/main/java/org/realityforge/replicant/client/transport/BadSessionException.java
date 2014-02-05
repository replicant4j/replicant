package org.realityforge.replicant.client.transport;

/**
 * Runtime session thrown when unable to locate specified session.
 */
public class BadSessionException
  extends RuntimeException
{
  public BadSessionException()
  {
    this( null, null );
  }

  public BadSessionException( final String message )
  {
    this( message, null );
  }

  public BadSessionException( final String message, final Throwable cause )
  {
    super( message, cause );
  }
}
