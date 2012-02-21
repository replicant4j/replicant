package org.realityforge.replicant.client;

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
  public String toString()
  {
    return "Change:" + System.identityHashCode( this );
  }
}
