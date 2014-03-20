package org.realityforge.replicant.client.transport;

import java.util.Date;
import org.realityforge.replicant.client.Change;

final class TestChange
  implements Change
{
  private final boolean _isUpdate;

  TestChange( final boolean update )
  {
    _isUpdate = update;
  }

  @Override
  public int getDesignatorAsInt()
  {
    return 0;
  }

  @Override
  public String getDesignatorAsString()
  {
    return null;
  }

  @Override
  public int getTypeID()
  {
    return 0;
  }

  @Override
  public boolean isUpdate()
  {
    return _isUpdate;
  }

  @Override
  public boolean containsKey( final String key )
  {
    return false;
  }

  @Override
  public boolean isNull( final String key )
  {
    return false;
  }

  @Override
  public int getIntegerValue( final String key )
  {
    return 0;
  }

  @Override
  public Date getDateValue( final String key )
  {
    return new Date();
  }

  @Override
  public String getStringValue( final String key )
  {
    return null;
  }

  @Override
  public boolean getBooleanValue( final String key )
  {
    return false;
  }

  @Override
  public int getChannelCount()
  {
    return 0;
  }

  @Override
  public int getChannelID( final int index )
  {
    return 0;
  }

  @Override
  public int getSubChannelIDAsInt( final int index )
  {
    return 0;
  }

  @Override
  public String getSubChannelIDAsString( final int index )
  {
    return null;
  }

  @Override
  public String toString()
  {
    return "Change:" + System.identityHashCode( this );
  }
}
