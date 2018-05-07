package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * A channel address is immutable reference that defines the channel address.
 * A "type" channel is addressed by the type while an "instance" channel is addressed
 * by the "type" and the instance "id"
 */
public final class ChannelAddress
  implements Comparable<ChannelAddress>
{
  @Nonnull
  private final Enum _channelType;
  @Nullable
  private final Integer _id;

  public ChannelAddress( @Nonnull final Enum channelType )
  {
    this( channelType, null );
  }

  public ChannelAddress( @Nonnull final Enum channelType, @Nullable final Integer id )
  {
    _channelType = Objects.requireNonNull( channelType );
    _id = id;
  }

  @Nonnull
  public Class getSystem()
  {
    return _channelType.getDeclaringClass();
  }

  @Nonnull
  public Enum getChannelType()
  {
    return _channelType;
  }

  @Nullable
  public Integer getId()
  {
    return _id;
  }

  @Override
  public String toString()
  {
    if ( Replicant.areNamesEnabled() )
    {
      return getName();
    }
    else
    {
      return super.toString();
    }
  }

  @Nonnull
  public String getName()
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( Replicant::areNamesEnabled,
                    () -> "Replicant-0054: ChannelAddress.getName() invoked when Replicant.areNamesEnabled() is false" );
    }
    return getSystem().getSimpleName() + "." + _channelType.toString() + ( null != _id ? ":" + _id : "" );
  }

  @Override
  public boolean equals( final Object o )
  {
    if ( this == o )
    {
      return true;
    }
    else if ( o == null || getClass() != o.getClass() )
    {
      return false;
    }
    else
    {
      final ChannelAddress that = (ChannelAddress) o;
      return Objects.equals( _channelType, that._channelType ) && Objects.equals( _id, that._id );
    }
  }

  @Override
  public int hashCode()
  {
    int result = _channelType.hashCode();
    result = 31 * result + ( _id != null ? _id.hashCode() : 0 );
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings( "unchecked" )
  @Override
  public int compareTo( @Nonnull final ChannelAddress o )
  {
    return getChannelType().compareTo( o.getChannelType() );
  }
}
