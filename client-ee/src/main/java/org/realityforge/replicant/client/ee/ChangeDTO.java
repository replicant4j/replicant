package org.realityforge.replicant.client.ee;

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.Nonnull;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import org.realityforge.replicant.client.Change;
import org.realityforge.replicant.shared.json.TransportConstants;

public class ChangeDTO
  implements Change
{
  private final JsonObject _object;

  public ChangeDTO( @Nonnull final JsonObject object )
  {
    _object = object;
  }

  @Override
  public int getDesignatorAsInt()
  {
    return _object.getInt( TransportConstants.ENTITY_ID );
  }

  @Override
  public String getDesignatorAsString()
  {
    return _object.getString( TransportConstants.ENTITY_ID );
  }

  @Override
  public int getTypeID()
  {
    return _object.getInt( TransportConstants.TYPE_ID );
  }

  @Override
  public boolean isUpdate()
  {
    return _object.containsKey( TransportConstants.DATA );
  }

  @Override
  public boolean containsKey( @Nonnull final String key )
  {
    return _object.containsKey( TransportConstants.DATA ) &&
           _object.getJsonObject( TransportConstants.DATA ).containsKey( key );
  }

  @Override
  public boolean isNull( @Nonnull final String key )
  {
    return !_object.containsKey( TransportConstants.DATA ) ||
           _object.getJsonObject( TransportConstants.DATA ).isNull( key );
  }

  @Override
  public int getIntegerValue( @Nonnull final String key )
  {
    return getData().getInt( key );
  }

  @Override
  @Nonnull
  public Date getDateValue( @Nonnull final String key )
  {
    final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );
    try
    {
      return dateFormat.parse( getStringValue( key ) );
    }
    catch ( final Exception e )
    {
      throw new IllegalStateException( e );
    }
  }

  @Override
  @Nonnull
  public String getStringValue( @Nonnull final String key )
  {
    return getData().getString( key );
  }

  @Override
  public boolean getBooleanValue( @Nonnull final String key )
  {
    return getData().getBoolean( key );
  }

  @Override
  public int getChannelCount()
  {
    return _object.getJsonArray( TransportConstants.CHANNELS ).size();
  }

  @Override
  public int getChannelID( final int index )
  {
    return _object.getJsonArray( TransportConstants.CHANNELS ).
      getJsonObject( index ).
      getInt( TransportConstants.CHANNEL_ID );
  }

  @Override
  public Object getSubChannelID( final int index )
  {
    final JsonObject channel = _object.getJsonArray( TransportConstants.CHANNELS ).getJsonObject( index );
    if ( !channel.containsKey( TransportConstants.SUBCHANNEL_ID ) ||
         channel.isNull( TransportConstants.SUBCHANNEL_ID ) )
    {
      return null;
    }
    else
    {
      final JsonValue value = channel.get( TransportConstants.SUBCHANNEL_ID );
      return value.getValueType() == JsonValue.ValueType.NUMBER ?
             ( (JsonNumber) value ).intValue() :
             value.getValueType() == JsonValue.ValueType.STRING ?
             ( (JsonString) value ).getString() :
             null;
    }
  }

  @Nonnull
  private JsonObject getData()
  {
    assert !_object.isNull( TransportConstants.DATA );
    return _object.getJsonObject( TransportConstants.DATA );
  }
}
