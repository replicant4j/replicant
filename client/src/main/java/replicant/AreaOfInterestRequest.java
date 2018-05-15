package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// TODO: This class should become package access once all relevant classes migrated to replicant package
public final class AreaOfInterestRequest
{
  @Nonnull
  private final ChannelAddress _address;
  @Nonnull
  private final AreaOfInterestAction _action;
  @Nullable
  private final Object _filter;
  private boolean _inProgress;

  public AreaOfInterestRequest( @Nonnull final ChannelAddress address,
                                @Nonnull final AreaOfInterestAction action,
                                @Nullable final Object filter )
  {
    _address = Objects.requireNonNull( address );
    _action = Objects.requireNonNull( action );
    _filter = filter;
  }

  @Nonnull
  public ChannelAddress getAddress()
  {
    return _address;
  }

  @Nonnull
  public AreaOfInterestAction getAction()
  {
    return _action;
  }

  @Nonnull
  public String getCacheKey()
  {
    final Integer id = _address.getId();
    return _address.getSystem().getSimpleName() + ":" + _address.getChannelType() + ( null != id ? ":" + id : "" );
  }

  @Nullable
  public Object getFilter()
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

  public void markAsComplete()
  {
    _inProgress = false;
  }

  public boolean match( @Nonnull final AreaOfInterestAction action,
                        @Nonnull final ChannelAddress descriptor,
                        @Nullable final Object filter )
  {
    return getAction().equals( action ) &&
           getAddress().equals( descriptor ) &&
           ( AreaOfInterestAction.REMOVE == action || FilterUtil.filtersEqual( filter, getFilter() ) );
  }

  @Override
  public String toString()
  {
    if ( Replicant.areNamesEnabled() )
    {
      final ChannelAddress address = getAddress();
      return "AreaOfInterestRequest[" +
             "Action=" + _action +
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
