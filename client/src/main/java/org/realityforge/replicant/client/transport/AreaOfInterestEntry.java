package org.realityforge.replicant.client.transport;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.AreaOfInterestAction;
import replicant.ChannelAddress;
import replicant.FilterUtil;

final class AreaOfInterestEntry
{
  @Nonnull
  private final ChannelAddress _address;
  @Nonnull
  private final AreaOfInterestAction _action;
  @Nullable
  private final Object _filterParameter;
  private boolean _inProgress;

  AreaOfInterestEntry( @Nonnull final ChannelAddress address,
                       @Nonnull final AreaOfInterestAction action,
                       @Nullable final Object filterParameter )
  {
    _address = Objects.requireNonNull( address );
    _action = Objects.requireNonNull( action );
    _filterParameter = filterParameter;
  }

  @Nonnull
  ChannelAddress getAddress()
  {
    return _address;
  }

  @Nonnull
  AreaOfInterestAction getAction()
  {
    return _action;
  }

  @Nonnull
  String getCacheKey()
  {
    return _address.getSystem().getSimpleName() + ":" + getAddress().toString();
  }

  @Nullable
  Object getFilterParameter()
  {
    return _filterParameter;
  }

  boolean isInProgress()
  {
    return _inProgress;
  }

  void markAsInProgress()
  {
    _inProgress = true;
  }

  void markAsComplete()
  {
    _inProgress = false;
  }

  boolean match( @Nonnull final AreaOfInterestAction action,
                 @Nonnull final ChannelAddress descriptor,
                 @Nullable final Object filter )
  {
    return getAction().equals( action ) &&
           getAddress().equals( descriptor ) &&
           ( AreaOfInterestAction.REMOVE == action || FilterUtil.filtersEqual( filter, getFilterParameter() ) );
  }

  @Override
  public String toString()
  {
    final ChannelAddress address = getAddress();
    return "AOI[SystemKey=" + address.getSystem().getSimpleName() +
           ",Channel=" + address +
           ",filter=" + FilterUtil.filterToString( _filterParameter ) +
           "]" + ( _inProgress ? "(InProgress)" : "" );
  }
}
