package org.realityforge.replicant.server.ee;

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
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelAction;
import org.realityforge.replicant.server.ChannelAddress;
import org.realityforge.replicant.server.transport.ReplicantSession;
import org.realityforge.replicant.server.transport.SubscriptionEntry;

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
    for ( final ChannelAddress address : addresses )
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
    final SubscriptionEntry existing = session.findSubscriptionEntry( address );
    final SubscriptionEntry entry = null == existing ? session.createSubscriptionEntry( address ) : existing;
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

  @SuppressWarnings( "SameParameterValue" )
  private static <T> Stream<List<T>> chunked( @Nonnull final Stream<T> stream, final int chunkSize )
  {
    final AtomicInteger index = new AtomicInteger( 0 );

    return stream
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
    try ( Statement statement = connection().createStatement() )
    {
      try ( ResultSet resultSet = statement.executeQuery( sql ) )
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
      final int sourceId = resultSet.getInt( sourceColumnName );
      final int targetId = resultSet.getInt( targetColumnName );
      final ChannelAddress targetAddress = new ChannelAddress( targetGraph, targetId );
      if ( !session.isSubscriptionEntryPresent( targetAddress ) )
      {
        final ChannelAddress sourceAddress = new ChannelAddress( sourceGraph, sourceId );
        final SubscriptionEntry sourceEntry = session.getSubscriptionEntry( sourceAddress );
        sourceEntry.registerOutwardSubscriptions( targetAddress );
        final SubscriptionEntry targetEntry =
          recordSubscription( session, changeSet, targetAddress, filter, false );
        targetEntry.registerInwardSubscriptions( sourceEntry.getAddress() );
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
    try ( Statement statement = connection().createStatement() )
    {
      try ( ResultSet resultSet = statement.executeQuery( sql ) )
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
      final int targetId = resultSet.getInt( targetGraphColumnName );
      final ChannelAddress address = new ChannelAddress( targetGraph, targetId );
      changeSet.mergeAction( address, ChannelAction.Action.UPDATE, filter );
      session.getSubscriptionEntry( address ).setFilter( filter );
    }
  }
}
