package org.realityforge.replicant.server.transport;

public class AttemptedToUpdateStaticFilterException
  extends RuntimeException
{
  public AttemptedToUpdateStaticFilterException()
  {
  }

  public AttemptedToUpdateStaticFilterException( final String message )
  {
    super( message );
  }

  public AttemptedToUpdateStaticFilterException( final String message, final Throwable cause )
  {
    super( message, cause );
  }

  public AttemptedToUpdateStaticFilterException( final Throwable cause )
  {
    super( cause );
  }
}
