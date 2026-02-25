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

  @Language( "TSQL" )
  protected String generateTempIdAndFilterIdTable( @Nonnull final List<ChannelAddress> addresses )
  {
    //noinspection SqlUnused
    return
      "DECLARE @IdAndFilterIds TABLE ( Id INTEGER NOT NULL, FilterInstanceId VARCHAR(255) NOT NULL );\n" +
      chunked( addresses.stream(), 900 )
        .map( chunk ->
                "INSERT INTO @IdAndFilterIds VALUES " +
                chunk.
                  stream()
                  .map( address -> "(" + address.rootId() + ",'" + address.filterInstanceId() + "')" )
                  .collect( Collectors.joining( "," ) ) ).
        collect( Collectors.joining( "\n" ) ) +
      "\n";
  }

  @Nonnull
  @SuppressWarnings( { "SameParameterValue", "DataFlowIssue" } )
  <T> Stream<List<T>> chunked( @Nonnull final Stream<T> stream, final int chunkSize )
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
