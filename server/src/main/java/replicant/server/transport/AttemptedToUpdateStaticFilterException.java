package replicant.server.transport;

class AttemptedToUpdateStaticFilterException
  extends RuntimeException
{
  AttemptedToUpdateStaticFilterException( final String message )
  {
    super( message );
  }
}
