package org.realityforge.replicant.client.transport;

import java.util.Date;
import javax.annotation.Nonnull;
import org.realityforge.guiceyloops.shared.ValueUtil;

final class TestChange
  implements Change
{
  private final boolean _isUpdate;

  TestChange( final boolean update )
  {
    _isUpdate = update;
  }

  @Override
  public int getId()
  {
    return 0;
  }

  @Override
  public int getTypeId()
  {
    return 0;
  }

  @Override
  public boolean isUpdate()
  {
    return _isUpdate;
  }

  @Override
  public boolean containsKey( @Nonnull final String key )
  {
    return false;
  }

  @Override
  public boolean isNull( @Nonnull final String key )
  {
    return false;
  }

  @Override
  public int getIntegerValue( @Nonnull final String key )
  {
    return 0;
  }

  @Nonnull
  @Override
  public Date getDateValue( @Nonnull final String key )
  {
    return new Date();
  }

  @Nonnull
  @Override
  public String getStringValue( @Nonnull final String key )
  {
    return ValueUtil.randomString();
  }

  @Override
  public boolean getBooleanValue( @Nonnull final String key )
  {
    return false;
  }

  @Override
  public int getChannelCount()
  {
    return 0;
  }

  @Override
  public int getChannelId( final int index )
  {
    return 0;
  }

  @Override
  public Integer getSubChannelId( final int index )
  {
    return null;
  }

  @Override
  public String toString()
  {
    return "Change:" + System.identityHashCode( this );
  }
}
