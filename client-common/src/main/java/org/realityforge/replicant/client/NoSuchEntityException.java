package org.realityforge.replicant.client;

@SuppressWarnings( "GwtInconsistentSerializableClass" )
public class NoSuchEntityException
  extends RuntimeException
{
  private final Class _type;
  private final Object _id;

  public NoSuchEntityException( final Class type, final Object id )
  {
    _type = type;
    _id = id;
  }

  public Class getType()
  {
    return _type;
  }

  public Object getID()
  {
    return _id;
  }

  @Override
  public String toString()
  {
    return "NoSuchEntityException[type=" + _type + ", id=" + _id + ']';
  }
}
