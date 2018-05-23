package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

// TODO: This class should become package access once all relevant classes migrated to replicant package
public final class AreaOfInterestRequest
{
  public enum Type
  {
    ADD, REMOVE, UPDATE
  }

  @Nonnull
  private final ChannelAddress _address;
  @Nonnull
  private final Type _type;
  @Nullable
  private final Object _filter;
  private boolean _inProgress;

  AreaOfInterestRequest( @Nonnull final ChannelAddress address,
                         @Nonnull final Type type,
                         @Nullable final Object filter )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> type != Type.REMOVE || null == filter,
                 () -> "Replicant-0027: AreaOfInterestRequest constructor passed a REMOVE " +
                       "request for address '" + address + "' with a non-null filter '" + filter + "'." );
    }
    _address = Objects.requireNonNull( address );
    _type = Objects.requireNonNull( type );
    _filter = filter;
  }

  @Nonnull
  ChannelAddress getAddress()
  {
    return _address;
  }

  @Nonnull
  public Type getType()
  {
    return _type;
  }

  @Nonnull
  String getCacheKey()
  {
    final Integer id = _address.getId();
    return _address.getSystemId() + "." + _address.getChannelId() + ( null != id ? "." + id : "" );
  }

  @Nullable
  Object getFilter()
  {
    return _filter;
  }

  public boolean isInProgress()
  {
    return _inProgress;
  }

  public void markAsInProgress()
  {
    _inProgress = true;
  }

  void markAsComplete()
  {
    _inProgress = false;
  }

  boolean match( @Nonnull final Type action, @Nonnull final ChannelAddress descriptor, @Nullable final Object filter )
  {
    return getType().equals( action ) &&
           getAddress().equals( descriptor ) &&
           ( Type.REMOVE == action || FilterUtil.filtersEqual( filter, getFilter() ) );
  }

  @Override
  public String toString()
  {
    if ( Replicant.areNamesEnabled() )
    {
      final ChannelAddress address = getAddress();
      return "AreaOfInterestRequest[" +
             "Type=" + _type +
             " Address=" + address +
             ( null == _filter ? "" : " Filter=" + FilterUtil.filterToString( _filter ) ) + "]" +
             ( _inProgress ? "(InProgress)" : "" );
    }
    else
    {
      return super.toString();
    }
  }
}
