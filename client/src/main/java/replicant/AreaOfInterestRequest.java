package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

final class AreaOfInterestRequest
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
  private int _requestId;

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
    _requestId = -1;
  }

  @Nonnull
  ChannelAddress getAddress()
  {
    return _address;
  }

  @Nonnull
  Type getType()
  {
    return _type;
  }

  @Nullable
  Object getFilter()
  {
    return _filter;
  }

  boolean isInProgress()
  {
    return -1 != _requestId;
  }

  int getRequestId()
  {
    return _requestId;
  }

  void markAsInProgress( final int requestId )
  {
    _requestId = requestId;
  }

  void markAsComplete()
  {
    _requestId = -1;
  }

  boolean match( @Nonnull final Type action, @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
    return getType().equals( action ) &&
           getAddress().equals( address ) &&
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
             ( -1 != _requestId ? "(InProgress)" : "" );
    }
    else
    {
      return super.toString();
    }
  }
}
