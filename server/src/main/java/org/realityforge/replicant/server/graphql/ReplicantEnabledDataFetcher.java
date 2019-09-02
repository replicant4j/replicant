package org.realityforge.replicant.server.graphql;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.server.ServerConstants;
import org.realityforge.replicant.server.ee.ReplicantContextHolder;
import org.realityforge.replicant.server.ee.ReplicationRequestUtil;
import org.realityforge.replicant.server.transport.ReplicantSession;
import org.realityforge.replicant.server.transport.ReplicantSessionManager;

/**
 * This fetcher sets up replicant context.
 * It should be used for top-level fetchers where the nested fetcher is not called across a boundary
 * and thus will not have replicant context initialized. It should already have the transaction established.
 */
public class ReplicantEnabledDataFetcher<T>
  implements DataFetcher<T>
{
  @Nonnull
  private final ReplicantSessionManager _sessionManager;
  @Nonnull
  private final EntityMessageEndpoint _endpoint;
  @Nonnull
  private final EntityManager _entityManager;
  @Nonnull
  private final TransactionSynchronizationRegistry _registry;
  @Nonnull
  private final String _name;
  @Nonnull
  private final DataFetcher<T> _fetcher;

  public ReplicantEnabledDataFetcher( @Nonnull final ReplicantSessionManager sessionManager,
                                      @Nonnull final EntityMessageEndpoint endpoint,
                                      @Nonnull final EntityManager entityManager,
                                      @Nonnull final TransactionSynchronizationRegistry registry,
                                      @Nonnull final String name,
                                      @Nonnull final DataFetcher<T> fetcher )
  {
    _sessionManager = Objects.requireNonNull( sessionManager );
    _endpoint = Objects.requireNonNull( endpoint );
    _entityManager = Objects.requireNonNull( entityManager );
    _registry = Objects.requireNonNull( registry );
    _name = Objects.requireNonNull( name );
    _fetcher = Objects.requireNonNull( fetcher );
  }

  @Override
  public T get( @Nonnull final DataFetchingEnvironment environment )
    throws Exception
  {
    final String sessionId = (String) ReplicantContextHolder.remove( ServerConstants.SESSION_ID_KEY );
    final Integer requestId = (Integer) ReplicantContextHolder.remove( ServerConstants.REQUEST_ID_KEY );
    final ReplicantSession session = null != sessionId ? _sessionManager.getSession( sessionId ) : null;
    return ReplicationRequestUtil.runRequest( _registry,
                                              _entityManager,
                                              _endpoint,
                                              _name,
                                              session,
                                              requestId,
                                              () -> _fetcher.get( environment ) );
  }
}
