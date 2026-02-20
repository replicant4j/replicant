package replicant.server.ee;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.intellij.lang.annotations.Language;
import replicant.server.ChangeSet;
import replicant.server.ChannelAction;
import replicant.server.ChannelAddress;
import replicant.server.ChannelLink;
import replicant.server.EntityMessage;
import replicant.server.transport.ReplicantChangeRecorder;
import replicant.server.transport.ReplicantSession;
import replicant.server.transport.ReplicantSessionContext;
import replicant.server.transport.SubscriptionEntry;

/**
 * Base class used to support implementation of SessionContext implementations.
 * Primarily it contains support for customizing bulk loads using SQL.
 */
@SuppressWarnings( "SqlNoDataSourceInspection" )
public abstract class AbstractSessionContextImpl
  implements ReplicantChangeRecorder, ReplicantSessionContext
{
  @Resource
  private TransactionSynchronizationRegistry _registry;

  @Nonnull
  @Override
  public Object deriveTargetFilter( @Nonnull final EntityMessage entityMessage,
                                    @Nonnull final ChannelAddress source,
                                    @Nullable final Object sourceFilter,
                                    @Nonnull final ChannelAddress target )
  {
    throw new IllegalStateException( "deriveTargetFilter called for link from " + source + " to " + target +
                                     ( null == sourceFilter ? "" : " with source filter " + sourceFilter ) +
                                     " in the context of the entity message " + entityMessage +
                                     " but no such graph link exists or the target graph has no filter parameter" );
  }

  @Nonnull
  @Override
  public String deriveTargetFilterInstanceId( @Nonnull final EntityMessage entityMessage,
                                              @Nonnull final ChannelLink link,
                                              @Nullable final Object sourceFilter,
                                              @Nullable final Object targetFilter )
  {
    final var source = link.source();
    final var target = link.target();
    throw new IllegalStateException( "deriveFilterInstanceId called for link from " + source + " to " + target +
                                     ( null == sourceFilter ? "" : " with source filter " + sourceFilter ) +
                                     " in the context of the entity message " + entityMessage +
                                     " but no such graph link exists or the target graph is not a " +
                                     "instanced filter graph" );
  }

  @Override
  public void bulkCollectDataForSubscribe( @Nullable final ReplicantSession session,
                                           @Nonnull final List<ChannelAddress> addresses,
                                           @Nullable final Object filter,
                                           @Nonnull final ChangeSet changeSet,
                                           final boolean isExplicitSubscribe )
  {
    doBulkCollectDataForSubscribe( session, addresses, filter, changeSet, isExplicitSubscribe );
  }

  protected abstract void doBulkCollectDataForSubscribe( @Nullable final ReplicantSession session,
                                                         @Nonnull final List<ChannelAddress> addresses,
                                                         @Nullable final Object filter,
                                                         @Nonnull final ChangeSet changeSet,
                                                         final boolean isExplicitSubscribe );

  /**
   * Record the EntityMessage for specified entity in the transactions EntityMessageSet.
   *
   * @param entity   the entity to record.
   * @param isUpdate true if change is an update, false if it is a delete.
   */
  @Override
  public void recordEntityMessageForEntity( @Nonnull final Object entity, final boolean isUpdate )
  {
    final var entityMessage = convertToEntityMessage( entity, isUpdate, false );
    if ( null != entityMessage )
    {
      EntityMessageCacheUtil.getEntityMessageSet( _registry ).merge( entityMessage );
    }
  }

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
    //noinspection SqlUnused
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
  @SuppressWarnings( { "SameParameterValue", "DataFlowIssue" } )
  protected <T> Stream<List<T>> chunked( @Nonnull final Stream<T> stream, final int chunkSize )
  {
    final var index = new AtomicInteger( 0 );

    return
      stream
        .collect( Collectors.groupingBy( x -> index.getAndIncrement() / chunkSize ) )
        .entrySet().stream()
        .sorted( Map.Entry.comparingByKey() )
        .map( Map.Entry::getValue );
  }

  /**
   * Converts the given object into an appropriate {@link EntityMessage}.
   * This method may be used for initial data load or for entity updates.
   * Implementations of this abstract method define the specific conversion logic.
   *
   * @param object        the source object to be converted; must not be null
   * @param isUpdate      a boolean indicating if the conversion is for an update
   * @param isInitialLoad a boolean indicating if the conversion is for an initial data load
   * @return the converted {@link EntityMessage}, or null if the conversion cannot be performed
   */
  @Nullable
  protected abstract EntityMessage convertToEntityMessage( @Nonnull final Object object,
                                                           final boolean isUpdate,
                                                           final boolean isInitialLoad );

  /**
   * Configure the SubscriptionEntries to reflect an auto graph link between the source and target graph.
   */
  protected void linkSubscriptionEntries( @Nonnull final SubscriptionEntry sourceEntry,
                                          @Nonnull final SubscriptionEntry targetEntry )
  {
    sourceEntry.registerOutwardSubscriptions( targetEntry.address() );
    targetEntry.registerInwardSubscriptions( sourceEntry.address() );
  }

  /**
   * Adds a channel link to the specified set using the given source and target channel IDs and an optional target root ID.
   * The source channel MUST be a type channel.
   * If the target root ID is not null, a new channel link is created and added to the set.
   *
   * @param links           the set to add the created channel link; must not be null
   * @param sourceChannelId the ID of the source channel
   * @param targetChannelId the ID of the target channel
   * @param targetRootId    the root ID associated with the target channel; may be null
   */
  protected void addChannelLink( @Nonnull final Set<ChannelLink> links,
                                 final int sourceChannelId,
                                 final int targetChannelId,
                                 final int targetRootId )
  {
    assert getSchemaMetaData().getChannelMetaData( targetChannelId ).isInstanceGraph();
    links.add( new ChannelLink( new ChannelAddress( sourceChannelId ),
                                new ChannelAddress( targetChannelId, targetRootId ),
                                null ) );
  }

  protected void maybeAddChannelLink( @Nonnull final Set<ChannelLink> links,
                                      final int sourceChannelId,
                                      final int targetChannelId,
                                      @Nullable final Integer targetRootId )
  {
    assert getSchemaMetaData().getChannelMetaData( targetChannelId ).isInstanceGraph();
    if ( null != targetRootId )
    {
      addChannelLink( links, sourceChannelId, targetChannelId, targetRootId );
    }
  }

  /**
   * Adds channel links to the provided set based on the source and target channel information.
   * The source channel MUST be an instance channel.
   * This method retrieves the list of root IDs associated with the source routing key from
   * the provided routing keys map and delegates to another method to add the links.
   *
   * @param routingKeys      a map containing routing keys and their associated serializable data; must not be null
   * @param links            the set to which the created channel links will be added; must not be null
   * @param sourceChannelId  the ID of the source channel
   * @param sourceRoutingKey the routing key for the source channel; must not be null
   * @param targetChannelId  the ID of the target channel
   * @param targetRootId     the root ID associated with the target channel; may be null
   */
  protected void addChannelLinks( @Nonnull Map<String, Serializable> routingKeys,
                                  @Nonnull final Set<ChannelLink> links,
                                  final int sourceChannelId,
                                  @Nonnull final String sourceRoutingKey,
                                  final int targetChannelId,
                                  final int targetRootId )
  {
    assert getSchemaMetaData().getChannelMetaData( targetChannelId ).isInstanceGraph();
    @SuppressWarnings( "unchecked" )
    final var sourceRootIds = (List<Integer>) routingKeys.get( sourceRoutingKey );
    if ( null != sourceRootIds )
    {
      for ( final var sourceRootId : sourceRootIds )
      {
        addChannelLink( links, sourceChannelId, sourceRootId, targetChannelId, targetRootId );
      }
    }
  }

  /**
   * Adds a channel link to the specified set of links if the targetRootId is not null.
   *
   * @param links           the set of channel links to which the new link will be added
   * @param sourceChannelId the ID of the source channel
   * @param sourceRootId    the ID of the source root
   * @param targetChannelId the ID of the target channel
   * @param targetRootId    the ID of the target root, may be null
   */
  protected void addChannelLink( @Nonnull final Set<ChannelLink> links,
                                 final int sourceChannelId,
                                 final int sourceRootId,
                                 final int targetChannelId,
                                 final int targetRootId )
  {
    assert getSchemaMetaData().getChannelMetaData( targetChannelId ).isInstanceGraph();
    assert !getSchemaMetaData().getChannelMetaData( targetChannelId ).requiresFilterInstanceId();
    assert !getSchemaMetaData().getChannelMetaData( targetChannelId ).requiresFilterInstanceId();
    links.add( new ChannelLink( new ChannelAddress( sourceChannelId, sourceRootId ),
                                new ChannelAddress( targetChannelId, targetRootId ),
                                null ) );
  }

  protected void maybeAddChannelLink( @Nonnull final Set<ChannelLink> links,
                                      final int sourceChannelId,
                                      final int sourceRootId,
                                      final int targetChannelId,
                                      @Nullable final Integer targetRootId )
  {
    if ( null != targetRootId )
    {
      addChannelLink( links, sourceChannelId, sourceRootId, targetChannelId, targetRootId );
    }
  }

  @SuppressWarnings( "unchecked" )
  protected void addInstanceRootRouterKey( @Nonnull final Map<String, Serializable> routerKeys,
                                           @Nonnull final String key,
                                           @Nonnull final Integer id )
  {
    ( (List<Integer>) routerKeys.computeIfAbsent( key, v -> new ArrayList<>() ) ).add( id );
  }

  protected int decodeIntAttribute( @Nonnull final ResultSet resultSet,
                                    @Nonnull final Map<String, Serializable> attributeValues,
                                    @Nonnull final String key,
                                    @Nonnull final String columnLabel )
    throws SQLException
  {
    final int value = resultSet.getInt( columnLabel );
    attributeValues.put( key, value );
    return value;
  }

  @Nullable
  protected Integer decodeNullableIntAttribute( @Nonnull final ResultSet resultSet,
                                                @Nonnull final Map<String, Serializable> attributeValues,
                                                @Nonnull final String key,
                                                @Nonnull final String columnLabel )
    throws SQLException
  {
    final var value = (Integer) resultSet.getObject( columnLabel );
    if ( null != value )
    {
      attributeValues.put( key, value );
    }
    return value;
  }

  protected void decodeTimestampAttribute( @Nonnull final ResultSet resultSet,
                                           @Nonnull final Map<String, Serializable> attributeValues,
                                           @Nonnull final String key,
                                           @Nonnull final String columnLabel )
    throws SQLException
  {
    attributeValues.put( key, resultSet.getTimestamp( columnLabel ).getTime() );
  }

  protected void decodeNullableTimestampAttribute( @Nonnull final ResultSet resultSet,
                                                   @Nonnull final Map<String, Serializable> attributeValues,
                                                   @Nonnull final String key,
                                                   @Nonnull final String columnLabel )
    throws SQLException
  {
    final var value = resultSet.getTimestamp( columnLabel );
    if ( null != value )
    {
      attributeValues.put( key, value.getTime() );
    }
  }

  protected void decodeDateAttribute( @Nonnull final ResultSet resultSet,
                                      @Nonnull final Map<String, Serializable> attributeValues,
                                      @Nonnull final String key,
                                      @Nonnull final String columnLabel )
    throws SQLException
  {
    attributeValues.put( key, toDateString( resultSet.getDate( columnLabel ) ) );
  }

  protected void decodeNullableDateAttribute( @Nonnull final ResultSet resultSet,
                                              @Nonnull final Map<String, Serializable> attributeValues,
                                              @Nonnull final String key,
                                              @Nonnull final String columnLabel )
    throws SQLException
  {
    final var value = resultSet.getDate( columnLabel );
    if ( null != value )
    {
      attributeValues.put( key, toDateString( value ) );
    }
  }

  @Nonnull
  protected String toDateString( @Nonnull final Date value )
  {
    return
      new Date( value.getTime() )
        .toInstant()
        .atZone( ZoneId.systemDefault() )
        .toLocalDate()
        .toString();
  }

  protected void decodeStringAttribute( @Nonnull final ResultSet resultSet,
                                        @Nonnull final Map<String, Serializable> attributeValues,
                                        @Nonnull final String key,
                                        @Nonnull final String columnLabel )
    throws SQLException
  {
    attributeValues.put( key, resultSet.getString( columnLabel ) );
  }

  protected void decodeNullableStringAttribute( @Nonnull final ResultSet resultSet,
                                                @Nonnull final Map<String, Serializable> attributeValues,
                                                @Nonnull final String key,
                                                @Nonnull final String columnLabel )
    throws SQLException
  {
    final var value = resultSet.getString( columnLabel );
    if ( null != value )
    {
      attributeValues.put( key, value );
    }
  }

  protected void decodeBooleanAttribute( @Nonnull final ResultSet resultSet,
                                         @Nonnull final Map<String, Serializable> attributeValues,
                                         @Nonnull final String key,
                                         @Nonnull final String columnLabel )
    throws SQLException
  {
    attributeValues.put( key, resultSet.getBoolean( columnLabel ) );
  }

  protected void decodeNullableBooleanAttribute( @Nonnull final ResultSet resultSet,
                                                 @Nonnull final Map<String, Serializable> attributeValues,
                                                 @Nonnull final String key,
                                                 @Nonnull final String columnLabel )
    throws SQLException
  {
    final var value = (Boolean) resultSet.getObject( columnLabel );
    if ( null != value )
    {
      attributeValues.put( key, value );
    }
  }
}
