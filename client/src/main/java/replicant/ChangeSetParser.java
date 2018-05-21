package replicant;

import java.io.StringReader;
import java.util.ArrayList;
import javax.annotation.Nonnull;
import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import org.jetbrains.annotations.Nullable;

/**
 * This is the class responsible for parsing change sets.
 * This class includes a "test" JVM implementation that will be ignored
 * when GWT compilation takes place. It is not yet handle all the varied
 * types in entities nor filters. Not suitable outside tests.
 */
final class ChangeSetParser
  extends AbstractChangeSetParser
{
  @GwtIncompatible
  @Nonnull
  @Override
  ChangeSet parseChangeSet( @Nonnull final String rawJsonData )
  {
    try ( final JsonReader reader = Json.createReader( new StringReader( rawJsonData ) ) )
    {
      final JsonObject object = reader.readObject();
      final int sequence = object.getInt( "last_id" );
      final String requestId =
        object.containsKey( "request_id" ) ? object.getString( "request_id" ) : null;
      final String etag =
        object.containsKey( "etag" ) ? object.getString( "etag" ) : null;
      final ChannelChange[] channelChanges = parseChannelChanges( object );
      final EntityChange[] entityChanges = parseEntityChanges( object );

      return ChangeSet.create( sequence, requestId, etag, channelChanges, entityChanges );
    }
  }

  @GwtIncompatible
  @Nullable
  private ChannelChange[] parseChannelChanges( @Nonnull final JsonObject object )
  {
    if ( object.containsKey( "channel_actions" ) )
    {
      final ArrayList<ChannelChange> changes = new ArrayList<>();
      for ( final JsonValue value : object.getJsonArray( "channel_actions" ) )
      {
        final JsonObject change = (JsonObject) value;
        final int cid = change.getInt( "cid" );
        final Integer scid = change.containsKey( "scid" ) ? change.getInt( "scid" ) : null;
        final ChannelChange.Action action =
          ChannelChange.Action.valueOf( change.getString( "action" ).toUpperCase() );

        // TODO: Filters not yet supported properly
        final Object filter = change.getOrDefault( "filter", null );

        changes.add( null == scid ?
                     ChannelChange.create( cid, action, filter ) :
                     ChannelChange.create( cid, scid, action, filter ) );
      }
      return changes.toArray( new ChannelChange[ 0 ] );
    }
    else
    {
      return null;
    }
  }

  @GwtIncompatible
  @Nullable
  private EntityChange[] parseEntityChanges( @Nonnull final JsonObject object )
  {
    if ( object.containsKey( "changes" ) )
    {
      final ArrayList<EntityChange> changes = new ArrayList<>();
      for ( final JsonValue value : object.getJsonArray( "changes" ) )
      {
        final JsonObject change = (JsonObject) value;

        final int id = change.getInt( "id" );
        final int typeId = change.getInt( "type" );

        final EntityChangeDataImpl changeData;
        if ( change.containsKey( "data" ) )
        {
          changeData = new EntityChangeDataImpl();
          final JsonObject data = change.getJsonObject( "data" );
          for ( final String key : data.keySet() )
          {
            final JsonValue v = data.get( key );
            final JsonValue.ValueType valueType = v.getValueType();
            if ( JsonValue.ValueType.NULL == valueType )
            {
              changeData.getData().put( key, null );
            }
            else if ( JsonValue.ValueType.FALSE == valueType )
            {
              changeData.getData().put( key, false );
            }
            else if ( JsonValue.ValueType.TRUE == valueType )
            {
              changeData.getData().put( key, true );
            }
            else if ( JsonValue.ValueType.NUMBER == valueType )
            {
              //TODO: Handle real/float values
              changeData.getData().put( key, ( (JsonNumber) v ).intValue() );
            }
            else
            {
              //TODO: Handle all the other types valid here
              assert JsonValue.ValueType.STRING == valueType;
              changeData.getData().put( key, ( (JsonString) v ).getString() );
            }
          }
        }
        else
        {
          changeData = null;
        }

        final ArrayList<EntityChannel> entityChannels = new ArrayList<>();
        for ( final JsonValue channelReference : change.getJsonArray( "channels" ) )
        {
          final JsonObject channel = (JsonObject) channelReference;
          final int cid = channel.getInt( "cid" );
          final Integer scid = change.containsKey( "scid" ) ? change.getInt( "scid" ) : null;
          entityChannels.add( null == scid ? EntityChannel.create( cid ) : EntityChannel.create( cid, scid ) );
        }

        final EntityChannel[] channels = entityChannels.toArray( new EntityChannel[ 0 ] );
        changes.add( null == changeData ?
                     EntityChange.create( id, typeId, channels ) :
                     EntityChange.create( id, typeId, channels, changeData ) );
      }
      return changes.toArray( new EntityChange[ 0 ] );
    }
    else
    {
      return null;
    }
  }
}
