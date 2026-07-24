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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import javax.json.JsonObject;
import javax.persistence.EntityManager;
import org.intellij.lang.annotations.Language;
import replicant.server.ChannelAddress;
import replicant.server.EntityMessage;
import replicant.server.transport.Packet;
import replicant.server.transport.ReplicantChangeRecorder;
import replicant.server.transport.ReplicantSession;
import replicant.server.transport.ReplicantSessionContext;

/**
 * Base class used to support implementation of SessionContext implementations.
 * Primarily it contains support for customizing bulk loads using SQL.
 */
@SuppressWarnings( { "SqlNoDataSourceInspection", "SameParameterValue" } )
public abstract class AbstractSessionContextImpl
  implements ReplicantChangeRecorder, ReplicantSessionContext
{
  @NonNull
  @Override
  public JsonObject deriveTargetFilter( @NonNull final EntityMessage entityMessage,
                                        @NonNull final ChannelAddress source,
                                        @Nullable final JsonObject sourceFilter,
                                        @NonNull final ChannelAddress target )
  {
    throw new IllegalStateException( "deriveTargetFilter called for link from " + source + " to " + target +
                                     ( null == sourceFilter ? "" : " with source filter " + sourceFilter ) +
                                     " in the context of the entity message " + entityMessage +
                                     " but no such graph link exists or the target graph has no filter parameter" );
  }

  @NonNull
  @Override
  public String deriveTargetFilterInstanceId( @NonNull final EntityMessage entityMessage,
                                              @NonNull final ChannelAddress source,
                                              @Nullable final JsonObject sourceFilter,
                                              @NonNull final ChannelAddress target,
                                              @Nullable final JsonObject targetFilter )
  {
    throw new IllegalStateException( "deriveTargetFilterInstanceId called for link from " + source + " to " + target +
                                     ( null == sourceFilter ? "" : " with source filter " + sourceFilter ) +
                                     ( null == targetFilter ? "" : " with target filter " + targetFilter ) +
                                     " in the context of the entity message " + entityMessage +
                                     " but no such graph link exists or the target graph does not require a filter " +
                                     "instance id" );
  }

  @Override
  public void preSendChangeMessage( @NonNull final ReplicantSession session, @NonNull final Packet packet )
  {
  }

  @NonNull
  protected abstract EntityManager em();

  @NonNull
  protected Connection connection()
  {
    return em().unwrap( Connection.class );
  }

  @Language( "TSQL" )
  protected String generateTempIdTableFromIds( @NonNull final Collection<Integer> idSet )
  {
    //noinspection SqlUnused
    return "DECLARE @Ids TABLE ( Id INTEGER NOT NULL );\n" +
           chunked( idSet.stream(), 900 )
             .map( ids -> "INSERT INTO @Ids VALUES " +
                          ids.stream().map( id -> "(" + id + ")" ).collect( Collectors.joining( "," ) ) )
             .collect( Collectors.joining( "\n" ) ) +
           "\n";
  }

  @Language( "TSQL" )
  protected String generateTempIdTable( @NonNull final Collection<ChannelAddress> addresses )
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
  protected String generateTempIdAndFilterIdTable( @NonNull final Collection<ChannelAddress> addresses )
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

  @NonNull
  @SuppressWarnings( { "SameParameterValue", "DataFlowIssue" } )
  protected <T> Stream<List<T>> chunked( @NonNull final Stream<T> stream, final int chunkSize )
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
  protected abstract EntityMessage convertToEntityMessage( @NonNull final Object object,
                                                           final boolean isUpdate,
                                                           final boolean isInitialLoad );

  @Nullable
  @Override
  public EntityMessage convertToEntityMessage( @NonNull final Object object, final boolean isUpdate )
  {
    return convertToEntityMessage( object, isUpdate, false );
  }

  @SuppressWarnings( "unchecked" )
  protected void addInstanceRootRouterKey( @NonNull final Map<String, Serializable> routerKeys,
                                           @NonNull final String key,
                                           @NonNull final Integer id )
  {
    ( (List<Integer>) routerKeys.computeIfAbsent( key, v -> new ArrayList<>() ) ).add( id );
  }

  protected int decodeIntAttribute( @NonNull final ResultSet resultSet,
                                    @NonNull final Map<String, Serializable> attributeValues,
                                    @NonNull final String key,
                                    @NonNull final String columnLabel )
    throws SQLException
  {
    final var value = resultSet.getInt( columnLabel );
    attributeValues.put( key, value );
    return value;
  }

  @Nullable
  protected Integer decodeNullableIntAttribute( @NonNull final ResultSet resultSet,
                                                @NonNull final Map<String, Serializable> attributeValues,
                                                @NonNull final String key,
                                                @NonNull final String columnLabel )
    throws SQLException
  {
    final var value = (Integer) resultSet.getObject( columnLabel );
    if ( null != value )
    {
      attributeValues.put( key, value );
    }
    return value;
  }

  protected void decodeTimestampAttribute( @NonNull final ResultSet resultSet,
                                           @NonNull final Map<String, Serializable> attributeValues,
                                           @NonNull final String key,
                                           @NonNull final String columnLabel )
    throws SQLException
  {
    attributeValues.put( key, resultSet.getTimestamp( columnLabel ).getTime() );
  }

  protected void decodeNullableTimestampAttribute( @NonNull final ResultSet resultSet,
                                                   @NonNull final Map<String, Serializable> attributeValues,
                                                   @NonNull final String key,
                                                   @NonNull final String columnLabel )
    throws SQLException
  {
    final var value = resultSet.getTimestamp( columnLabel );
    if ( null != value )
    {
      attributeValues.put( key, value.getTime() );
    }
  }

  protected void decodeDateAttribute( @NonNull final ResultSet resultSet,
                                      @NonNull final Map<String, Serializable> attributeValues,
                                      @NonNull final String key,
                                      @NonNull final String columnLabel )
    throws SQLException
  {
    attributeValues.put( key, toDateString( resultSet.getDate( columnLabel ) ) );
  }

  protected void decodeNullableDateAttribute( @NonNull final ResultSet resultSet,
                                              @NonNull final Map<String, Serializable> attributeValues,
                                              @NonNull final String key,
                                              @NonNull final String columnLabel )
    throws SQLException
  {
    final var value = resultSet.getDate( columnLabel );
    if ( null != value )
    {
      attributeValues.put( key, toDateString( value ) );
    }
  }

  @NonNull
  protected String toDateString( @NonNull final Date value )
  {
    return
      new Date( value.getTime() )
        .toInstant()
        .atZone( ZoneId.systemDefault() )
        .toLocalDate()
        .toString();
  }

  protected void decodeStringAttribute( @NonNull final ResultSet resultSet,
                                        @NonNull final Map<String, Serializable> attributeValues,
                                        @NonNull final String key,
                                        @NonNull final String columnLabel )
    throws SQLException
  {
    attributeValues.put( key, resultSet.getString( columnLabel ) );
  }

  protected void decodeNullableStringAttribute( @NonNull final ResultSet resultSet,
                                                @NonNull final Map<String, Serializable> attributeValues,
                                                @NonNull final String key,
                                                @NonNull final String columnLabel )
    throws SQLException
  {
    final var value = resultSet.getString( columnLabel );
    if ( null != value )
    {
      attributeValues.put( key, value );
    }
  }

  protected void decodeBooleanAttribute( @NonNull final ResultSet resultSet,
                                         @NonNull final Map<String, Serializable> attributeValues,
                                         @NonNull final String key,
                                         @NonNull final String columnLabel )
    throws SQLException
  {
    attributeValues.put( key, resultSet.getBoolean( columnLabel ) );
  }

  protected void decodeNullableBooleanAttribute( @NonNull final ResultSet resultSet,
                                                 @NonNull final Map<String, Serializable> attributeValues,
                                                 @NonNull final String key,
                                                 @NonNull final String columnLabel )
    throws SQLException
  {
    final var value = (Boolean) resultSet.getObject( columnLabel );
    if ( null != value )
    {
      attributeValues.put( key, value );
    }
  }
}
