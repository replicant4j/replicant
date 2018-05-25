package org.realityforge.replicant.client.transport;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.Connection;
import replicant.Connector;
import replicant.ReplicantContext;
import replicant.SafeProcedure;
import replicant.SystemSchema;
import replicant.Transport;

/**
 * Class from which to extend to implement a service that loads data from a change set.
 * Data is loaded incrementally and the load can be broken up into several
 * steps to avoid locking a thread such as in GWT.
 */
@SuppressWarnings( { "WeakerAccess", "unused" } )
public abstract class AbstractDataLoaderService
  extends Connector
{
  private final SessionContext _sessionContext;

  protected AbstractDataLoaderService( @Nullable final ReplicantContext context,
                                       @Nonnull final SystemSchema schema,
                                       @Nonnull final Transport transport,
                                       @Nonnull final SessionContext sessionContext )
  {
    super( context, schema, transport );
    _sessionContext = Objects.requireNonNull( sessionContext );
  }

  @Nonnull
  protected final SessionContext getSessionContext()
  {
    return _sessionContext;
  }

}
