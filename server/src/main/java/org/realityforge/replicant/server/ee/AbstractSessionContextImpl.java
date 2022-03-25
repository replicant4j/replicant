package org.realityforge.replicant.server.ee;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
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
    final SubscriptionEntry existing =
      session.findSubscriptionEntry( address );
    final SubscriptionEntry entry =
      null == existing ? session.createSubscriptionEntry( address ) : existing;
    if ( explicitSubscribe )
    {
      entry.setExplicitlySubscribed( explicitSubscribe );
    }
    entry.setFilter( filter );
    changeSet.mergeAction( address,
                           null == existing ?
                           ChannelAction.Action.ADD :
                           ChannelAction.Action.UPDATE,
                           filter );
    return entry;
  }

  @Language( "TSQL" )
  protected String generateTempIdTable( @Nonnull final List<ChannelAddress> addresses )
    throws SQLException
  {
    return
      "DECLARE @Ids TABLE ( Id INTEGER NOT NULL );\n" +
      "INSERT INTO @Ids VALUES " +
      addresses.stream()
        .map( a -> "(" + a.getSubChannelId() + ")" )
        .collect( Collectors.joining( "," ) ) +
      "\n";
  }

  protected void linkSourceGraphToTargetGraph( @Nonnull final ReplicantSession session,
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
        linkSourceGraphToTargetGraph( session,
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

  protected void linkSourceGraphToTargetGraph( @Nonnull final ReplicantSession session,
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
      final int calendarId = resultSet.getInt( sourceColumnName );
      final int resourceId = resultSet.getInt( targetColumnName );
      final ChannelAddress targetAddress =
        new ChannelAddress( targetGraph, calendarId );
      if ( !session.isSubscriptionEntryPresent( targetAddress ) )
      {
        final ChannelAddress sourceAddress =
          new ChannelAddress( sourceGraph, resourceId );
        final SubscriptionEntry sourceEntry =
          session.getSubscriptionEntry( sourceAddress );
        sourceEntry.registerOutwardSubscriptions( targetAddress );
        final SubscriptionEntry targetEntry =
          recordSubscription( session, changeSet, targetAddress, filter, false );
        targetEntry.registerInwardSubscriptions( sourceEntry.getAddress() );
      }
    }
  }

}
