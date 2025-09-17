package replicant.server.ee;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import org.intellij.lang.annotations.Language;
import replicant.server.ChangeSet;
import replicant.server.ChannelAction;
import replicant.server.ChannelAddress;
import replicant.server.transport.ReplicantSession;
import replicant.server.transport.SubscriptionEntry;

/**
 * Base class used to support implementation of SessionContext implementations.
 * Primarily it contains support for customizing bulk loads using SQL.
 */
public abstract class AbstractSessionContextImpl
{
  @Nonnull
  protected abstract EntityManager em();

  @Nonnull
  protected Connection connection()
  {
    return em().unwrap( Connection.class );
  }

  protected void recordSubscriptions( @Nonnull final ReplicantSession session,
                                      @Nonnull final ChangeSet changeSet,
                                      @Nonnull final Collection<ChannelAddress> addresses,
                                      @Nullable final Object filter,
                                      final boolean explicitSubscribe )
  {
    for ( final var address : addresses )
    {
      recordSubscription( session, changeSet, address, filter, explicitSubscribe );
    }
  }

  @Nonnull
  protected SubscriptionEntry recordSubscription( @Nonnull final ReplicantSession session,
                                                  @Nonnull final ChangeSet changeSet,
                                                  @Nonnull final ChannelAddress address,
                                                  @Nullable final Object filter,
                                                  final boolean explicitSubscribe )
  {
    final var existing = session.findSubscriptionEntry( address );
    final var entry = null == existing ? session.createSubscriptionEntry( address ) : existing;
    if ( explicitSubscribe )
    {
      entry.setExplicitlySubscribed( true );
    }
    entry.setFilter( filter );
    changeSet.mergeAction( address, null == existing ? ChannelAction.Action.ADD : ChannelAction.Action.UPDATE, filter );
    return entry;
  }

  @Language( "TSQL" )
  protected String generateTempIdTable( @Nonnull final List<ChannelAddress> addresses )
  {
    return
      "DECLARE @Ids TABLE ( Id INTEGER NOT NULL );\n" +
      chunked( addresses.stream().map( ChannelAddress::rootId ), 900 )
        .map( ids ->
                "INSERT INTO @Ids VALUES " +
                ids.stream().map( id -> "(" + id + ")" ).collect( Collectors.joining( "," ) ) ).
        collect( Collectors.joining( "\n" ) ) +
      "\n";
  }

  @Nonnull
  @SuppressWarnings( "SameParameterValue" )
  private static <T> Stream<List<T>> chunked( @Nonnull final Stream<T> stream, final int chunkSize )
  {
    final var index = new AtomicInteger( 0 );

    return
      stream
        .collect( Collectors.groupingBy( x -> index.getAndIncrement() / chunkSize ) )
        .entrySet().stream()
        .sorted( Map.Entry.comparingByKey() )
        .map( Map.Entry::getValue );
  }

  protected void bulkLinkFromSourceGraphToTargetGraph( @Nonnull final ReplicantSession session,
                                                       @Nullable final Object filter,
                                                       @Nonnull final ChangeSet changeSet,
                                                       final int sourceGraph,
                                                       @Nonnull final String sourceColumnName,
                                                       final int targetGraph,
                                                       @Nonnull final String targetColumnName,
                                                       @Language( "TSQL" ) @Nonnull final String sql )
    throws SQLException
  {
    try ( var statement = connection().createStatement() )
    {
      try ( var resultSet = statement.executeQuery( sql ) )
      {
        bulkLinkFromSourceGraphToTargetGraph( session,
                                              filter,
                                              changeSet,
                                              sourceGraph,
                                              sourceColumnName,
                                              targetGraph,
                                              targetColumnName,
                                              resultSet );
      }
    }
  }

  protected void bulkLinkFromSourceGraphToTargetGraph( @Nonnull final ReplicantSession session,
                                                       @Nullable final Object filter,
                                                       @Nonnull final ChangeSet changeSet,
                                                       final int sourceGraph,
                                                       @Nonnull final String sourceColumnName,
                                                       final int targetGraph,
                                                       @Nonnull final String targetColumnName,
                                                       @Nonnull final ResultSet resultSet )
    throws SQLException
  {
    while ( resultSet.next() )
    {
      final var sourceId = resultSet.getInt( sourceColumnName );
      final var targetId = resultSet.getInt( targetColumnName );
      final var targetAddress = new ChannelAddress( targetGraph, targetId );
      if ( !session.isSubscriptionEntryPresent( targetAddress ) )
      {
        final var sourceAddress = new ChannelAddress( sourceGraph, sourceId );
        final var sourceEntry = session.getSubscriptionEntry( sourceAddress );
        sourceEntry.registerOutwardSubscriptions( targetAddress );
        final var targetEntry = recordSubscription( session, changeSet, targetAddress, filter, false );
        targetEntry.registerInwardSubscriptions( sourceEntry.address() );
      }
    }
  }

  protected void updateLinksToTargetGraph( @Nonnull final ReplicantSession session,
                                           @Nonnull final ChangeSet changeSet,
                                           @Nonnull final Object filter,
                                           final int targetGraph,
                                           @Nonnull final String targetGraphColumnName,
                                           @Language( "TSQL" ) @Nonnull final String sql )
    throws SQLException
  {
    try ( var statement = connection().createStatement() )
    {
      try ( var resultSet = statement.executeQuery( sql ) )
      {
        updateLinksToTargetGraph( session, changeSet, filter, targetGraphColumnName, targetGraph, resultSet );
      }
    }
  }

  protected void updateLinksToTargetGraph( @Nonnull final ReplicantSession session,
                                           @Nonnull final ChangeSet changeSet,
                                           @Nullable final Object filter,
                                           final String targetGraphColumnName,
                                           final int targetGraph,
                                           @Nonnull final ResultSet resultSet )
    throws SQLException
  {
    while ( resultSet.next() )
    {
      final var targetId = resultSet.getInt( targetGraphColumnName );
      final var address = new ChannelAddress( targetGraph, targetId );
      changeSet.mergeAction( address, ChannelAction.Action.UPDATE, filter );
      session.getSubscriptionEntry( address ).setFilter( filter );
    }
  }
}
